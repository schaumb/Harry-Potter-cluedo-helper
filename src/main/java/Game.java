import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Game implements IGame {
    private final ICommander commander;
    private final Printer printer;
    private final List<String> allName;

    private final HashMap<String, List<ICommander.ICommand>> mapOfCommands = new HashMap<>();
    private final List<ICommander.ICommand> allCommand = new ArrayList<>();
    private final List<ICommander.ICommand> declinedFinals = new ArrayList<>();

    private final EnumMap<ICommander.Item, ArrayList<String>> canContainsItem = new EnumMap<>(ICommander.Item.class);
    private final EnumMap<ICommander.Location, ArrayList<String>> canContainsLocation = new EnumMap<>(ICommander.Location.class);
    private final EnumMap<ICommander.Suspect, ArrayList<String>> canContainsSuspect = new EnumMap<>(ICommander.Suspect.class);
    private final EnumMap<ICommander.TypesType, HashMap<String, Integer>> canContainsCount = new EnumMap<>(ICommander.TypesType.class);
    private final EnumMap<ICommander.TypesType, EnumMap<? extends Enum<?>, ArrayList<String>>> canContainsMaps =
            new EnumMap<ICommander.TypesType, EnumMap<? extends Enum<?>, ArrayList<String>>>(ICommander.TypesType.class) {{


        put(ICommander.TypesType.ITEM, canContainsItem);
        put(ICommander.TypesType.LOCATION, canContainsLocation);
        put(ICommander.TypesType.SUSPECT, canContainsSuspect);
    }};

    private boolean end = false;

    public Game(ICommander commander, Function<IGame, Printer> printerCreator) {
        this.commander = commander;
        this.commander.init();

        this.printer = printerCreator.apply(this);
        this.allName = new ArrayList<>(commander.getNames());
        for (ICommander.SpecPlayers specPlayers : ICommander.SpecPlayers.values()) {
            this.allName.add(specPlayers.toString());
        }

        fillWithData(canContainsItem, ICommander.Item.values(), unused -> new ArrayList<>(allName));
        fillWithData(canContainsLocation, ICommander.Location.values(), unused -> new ArrayList<>(allName));
        fillWithData(canContainsSuspect, ICommander.Suspect.values(), unused -> new ArrayList<>(allName));
        fillWithData(canContainsCount, ICommander.TypesType.values(), value -> {
            final HashMap<String, Integer> canContainsMap = new HashMap<>();
            final int size = value.getEnumLength();
            final List<String> names = commander.getNames();
            final int players = names.size();
            final int containsPerPlayer = size / players;

            canContainsMap.put("___player", containsPerPlayer);
            for (String name : names) {
                canContainsMap.put(name, containsPerPlayer);
            }

            canContainsMap.put(ICommander.SpecPlayers.Result.toString(), 1);

            String dumbledoreName = ICommander.SpecPlayers.Dumbledore.toString();
            int dumbledoreGot = size - containsPerPlayer * players - 1;
            canContainsMap.put(dumbledoreName, dumbledoreGot);
            if (dumbledoreGot == 0) {
                for (ArrayList<String> strings : canContainsMaps.get(value).values()) {
                    strings.remove(dumbledoreName);
                }
            }

            return canContainsMap;
        });

    }

    private <T extends Enum<T>, U> void fillWithData(EnumMap<T, U> canContainsItem, T[] values, Function<T, U> supplier) {
        for (T value : values) {
            canContainsItem.put(value, supplier.apply(value));
        }
    }

    public void start() {
        while(!isEnd()) {
            printer.print();

            final ICommander.ICommand nextCommand = commander.getNextCommand();
            allCommand.add(nextCommand);

            switch (nextCommand.getCommandType()) {
                case Guess:
                    if (nextCommand.getShowerPlayer() == null) {
                        for (String who : commander.getNames()) {
                            if (who.equals(nextCommand.getGuessPlayer()))
                                continue;

                            decline(who, nextCommand.getItem());
                            decline(who, nextCommand.getLocation());
                            decline(who, nextCommand.getSuspect());
                        }
                    } else {
                        final List<String> names = commander.getNames();
                        final int size = names.size();
                        final int from = names.indexOf(nextCommand.getGuessPlayer());
                        final int to = names.indexOf(nextCommand.getShowerPlayer());

                        for (int i = (from + 1) % size; i != to; i = (i + 1) % size) {
                            decline(names.get(i), nextCommand.getItem());
                            decline(names.get(i), nextCommand.getLocation());
                            decline(names.get(i), nextCommand.getSuspect());
                        }

                        mapOfCommands.computeIfAbsent(nextCommand.getShowerPlayer(), unused -> new ArrayList<>())
                                .add(nextCommand);
                    }

                    break;
                case Show:
                    if (nextCommand.getItem() != null) {
                        assign(nextCommand.getShowerPlayer(), nextCommand.getItem());
                    }
                    if (nextCommand.getLocation() != null) {
                        assign(nextCommand.getShowerPlayer(), nextCommand.getLocation());
                    }
                    if (nextCommand.getSuspect() != null) {
                        assign(nextCommand.getShowerPlayer(), nextCommand.getSuspect());
                    }
                    break;
                case Final_Guess:
                    declinedFinals.add(nextCommand);
                    break;
                case End:
                    end = true;
                    break;
            }
            tryGuess();
        }
    }

    public <T extends Enum<T>> Boolean isAssignedTo(String who, Enum<T> item) {
        return isAssignedTo(who, canContainsMaps.get(ICommander.TypesType.getFromClass(item.getClass())).get(item));
    }

    @Override
    public List<String> getAllName() {
        return allName;
    }

    @Override
    public Set<ICommander.ICommand> getUnusedCommands() {
        return mapOfCommands.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    @Override
    public List<ICommander.ICommand> getAllCommand() {
        return allCommand;
    }

    public <T extends Enum<T>> boolean canAssign(String who, T item) {
        final ICommander.TypesType typesType = ICommander.TypesType.getFromClass(item.getClass());
        final Map<? extends Enum<?>, ArrayList<String>> arrayListMap = canContainsMaps.get(typesType);
        return canAssign(
                isAssignedTo(who, arrayListMap.get(item)),
                getAssignedTypesCount(who, arrayListMap, Boolean.TRUE),
                canContainsCount.get(typesType).get(who));
    }

    private int getAssignedTypesCount(String who, Map<? extends Enum<?>, ArrayList<String>> arrayListMap, Boolean type) {
        int current = 0;
        for (Map.Entry<? extends Enum<?>, ArrayList<String>> arrayListEntry : arrayListMap.entrySet()) {
            if (isAssignedTo(who, arrayListEntry.getValue()) == type) {
                ++current;
            }
        }
        return current;
    }

    private Boolean isAssignedTo(String who, ArrayList<String> where) {
        return where.contains(who) ? where.size() == 1 ? Boolean.TRUE : null : Boolean.FALSE;
    }

    private boolean canAssign(Boolean isAssignedTo,
                                                  int assignedCount,
                                                  int containsCount) {
        return isAssignedTo != null ? isAssignedTo : assignedCount < containsCount;
    }



    private <T extends Enum<T>> void assign(String who, Enum<T> item) {
        final ICommander.TypesType typesType = ICommander.TypesType.getFromClass(item.getClass());
        final Map<? extends Enum<?>, ArrayList<String>> arrayListMap = canContainsMaps.get(typesType);
        final ArrayList<String> list = arrayListMap.get(item);
        final Boolean assignedTo = isAssignedTo(who, list);

        if (assignedTo == Boolean.TRUE)
            return;

        final int assignedTypesCount = getAssignedTypesCount(who, arrayListMap, Boolean.TRUE);
        final int containsCount = canContainsCount.get(typesType).get(who);

        if (!canAssign(assignedTo, assignedTypesCount, containsCount))
            throw new RuntimeException("Cannot assign " + who + " to item " + item);

        assign(who, arrayListMap, list, assignedTypesCount, containsCount);
    }

    private <T extends Enum<T>> void decline(String who, T item) {
        final ICommander.TypesType typesType = ICommander.TypesType.getFromClass(item.getClass());
        final Map<? extends Enum<?>, ArrayList<String>> arrayListMap = canContainsMaps.get(typesType);
        final ArrayList<String> list = arrayListMap.get(item);
        final Boolean assignedTo = isAssignedTo(who, list);

        if (assignedTo == Boolean.FALSE)
            return;
        if (assignedTo == Boolean.TRUE)
            throw new RuntimeException("Cannot decline " + who + " to item " + item);

        final int assignedCount = getAssignedTypesCount(who, arrayListMap, Boolean.TRUE);
        final int declinedCount = getAssignedTypesCount(who, arrayListMap, Boolean.FALSE);
        final int maxCount = typesType.getEnumLength();
        final int containsCount = canContainsCount.get(typesType).get(who);

        decline(who, arrayListMap, list, maxCount, declinedCount, assignedCount, containsCount);
    }

    private void assign(String who,
                        Map<? extends Enum<?>, ArrayList<String>> arrayListMap,
                        ArrayList<String> list,
                        int assignedTypesCount,
                        int containsCount) {
        list.clear();
        list.add(who);

        if (assignedTypesCount + 1 == containsCount) {
            for (Map.Entry<? extends Enum<?>, ArrayList<String>> arrayListEntry : arrayListMap.entrySet()) {
                if (isAssignedTo(who, arrayListEntry.getValue()) == null) {
                    arrayListEntry.getValue().remove(who);
                }
            }
        }
    }

    private void decline(String who,
                         Map<? extends Enum<?>, ArrayList<String>> arrayListMap,
                         ArrayList<String> list,
                         int maxCount,
                         int declinedCount,
                         int assignedCount,
                         int containsCount) {
        list.remove(who);

        if (maxCount - declinedCount - 1 == containsCount && assignedCount != containsCount) {
            for (Map.Entry<? extends Enum<?>, ArrayList<String>> arrayListEntry : arrayListMap.entrySet()) {
                if (isAssignedTo(who, arrayListEntry.getValue()) == null) {
                    assign(who, arrayListMap, arrayListEntry.getValue(), assignedCount, containsCount);
                }
            }
        }
    }


    private void tryGuess() {
        boolean wasChange;

        do {
            wasChange = false;

            for (String who : getAllName()) {
                for (ICommander.TypesType typesType : ICommander.TypesType.values()) {
                    final Map<? extends Enum<?>, ArrayList<String>> arrayListMap = canContainsMaps.get(typesType);

                    int assignedCount = getAssignedTypesCount(who, arrayListMap, Boolean.TRUE);
                    int declinedCount = getAssignedTypesCount(who, arrayListMap, Boolean.FALSE);
                    final int maxCount = typesType.getEnumLength();
                    final int containsCount = canContainsCount.get(typesType).get(who);

                    if (assignedCount + declinedCount < maxCount) {
                        if (maxCount - declinedCount == containsCount) {
                            wasChange = true;
                            for (ArrayList<String> list : arrayListMap.values()) {
                                if (isAssignedTo(who, list) == null) {
                                    assign(who, arrayListMap, list, assignedCount++, containsCount);
                                }
                            }
                        }
                        if (assignedCount == containsCount) {
                            wasChange = true;
                            for (ArrayList<String> list : arrayListMap.values()) {
                                if (isAssignedTo(who, list) == null) {
                                    decline(who, arrayListMap, list, maxCount, declinedCount++, assignedCount, containsCount);
                                }
                            }
                        }
                    }
                }
            }

            if (wasChange)
                continue;

            for (String who : commander.getNames()) {
                final ArrayList<HashMap<Class<? extends Enum<?>>, Enum<?>>> conjunctions = getConjunctions(who);

                if (conjunctions.removeIf(map -> {
                    if (map.size() == 1) {
                        assign(who, map.values().iterator().next());
                        return true;
                    }
                    return false;
                })) {
                    wasChange = true;
                }


                for (Map.Entry<Class<? extends Enum<?>>, HashSet<Enum<?>>> possibilitiesEntry :
                        getPossibilitiesFromDisjunctions(new MakeDisjunctionFromConjunctions(
                                conjunctions).getDisjunctions()).entrySet()) {
                    final HashSet<Enum<?>> hashSet = possibilitiesEntry.getValue();
                    if (hashSet.contains(null)) // it means some possibility not contained this variable
                        continue;
                    final ICommander.TypesType typesType = ICommander.TypesType.getFromClass(possibilitiesEntry.getKey());
                    final Map<? extends Enum<?>, ArrayList<String>> arrayListMap = canContainsMaps.get(typesType);

                    int assignedCount = getAssignedTypesCount(who, arrayListMap, Boolean.TRUE);
                    int declinedCount = getAssignedTypesCount(who, arrayListMap, Boolean.FALSE);
                    final int maxCount = typesType.getEnumLength();
                    final int containsCount = canContainsCount.get(typesType).get(who);

                    if (hashSet.size() == containsCount) {
                        for (Enum<?> anEnum : hashSet) {
                            final ArrayList<String> list = arrayListMap.get(anEnum);
                            if (isAssignedTo(who, list) == null) {
                                assign(who, arrayListMap, list, assignedCount++, containsCount);
                            }
                        }
                        wasChange = true;
                    } else if (hashSet.size() > containsCount) {
                        for (Map.Entry<? extends Enum<?>, ArrayList<String>> arrayListEntry : arrayListMap.entrySet()) {
                            final ArrayList<String> list = arrayListEntry.getValue();
                            if (!hashSet.contains(arrayListEntry.getKey()) && isAssignedTo(who, list) == null) {
                                decline(who, arrayListMap, list, maxCount, declinedCount++,
                                        assignedCount, containsCount);
                                wasChange = true;
                            }
                        }
                    } else
                        throw new RuntimeException("Possibilities cannot make this");
                }
            }
        } while (wasChange);
    }

    private ArrayList<HashMap<Class<? extends Enum<?>>, Enum<?>>> getConjunctions(String who) {
        final List<ICommander.ICommand> commands = mapOfCommands.getOrDefault(who, new ArrayList<>());
        final Iterator<ICommander.ICommand> iterator = commands.iterator();
        final ArrayList<HashMap<Class<? extends Enum<?>>, Enum<?>>> conjunctions = new ArrayList<>();
        while (iterator.hasNext()) {
            final ICommander.ICommand command = iterator.next();
            Boolean itemAssigned;
            Boolean locationAssigned;
            Boolean suspectAssigned;
            if ((itemAssigned = isAssignedTo(who, command.getItem())) == Boolean.TRUE ||
                    (locationAssigned = isAssignedTo(who, command.getLocation())) == Boolean.TRUE ||
                    (suspectAssigned = isAssignedTo(who, command.getSuspect())) == Boolean.TRUE) {
                iterator.remove();
                continue;
            }

            HashMap<Class<? extends Enum<?>>, Enum<?>> possibles = new HashMap<>();

            if (itemAssigned == null)
                possibles.put(ICommander.Item.class, command.getItem());
            if (locationAssigned == null)
                possibles.put(ICommander.Location.class, command.getLocation());
            if (suspectAssigned == null)
                possibles.put(ICommander.Suspect.class, command.getSuspect());

            conjunctions.add(possibles);
        }
        return conjunctions;
    }

    private class MakeDisjunctionFromConjunctions {
        private int i = 0;
        private final HashMap<Class<? extends Enum<?>>, List<Enum<?>>> currentState = new HashMap<>();
        private final ArrayList<HashMap<Class<? extends Enum<?>>, Enum<?>>> conjunctions;
        private final ArrayList<HashMap<Class<? extends Enum<?>>, List<Enum<?>>>> disjunctions = new ArrayList<>();


        private MakeDisjunctionFromConjunctions(ArrayList<HashMap<Class<? extends Enum<?>>, Enum<?>>> conjunctions) {
            this.conjunctions = conjunctions;
        }

        private void doIt() {
            if (conjunctions.size() == i) {
                disjunctions.add(new HashMap<>(currentState));
                return;
            }

            final Set<Map.Entry<Class<? extends Enum<?>>, Enum<?>>> entries = conjunctions.get(i).entrySet();

            ++i;
            for (Map.Entry<Class<? extends Enum<?>>, Enum<?>> classEnumEntry : entries) {
                final List<Enum<?>> anEnums = currentState.computeIfAbsent(classEnumEntry.getKey(), unused -> new ArrayList<>());

                final ICommander.TypesType fromClass = ICommander.TypesType.getFromClass(classEnumEntry.getKey());
                final Integer maxCanContains = canContainsCount.get(fromClass).get("___player");

                boolean needToAdd = !anEnums.contains(classEnumEntry.getValue());
                if (anEnums.size() == maxCanContains && needToAdd)
                    continue;

                if (needToAdd) {
                    anEnums.add(classEnumEntry.getValue());
                }

                doIt();

                if (needToAdd) {
                    anEnums.remove(classEnumEntry.getValue());

                    if (anEnums.isEmpty()) {
                        currentState.remove(classEnumEntry.getKey());
                    }
                }
            }
            --i;
        }

        ArrayList<HashMap<Class<? extends Enum<?>>, List<Enum<?>>>> getDisjunctions() {
            if (disjunctions.isEmpty()) {
                doIt();
            }
            return disjunctions;
        }
    }

    private HashMap<Class<? extends Enum<?>>, HashSet<Enum<?>>> getPossibilitiesFromDisjunctions(
            ArrayList<HashMap<Class<? extends Enum<?>>, List<Enum<?>>>> disjunctions) {
        HashMap<Class<? extends Enum<?>>, HashSet<Enum<?>>> possibilities = new HashMap<>();
        if (disjunctions.size() > 1) {
            for (HashMap<Class<? extends Enum<?>>, List<Enum<?>>> disjunction : disjunctions) {
                if (possibilities.size() == 0) {
                    for (Map.Entry<Class<? extends Enum<?>>, List<Enum<?>>> classEnumEntry : disjunction.entrySet()) {
                        possibilities.computeIfAbsent(classEnumEntry.getKey(), unused -> new HashSet<>())
                                .addAll(classEnumEntry.getValue());
                    }
                } else {
                    for (Map.Entry<Class<? extends Enum<?>>, List<Enum<?>>> classEnumEntry : disjunction.entrySet()) {
                        final HashSet<Enum<?>> enums = possibilities.computeIfAbsent(classEnumEntry.getKey(), unused -> {
                            final HashSet<Enum<?>> objects = new HashSet<>();
                            objects.add(null);
                            return objects;
                        });
                        enums.addAll(classEnumEntry.getValue());

                        final ICommander.TypesType fromClass = ICommander.TypesType.getFromClass(classEnumEntry.getKey());
                        final Integer maxCanContains = canContainsCount.get(fromClass).get("___player");

                        if (classEnumEntry.getValue().size() < maxCanContains) {
                            enums.add(null);
                        }
                    }
                    for (Map.Entry<Class<? extends Enum<?>>, HashSet<Enum<?>>> classHashSetEntry : possibilities.entrySet()) {
                        if (!disjunction.containsKey(classHashSetEntry.getKey())) {
                            classHashSetEntry.getValue().add(null);
                        }
                    }
                }
            }
        }
        return possibilities;
    }


    private boolean isEnd() {
        return end;
    }
}
