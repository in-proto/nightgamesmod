package nightgames.pet;

import nightgames.characters.Character;
import nightgames.characters.CharacterSex;
import nightgames.characters.Growth;
import nightgames.characters.body.PussyPart;
import nightgames.characters.body.mods.catcher.DemonicMod;
import nightgames.combat.Combat;
import nightgames.skills.Grind;
import nightgames.skills.Piston;
import nightgames.skills.PussyGrind;
import nightgames.skills.Thrust;
import nightgames.skills.petskills.*;

import java.util.Optional;

public class ImpFem extends Pet {
    public ImpFem(Character owner) {
        super("imp", owner, Ptype.impfem, 3, 2);
    }

    public ImpFem(Character owner, int power, int ac) {
        super("imp", owner, Ptype.impfem, power, ac);
    }

    @Override
    public String describe() {
        return null;
    }

    @Override
    public void vanquish(Combat c, Pet opponent) {
        var instakillScene = instakillScene(opponent);
        if (instakillScene.isPresent()) {
            c.write(getSelf(), instakillScene.get());
            c.removePet(opponent.getSelf());
        } else {
            (new ImpTease(getSelf())).resolve(c, opponent.getSelf());
        }
    }

    public Optional<String> instakillScene(Pet opponent) {
        switch (opponent.type()) {
            case fairyfem:
                return Optional.of(own() + "imp grabs " + opponent.own()
                        + "faerie and inserts the tiny girl into her soaking cunt. She pulls the faerie out after only a few seconds, but "
                        + "the sprite is completely covered with the imp's aphrodisiac wetness. The demon simply watches as the horny fae girl frantically masturbates in a sex "
                        + "drunk daze. It doesn't take long until the faerie disappears with an orgasmic moan.");
            case fairymale:
                return Optional.of(own() + "imp catches " + opponent.own()
                        + "faerie boy and holds him in her palm. The imp uses one finger to toy with the fae's tiny penis and the little "
                        + "male squirms helplessly. She dexterously rubs the faerie until it cums on her finger and vanishes with a flash.");
            case impfem:
                return Optional.of("The two female imps grapple with each other and " + opponent.own() + " imp throws "
                        + own() + "imp to the floor. " + opponent.own() + " imp approaches to press her "
                        + "advantage, but " + own()
                        + "imp's tail suddenly thrusts into her pussy. As the tails fucks her, " + own()
                        + "imp collects some of her own wetness and forces the "
                        + "aphrodisiac filled fluid into the other female's mouth. " + opponent.own()
                        + "imp's orgasmic moan is stifled and she vanishes in a puff of brimstone.");
            case impmale:
                return Optional.of(own() + "imp grapples with " + opponent.own()
                        + "imp and gets a hold of his erection. She uses her leverage to parade the male around the edge of the battle. "
                        + "She strokes the demonic dick to the edge of orgasm and then mercilessly slams her knee into his balls. The male howls in pain and disappears.");
            case slime:
                return Optional.of(own() + "imp shoves both her hands into " + opponent.own()
                        + "slime. The slime trembles at her touch, encouraging her to wiggle her fingers more inside its "
                        + "semi-solid body. The slime writhes more and more before it suddenly shudders, then slowly melts into a puddle.");
            default:
                return Optional.empty();
        }
    }

    @Override
    public void caught(Combat c, Character captor) {
        if (owner().human()) {
            c.write(captor, captor.getName()
                            + " grabs your imp and forces her to bend over. She thrusts two fingers into the little demon's pussy and pumps until she's overflowing with wetness. She "
                            + "removes her fingers from the imp's lower lips and forces them into the creature's mouth. Your demon, affected by the aphrodisiacs in her own juices, spreads her legs "
                            + "to " + captor.getName() + " and makes a pleading sound. " + captor.getName()
                            + " rubs and pinches the imp's clit until she spasms and disappears.");
        } else if (captor.human()) {
            c.write(captor, "You manage to catch " + own()
                            + "imp by her tail and pull her off balance. The imp falls to the floor and you plant your foot on her wet box before she can "
                            + "recover. You rub her slick folds with the sole of your foot and the demon writhes in pleasure, letting out incoherent whimpers. You locate her engorged clit "
                            + "with your toes and rub it quickly to finish her off. The imp climaxes and vanishes, leaving no trace except the wetness on your foot.");
        }
        c.removePet(getSelf());
    }
    
    @Override
    protected void buildSelf() {
        PetCharacter self = new PetCharacter(this, owner().nameOrPossessivePronoun() + " " + getName(), getName(), new Growth(), getPower());
        // imps are about as tall as goblins, maybe a bit shorter
        self.body.setHeight(110);
        if (getPower() > 30) {
            var pussy = new PussyPart();
            pussy.addMod(new DemonicMod());
            self.body.add(pussy);
        } else {
            self.body.add(new PussyPart());
        }
        self.body.finishBody(CharacterSex.female);
        self.learn(new ImpAssault(self));
        self.learn(new Thrust(self));
        self.learn(new Grind(self));
        self.learn(new Piston(self));
        self.learn(new PussyGrind(self));
        self.learn(new ImpTease(self));
        self.learn(new ImpStrip(self));
        self.learn(new ImpFacesit(self));
        self.learn(new ImpSemenSquirt(self));
        setSelf(self);
    }
}
