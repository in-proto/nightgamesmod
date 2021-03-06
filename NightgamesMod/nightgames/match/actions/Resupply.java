package nightgames.match.actions;

import nightgames.areas.Area;
import nightgames.match.Action;
import nightgames.match.Encounter;
import nightgames.match.Participant;

import java.util.Optional;

public abstract class Resupply extends Action {

    public interface Trigger {
        void onActionStart(Participant usedAction);
    }

    public class Instance extends Action.Instance {

        protected Instance(Participant user, Area location) {
            super(user, location);
        }

        @Override
        public void execute() {
            user.state = new State();
            messageOthersInLocation(user.getCharacter().getGrammar().subject().defaultNoun() +
                    " heads for one of the safe rooms, probably to get a change of clothes.");
        }
    }

    public static class State implements Participant.State {

        @Override
        public boolean allowsNormalActions() {
            return false;
        }

        @Override
        public void move(Participant p) {
            p.invalidAttackers.clear();
            p.getCharacter().change();
            p.state = new Ready();
            p.getCharacter().getWillpower().renew();
        }

        @Override
        public boolean isDetectable() {
            return true;
        }

        @Override
        public Optional<Runnable> eligibleCombatReplacement(Encounter encounter, Participant p, Participant other) {
            throw new UnsupportedOperationException(String.format("%s can't be attacked while resupplying",
                    p.getCharacter().getTrueName()));
        }

        @Override
        public Optional<Runnable> ineligibleCombatReplacement(Participant p, Participant other) {
            return Optional.empty();
        }

        @Override
        public int spotCheckDifficultyModifier(Participant p) {
            throw new UnsupportedOperationException(String.format("%s can't be attacked while resupplying",
                    p.getCharacter().getTrueName()));
        }

    }

    protected Resupply() {
        super("Resupply");
    }

    @Override
    public boolean usable(Participant user) {
        return !user.getCharacter().bound();
    }
}
