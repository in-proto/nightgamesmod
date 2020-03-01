package nightgames.actions;

import nightgames.characters.Character;
import nightgames.characters.State;
import nightgames.global.Flag;
import nightgames.global.Global;
import nightgames.items.Item;
import nightgames.match.Participant;
import nightgames.match.ftc.FTCMatch;

import java.util.Set;
import java.util.stream.Collectors;

public class Resupply extends Action {
    private static final long serialVersionUID = -3349606637987124335L;

    private final boolean permissioned;
    private final Set<Character> validCharacters;

    public Resupply() {
        super("Resupply");
        permissioned = false;
        validCharacters = Set.of();
    }

    public Resupply(Set<Participant> validParticipants) {
        super("Resupply");
        permissioned = true;
        validCharacters = validParticipants.stream().map(Participant::getCharacter).collect(Collectors.toSet());
    }

    @Override
    public boolean usable(Participant user) {
        return !user.getCharacter().bound() && (!permissioned || validCharacters.contains(user));
    }

    @Override
    public IMovement execute(Character user) {
        if (Global.checkFlag(Flag.FTC)) {
            FTCMatch match = (FTCMatch) Global.getMatch();
            if (user.human()) {
                Global.gui().message("You get a change of clothes from the chest placed here.");
            }
            if (user.has(Item.Flag) && !match.isPrey(user)) {
                match.turnInFlag(user);
            } else if (match.canCollectFlag(user)) {
                match.grabFlag();
            }
        } else {
            if (user.human()) {
                if (Global.getMatch().getCondition().name().equals("nudist")) {
                    Global.gui().message(
                                    "You check in so that you're eligible to fight again, but you still don't get any clothes.");
                } else {
                    Global.gui().message("You pick up a change of clothes and prepare to get back in the fray.");
                }
            }
        }
        user.state = State.resupplying;
        return Movement.resupply;
    }

    @Override
    public IMovement consider() {
        return Movement.resupply;
    }

}
