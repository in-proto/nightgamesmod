package nightgames.areas;

import nightgames.characters.Attribute;
import nightgames.characters.Character;
import nightgames.global.Global;
import nightgames.match.Action;
import nightgames.match.Encounter;
import nightgames.match.Participant;
import nightgames.match.actions.Move;
import nightgames.status.Stsflag;
import nightgames.trap.Trap;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class Area implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -1372128249588089014L;
    public String name;
    private ArrayList<Participant> present = new ArrayList<>();
    private final DescriptionModule descriptions;

    public Encounter fight;
    public boolean alarm = false;
    public ArrayList<Deployable> env = new ArrayList<>();
    public transient MapDrawHint drawHint = new MapDrawHint();
    private boolean pinged;
    private Set<AreaAttribute> attributes = Set.of();
    private Set<Action> possibleActions = new HashSet<>();
    private Trap.Instance trap = null;

    public Area(String name, DescriptionModule descriptions) {
        this.name = name;
        this.descriptions = descriptions;
    }

    public Area(String name, DescriptionModule descriptions, Set<AreaAttribute> attributes) {
        this(name, descriptions);
        this.attributes = attributes;
    }

    public static void addDoor(Area one, Area other) {
        one.possibleActions.add(Move.normal(other));
        other.possibleActions.add(Move.normal(one));
    }

    public void shortcut(Area sc) {
        possibleActions.add(Move.shortcut(sc));
    }

    public void jump(Area adj){
        possibleActions.add(Move.ninjaLeap(adj));
    }

    public boolean open() {
        return attributes.contains(AreaAttribute.Open);
    }

    public boolean ping(int perception) {
        if (fight != null) {
            return true;
        }
        for (Participant participant : present) {
            if (!(participant.getCharacter().check(Attribute.Cunning, Global.random(20) + perception) || !participant.state.isDetectable()) || open()) {
                return true;
            }
        }
        return alarm;
    }

    public void enter(Character c) {
        var p = Global.getMatch().findParticipant(c);
        present.add(p);
        System.out.printf("%s enters %s: %s\n", p.getCharacter().getTrueName(), name, env);
        List<Deployable> deps = new ArrayList<>(env);
        if (trap != null && trap.resolve(p)) {
            return;
        }
        for (Deployable dep : deps) {
            if (dep != null && dep.resolve(p)) {
                return;
            }
        }
    }

    /**
     * Returns true if we took control of the turn
     */
    public boolean encounter(Participant p) {
        // We can't run encounters if a fight is already occurring.
        if (fight != null) {
            var intrusionOptions = fight.getCombatIntrusionOptions(p);
            if (!intrusionOptions.isEmpty()) {
                p.intrudeInCombat(intrusionOptions, () -> fight.watch());
                return true;
            }
        } else if (present.size() > 1) {
            for (Participant opponent : present) {          //FIXME: Currently - encounters repeat - Does this check if they are busy?
                if (opponent != p
                        // && Global.getMatch().canEngage(p, opponent)
                ) {
                    var fight = new Encounter(p, opponent, this);
                    return fight.spotCheck();
                }
            }
        }
        return false;
    }

    public boolean opportunity(Character target, Trap.Instance trap) {
        var targetParticipant = Global.getMatch().findParticipant(target);
        for (Participant opponent : present) {
            if (opponent != targetParticipant) {
                if (targetParticipant.canStartCombat(opponent) && opponent.canStartCombat(targetParticipant) && fight == null) {
                    opponent.getIntelligence().promptTrap(
                            targetParticipant,
                            () -> fight.trap(opponent, targetParticipant, trap),
                            () -> {
                            });
                    return true;
                }
            }
        }
        clearTrap();
        return false;
    }

    public boolean humanPresent() {
        for (Participant player : present) {
            if (player.getCharacter().human()) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return present.isEmpty();
    }

    public void exit(Character p) {
        present.remove(p);
    }

    public void endEncounter() {
        fight = null;
    }

    public String getMovementToAreaDescription(Character c) {
        return descriptions.movedToLocation();
    }

    public void place(Deployable thing) {
        if (thing instanceof Trap.Instance) {
            trap = (Trap.Instance) thing;
        } else {
            env.add(thing);
        }
    }

    public void setTrap(Trap.Instance t) {
        this.trap = t;
    }

    public void clearTrap() {
        this.trap = null;
    }

    public void remove(Deployable triggered) {
        env.remove(triggered);
    }

    public Optional<Trap.Instance> getTrap() {
        return Optional.ofNullable(trap);
    }

    public Optional<Trap.Instance> getTrap(Participant p) { return getTrap().filter(trap -> trap.getOwner() == p); }

    public void setPinged(boolean b) {
        this.pinged = b;
    }

    public boolean isPinged() {
        return pinged;
    }

    public boolean isDetected() {
        return present.stream().anyMatch(c -> c.getCharacter().is(Stsflag.detected));
    }

    public List<Action.Instance> possibleActions(Participant p) {
        return possibleActions.stream()
                .filter(action -> action.usable(p))
                .map(action -> action.newInstance(p, this))
                .collect(Collectors.toList());
    }

    public Set<Participant> getOccupants() {
        return Set.copyOf(present);
    }

    // Stealthily slips a character into a room without triggering anything. Use with caution.
    public void place(Participant p) {
        present.add(p);
    }

    public void setMapDrawHint(MapDrawHint hint) {
        drawHint = hint;
    }

    public Set<Action> getPossibleActions() {
        return possibleActions;
    }

    public String describe() {
        return descriptions.whereAmI() + possibleActions.stream()
                .filter(act -> act instanceof Action.LocationDescription)
                .map(act -> ((Action.LocationDescription) act).describeLocation())
                .reduce(String::concat);
    }

}
