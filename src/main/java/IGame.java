import java.util.List;
import java.util.Set;

public interface IGame {

    <T extends Enum<T>> Boolean isAssignedTo(String who, Enum<T> item);

    List<String> getAllName();

    Set<ICommander.ICommand> getUnusedCommands();

    List<ICommander.ICommand> getAllCommand();
}
