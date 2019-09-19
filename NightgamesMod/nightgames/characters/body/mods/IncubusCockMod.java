package nightgames.characters.body.mods;

import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.body.BodyPart;
import nightgames.characters.body.CockMod;
import nightgames.combat.Combat;
import nightgames.global.Global;
import nightgames.pet.PetCharacter;
import nightgames.skills.damage.DamageType;
import nightgames.status.Drained;
import nightgames.status.Enthralled;

public class IncubusCockMod extends CockMod {
    public IncubusCockMod(String name, double hotness, double pleasure, double sensitivity) {
        super(name, hotness, pleasure, sensitivity);
    }

    @Override
    public double applyBonuses(Combat c, Character self, Character opponent, BodyPart part, BodyPart target, double damage) {
        double bonus = super.applyBonuses(c, self, opponent, part, target, damage);
        String message = String
            .format("%s demonic appendage latches onto %s will, trying to draw it into %s.",
                self.nameOrPossessivePronoun(), opponent.nameOrPossessivePronoun(),
                self.reflectivePronoun());
        int amtDrained;
        if (target.moddedPartCountsAs(opponent, FeralMod.INSTANCE)) {
            message += String.format(" %s %s gladly gives it up, eager for more pleasure.",
                opponent.possessiveAdjective(), target.describe(opponent));
            amtDrained = 5;
            bonus += 2;
        } else if (target.moddedPartCountsAs(opponent, CyberneticMod.INSTANCE)) {
            message += String.format(
                " %s %s does not oblige, instead sending a pulse of electricity through %s %s and up %s spine",
                opponent.nameOrPossessivePronoun(), target.describe(opponent),
                self.nameOrPossessivePronoun(), part.describe(self), self.possessiveAdjective());
            self.pain(c, opponent, Global.random(9) + 4);
            amtDrained = 0;
        } else {
            message += String
                .format(" Despite %s best efforts, some of the elusive energy passes into %s.",
                    opponent.nameOrPossessivePronoun(), self.nameDirectObject());
            amtDrained = 3;
        }
        int strength = (int) self.modifyDamage(DamageType.drain, opponent, amtDrained);
        if (amtDrained != 0) {
            if (self.isPet()) {
                Character master = ((PetCharacter) self).getSelf().owner();
                c.write(self, Global.format(
                    "The stolen strength seems to flow through to {self:possessive} {other:master} through {self:possessive} infernal connection.",
                    self, master));
                opponent.drainWillpower(c, master, strength);
            } else {
                opponent.drainWillpower(c, self, strength);
            }
        }
        c.write(self, message);
        return bonus;
    }

    @Override
    public void onOrgasmWith(Combat c, Character self, Character opponent, BodyPart part, BodyPart target, boolean selfCame) {
        if (this.equals(incubus) && c.getStance().inserted(self)) {
            if (selfCame) {
                if (target.moddedPartCountsAs(opponent, CyberneticMod.INSTANCE)) {
                    c.write(self, String.format(
                        "%s demonic seed splashes pointlessly against the walls of %s %s, failing even in %s moment of defeat.",
                        self.nameOrPossessivePronoun(), opponent.nameOrPossessivePronoun(),
                        target.describe(opponent), self.possessiveAdjective()));
                } else {
                    int duration = Global.random(3) + 2;
                    String message = String.format(
                        "The moment %s erupts inside %s, %s mind goes completely blank, leaving %s pliant and ready.",
                        self.subject(), opponent.subject(), opponent.possessiveAdjective(),
                        opponent.directObject());
                    if (target.moddedPartCountsAs(opponent, FeralMod.INSTANCE)) {
                        message += String.format(" %s no resistance to the subversive seed.",
                            Global.capitalizeFirstLetter(opponent.subjectAction("offer", "offers")));
                        duration += 2;
                    }
                    opponent.add(c, new Enthralled(opponent, self, duration));
                    c.write(self, message);
                }
            } else {
                if (!target.moddedPartCountsAs(opponent, CyberneticMod.INSTANCE)) {
                    c.write(self, String.format(
                        "Sensing %s moment of passion, %s %s greedily draws upon the rampant flows of orgasmic energy within %s, transferring the power back into %s.",
                        opponent.nameOrPossessivePronoun(), self.nameOrPossessivePronoun(),
                        part.describe(self), opponent.directObject(), self.directObject()));
                    int attDamage = target.moddedPartCountsAs(opponent, FeralMod.INSTANCE) ? 10 : 5;
                    int willDamage = target.moddedPartCountsAs(opponent, FeralMod.INSTANCE) ? 10 : 5;
                    Drained.drain(c, self, opponent, Attribute.Power, attDamage, 20, true);
                    Drained.drain(c, self, opponent, Attribute.Cunning, attDamage, 20, true);
                    Drained.drain(c, self, opponent, Attribute.Seduction, attDamage, 20, true);
                    opponent.drainWillpower(c, self, (int) self.modifyDamage(DamageType.drain, opponent, willDamage));
                }
            }
        }
    }

    @Override
    public String describeAdjective(String partType) {
        return "corruption";
    }
}