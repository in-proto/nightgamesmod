package nightgames.match.actions;

import nightgames.characters.Character;
import nightgames.match.Action;
import nightgames.match.Participant;
import nightgames.trap.Trap;

public class SetTrap extends Action {

    private static final class Aftermath extends Action.Aftermath {
        private Aftermath() { }

        @Override
        public String describe(Character c) {
            return " start rigging up something weird, probably a trap.";
        }
    }

    public static final class Instance extends Action.Instance {
        private Instance(Action self, Participant user) {
            super(self, user);
        }

        @Override
        public Action.Aftermath execute() {
            return self.execute(user);
        }
    }

    private Trap trap;

    public SetTrap(Trap trap) {
        super("Set(" + trap.toString() + ")");
        this.trap = trap;
    }

    @Override
    public boolean usable(Participant user) {
        return trap.recipe(user) && !user.getLocation().open()
                && trap.requirements(user)
                && !user.getCharacter().bound()
                && user.getLocation().getTrap().isEmpty();
    }

    @Override
    public Instance newInstance(Participant user) {
        return new Instance(this, user);
    }

    @Override
    public Action.Aftermath execute(Participant user) {
        var result = trap.instantiate(user);
        user.getLocation().setTrap(result.instance);
        user.getCharacter().message(result.message);
        return new Aftermath();
    }

}
