package nightgames.trap;

import nightgames.areas.Deployable;
import nightgames.characters.Character;
import nightgames.stance.Position;

import java.util.Optional;

public abstract class Trap implements Deployable {
    
    protected Character owner;
    private final String name;
    private int strength;
    protected Trap(String name, Character owner) {
        this.name = name;
        this.owner = owner;
        this.setStrength(0);
    }
    
    protected abstract void trigger(Character target);

    public boolean decoy() {
        return false;
    }

    public abstract boolean recipe(Character owner);

    public abstract boolean requirements(Character owner);

    public abstract String setup(Character owner);

    public boolean resolve(Character active) {
        if (active != owner) {
            trigger(active);
            return true;
        }
        return false;
    }

    public void setStrength(Character user) {
        this.strength = user.getLevel();
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }

    public int getStrength() {
        return strength;
    }

    @Override
    public final Character owner() {
        return owner;
    }

    @Override
    public final String toString() {
        return getName();
    }

    @Override
    public final boolean equals(Object obj) {
        return obj != null && getName().equals(obj.toString());
    }
    
    public Optional<Position> capitalize(Character attacker, Character victim) {
        return Optional.empty();
    }

    public String getName() {
        return name;
    }

}
