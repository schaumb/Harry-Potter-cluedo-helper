import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrinterImpl extends Printer {
    private final PrintWriter out = new PrintWriter(System.out);
    private final List<Enum<?>> values;

    enum SeparatorEnum {
        SEPARATOR
    }

    protected PrinterImpl(IGame game) {
        super(game);
        this.values = Stream.of(
                SeparatorEnum.values(),
                ICommander.Item.values(),
                SeparatorEnum.values(),
                ICommander.Suspect.values(),
                SeparatorEnum.values(),
                ICommander.Location.values(),
                SeparatorEnum.values())
                .flatMap(Stream::of).collect(Collectors.toList());
    }

    String commandToString(ICommander.ICommand command) {
        return String.format("%.3s %.5s %.5s %.5s %.3s", command.getShowerPlayer(), command.getItem(), command.getSuspect(), command.getLocation(), command.getGuessPlayer() == null ? "" : command.getGuessPlayer());
    }

    @Override
    public void print() {
        final Iterator<ICommander.ICommand> iterator = game.getAllCommand().iterator();
        final Iterator<ICommander.ICommand> iterator2 = game.getUnusedCommands().iterator();

        Runnable newline = () -> {
            out.print('|');

            if (iterator.hasNext()) {
                out.printf(" %-39s", commandToString(iterator.next()));
            }
            if (iterator2.hasNext()) {
                out.printf(" %-39s", commandToString(iterator2.next()));
            }
            out.println();
        };

        final List<String> names = game.getAllName();
        out.printf("%20s", "");
        for (String name : names) {
            out.printf("|%-3s", name.substring(0, Math.min(3, name.length())));
        }
        newline.run();

        for (Enum<?> value : values) {
            if (value.equals(SeparatorEnum.SEPARATOR)) {
                out.print(String.format("%0" + (20 + (3+1) * names.size()) + "d", 0).replace('0', '-'));

                newline.run();
                continue;
            }

            final String valueString = value.toString();
            out.printf("%20s", valueString.substring(0, Math.min(20, valueString.length())));
            for (String name : names) {
                final Boolean apply = game.isAssignedTo(name, value);
                out.printf("| %c ", apply == null ? ' ' : apply ? '+' : '-');
            }
            newline.run();
        }
        while (iterator.hasNext()) {
            out.printf("%" + (20 + (3+1) * names.size()) + "s", "");
            newline.run();
        }
        out.flush();
    }
}
