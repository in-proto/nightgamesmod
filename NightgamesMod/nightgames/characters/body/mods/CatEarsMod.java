package nightgames.characters.body.mods;

import java.util.Optional;
import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.characters.body.BodyPart;

public class CatEarsMod extends PartMod {
    public static final String TYPE = "cat ears";

    public CatEarsMod() {
        super(TYPE, .15, .5, .5);
    }

    @Override
    public String describeAdjective(String partType) {
        return "cat";
    }

    @Override
    public Optional<Integer> attributeModifier(Attribute a) {
        if (a == Attribute.Seduction) {
            return Optional.of(3);
        }
        return Optional.empty();
    }

    @Override
    public String getLongDescriptionOverride(
        Character self,
        BodyPart part,
        String previousDescription) {
        return "Cute " + part.fullDescribe(self) + " tops " + self.possessiveAdjective() + " head.";
    }
}
