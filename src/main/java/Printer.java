import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Printer {
    protected final IGame game;

    protected Printer(IGame game) {
        this.game = game;
    }

    public abstract void print();
}
