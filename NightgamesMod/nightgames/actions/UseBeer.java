package nightgames.actions;

import nightgames.characters.Character;
import nightgames.global.Global;
import nightgames.items.Item;
import nightgames.match.Participant;
import nightgames.match.Status;
import nightgames.status.Buzzed;

public class UseBeer extends Action {

    private static class Aftermath extends Action.Aftermath {
        private Aftermath() {}

        @Override
        public String describe(Character c) {
            return Movement.beer.describe(c);
        }
    }

    public UseBeer() {
        super( "Beer");
    }

    @Override
    public boolean usable(Participant user) {
        return !user.getCharacter().bound();
    }

    @Override
    public Action.Aftermath execute(Participant user) {
        if (user.getCharacter().human()) {
            Global.gui().message("You pop open a beer and chug it down, feeling buzzed and a bit slugish.");
        }
        user.getCharacter().addNonCombat(new Status(new Buzzed(user.getCharacter())));
        user.getCharacter().consume(Item.Beer, 1);
        return new Aftermath();
    }

}
