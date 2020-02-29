package nightgames.match;

import nightgames.characters.Character;
import nightgames.combat.Combat;
import nightgames.global.Encs;
import nightgames.trap.Trap;

public interface Encounter {
    boolean battle();

    Combat getCombat();

    boolean checkIntrude(Character c);

    void intrude(Character intruder, Character assist);

    void trap(Character opportunist, Character target, Trap.Instance trap);

    boolean spotCheck();

    Character getPlayer(int idx);

    void parse(Encs choice, Character primary, Character opponent);

    void parse(Encs choice, Character primary, Character opponent, Trap.Instance trap);

    void watch();
}
