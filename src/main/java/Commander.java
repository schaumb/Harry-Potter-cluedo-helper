import java.util.*;

public class Commander implements ICommander {
    private final Scanner scanner = new Scanner(System.in);
    private final List<String> names = new ArrayList<>();
    private String[] namesArray;

    private final Queue<ICommand> commandQueue = new ArrayDeque<>();

    @Override
    public List<String> getNames() {
        return names;
    }

    @Override
    public void init() {
        System.err.println("How many player plays?");
        final int n = scanner.nextInt();

        for (int i = 0; i < n; ++i) {
            System.err.println("Get " + i + ". player name: ");
            String name = scanner.nextLine();
            while (name.trim().isEmpty()) {
                name = scanner.nextLine();
            }
            names.add(name);
        }
        namesArray = names.toArray(new String[names.size()+2]);

        namesArray[namesArray.length - 2] = SpecPlayers.Dumbledore.toString();
        namesArray[namesArray.length - 1] = SpecPlayers.Result.toString();
    }

    <T> T findWithPrefix(T[] elem, String from, boolean throwIfNotFound) {
        from = from.toLowerCase();

        T res = null;
        for (T t : elem) {
            if (t != null && t.toString().toLowerCase().startsWith(from)) {
                if (res != null) {
                    throw new RuntimeException("Multiple find element: " + from + ". Found " + res + ", and " + t);
                } else {
                    res = t;
                }
            }
        }

        if (throwIfNotFound && res == null) {
            throw new RuntimeException("Not recognized element: " + from + " possibles: " + Arrays.toString(elem));
        }

        return res;
    }

    private CommandImpl createCommandFromLine(String line, CommandType type) {
        final String[] split = line.split("[ ]+");

        String showerPlayer = findWithPrefix(namesArray, split[0], false);

        Item item = findWithPrefix(Item.values(), split[1], false);
        Suspect suspect = findWithPrefix(Suspect.values(), split[2], false);
        Location location = findWithPrefix(Location.values(), split[3], false);

        String guessPlayer = split.length > 4 ? findWithPrefix(namesArray, split[4], true) : null;


        return new CommandImpl(type, guessPlayer, item, suspect, location, showerPlayer);
    }

    @Override
    public ICommand getNextCommand() {
        while (commandQueue.isEmpty()) {
            System.err.println("Please write guess: <shower_name / null / dumbledore / result> <item> <suspect> <location> [<guess player>]");
            String line = scanner.nextLine();

            try {
                final CommandImpl commandFromLine = createCommandFromLine(line, CommandType.Guess);
                if (Objects.equals(commandFromLine.getGuessPlayer(), commandFromLine.getShowerPlayer())) {
                    throw new RuntimeException("Cannot have the same guess and shower player: " + commandFromLine.getGuessPlayer());
                }
                if (commandFromLine.getGuessPlayer() == null) {
                    commandFromLine.setCommandType(CommandType.Show);
                    if (commandFromLine.getItem() == null && commandFromLine.getSuspect() == null && commandFromLine.getLocation() == null) {
                        throw new RuntimeException("Nothing to show");
                    }
                }
                else if (
                        // !Objects.equals(commandFromLine.getShowerPlayer(), SpecPlayers.Dumbledore.toString()) &&
                        (commandFromLine.getItem() == null || commandFromLine.getSuspect() == null || commandFromLine.getLocation() == null)) {
                    throw new RuntimeException(String.format("Missing data: %.5s/%.5s/%.5s", commandFromLine.getItem(), commandFromLine.getSuspect(), commandFromLine.getLocation()));
                }
                if (Objects.equals(commandFromLine.getShowerPlayer(), SpecPlayers.Result.toString())) {
                    commandFromLine.setCommandType(CommandType.Final_Guess);
                }
                commandQueue.add(commandFromLine);
            } catch (RuntimeException e) {
                e.printStackTrace(System.err);
                System.err.println(e.toString());
            }
        }
        return commandQueue.remove();
    }


    public static class CommandImpl implements ICommand {
        private CommandType commandType;
        private final String guessPlayer;
        private final Item item;
        private final Suspect suspect;
        private final Location location;
        private final String showerPlayer;

        public CommandImpl(CommandType commandType, String guessPlayer, Item item, Suspect suspect, Location location, String showerPlayer) {
            this.commandType = commandType;
            this.guessPlayer = guessPlayer;
            this.item = item;
            this.suspect = suspect;
            this.location = location;
            this.showerPlayer = showerPlayer;
        }

        @Override
        public CommandType getCommandType() {
            return commandType;
        }

        public CommandImpl setCommandType(CommandType commandType) {
            this.commandType = commandType;
            return this;
        }

        @Override
        public String getGuessPlayer() {
            return guessPlayer;
        }

        @Override
        public Item getItem() {
            return item;
        }

        @Override
        public Suspect getSuspect() {
            return suspect;
        }

        @Override
        public Location getLocation() {
            return location;
        }

        @Override
        public String getShowerPlayer() {
            return showerPlayer;
        }
    }
}
