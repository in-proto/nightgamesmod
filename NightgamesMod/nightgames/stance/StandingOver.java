package nightgames.stance;

import nightgames.characters.Character;
import nightgames.combat.Combat;

public class StandingOver extends AbstractFacingStance {

    public StandingOver(Character top, Character bottom) {
        super(top, bottom, Stance.standingover);

    }

    @Override
    public String describe(Combat c) {
        if (top.human()) {
            return "You are standing over " + bottom.getName() + ", who is helpless on the ground.";
        } else {
            return String.format("%s flat on %s back, while %s stands over %s.",
                            bottom.subjectAction("are", "is"), bottom.possessiveAdjective(),
                            top.subject(), bottom.objectPronoun());
        }
    }

    @Override
    public boolean mobile(Character c) {
        return true;
    }

    @Override
    public boolean kiss(Character c, Character target) {
        return c != top && c != bottom;
    }

    @Override
    public String image() {
        if (bottom.hasPussy()) {
            return "standing_m.jpg";
        } else {
            return "standing_f.jpg";
        }
    }

    @Override
    public boolean dom(Character c) {
        return c == top;
    }

    @Override
    public boolean sub(Character c) {
        return c == bottom;
    }

    @Override
    public boolean reachTop(Character c) {
        return c != top && c != bottom;
    }

    @Override
    public boolean reachBottom(Character c) {
        return c != top && c != bottom;
    }

    @Override
    public boolean prone(Character c) {
        return c == bottom;
    }

    @Override
    public boolean feet(Character c, Character target) {
        return target == bottom;
    }

    @Override
    public boolean oral(Character c, Character target) {
        return target == bottom && c != top;
    }

    @Override
    public boolean behind(Character c) {
        return false;
    }

    @Override
    public boolean inserted(Character c) {
        return false;
    }

    @Override
    public float priorityMod(Character self) {
        return getSubDomBonus(self, 2.0f);
    }

    @Override
    public double pheromoneMod(Character self) {
        return 1.5;
    }

    @Override
    public Position.Dominance dominance() {
        return Position.Dominance.GIVE_AND_TAKE;
    }
    
    @Override
    public int distance() {
        return 2;
    }
}
