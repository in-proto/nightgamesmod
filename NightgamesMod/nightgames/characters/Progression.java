package nightgames.characters;

import com.google.gson.JsonObject;
import nightgames.beans.Property;

public class Progression {
    private static final String JSON_LEVEL = "level";
    private static final String JSON_XP = "xp";
    private static final String JSON_RANK = "rank";

    private int level;
    private Property<Integer> xp = new Property<>(0);
    private int rank = 0;

    Progression(int level) {
        this.level = level;
    }

    Progression(JsonObject js) {
        this.level = js.get(JSON_LEVEL).getAsInt();
        this.xp = new Property<>(js.get(JSON_XP).getAsInt());
        this.rank = js.get(JSON_RANK).getAsInt();
    }

    JsonObject save() {
        var object = new JsonObject();
        object.addProperty(JSON_LEVEL, level);
        object.addProperty(JSON_XP, xp.get());
        object.addProperty(JSON_RANK, rank);
        return object;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public Property<Integer> getXPProperty() {
        return xp;
    }

    public int getXp() {
        return xp.get();
    }

    public void setXp(int xp) {
        this.xp.set(xp);
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    boolean hasSameStats(Progression other) {
        return level == other.level &&
                xp == other.xp &&
                rank == other.rank;
    }

    public boolean canLevelUp() {
        return xp.get() > Progression.xpRequirementForNextLevel(level);
    }

    public void levelUp() {
        assert canLevelUp();
        xp.set(xp.get() - Progression.xpRequirementForNextLevel(level));
        // TODO: level += 1
    }

    private static int xpRequirementForNextLevel(int currentLevel) {
        return Math.min(45 + 5 * currentLevel, 100);
    }
}
