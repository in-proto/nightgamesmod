package nightgames.skills;

import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.Trait;
import nightgames.characters.body.FeetPart;
import nightgames.combat.Combat;
import nightgames.combat.Result;
import nightgames.global.Global;
import nightgames.items.clothing.ClothingSlot;

public class TakeOffShoes extends Skill {

    public TakeOffShoes(Character self) {
        super("Remove Shoes", self);
    }

    @Override
    public boolean requirements(Combat c, Character user, Character target) {
        return (user.get(Attribute.Cunning) >= 5 && !user.human()) || target.body.getFetish(
            FeetPart.TYPE).isPresent() && getSelf().has(Trait.direct);
    }

    @Override
    public boolean usable(Combat c, Character target) {
        return getSelf().canAct() && c.getStance().mobile(getSelf()) && !getSelf().outfit.hasNoShoes();
    }

    @Override
    public String describe(Combat c) {
        return "Remove your own shoes";
    }

    @Override
    public float priorityMod(Combat c) {
        return c.getOpponentCharacter(getSelf()).body.getFetish(FeetPart.TYPE).isPresent() && c.getOpponentCharacter(getSelf()).body.getFetish(
            FeetPart.TYPE).get().magnitude > .25 && !c.getOpponentCharacter(getSelf()).stunned() ? 1.0f : -5.0f;
    }

    @Override
    public boolean resolve(Combat c, Character target) {
        getSelf().strip(ClothingSlot.feet, c);
        if (target.body.getFetish(FeetPart.TYPE).isPresent() && target.body.getFetish(FeetPart.TYPE).get().magnitude > .25) {
            writeOutput(c, Result.special, target);
            target.temptWithSkill(c, getSelf(), getSelf().body.getRandomFeet(), Global.random(17, 26), this);
        } else {
            writeOutput(c, Result.normal, target);
        }
        return true;
    }

    @Override
    public Skill copy(Character user) {
        return new TakeOffShoes(user);
    }

    @Override
    public Tactics type(Combat c) {
        Character target = c.getOpponentCharacter(getSelf());
        return target.body.getFetish(FeetPart.TYPE).isPresent() && target.body.getFetish(
            FeetPart.TYPE).get().magnitude > .25 ? Tactics.pleasure : Tactics.misc;
    }

    @Override
    public String deal(Combat c, int damage, Result modifier, Character target) {
        if (modifier == Result.special) {
            return Global.format("{self:SUBJECT} take a moment to slide off {self:possessive} footwear with slow exaggerated motions. {other:SUBJECT-ACTION:gulp|gulps}. "
                            + "While {other:pronoun-action:know|knows} what {self:pronoun} are doing, it changes nothing as desire fills {other:possessive} eyes.", getSelf(), target);
        }
        return "You take a moment to kick off your footwear.";
    }

    @Override
    public String receive(Combat c, int damage, Result modifier, Character target) {
        if (modifier == Result.special) {
            return Global.format("{self:SUBJECT} takes a moment to slide off {self:possessive} footwear with slow exaggerated motions. {other:SUBJECT-ACTION:gulp|gulps}. "
                            + "While {other:pronoun-action:know|knows} what {self:pronoun} is doing, it changes nothing as desire fills {other:possessive} eyes.", getSelf(), target);
        }
        return getSelf().subject() + " takes a moment to kick off " + getSelf().possessiveAdjective() + " footwear.";
    }
}
