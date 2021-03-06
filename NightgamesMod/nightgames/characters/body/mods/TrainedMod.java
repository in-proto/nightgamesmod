package nightgames.characters.body.mods;

import nightgames.characters.Character;
import nightgames.characters.body.BodyPart;
import nightgames.combat.Combat;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

public class TrainedMod extends PartMod {
    public static final String TYPE = "trained";

    public TrainedMod() {
        super(TYPE, .2, .2, -.2);
    }

    public double applyBonuses(Combat c, Character self, Character opponent, BodyPart part, BodyPart target, double damage) { 
        if (opponent.human()) {
            JtwigModel model = JtwigModel.newModel()
                .with("self", self)
                .with("opponent", opponent)
                .with("part", part)
                .with("target", target);
            c.write(self, APPLY_BONUS_TEMPLATE.render(model));
        }
        return 0;
    }

    @Override
    public String describeAdjective(String partType) {
        return "expertly-trained appearance";
    }

    private static final JtwigTemplate APPLY_BONUS_TEMPLATE = JtwigTemplate.inlineTemplate(
        "{{ self.possessiveAdjective() }} trained {{ part.getType() }} feels positively exquisite. "
            + "It's taking all your concentration not to instantly shoot your load.");

}
