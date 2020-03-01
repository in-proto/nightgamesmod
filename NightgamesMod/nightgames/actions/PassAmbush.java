package nightgames.actions;

import nightgames.characters.Character;
import nightgames.characters.State;
import nightgames.global.Global;
import nightgames.match.Participant;

public class PassAmbush extends Action {

    private static final long serialVersionUID = -1745311550506911281L;

    public PassAmbush() {
        super("Try Ambush");
    }

    @Override
    public boolean usable(Participant user) {
        return user.getCharacter().state != State.inPass
                && !user.getCharacter().bound();
    }

    @Override
    public IMovement execute(Character user) {
        if (user.human()) {
            Global.gui().message(
                            "You try to find a decent hiding place in the irregular" + " rock faces lining the pass.");
        }
        user.state = State.inPass;
        return Movement.ftcPassAmbush;
    }

    @Override
    public IMovement consider() {
        return Movement.ftcPassAmbush;
    }

}
