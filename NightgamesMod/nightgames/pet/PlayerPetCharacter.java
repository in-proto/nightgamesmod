package nightgames.pet;

import nightgames.characters.Player;
import nightgames.global.Global;
import nightgames.items.clothing.Outfit;

import java.util.HashMap;

public class PlayerPetCharacter extends PetCharacter {
    private Player prototype;

    public PlayerPetCharacter(String name, Pet self, Player prototypeCharacter, int level) throws CloneNotSupportedException {
        super(self, name, prototypeCharacter.getType() + "Pet", prototypeCharacter.getGrowth(), 1);
        prototype = (Player) prototypeCharacter.clone();
        prototype.applyBasicStats(this);
        for (int i = 1; i < level; i++) {
            getGrowth().levelUp(this);
            prototype.getLevelUpFor(i).apply(this);
            this.level += 1;
        }
        this.att = new HashMap<>(prototype.att);
        this.clearTraits();
        prototype.getTraitsPure().forEach(this::addTraitDontSaveData);
        this.getSkills().clear();
        this.body = prototypeCharacter.body.clone(this);
        this.outfit = new Outfit(prototypeCharacter.outfit);
        getStamina().renew();
        getArousal().renew();
        getMojo().renew();
        Global.learnSkills(this);
    }
}