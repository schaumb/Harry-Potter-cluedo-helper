import java.util.List;

public interface ICommander {
    List<String> getNames();

    enum SpecPlayers {
        Dumbledore,
        Result,
    }
    enum Item {
        SLEEPING_DRAUGHT,
        VANISHING_CABINET,
        PORTKEY,
        IMPEDIMENTA,
        PETRIFICUS_TOTALUS,
        MANDRAKE
    }

    enum Suspect {
        DRACO_MALFOY,
        CRABBE_AND_GOYLE,
        LUCIUS_MALFOY,
        DOLORES_UMBRIDGE,
        PETER_PETTIGREW,
        BELLATRIX_LEXTRANGE
    }


    enum Location {
        GREAT_HALL,
        HOSPITAL_WING,
        ROOM_OF_REQUIREMENT,
        POTIONS_CLASSROOM,
        TROPHY_ROOM,
        DIVINATION_CLASSROOM,
        OWLERY,
        LIBRARY,
        DEFENSE_AGAIN_DARK_ARTS
    }

    enum TypesType {
        ITEM(Item.class),
        SUSPECT(Suspect.class),
        LOCATION(Location.class);

        private final Class<? extends Enum<?>> clazz;
        private final int enumLength;


        TypesType(Class<? extends Enum<?>> clazz) {
            this.clazz = clazz;
            this.enumLength = clazz.getEnumConstants().length;
        }

        public static TypesType getFromClass(Class<?> clazz) {
            return valueOf(clazz.getSimpleName().toUpperCase());
        }

        public Class<? extends Enum<?>> getEnumClass() {
            return clazz;
        }

        public int getEnumLength() {
            return enumLength;
        }
    }

    enum CommandType {
        Guess,
        Show,
        Final_Guess,
        End
    }

    interface ICommand {
        CommandType getCommandType();

        String getGuessPlayer();
        Suspect getSuspect();
        Item getItem();
        Location getLocation();
        String getShowerPlayer();
    }

    void init();

    ICommand getNextCommand();
}
