package nightgames.characters;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import nightgames.areas.Area;
import nightgames.areas.DescriptionModule;
import nightgames.beans.Property;
import nightgames.characters.body.*;
import nightgames.characters.body.BreastsPart.Size;
import nightgames.characters.body.mods.catcher.DemonicMod;
import nightgames.characters.body.mods.pitcher.IncubusCockMod;
import nightgames.characters.corestats.ArousalStat;
import nightgames.characters.corestats.MojoStat;
import nightgames.characters.corestats.StaminaStat;
import nightgames.characters.corestats.WillpowerStat;
import nightgames.characters.custom.AiModifiers;
import nightgames.combat.Assistant;
import nightgames.combat.Combat;
import nightgames.combat.CombatantData;
import nightgames.combat.Result;
import nightgames.daytime.*;
import nightgames.global.Configuration;
import nightgames.global.Flag;
import nightgames.global.Global;
import nightgames.global.Scene;
import nightgames.grammar.Person;
import nightgames.items.Item;
import nightgames.items.Loot;
import nightgames.items.clothing.Clothing;
import nightgames.items.clothing.ClothingSlot;
import nightgames.items.clothing.ClothingTrait;
import nightgames.items.clothing.Outfit;
import nightgames.json.JsonUtils;
import nightgames.match.Action;
import nightgames.match.Dialog;
import nightgames.match.Intelligence;
import nightgames.match.Match;
import nightgames.match.actions.UseBeer;
import nightgames.match.actions.UseEnergyDrink;
import nightgames.match.actions.UseLubricant;
import nightgames.pet.arms.ArmManager;
import nightgames.skills.*;
import nightgames.skills.damage.DamageType;
import nightgames.stance.Stance;
import nightgames.status.*;
import nightgames.status.addiction.Addiction;
import nightgames.status.addiction.Addiction.Severity;
import nightgames.status.addiction.AddictionType;
import nightgames.status.addiction.Dominance;
import nightgames.status.addiction.MindControl;
import nightgames.traits.*;
import nightgames.trap.Trap;
import nightgames.utilities.DebugHelper;
import nightgames.utilities.ProseUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public abstract class Character extends Observable implements Cloneable {

    private static final String JSON_PROGRESSION = "progression";

    private String name;
    public CharacterSex initialGender;
    private Progression progression;
    public int money;
    public Map<Attribute, Integer> att;             //Attributes are good opportunity to move to OOP Implementation - They are very similar to meters with base and modified values - DSM
    public StaminaStat stamina;
    public ArousalStat arousal;
    protected MojoStat mojo;
    protected WillpowerStat willpower;
    public Outfit outfit;
    public List<Clothing> outfitPlan;               //List is good but ArrayList is more powerful because it's serializable. - DSM 
    public Property<Area> location;                        //What does this do? Is it the characters Current Location? This should be stored as a String or implemented as a token on a larger GameMap - DSM
    private CopyOnWriteArrayList<Skill> skills;     //Skills are unlikely objects to mutate tow warrant this - just opinion. - DSM
    public List<Status> status;                     //List is not Serializable.  Marge into StatusEffect- DSM
    private Set<Stsflag> statusFlags;                //Can be merged into a StatusEffect object and made serializable. - DSM
    private CopyOnWriteArrayList<Trait> traits;     //If traits are implemented like all the skills are, then this can just be an ArrayList. - DSM
    private Map<Trait, Integer> temporaryAddedTraits;
    private Map<Trait, Integer> temporaryRemovedTraits;
    public Set<Status> removelist;                  //Rename for clarity? - DSM 
    public Set<Status> addlist;                     //Rename for clarity?   -DSM
    private Map<String, Integer> cooldowns;          //May not require this if we add new Skills to characters and they may track their own requirements and cooldowns. - DSM
    private Map<Item, Integer> inventory;
    private Map<String, Integer> flags;             //Needs to be more strongly leveraged in mechanics.  -DSM
    protected Item trophy;                          
    protected Map<String, Integer> attractions;
    private Map<String, Integer> affections;
    public HashSet<Clothing> closet;                //If clothing can be destroyed, it should stand to reason that characters should purchase replace. Consider reworking - DSM            
    public Body body;                               //While current implementation allows for many kinds of parts - it means controlling and finding them gets difficult. - DSM
    public int availableAttributePoints;            
    public boolean orgasmed;                        //Merge into tracker object for combat session. -DSM
    public boolean custom;                          //This is not necessary. Every character should be based off custom implementation and added as a configuration is chosen. -DSM
    private boolean pleasured;                      //Merge into tracker object for combat session. - DSM
    public int orgasms;                             //Merge into tracker object for combat session. - DSM
    public int cloned;                              //Merge into tracker object for combat session. - DSM 
    private Map<Integer, LevelUpData> levelPlan;    //This has bloated save files quite a bit, making an XML save file attributeModifier very desireable for editing and reading. - DSM
    private Growth growth;                          //FIXME: Growth, as well as a host of many variables in many classes, have many public variables. Move to protected or private and implement mutators. The compliler is your friend. - DSM

    /**Constructor for a character - creates a character off of a name and level. Base Attributes start at 5 and other stats are derived from that. 
     * @param name
     * The name of the character. 
     * @param level
     * The level that the character starts at. 
     * */
    public Character(String name, int level) {
        this.name = name;
        this.progression = new Progression(level);
        this.growth = new Growth();
        cloned = 0;
        custom = false;
        body = new Body(this);
        att = new HashMap<>();
        cooldowns = new HashMap<>();
        flags = new HashMap<>();
        levelPlan = new HashMap<>();
        att.put(Attribute.Power, 5);
        att.put(Attribute.Cunning, 5);
        att.put(Attribute.Seduction, 5);
        att.put(Attribute.Perception, 5);
        att.put(Attribute.Speed, 5);
        money = 0;
        stamina = new StaminaStat(22 + 3 * level);
        arousal = new ArousalStat(90 + 10 * level);
        mojo = new MojoStat(100);
        willpower = new WillpowerStat(40);
        orgasmed = false;
        pleasured = false;

        outfit = new Outfit();
        outfitPlan = new ArrayList<>();

        closet = new HashSet<>();
        skills = (new CopyOnWriteArrayList<>());
        status = new ArrayList<>();
        statusFlags = EnumSet.noneOf(Stsflag.class);
        traits = new CopyOnWriteArrayList<>();
        temporaryAddedTraits = new HashMap<>();
        temporaryRemovedTraits = new HashMap<>();
        removelist = new HashSet<>();
        addlist = new HashSet<>();
        //Can be changed into a flag that is stored in flags. -DSM
        inventory = new HashMap<>();
        attractions = new HashMap<>(2);
        affections = new HashMap<>(2);
        location = new Property<>(new Area("", new DescriptionModule.ErrorDescriptionModule()));
        // this.combatStats = new CombatStats();       //TODO: Reading, writing, cloning?

        getProgression().setRank(0);

        Global.learnSkills(this);
    }

    // public CombatStats getCombatStats() {  return combatStats;  }  public void setCombatStats(CombatStats combatStats) {  this.combatStats = combatStats; }


    /**Overridden clone() method for Character. Returns a character with values the same as this one.
     * 
     * @return
     * Returns a clone of this object.  
     * 
     * @throws CloneNotSupportedException
     * Is thrown when this object does not support the Cloneable interface.
     * */
    @Override
public Character clone() throws CloneNotSupportedException {
        Character c = (Character) super.clone();
        c.att = new HashMap<>(att);
        c.stamina = stamina.copy();
        c.cloned = cloned + 1;
        c.arousal = arousal.copy();
        c.mojo = mojo.copy();
        c.willpower = willpower.copy();
        c.outfitPlan = new ArrayList<>(outfitPlan);
        c.outfit = new Outfit(outfit);
        c.flags = new HashMap<>(flags);
        c.status = status; // Will be deep-copied in finishClone()
        c.traits = new CopyOnWriteArrayList<>(traits);
        c.temporaryAddedTraits = new HashMap<>(temporaryAddedTraits);
        c.temporaryRemovedTraits = new HashMap<>(temporaryRemovedTraits);

        // TODO! We should NEVER modify the growth in a combat sim. If this is not true, this needs to be revisited and deepcloned.
        c.growth = (Growth) growth.clone();

        c.removelist = new HashSet<>(removelist);
        c.addlist = new HashSet<>(addlist);
        c.inventory = new HashMap<>(inventory);
        c.attractions = new HashMap<>(attractions);
        c.affections = new HashMap<>(affections);
        c.skills = (new CopyOnWriteArrayList<>(getSkills()));
        c.body = body.clone();
        c.body.character = c;
        c.orgasmed = orgasmed;
        c.statusFlags = EnumSet.copyOf(statusFlags);
        c.levelPlan = new HashMap<>();
        for (Entry<Integer, LevelUpData> entry : levelPlan.entrySet()) {
            levelPlan.put(entry.getKey(), (LevelUpData)entry.getValue().clone());
        }
        return c;
    }

    /**This seems to be a helper method used to iterate over the statuses. It's called by the combat log and Class, as well as the informant.  
     *
     * @param other
     * 
     * */
    public final void finishClone(Character other) {
        List<Status> oldstatus = status;
        status = new ArrayList<>();
        for (Status s : oldstatus) {
            status.add(s.instance(this, other));
        }
    }

    /**Returns the name of this character, presumably at the Character level. 
     * 
     * FIXME: presumably this.name, but it always helps to be explicit. This could be an accessor instead, which would be helpful and more conventional - DSM
     * 
     * @return 
     * returning the name at this Character level
     * */
    public String getTrueName() {
        return name;
    }

    /**Gets the Resistances list for this character. 
     * 
     * NOTE: Need more insight into this method. 
     * 
     * @param c
     * The Combat class.
     * @return 
     * Returns a list of resistances. 
     * */
    public List<Resistance> getResistances(Combat c) {
        List<Resistance> resistances = traits.stream().map(Trait::getResistance).collect(Collectors.toList());
        if (c != null) {
            var petOptional = c.assistantsOf(this).stream().filter(pet -> pet.getCharacter().has(Trait.protective)).findAny();
            petOptional.ifPresent(petCharacter -> resistances.add((combat, self, status) -> {
                if (Global.random(100) < 50 && status.flags().contains(Stsflag.debuff) && status.flags().contains(Stsflag.purgable)) {
                    return petCharacter.getCharacter().nameOrPossessivePronoun() + " Protection";
                }
                return "";
            }));
        }
        return resistances;
    }

    /**Nondescriptive getter for some value. 
     * 
     * FIXME: No, really, what is this and why is it needed? - DSM
     * 
     * @param a
     * The Attribute whose value we wish to get. 
     * 
     * @return
     * Returns a value based on a total complied from a combinations of Traits, ClothingTraits, and Attributes. 
     * */
    public final int get(Attribute a) {
        if (a == Attribute.Slime && !has(Trait.slime)) {
            // always return 0 if there's no trait for it.
            return 0;
        }
        int total = getPure(a);
        for (Status s : getStatuses()) {
            total += s.mod(a);
        }
        total += body.mod(a, total);
        switch (a) {
            case Arcane:
                if (outfit.has(ClothingTrait.mystic)) {
                    total += 2;
                }
                if (has(Trait.kabbalah)) {
                    total += 10;
                }
                break;
            case Dark:
                if (outfit.has(ClothingTrait.broody)) {
                    total += 2;
                }
                if (has(Trait.fallenAngel)) {
                    total += 10;
                }
                break;
            case Ki:
                if (outfit.has(ClothingTrait.martial)) {
                    total += 2;
                }
                if (has(Trait.valkyrie)) {
                    total += 5;
                }
                break;
            case Fetish:
                if (outfit.has(ClothingTrait.kinky)) {
                    total += 2;
                }
                break;
            case Cunning:
                if (has(Trait.FeralAgility) && is(Stsflag.feral)) {
                    // extra 5 strength at 10, extra 17 at 60.
                    total += Math.pow(getProgression().getLevel(), .7);
                }
                break;
            case Power:
                if (has(Trait.testosterone) && hasDick()) {
                    total += Math.min(20, 10 + getProgression().getLevel() / 4);
                }
                if (has(Trait.FeralStrength) && is(Stsflag.feral)) {
                    // extra 5 strength at 10, extra 17 at 60.
                    total += Math.pow(getProgression().getLevel(), .7);
                }
                if (has(Trait.valkyrie)) {
                    total += 10;
                }
                break;
            case Science:
                if (has(ClothingTrait.geeky)) {
                    total += 2;
                }
                break;
            case Hypnosis:
                if (has(Trait.Illusionist)) {
                    total += getPure(Attribute.Arcane) / 2;
                }
                break;
            case Speed:
                if (has(ClothingTrait.bulky)) {
                    total -= 1;
                }
                if (has(ClothingTrait.shoes)) {
                    total += 1;
                }
                if (has(ClothingTrait.heels) && !has(Trait.proheels)) {
                    total -= 2;
                }
                if (has(ClothingTrait.highheels) && !has(Trait.proheels)) {
                    total -= 1;
                }
                if (has(ClothingTrait.higherheels) && !has(Trait.proheels)) {
                    total -= 1;
                }
                break;
            case Seduction:
                if (has(Trait.repressed)) {
                    total /= 2;
                }
                break;
            default:
                break;
        }
        return Math.max(0, total);
    }
    
    /**Determines if the Outfit has a given ClothingTrait attribute in the parameter.
     * 
     * FIXME: This should be renamed and merged/refactored with a clothingset. This level of access may not be necessary. - DSM
     * 
     * @param attribute
     * The ClothingTrait Attribute to be searched for. 
     * 
     *  @return
     *  Returns true if the outfit has the given attribute.  
     * */
    public final boolean has(ClothingTrait attribute) {
        return outfit.has(attribute);
    }

    /**Returns the unmodified value of a given attribute.
     * 
     * FIXME: This could be an accessor of an unmodified Attribute value, instead. - DSM 
     * 
     * @param a
     * The attribute to have its pure value calculated.
     * @return total
     * 
     * */
    public final int getPure(Attribute a) {
        int total = 0;
        if (att.containsKey(a) && !a.equals(Attribute.Willpower)) {
            total = att.get(a);
        }
        return total;
    }

    /**Checks the attribute against the difficulty class. Returns true if it passes.
     * 
     * NOTE: This class seems to be more like a debugging class. It should be moved. -DSM
     * FIXME: This should not be in character - as it's very useful in many other places. - DSM
     *  
     *  @param a
     *  The attribute to roll a check against
     *  
     *  @param dc
     *  The Difficulty Class to roll the dice against.
     *  
     *  @return
     *  Returns true if the roll beats the DC.
     * */
    public final boolean check(Attribute a, int dc) {
        int rand = Global.random(20);
        if (rand == 0) {
            // critical hit
            return true;
        }
        if (rand == 19) {
            // critical miss
            return false;
        }
        return get(a) != 0 && get(a) + rand >= dc;
    }

    public final Progression getProgression() {
        return progression;
    }

    /**Simple method for gaining the amount of exp given in i and updates the character accordingly.
     * Accounts for traits like fastlearner and Leveldrainer.
     * 
     * @param i
     * The value of experience to increment by.
     * */
    public final void gainXP(int i) {
        assert i >= 0;
        double rate = 1.0;
        if (has(Trait.fastLearner)) {
            rate += .2;
        }
        rate *= Global.xpRate;
        i = (int) Math.round(i * rate);

        progression.gainXP(i);
    }

    public final void rankup() {
        progression.setRank(progression.getRank() + 1);
    }

    public abstract void ding(Combat c);

    /**Modifies a given base damage value by a given parameters.
     * 
     * @param type
     * The damage type - used to obtain further information on defenses of both user and target.
     * 
     * @param other
     * The target of the damage. Their defenses influence the multiplier.
     * 
     * @param baseDamage
     * The base damage to be modified. 
     * 
     * @return 
     * Returns a minium value of a double calculated from a moderation between the maximum and minimum damage.
     *  
     *  */
    public final double modifyDamage(DamageType type, Character other, double baseDamage) {
        // so for each damage type, one level from the attacker should result in about 3% increased damage, while a point in defense should reduce damage by around 1.5% per level.
        // this differential should be max capped to (2 * (100 + attacker's level * 1.5))%
        // this differential should be min capped to (.5 * (100 + attacker's level * 1.5))%
        double maxDamage = baseDamage * 2 * (1 + .015 * getProgression().getLevel());
        double minDamage = baseDamage * .5 * (1 + .015 * getProgression().getLevel());
        double multiplier = (1 + .03 * getOffensivePower(type) - .015 * other.getDefensivePower(type));
        double damage = baseDamage * multiplier;
        return Math.min(Math.max(minDamage, damage), maxDamage);
    }

    /**Gets a defensive power value of this character bby a given DamageType. Each damage type in the game has a formula based on a value gotten from a character's Attribute.
     * 
     * @param type
     * The Damage type to check. 
     * @return
     * Returns a different value based upon the damage type.
     * */
    private final double getDefensivePower(DamageType type){
        switch (type) {
            case arcane:
                return get(Attribute.Arcane) + get(Attribute.Dark) / 2 + get(Attribute.Divinity) / 2 + get(Attribute.Ki) / 2;
            case biological:
                return get(Attribute.Animism) / 2 + get(Attribute.Bio) / 2 + get(Attribute.Medicine) / 2 + get(Attribute.Science) / 2 + get(Attribute.Cunning) / 2 + get(Attribute.Seduction) / 2;
            case pleasure:
                return get(Attribute.Seduction);
            case temptation:
                return (get(Attribute.Seduction) * 2 + get(Attribute.Submissive) * 2 + get(Attribute.Cunning)) / 2.0;
            case technique:
                return get(Attribute.Cunning);
            case physical:
                return (get(Attribute.Power) * 2 + get(Attribute.Cunning)) / 2.0;
            case gadgets:
                return get(Attribute.Cunning);
            case drain:
                return (get(Attribute.Dark) * 2 + get(Attribute.Arcane)) / 2.0;
            case stance:
                return (get(Attribute.Cunning) * 2 + get(Attribute.Power)) / 2.0;
            case weaken:
                return (get(Attribute.Dark) * 2 + get(Attribute.Divinity)) / 2.0;
            case willpower:
                return (get(Attribute.Dark) + get(Attribute.Fetish) + get(Attribute.Divinity) * 2 + getProgression().getLevel()) / 2.0;
            default:
                return 0;
        }
    }
    /**Gets an offensive power value of this character bby a given DamageType. Each damage type in the game has a formula based on a value gotten from a character's Attribute.
     * 
     * @param type
     * The Damage type to check. 
     * @return
     * Returns a different value based upon the damage type.
     * */
    private final double getOffensivePower(DamageType type){
        switch (type) {
            case biological:
                return (get(Attribute.Animism) + get(Attribute.Bio) + get(Attribute.Medicine) + get(Attribute.Science)) / 2;
            case gadgets:
                double power = (get(Attribute.Science) * 2 + get(Attribute.Cunning)) / 3.0;
                if (has(Trait.toymaster)) {
                    power += 20;
                }
                return power;
            case pleasure:
                return get(Attribute.Seduction);
            case arcane:
                return get(Attribute.Arcane);
            case temptation:
                return (get(Attribute.Seduction) * 2 + get(Attribute.Cunning)) / 3.0;
            case technique:
                return get(Attribute.Cunning);
            case physical:
                return (get(Attribute.Power) * 2 + get(Attribute.Cunning) + get(Attribute.Ki) * 2) / 3.0;
            case drain:
                return (get(Attribute.Dark) * 2 + get(Attribute.Arcane)) / (has(Trait.gluttony) ? 1.5 : 2.0);
            case stance:
                return (get(Attribute.Cunning) * 2 + get(Attribute.Power)) / 3.0;
            case weaken:
                return (get(Attribute.Dark) * 2 + get(Attribute.Divinity) + get(Attribute.Ki)) / 3.0;
            case willpower:
                return (get(Attribute.Dark) + get(Attribute.Fetish) + get(Attribute.Divinity) * 2 + getProgression().getLevel()) / 3.0;
            default:
                return 0;
        }
    }
    
    /**Recursive? half-method for dealing with pain. Calls pain with a different Signature.
     *  
     *  TODO: Someone explain this implementation.
     *  
     * */
    public final void pain(Combat c, Character other, int i) {
        pain(c, other, i, true, true);
    }

    /**Recursive? half-method for dealing with pain. Processes pain considering several traits, attributes and positions.
     *  
     * @param c
     * The combat to make use of this method.
     * 
     * @param other
     * The opponent.
     * 
     * @param i 
     * The value of pain.
     * 
     * @param primary
     * 
     * @param physical
     * Indicates if hte pain is physical.
     *
     * */
    public final void pain(Combat c, Character other, int i, boolean primary, boolean physical) {
        int pain = i;
        int bonus = 0;
        if (is(Stsflag.rewired) && physical) {
            String message = String.format("%s pleasured for <font color='rgb(255,50,200)'>%d<font color='white'>\n",
                            Global.capitalizeFirstLetter(subjectWas()), pain);
            if (c != null) {
                c.writeSystemMessage(message, true);
            }
            arouse(pain, c);
            return;
        }
        if (has(Trait.slime)) {
            bonus += Slime.painModifier(pain);
            if (c != null) {
                c.write(this, Slime.textOnPain(getGrammar()));
            }
        }
        if (c != null) {
            if (has(Trait.cute) && other != null && other != this && primary && physical) {
                bonus += Cute.painModifier(this, pain);
                c.write(this, Cute.textOnPain(this.getGrammar(), other.getGrammar()));
            }
            if (other != null && other != this && other.has(Trait.dirtyfighter)
                    && (c.getStance().prone(other) || c.getStance().sub(other))
                    && physical) {
                bonus += DirtyFighter.painModifier();
                c.write(this, DirtyFighter.textOnPain(this.getGrammar(), other.getGrammar()));
            }

            if (has(Trait.sacrosanct) && physical && primary) {
                c.write(this, Global.format(
                                "{other:SUBJECT-ACTION:well|wells} up with guilt at hurting such a holy being. {self:PRONOUN-ACTION:become|becomes} temporarily untouchable in {other:possessive} eyes.",
                                this, other));
                add(c, new Alluring(this, 1));
            }
            for (Status s : getStatuses()) {
                bonus += s.damage(c, pain);
            }
        }
        pain += bonus;
        pain = Math.max(1, pain);
        emote(Emotion.angry, pain / 3);

        // threshold at which pain calms you down
        int painAllowance = Math.max(10, getStamina().max() / 6);
        int arousalLoss = pain - painAllowance;
        if (other != null && other.has(Trait.wrassler)) {
            arousalLoss = Wrassler.inflictedPainArousalLossModifier(pain, painAllowance);
        }
        if (arousalLoss > 0 && !is(Stsflag.masochism)) {
            calm(c, arousalLoss);
        }
        // if the pain exceeds the threshold and you aren't a masochist
        // calm down by the overflow
        if (c != null) {
            c.writeSystemMessage(String.format("%s hurt for <font color='rgb(250,10,10)'>%d<font color='white'>",
                subjectWas(), pain), true);
        }
        if (other != null && other.has(Trait.sadist) && !is(Stsflag.masochism)) {
            c.write("<br/>"+Global.capitalizeFirstLetter(
                            String.format("%s blows hits all the right spots and %s to some masochistic tendencies.", 
                                            other.nameOrPossessivePronoun(), subjectAction("awaken"))));
            add(c, new Masochistic(this));
        }
        // if you are a masochist, arouse by pain up to the threshold.
        if (is(Stsflag.masochism) && physical) {
            this.arouse(Math.max(i, painAllowance), c);
        }
        if (other != null && other.has(Trait.disablingblows) && Global.random(5) == 0) {
            int mag = Global.random(3) + 1;
            c.write(other, Global.format("Something about the way {other:subject-action:hit|hits}"
                            + " {self:name-do} seems to strip away {self:possessive} strength.", this, other));
            add(c, new Abuff(this, Attribute.Power, -mag, 10));
        }
        stamina.exhaust(pain);
    }

    /**Drains this character's stamina by value i.
     * 
     * @param c
     * The combat that requires this method.
     * 
     * @param drainer
     * the character that is performing the drain on this character.
     * 
     * @param i
     * The base value to drain this character's stamina.
     * */
    public final void drain(Combat c, Character drainer, int i) {
        int drained = i;
        int bonus = 0;

        for (Status s : getStatuses()) {
            bonus += s.drained(c, drained);
        }
        drained += bonus;
        if (drained >= stamina.get()) {
            drained = stamina.get();
        }
        if (drained > 0) {
            if (c != null) {
                c.writeSystemMessage(
                                String.format("%s drained of <font color='rgb(200,200,200)'>%d<font color='white'> stamina by %s",
                                                subjectWas(), drained, drainer.subject()), true);
            }
            stamina.exhaust(drained);
            drainer.stamina.recover(drained);
        }
    }

    /**Weaken's this character's Stamina by value i.
     * 
     * @param c
     * The combat requiring this method. 
     * 
     * @param i 
     * The base value, which is modified by bonuses.
     * 
     * */
    public final void weaken(Combat c, final int i) {
        int weak = i;
        int bonus = 0;
        for (Status s : getStatuses()) {
            bonus += s.weakened(c, i);
        }
        weak += bonus;
        weak = Math.max(1, weak);
        if (weak >= stamina.get()) {
            weak = stamina.get();
        }
        if (weak > 0) {
            if (c != null) {
                c.writeSystemMessage(String.format("%s weakened by <font color='rgb(200,200,200)'>%d<font color='white'>",
                                subjectWas(), weak), true);
            }
            stamina.exhaust(weak);
        }
    }

    public final void heal(Combat c, int i) {
        heal(c, i, "");
    }
    public final void heal(Combat c, int i, String reason) {
        i = Math.max(1, i);
        if (c != null) {
            c.writeSystemMessage(String.format("%s healed for <font color='rgb(100,240,30)'>%d<font color='white'>%s",
                            subjectWas(), i, reason), true);
        }
        stamina.recover(i);
    }

    public final String subject() {
        return getGrammar().subject().defaultNoun();
    }

    public final int pleasure(int i, Combat c, Character source) {
        return resolvePleasure(i, c, source, Body.nonePart, Body.nonePart);
    }

    public final int resolvePleasure(int i, Combat c, Character source, BodyPart selfPart, BodyPart opponentPart) {
        int pleasure = i;

        emote(Emotion.horny, i / 4 + 1);
        if (pleasure < 1) {
            pleasure = 1;
        }
        pleasured = true;
        // pleasure = 0;
        arousal.pleasure(pleasure);
        if (checkOrgasm()) {
            doOrgasm(c, source, selfPart, opponentPart);
        }
        return pleasure;
    }

    public final void temptNoSkillNoTempter(Combat c, int i) {
        temptNoSkillNoSource(c, null, i);
    }

    public final void temptNoSkillNoSource(Combat c, Character tempter, int i) {
        tempt(c, tempter, null, i, Optional.empty());
    }

    public final void temptNoSource(Combat c, Character tempter, int i, Skill skill) {
        tempt(c, tempter, null, i, Optional.ofNullable(skill));
    }

    public final void temptNoSkill(Combat c, Character tempter, BodyPart with, int i) {
        tempt(c, tempter, with, i, Optional.empty());
    }

    public final void temptWithSkill(Combat c, Character tempter, BodyPart with, int i, Skill skill) {
        tempt(c, tempter, with, i, Optional.ofNullable(skill));
    }

    /**Tempts this character with a bodypart, accounting for various skills, the opponent, traits and statuses.
     * 
     * FIXME: This is entirely too long, and would be a good opportunity for cleaning up. Several processes are at work, here, and Objectifying Skills would contribute to cleaning this up. - DSM
     *  
     * @param c
     * The combar requiring this method.
     * 
     * @param tempter
     * The character tempting this character.
     * 
     * @param with
     * The bodypart they are tempting this character with. 
     * 
     * @param i
     * The base tempt value?
     * 
     * @param skillOptional
     *  An optional Skill.
     * 
     * */
    public final void tempt(Combat c, Character tempter, BodyPart with, int i, Optional<Skill> skillOptional) {
        String extraMsg = "";
        double baseModifier = 1.0;
        if (has(Trait.oblivious)) {
            extraMsg += " (Oblivious)";
            baseModifier *= .1;
        }
        if (has(Trait.Unsatisfied) && (getArousal().percent() >= 50 || getWillpower().percent() < 25)) {
            extraMsg += " (Unsatisfied)";
            if (c != null && c.getOpponentCharacter(this).human()) {
                baseModifier *= .2;
            } else {
                baseModifier *= .66;
            }
        }

        int bonus = 0;
        for (Status s : getStatuses()) {
            bonus += s.tempted(c, i);
        }

        if (has(Trait.desensitized2)) {
            bonus -= i / 2;
        }

        String bonusString = "";
        if (bonus > 0) {
            bonusString = String.format(" + <font color='rgb(240,60,220)'>%d<font color='white'>", bonus);
        } else if (bonus < 0) {
            bonusString = String.format(" - <font color='rgb(120,180,200)'>%d<font color='white'>", Math.abs(bonus));
        }

        if (tempter != null) {
            int dmg;
            String message;
            double temptMultiplier = baseModifier;
            double stalenessModifier = 1.0;
            String stalenessString = "";

            if (skillOptional.isPresent()) {
                stalenessModifier = c.getCombatantData(skillOptional.get().getSelf()).getMoveModifier(skillOptional.get());
                if (Math.abs(stalenessModifier - 1.0) >= .1 ) {
                    stalenessString = String.format(", staleness: %.1f", stalenessModifier);
                }
            }

            if (with != null) {
                // triple multiplier for the body part
                temptMultiplier *= tempter.body.getCharismaBonus(c, this) + with.getHotness(tempter, this) * 2;
                dmg = (int) Math.max(0, Math.round((i + bonus) * temptMultiplier * stalenessModifier));
                if (Global.checkFlag(Flag.basicSystemMessages)) {
                    message = String.format("%s tempted by %s %s for <font color='rgb(240,100,100, arg1)'>%d"
                                    + "<font color='white'>\n", 
                                  Global.capitalizeFirstLetter(tempter.subject()),
                                  tempter.nameOrPossessivePronoun(), with.describe(tempter), dmg);
                } else {
                    message = String.format(
                                    "%s tempted by %s %s for <font color='rgb(240,100,100)'>%d<font color="
                                    + "'white'> (base:%d%s, charisma:%.1f%s)%s\n",
                                    Global.capitalizeFirstLetter(subjectWas()), tempter.nameOrPossessivePronoun(),
                                    with.describe(tempter), dmg, i, bonusString, temptMultiplier, stalenessString, extraMsg);
                    
                }
            } else {
                temptMultiplier *= tempter.body.getCharismaBonus(c, this);
                if (c != null && tempter.has(Trait.obsequiousAppeal)
                    && c.getStance().sub(tempter)) {
                    temptMultiplier *= 2;
                }
                dmg = Math.max((int) Math.round((i + bonus) * temptMultiplier * stalenessModifier), 0);
                if (Global.checkFlag(Flag.basicSystemMessages)) {
                    message = String.format("%s tempted %s for <font color='rgb(240,100,100, arg1)'>%d"
                                    + "<font color='white'>\n", 
                                  Global.capitalizeFirstLetter(tempter.subject()),
                                  tempter == this ? reflexivePronoun() : nameDirectObject(), dmg);
                } else {
                    message = String.format(
                             "%s tempted %s for <font color='rgb(240,100,100)'>%d<font color='white'> "
                             + "(base:%d%s, charisma:%.1f%s)%s\n",
                              Global.capitalizeFirstLetter(tempter.subject()),
                              tempter == this ? reflexivePronoun() : nameDirectObject(),
                              dmg, i, bonusString, temptMultiplier, stalenessString, extraMsg);
                    
                }
            }
            if (c != null) {
                c.writeSystemMessage(message, Global.checkFlag(Flag.basicSystemMessages));
            }
            tempt(dmg);

            if (tempter.has(Trait.mandateOfHeaven)) {
                double arousalPercent = dmg / getArousal().max() * 100;
                CombatantData data = c.getCombatantData(this);
                data.setDoubleFlag(Combat.TEMPT_WORSHIP_BONUS, data.getDoubleFlag(Combat.TEMPT_WORSHIP_BONUS) + arousalPercent);
                double newWorshipBonus = data.getDoubleFlag(Combat.TEMPT_WORSHIP_BONUS);
                if (newWorshipBonus < 10 ) {
                    // nothing yet?
                } else if (newWorshipBonus < 25) {
                    c.write(tempter, Global.format("There's a nagging urge for {self:name-do} to throw {self:reflective} at {other:name-possessive} feet and beg for release.", this, tempter));
                } else if (newWorshipBonus < 50) {
                    c.write(tempter, Global.format("{self:SUBJECT-ACTION:feel|feels} an urge to throw {self:reflective} at {other:name-possessive} feet and beg for release.", this, tempter));
                } else {
                    c.write(tempter, Global.format("{self:SUBJECT-ACTION:are|is} feeling an irresistable urge to throw {self:reflective} at {other:name-possessive} feet and beg for release.", this, tempter));
                }
            }
        } else {
            int damage = Math.max(0, (int) Math.round((i + bonus) * baseModifier));
            if (c != null) {
                c.writeSystemMessage(
                                String.format("%s tempted for <font color='rgb(240,100,100)'>%d<font color='white'>%s\n",
                                                subjectWas(), damage, extraMsg), false);
            }
            tempt(damage);
        }
    }

    public final void arouse(int i, Combat c) {
        arouse(i, c, "");
    }
    
    /**Half-recursive method for arousing this character. Performs the heavy lifting of arounse (int, combat)
     * @param i
     * The base value of arousal. 
     * @param c
     * The combat required for this function.
     * @param source
     * The source of the arousal damage.
     * */
    public final void arouse(int i, Combat c, String source) {
        String extraMsg = "";
        if (has(Trait.Unsatisfied) && (getArousal().percent() >= 50 || getWillpower().percent() < 25)) {
            extraMsg += " (Unsatisfied)";
            // make it much less effective vs NPCs because they're bad at exploiting the weakness
            if (c != null && c.getOpponentCharacter(this).human()) {
                i = Math.max(1, i / 5);
            } else {
                i = Math.max(1, i * 2 / 3);
            }
        }
        String message = String.format("%s aroused for <font color='rgb(240,100,100)'>%d<font color='white'> %s%s\n",
                        Global.capitalizeFirstLetter(subjectWas()), i, source, extraMsg);
        if (c != null) {
            c.writeSystemMessage(message, true);
        }
        tempt(i);
    }

    public String subjectAction(String verb, String pluralverb) {
        return subject() + " " + pluralverb;
    }

    public String subjectAction(String verb) {
        return subjectAction(verb, ProseUtils.getThirdPersonFromFirstPerson(verb));
    }

    protected String subjectWas() {
        return subject() + " was";
    }
    
    /**Tempts this character. Simple method valled by many other classes to do a simple amount of arousal to this character.
     * 
     * @param i
     * The base value. 
     * */
    public void tempt(int i) {
        int temptation = i;
        int bonus = 0;

        emote(Emotion.horny, i / 4);
        arousal.pleasure(temptation);
    }
    
    /**Calms this character. 
     * @param c 
     * The combat that this method requires.
     * @param i 
     * The base base value.
     * 
     * */
    public void calm(Combat c, int i) {
        if (i > 0) {
            if (c != null) {
                String message = String.format("%s calmed down by <font color='rgb(80,145,200)'>%d<font color='white'>\n",
                                Global.capitalizeFirstLetter(subjectAction("have", "has")), i);
                c.writeSystemMessage(message, true);
            }
            arousal.calm(i);
        }
    }

    public StaminaStat getStamina() {
        return stamina;
    }

    public ArousalStat getArousal() {
        return arousal;
    }

    public MojoStat getMojo() {
        return mojo;
    }

    public WillpowerStat getWillpower() {
        return willpower;
    }

    public void buildMojo(Combat c, int percent) {
        buildMojo(c, percent, "");
    }

    /**Builds this character's mojo based upon percentage and source.
     * 
     * @param percent
     * The base percentage of mojo to gain.
     * 
     * @param source
     * The source of Mojo gain.
     * 
     * */
    public void buildMojo(Combat c, int percent, String source) {
        if (Dominance.mojoIsBlocked(this, c)) {
            c.write(c.getOpponentCharacter(this),
                            String.format("Enraptured by %s display of dominance, %s no mojo.", 
                                            c.getOpponentCharacter(this).nameOrPossessivePronoun(), subjectAction("build")));
            return;
        }
        
        int x = percent * Math.min(mojo.max(), 200) / 100;
        int bonus = 0;
        for (Status s : getStatuses()) {
            bonus += s.gainmojo(x);
        }
        x += bonus;
        if (x > 0) {
            mojo.build(x);
            if (c != null) {
                c.writeSystemMessage(Global.capitalizeFirstLetter(
                                String.format("%s <font color='rgb(100,200,255)'>%d<font color='white'> mojo%s.",
                                                subjectAction("built", "built"), x, source)), true);
            }
        } else if (x < 0) {
            loseMojo(c, x);
        }
    }

    public void spendMojo(Combat c, int i) {
        spendMojo(c, i, "");
    }

    public void spendMojo(Combat c, int i, String source) {
        int cost = i;
        int bonus = 0;
        for (Status s : getStatuses()) {
            bonus += s.spendmojo(i);
        }
        cost += bonus;
        mojo.deplete(cost);
        if (c != null && i != 0) {
            c.writeSystemMessage(Global.capitalizeFirstLetter(
                            String.format("%s <font color='rgb(150,150,250)'>%d<font color='white'> mojo%s.",
                                            subjectAction("spent", "spent"), cost, source)), true);
        }
    }

    public int loseMojo(Combat c, int i) {
        return loseMojo(c, i, "");
    }

    public int loseMojo(Combat c, int i, String source) {
        mojo.deplete(i);
        if (c != null) {
            c.writeSystemMessage(Global.capitalizeFirstLetter(
                            String.format("%s <font color='rgb(150,150,250)'>%d<font color='white'> mojo%s.",
                                            subjectAction("lost", "lost"), i, source)), true);
        }
        return i;
    }

    public Area location() {
        return location.get();
    }

    public int init() {
        return att.get(Attribute.Speed) + Global.random(10);
    }

    public boolean reallyNude() {
        return topless() && pantsless();
    }

    public boolean torsoNude() {
        return topless() && pantsless();
    }

    public boolean mostlyNude() {
        return breastsAvailable() && crotchAvailable();
    }

    public boolean breastsAvailable() {
        return outfit.slotOpen(ClothingSlot.top);
    }

    public boolean crotchAvailable() {
        return outfit.slotOpen(ClothingSlot.bottom);
    }

    public void dress(Combat c) {
        outfit.dress(c.getCombatantData(this).getClothespile());
    }

    public void change() {
        outfit.undress();
        outfit.dress(outfitPlan);
        if (Global.getMatch() != null) {
            Global.getMatch().getCondition().handleOutfit(this);
        }
    }

    public String getName() {
        Disguised disguised = (Disguised) getStatus(Stsflag.disguised);
        if (disguised != null) {
            return disguised.getTarget().getTrueName();
        }
        return name;
    }

    public void completelyNudify(Combat c) {
        List<Clothing> articles = outfit.undress();
        if (c != null) {
            articles.forEach(article -> c.getCombatantData(this).addToClothesPile(this, article));
        }
    }

    /* undress without any modifiers */
    public void undress(Combat c) {
        if (!breastsAvailable() || !crotchAvailable()) {
            // first time only strips down to what blocks fucking
            outfit.strip().forEach(article -> c.getCombatantData(this).addToClothesPile(this, article));
        } else {
            // second time strips down everything
            outfit.undress().forEach(article -> c.getCombatantData(this).addToClothesPile(this, article));
        }
    }

    /* undress non indestructibles */
    public boolean nudify() {
        if (!breastsAvailable() || !crotchAvailable()) {
            // first time only strips down to what blocks fucking
            outfit.forcedstrip();
        } else {
            // second time strips down everything
            outfit.undressOnly(c -> !c.is(ClothingTrait.indestructible));
        }
        return mostlyNude();
    }

    public Clothing strip(Clothing article, Combat c) {
        if (article == null) {
            return null;
        }
        Clothing res = outfit.unequip(article);
        c.getCombatantData(this).addToClothesPile(this, res);
        return res;
    }

    public Clothing strip(ClothingSlot slot, Combat c) {
        return strip(outfit.getTopOfSlot(slot), c);
    }

    public Clothing stripRandom(Combat c) {
        return stripRandom(c, false);
    }

    public void gainTrophy(Combat c, Character target) {
        Optional<Clothing> underwear = target.outfitPlan.stream()
                        .filter(article -> article.getSlots().contains(ClothingSlot.bottom) && article.getLayer() == 0)
                        .findFirst();
        if (!underwear.isPresent() || c.getCombatantData(target).getClothespile().contains(underwear.get())) {
            this.gain(target.getTrophy());
        }
    }

    public Clothing shredRandom() {
        ClothingSlot slot = outfit.getRandomShreddableSlot();
        if (slot != null) {
            return shred(slot);
        }
        return null;
    }

    public boolean topless() {
        return outfit.slotEmpty(ClothingSlot.top);
    }

    public boolean pantsless() {
        return outfit.slotEmpty(ClothingSlot.bottom);
    }

    public Clothing stripRandom(Combat c, boolean force) {
        return strip(force ? outfit.getRandomEquippedSlot() : outfit.getRandomNakedSlot(), c);
    }

    public Clothing getRandomStrippable() {
        ClothingSlot slot = getOutfit().getRandomEquippedSlot();
        return slot == null ? null : getOutfit().getTopOfSlot(slot);
    }

    public Clothing shred(ClothingSlot slot) {
        Clothing article = outfit.getTopOfSlot(slot);
        if (article == null || article.is(ClothingTrait.indestructible)) {
            System.err.println("Tried to shred clothing that doesn't exist at slot " + slot.name() + " at clone "
                            + cloned);
            System.err.println(outfit.toString());
            Thread.dumpStack();
            return null;
        } else {
            // don't add it to the pile
            return outfit.unequip(article);
        }
    }

    private void countdown(Map<Trait, Integer> counters) {
        Iterator<Map.Entry<Trait, Integer>> it = counters.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Trait, Integer> ent = it.next();
            int remaining = ent.getValue() - 1;
            if (remaining > 0) {
                ent.setValue(remaining);
            } else {
                it.remove();
            }
        }
    }

    public void tick(Combat c) {            
        body.tick(c);
        countdown(temporaryAddedTraits);
        countdown(temporaryRemovedTraits);
    }

    public Collection<Trait> getTraits() {
        Collection<Trait> allTraits = new HashSet<>();
        allTraits.addAll(traits);
        allTraits.addAll(temporaryAddedTraits.keySet());
        allTraits.removeAll(temporaryRemovedTraits.keySet());
        return allTraits;
    }
    
    public void clearTraits() {
        List<Trait> traitsToRemove = new ArrayList<>(traits);
        traitsToRemove.forEach(this::removeTraitDontSaveData);
    }

    public Collection<Trait> getTraitsPure() {
        return Collections.unmodifiableCollection(traits);
    }

    public boolean addTemporaryTrait(Trait t, int duration) {
        if (!getTraits().contains(t)) {
            temporaryAddedTraits.put(t, duration);
            return true;
        } else if (temporaryAddedTraits.containsKey(t)) {
            temporaryAddedTraits.put(t, Math.max(duration, temporaryAddedTraits.get(t)));
            return true;
        }
        return false;
    }

    public boolean removeTemporarilyAddedTrait(Trait t) {
        if (temporaryAddedTraits.containsKey(t)) {
            temporaryAddedTraits.remove(t);
            return true;
        }
        return false;
    }

    public boolean removeTemporaryTrait(Trait t, int duration) {
        if (temporaryRemovedTraits.containsKey(t)) {
            temporaryRemovedTraits.put(t, Math.max(duration, temporaryRemovedTraits.get(t)));
            return true;
        } else if (traits.contains(t)) {
            temporaryRemovedTraits.put(t, duration);
            return true;
        }
        return false;
    }

    public LevelUpData getLevelUpFor(int level) {
        levelPlan.putIfAbsent(level, new LevelUpData());
        return levelPlan.get(level);
    }

    public void modAttributeDontSaveData(Attribute a, int i) {
        modAttributeDontSaveData(a, i, false);
    }

    public void modAttributeDontSaveData(Attribute a, int i, boolean silent) {
        if (human() && i != 0 && !silent && cloned == 0) {
            Global.writeIfCombat(Global.gui().combat, this,
                    "You have " + (i > 0 ? "gained" : "lost") + " " + Math.abs(i) + " " + a.name());
            Global.updateIfCombat(Global.gui().combat);
        }
        if (a.equals(Attribute.Willpower)) {
            getWillpower().gain(i * 2);
        } else {
            att.put(a, att.getOrDefault(a, 0) + i);
        }
    }

    public void mod(Attribute a, int i) {
        mod(a, i, false);
    }

    public void mod(Attribute a, int i, boolean silent) {
        modAttributeDontSaveData(a, i, silent);
        getLevelUpFor(getProgression().getLevel()).modAttribute(a, i);
    }

    public boolean addTraitDontSaveData(Trait t) {
        if (t == null) {
            System.err.println("Tried to add an null trait!");
            DebugHelper.printStackFrame(5, 1);
            return false;
        }
        if (traits.addIfAbsent(t)) {
            if (t.equals(Trait.mojoMaster)) {
                mojo.gain(20);
            }
            return true;
        }
        return false;
    }

    public boolean add(Trait t) {
        if (addTraitDontSaveData(t)) {
            getLevelUpFor(getProgression().getLevel()).addTrait(t);
            return true;
        }
        return false;
    }

    public boolean removeTraitDontSaveData(Trait t) {
        if (traits.remove(t)) {
            if (t.equals(Trait.mojoMaster)) {
                mojo.gain(-20);
            }
            return true;
        }
        return false;
    }

    public boolean remove(Trait t) {
        if (removeTraitDontSaveData(t)) {
            getLevelUpFor(getProgression().getLevel()).removeTrait(t);
            return true;
        }
        return false;
    }

    public boolean hasPure(Trait t) {
        return getTraits().contains(t);
    }

    public boolean has(Trait t) {
        boolean hasTrait = false;
        if (t.parent != null) {
            hasTrait = getTraits().contains(t.parent);
        }
        if (outfit.has(t)) {
            return true;
        }
        hasTrait = hasTrait || hasPure(t);
        return hasTrait;
    }

    public boolean hasDick() {
        return body.getRandomCock() != null;
    }

    public boolean hasBalls() {
        return body.getRandomBalls() != null;
    }

    public boolean hasPussy() {
        return body.getRandomPussy() != null;
    }

    public boolean hasBreasts() {
        return body.getRandomBreasts() != null;
    }

    public int countFeats() {
        int count = 0;
        for (Trait t : traits) {
            if (t.isFeat()) {
                count++;
            }
        }
        return count;
    }

    public void regen() {
        regen(null, false);
    }

    public void regen(Combat c) {
        regen(c, true);
    }

    public void regen(Combat c, boolean combat) {
        getAddictions().forEach(Addiction::refreshWithdrawal);
        int regen = 1;
        // TODO can't find the concurrent modification error, just use a copy
        // for now I guess...
        for (Status s : new HashSet<>(getStatuses())) {
            regen += s.regen(c);
        }
        if (has(Trait.BoundlessEnergy)) {
            regen += 1;
        }
        if (regen > 0) {
            heal(c, regen);
        } else {
            weaken(c, -regen);
        }
        if (combat) {
            if (has(Trait.exhibitionist) && mostlyNude()) {
                buildMojo(c, 5);
            }
            if (outfit.has(ClothingTrait.stylish)) {
                buildMojo(c, 1);
            }
            if (has(Trait.SexualGroove)) {
                buildMojo(c, 3);
            }
            if (outfit.has(ClothingTrait.lame)) {
                buildMojo(c, -1);
            }
        }
    }

    public void preturnUpkeep() {
        orgasmed = false;
    }

    public void addNonCombat(nightgames.match.Status status) {
        add(null, status.inflictedStatus);
    }

    public boolean has(Status status) {
        return this.status.stream().anyMatch(s -> s.flags().containsAll(status.flags()) && status.flags()
                        .containsAll(status.flags()) && s.getClass().equals(status.getClass()) && s.getVariant().equals(status.getVariant()));
    }

    public void add(Combat c, Status status) {
        boolean cynical = false;
        String message = "";
        boolean done = false;
        Status effectiveStatus = status;
        for (Status s : getStatuses()) {
            if (s.flags().contains(Stsflag.cynical)) {
                cynical = true;
            }
        }
        if (cynical && status.mindgames()) {
            message = subjectAction("resist", "resists") + " " + status.name + " (Cynical).";
            done = true;
        } else {
            for (Resistance r : getResistances(c)) {
                String resistReason = "";
                resistReason = r.resisted(c, this, status);
                if (!resistReason.isEmpty()) {
                    message = subjectAction("resist", "resists") + " " + status.name + " (" + resistReason + ").";
                    done = true;
                    break;
                }
            }
        }
        if (!done) {
            boolean unique = true;
            for (Status s : this.status) {
                if (s.getClass().equals(status.getClass())
                    && s.getVariant().equals(status.getVariant())) {
                    s.replace(status);
                    message = s.initialMessage(c, Optional.of(status));
                    done = true;
                    effectiveStatus = s;
                    break;
                }
                if (s.overrides(status)) {
                    unique = false;
                }
            }
            if (!done && unique) {
                this.status.add(status);
                message = status.initialMessage(c, Optional.empty());
                done = true;
            }
        }
        if (done) {
            if (!message.isEmpty()) {
                message = Global.capitalizeFirstLetter(message);
                if (c != null) {
                    if (!c.getOpponentCharacter(this).human() || !c.getOpponentCharacter(this).is(Stsflag.blinded)) {
                        c.write(this, "<b>" + message + "</b>");
                    }
                    effectiveStatus.onApply(c, c.getOpponentCharacter(this));
                } else if (human() || location() != null && location().humanPresent()) {
                    Global.gui().message("<b>" + message + "</b>");
                    effectiveStatus.onApply(null, null);
                }
            }
        }
    }

    private double getPheromonesChance(Combat c) {
        double baseChance = .1 + getExposure() / 3 + (arousal.getOverflow() + arousal.get()) / (float) arousal.max();
        double mod = c.getStance().pheromoneMod(this);
        if (has(Trait.FastDiffusion)) {
            mod = Math.max(2, mod);
        }
        return Math.min(1, baseChance * mod);
    }

    public boolean rollPheromones(Combat c) {
        double chance = getPheromonesChance(c);
        double roll = Global.randomdouble();
        // System.out.println("Pheromones: rolled " + Global.formatDecimal(roll)
        // + " vs " + chance + ".");
        return roll < chance;
    }

    public int getPheromonePower() {
        //return (int) (2 + Math.sqrt(get(Attribute.Animism) + get(Attribute.Bio)) / 2);
    	return 5;
    }

    public void dropStatus(Combat c, Character opponent) {
        Set<Status> removedStatuses = status.stream().filter(s -> !s.meetsRequirements(c, this, opponent))
                        .collect(Collectors.toSet());
        removedStatuses.addAll(removelist);
        removedStatuses.forEach(s -> {
            s.onRemove(c, opponent);
        });
        status.removeAll(removedStatuses);
        for (Status s : addlist) {
            add(c, s);
        }
        removelist.clear();
        addlist.clear();
    }

    public void removeStatusNoSideEffects() {
        status.removeAll(removelist);
        removelist.clear();
    }

    public boolean is(Stsflag sts) {
        if (statusFlags.contains(sts))
            return true;
        for (Status s : getStatuses()) {
            if (s.flags().contains(sts)) {
                return true;
            }
        }
        return false;
    }

    public boolean is(Stsflag sts, String variant) {
        for (Status s : getStatuses()) {
            if (s.flags().contains(sts) && s.getVariant().equals(variant)) {
                return true;
            }
        }
        return false;
    }

    public boolean stunned() {
        for (Status s : getStatuses()) {
            if (s.flags().contains(Stsflag.stunned) || s.flags().contains(Stsflag.falling)) {
                return true;
            }
        }
        return false;
    }

    public boolean distracted() {
        for (Status s : getStatuses()) {
            if (s.flags().contains(Stsflag.distracted) || s.flags().contains(Stsflag.trance)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasStatus(Stsflag flag) {
        for (Status s : getStatuses()) {
            if (s.flags().contains(flag)) {
                return true;
            }
        }
        return false;
    }

    public void removeStatus(Status status) {
        removelist.add(status);
    }

    public void removeStatus(Stsflag flag) {
        for (Status s : getStatuses()) {
            if (s.flags().contains(flag)) {
                removelist.add(s);
            }
        }
    }

    public boolean bound() {
        return is(Stsflag.bound);
    }

    public void free() {
        for (Status s : getStatuses()) {
            if (s.flags().contains(Stsflag.bound)) {
                removelist.add(s);
            }
        }
    }

    public void struggle() {
        for (Status s : getStatuses()) {
            s.struggle(this);
        }
    }

    /**Returns the chance of escape for this character based upon a total of bonuses from traits and stances.*/
    public int getEscape(Combat c, Character from) {
        int total = 0;
        for (Status s : getStatuses()) {
            total += s.escape();
        }
        if (has(Trait.freeSpirit)) {
            total += 5;
        }
        if (has(Trait.Slippery)) {
            total += 10;
        }
        if (from != null) {
            if (from.has(Trait.Clingy)) {
                total -= 5;
            }
            if (from.has(Trait.FeralStrength) && from.is(Stsflag.feral)) {
                total -= 5;
            }
        }
        if (c != null && checkAddiction(AddictionType.DOMINANCE, c.getOpponentCharacter(this))) {
            total -= getAddiction(AddictionType.DOMINANCE).get().getCombatSeverity().ordinal() * 8;
        }
        if (has(Trait.FeralStrength) && is(Stsflag.feral)) {
            total += 5;
        }
        if (c != null) {
            int stanceMod = c.getStance().getEscapeMod(c, this);
            if (stanceMod < 0) {
                if (bound()) {
                    total += stanceMod / 2;
                } else {
                    total += stanceMod;
                }
            }
        }
        return total;
    }

    public boolean canMasturbate() {
        return !(stunned() || bound() || is(Stsflag.distracted) || is(Stsflag.enthralled));
    }

    public boolean canAct() {
        return !(stunned() || distracted() || bound() || is(Stsflag.enthralled));
    }

    public boolean canRespond() {
        return !(stunned() || distracted() || is(Stsflag.enthralled));
    }

    public abstract String describe(int per, Character observer);

    public abstract boolean resist3p(Combat c, Character target, Character assist);

    public void displayStateMessage(Optional<Trap.Instance> knownTrap) {};

    /**abstract method for determining if this character is human - meaning the player.
     * TODO: Reccomend renaming to isHuman(), to make more meaningful name and easier to find.*/
    public abstract boolean human();

    public abstract String bbLiner(Combat c, Character target);

    public abstract String nakedLiner(Combat c, Character target);

    public abstract String stunLiner(Combat c, Character target);

    public abstract String taunt(Combat c, Character target);

    /**Determines if this character is controlled by a human.
     * 
     * NOTE: Since there are currently no mechanisms available to control another character, a simple boolean for isPLayer might suffice. 
     * 
     * @param c
     * The combat required for this method.
     * */
    public boolean humanControlled(Combat c) {
        return human();
    }

    private static final String jsCoreStats = "coreStats";
    private static final String jsStamina = "stamina";
    private static final String jsArousal = "arousal";
    private static final String jsMojo = "mojo";
    private static final String jsWillpower = "willpower";


    /**Saves this character using a JsonObject.  
     * 
     * This currently creates a large amount of sprawl in the save file, but moving towards object-oriented packages of members or XML may help. - DSM
     * 
     * */
    public JsonObject save() {
        JsonObject saveObj = new JsonObject();
        saveObj.addProperty("name", name);
        saveObj.addProperty("type", getType());
        saveObj.add(JSON_PROGRESSION, progression.save());
        saveObj.addProperty("money", money);
        {
            JsonObject jsCoreStats = new JsonObject();
            jsCoreStats.add(jsStamina, stamina.save());
            jsCoreStats.add(jsArousal, arousal.save());
            jsCoreStats.add(jsMojo,  mojo.save());
            jsCoreStats.add(jsWillpower, willpower.save());
            saveObj.add(Character.jsCoreStats, jsCoreStats);
        }
        saveObj.add("affections", JsonUtils.JsonFromMap(affections));
        saveObj.add("attractions", JsonUtils.JsonFromMap(attractions));
        saveObj.add("attributes", JsonUtils.JsonFromMap(att));
        saveObj.add("outfit", JsonUtils.jsonFromCollection(outfitPlan));
        saveObj.add("closet", JsonUtils.jsonFromCollection(closet));
        saveObj.add("traits", JsonUtils.jsonFromCollection(traits));            //FIXME: May be contributing to levelup showing duplicate Trait entries making levelling and deleveling problematic. - DSM
        saveObj.add("body", body.save());
        saveObj.add("inventory", JsonUtils.JsonFromMap(inventory));
        saveObj.addProperty("human", human());
        saveObj.add("flags", JsonUtils.JsonFromMap(flags));
        saveObj.add("levelUps", JsonUtils.JsonFromMap(levelPlan));              //FIXME: May be contributing to levelup showing duplicate Trait entries making levelling and deleveling problematic. - DSM
        saveObj.add("growth", JsonUtils.getGson().toJsonTree(growth));
        // TODO eventually this should load any status, for now just load addictions
        JsonArray status = new JsonArray();
        getAddictions().stream().map(Addiction::saveToJson).forEach(status::add);
        saveObj.add("status", status);
        saveInternal(saveObj);
        return saveObj;
    }

    protected void saveInternal(JsonObject obj) {

    }

    public abstract String getType();

    /**Loads this character from a Json Object that was output to file. 
     * 
     * */
    public void load(JsonObject object) {
        name = object.get("name").getAsString();
        progression = new Progression(object.get(JSON_PROGRESSION).getAsJsonObject());
        if (object.has("growth")) {
            growth = JsonUtils.getGson().fromJson(object.get("growth"), Growth.class);
            growth.removeNullTraits();
        }
        money = object.get("money").getAsInt();
        {
            JsonObject jsCoreStats = object.getAsJsonObject(Character.jsCoreStats);
            stamina = new StaminaStat(jsCoreStats.get(jsStamina).getAsJsonObject());
            arousal = new ArousalStat(jsCoreStats.get(jsArousal).getAsJsonObject());
            mojo = new MojoStat(jsCoreStats.get(jsMojo).getAsJsonObject());
            willpower = new WillpowerStat(jsCoreStats.get(jsWillpower).getAsJsonObject());
        }

        affections = JsonUtils.mapFromJson(object.getAsJsonObject("affections"), String.class, Integer.class);
        attractions = JsonUtils.mapFromJson(object.getAsJsonObject("attractions"), String.class, Integer.class);

        {
            outfitPlan.clear();
            JsonUtils.getOptionalArray(object, "outfit").ifPresent(this::addClothes);
        }
        {
            closet = new HashSet<>(
                            JsonUtils.collectionFromJson(object.getAsJsonArray("closet"), Clothing.class).stream()
                            .filter(c -> c != null).collect(Collectors.toList()));
        }
        {
            traits = new CopyOnWriteArrayList<>(
                            JsonUtils.collectionFromJson(object.getAsJsonArray("traits"), Trait.class).stream()
                            .filter(trait -> trait != null).collect(Collectors.toList()));
            if (getType().equals("Airi"))
                traits.remove(Trait.slime);
        }
        
        body = Body.load(object.getAsJsonObject("body"), this);
        att = JsonUtils.mapFromJson(object.getAsJsonObject("attributes"), Attribute.class, Integer.class);
        inventory = JsonUtils.mapFromJson(object.getAsJsonObject("inventory"), Item.class, Integer.class);

        flags.clear();
        JsonUtils.getOptionalObject(object, "flags")
                        .ifPresent(obj -> flags.putAll(JsonUtils.mapFromJson(obj, String.class, Integer.class)));
        if (object.has("levelUps")) {
            levelPlan = JsonUtils.mapFromJson(object.getAsJsonObject("levelUps"), Integer.class, LevelUpData.class);
        } else {
            levelPlan = new HashMap<>();
        }
        status = new ArrayList<>();
        for (JsonElement element : Optional.of(object.getAsJsonArray("status")).orElse(new JsonArray())) {
            try {
                Addiction addiction = Addiction.load(this, element.getAsJsonObject());
                if (addiction != null) {
                    status.add(addiction);
                }
            } catch (Exception e) {
                System.err.println("Failed to load status:");
                System.err.println(JsonUtils.getGson().toJson(element));
                e.printStackTrace();
            }
        }
        change();
        Global.gainSkills(this);
        Global.learnSkills(this);
    }

    private void addClothes(JsonArray array) {
        outfitPlan.addAll(
                        JsonUtils.stringsFromJson(array).stream().map(Clothing::getByID).collect(Collectors.toList()));
    }

    public abstract void afterParty();

    public boolean checkOrgasm() {
        return getArousal().isAtUnfavorableExtreme() && !is(Stsflag.orgasmseal) && pleasured;
    }

    /**Makes the character orgasm. Currently accounts for various traits involved with orgasms.
     * 
     * @param c
     * The combat that this method requires. 
     * @param opponent
     * The opponent that is making this orgasm happen.
     * @param selfPart
     * The part that is orgasming. Important for fluid mechanics and other traits and features.
     * @param opponentPart
     * The part in the opponent that is making this character orgasm. 
     * 
     * */
    public void doOrgasm(Combat c, Character opponent, BodyPart selfPart, BodyPart opponentPart) {
        int total = 1;
        if (this != opponent && opponent != null) {
            if (opponent.has(Trait.carnalvirtuoso)) {
                total++;
            }
            if (opponent.has(Trait.intensesuction)
                    && (outfit.has(ClothingTrait.harpoonDildo) || outfit.has(ClothingTrait.harpoonOnahole))
                    && Global.random(3) == 0) {
                total++;
            }
        }
        for (int i = 1; i <= total; i++) {
            resolveOrgasm(c, opponent, selfPart, opponentPart, i, total);
        }
    }

    /**Resolves the orgasm. Accounts for various traits and outputs dynamic text to the GUI.
     * 
     * @param c
     * The combat that this method requires. 
     * 
     * @param opponent
     * The opponent that is making this orgasm happen.
     * 
     * @param selfPart
     * The part that is orgasming. Important for fluid mechanics and other traits and features.
     * 
     * @param opponentPart
     * The part in the opponent that is making this character orgasm. 
     * 
     * @param times
     * The number of times this person is orgasming.
     * 
     * @param totalTimes 
     * The total amount of times that the character has orgasmed.
     * 
     * */
    protected void resolveOrgasm(Combat c, Character opponent, BodyPart selfPart, BodyPart opponentPart, int times, int totalTimes) {
        if (has(Trait.HiveMind)) {
            if (HiveMind.resolveOrgasm(c, this, opponent)) {
                return;
            }
        }

        String orgasmLiner = "<b>" + orgasmLiner(c,
            opponent == null ? c.getOpponentCharacter(this) : opponent) + "</b>";
        String opponentOrgasmLiner = (opponent == null || opponent == this || opponent.isPet()) ? "" : 
            "<b>" + opponent.makeOrgasmLiner(c, this) + "</b>";
        orgasmed = true;
        if (times == 1) {
            c.write(this, "<br/>");
        }
        if (opponent == this) {
            resolvePreOrgasmForSolo(c, opponent, selfPart, times);
        } else {
            resolvePreOrgasmForOpponent(c, opponent, selfPart, opponentPart, times, totalTimes);
        }
        int overflow = arousal.getOverflow();
        c.write(this,
            String.format("<font color='rgb(255,50,200)'>%s<font color='white'> arousal overflow",
                overflow));
        if (this != opponent) {
            resolvePostOrgasmForOpponent(c, opponent, selfPart, opponentPart);
        }
        orgasm();
        if (is(Stsflag.feral)) {
            arousal.pleasure(arousal.max() / 2);
        }
        float extra = 25.0f * overflow / (arousal.max());

        int willloss = getOrgasmWillpowerLoss();
        loseWillpower(c, willloss, Math.round(extra), true, "");
        if (has(Trait.sexualDynamo)) {
            c.write(this, Global.format("{self:NAME-POSSESSIVE} climax makes "
                + "{self:direct-object} positively gleam with erotic splendor; "
                + "{self:possessive} every move seems more seductive than ever.",
                this, opponent));
            add(c, new Abuff(this, Attribute.Seduction, 5, 10));
        }
        if (has(Trait.lastStand)) {
            var tighten = new OrgasmicTighten(this);
            var thrust = new OrgasmicThrust(this);
            if (tighten.usable(c, opponent)) {
                tighten.resolve(c, opponent);
            }
            if (thrust.usable(c, opponent)) {
                thrust.resolve(c, opponent);
            }
        }
        if (this != opponent && times == totalTimes && canRespond()) {          //FIXME: Explicitly Parentesize for clear order of operations. - DSM
            c.write(this, orgasmLiner);
            c.write(opponent, opponentOrgasmLiner);
        }

        if (has(Trait.nymphomania)
            && (Global.random(100) < Math.sqrt(get(Attribute.Nymphomania) + get(Attribute.Animism)) * 10)
            && !getWillpower().isAtUnfavorableExtreme()
            && times == totalTimes) {
            if (human()) {
                c.write("Cumming actually made you feel kind of refreshed, albeit with a "
                    + "burning desire for more.");
            } else {
                c.write(Global.format(
                    "After {self:subject} comes down from {self:possessive} orgasmic high, "
                        + "{self:pronoun} doesn't look satisfied at all. There's a mad glint in "
                        + "{self:possessive} eye that seems to be endlessly asking for more.",
                    this, opponent));
            }
            restoreWillpower(c,
                5 + Math.min((get(Attribute.Animism) + get(Attribute.Nymphomania)) / 5, 15));
        }
        if (times == totalTimes) {
            List<Status> purgedStatuses = getStatuses().stream()
                .filter(status ->
                    (status.mindgames() && status.flags().contains(Stsflag.purgable))
                    || status.flags().contains(Stsflag.orgasmPurged))
                .collect(Collectors.toList());
            if (!purgedStatuses.isEmpty()){
                if (human()) {
                    c.write(this, "<b>Your mind clears up after your release.</b>");
                } else {
                    c.write(this, "<b>You see the light of reason return to "
                        + nameDirectObject() + " after "
                        + possessiveAdjective() + " release.</b>");
                }
                purgedStatuses.forEach(this::removeStatus);
            }
        }

        if (checkAddiction(AddictionType.CORRUPTION, opponent)
            && selfPart != null
            && opponentPart != null) {
            if (c.getStance().havingSex(c, this)
                && (c.getCombatantData(this).getIntegerFlag("ChoseToFuck") == 1)) {
                c.write(this,
                    Global.format("{self:NAME-POSSESSIVE} willing sacrifice to "
                        + "{other:name-do} greatly reinforces the corruption inside "
                        + "of {self:direct-object}.", this, opponent));
                addict(c, AddictionType.CORRUPTION, opponent, Addiction.HIGH_INCREASE);
            }
            if (opponent.has(Trait.TotalSubjugation)
                && c.getStance().en == Stance.succubusembrace) {
                c.write(this,
                    Global.format("The succubus takes advantage of {self:name-possessive} "
                        + "moment of vulnerability and overwhelms {self:posssessive} mind with "
                        + "{other:possessive} soul-corroding lips.", this, opponent));
                addict(c, AddictionType.CORRUPTION, opponent, Addiction.HIGH_INCREASE);
            }
        }
       if (checkAddiction(AddictionType.ZEAL, opponent)
           && selfPart != null
           && opponentPart != null
           && c.getStance().penetratedBy(c, opponent, this)
           && selfPart.isType(CockPart.TYPE)) {
            c.write(this,
                Global.format("Experiencing so much pleasure inside of {other:name-do} "
                    + "reinforces {self:name-possessive} faith in the lovely goddess.",
                    this, opponent));
            addict(c, AddictionType.ZEAL, opponent, Addiction.MED_INCREASE);
        }

        if (checkAddiction(AddictionType.ZEAL, opponent)
            && selfPart != null
            && opponentPart != null
            && c.getStance().penetratedBy(c, this, opponent)
            && opponentPart.isType(CockPart.TYPE)
            && (selfPart.isType(PussyPart.TYPE) || selfPart.isType(AssPart.TYPE))) {
            c.write(this,
                Global.format("Experiencing so much pleasure from {other:name-possessive} "
                    + "cock inside {self:direct-object} reinforces {self:name-possessive} faith.",
                    this, opponent));
            addict(c, AddictionType.ZEAL, opponent, Addiction.MED_INCREASE);
        }
        if (checkAddiction(AddictionType.BREEDER, opponent)) {
            // Clear combat addiction
            unaddictCombat(AddictionType.BREEDER, opponent, 1.f, c);
        }
        if (checkAddiction(AddictionType.DOMINANCE, opponent) && c.getStance().dom(opponent)) {
            c.write(this, "Getting dominated by " + opponent.nameDirectObject()
                + " seems to excite " + nameDirectObject() + " even more.");
            addict(c, AddictionType.DOMINANCE, opponent, Addiction.LOW_INCREASE);
        }
        orgasms += 1;
    }

    /**Helper method for resolveOrgasm(). Writes dynamic text to the GUI based on bodypart. 
     *
     * @param c
     * The combat that this method requires. 
     * @param opponent
     * The opponent that is making this orgasm happen.
     * @param selfPart
     * The part that is orgasming. Important for fluid mechanics and other traits and features.
     * @param times
     * The number of times this person is orgasming.
     * 
     * .*/
    private void resolvePreOrgasmForSolo(Combat c, Character opponent, BodyPart selfPart, int times) {
        if (selfPart != null && selfPart.isType(CockPart.TYPE)) {
            if (times == 1) {
                c.write(this, Global.format(
                    "<b>{self:NAME-POSSESSIVE} back arches as thick ropes of jizz fire from "
                        + "{self:possessive} dick and land on {self:reflective}.</b>",
                    this, opponent));
            } else {
                c.write(this, Global.format(
                    "<b>{other:SUBJECT-ACTION:expertly coax|expertly coaxes} yet another "
                        + "orgasm from {self:name-do}, leaving {self:direct-object} completely "
                        + "spent.</b>",
                    this, opponent));
            }
        } else {
            if (times == 1) {
                c.write(this, Global.format(
                    "<b>{self:SUBJECT-ACTION:shudder|shudders} as {self:pronoun} "
                        + "{self:action:bring|brings} {self:reflective} to a toe-curling climax.</b>",
                    this, opponent));
            } else {
                c.write(this, Global.format(
                    "<b>{other:SUBJECT-ACTION:expertly coax|expertly coaxes} yet another "
                        + "orgasm from {self:name-do}, leaving {self:direct-object} completely "
                        + "spent.</b>",
                    this, opponent));
            }
        }
    }
    
    /**Helper method for resolving the opponent's orgasm. Helps write text to the GUI.
     * 
     * @param c
     * The combat that this method requires. 
     * @param opponent
     * The opponent that is making this orgasm happen.
     * @param selfPart
     * The part that is orgasming. Important for fluid mechanics and other traits and features.
     * @param opponentPart
     * The opopnent's part that is orgasming.
     * */
    private void resolvePreOrgasmForOpponent(Combat c, Character opponent, BodyPart selfPart, BodyPart opponentPart,
        int times, int total) {
        if (c.getStance().inserted(this) && !has(Trait.strapped)) {
            Character partner = c.getStance().getPenetratedCharacter(c, this);
            BodyPart holePart = Global.pickRandom(c.getStance().getPartsFor(c, partner, this)).orElse(null);
            if (times == 1) {
                String hole = "pulsing hole";
                if (holePart != null && holePart.isType(BreastsPart.TYPE)) {
                    hole = "cleavage";
                } else if (holePart != null && holePart.isType(MouthPart.TYPE)) {
                    hole = "hungry mouth";
                }
                c.write(this, Global.format(
                    "<b>{self:SUBJECT-ACTION:tense|tenses} up as {self:possessive} hips "
                        + "wildly buck against {other:name-do}. In no time, {self:possessive} hot "
                        + "seed spills into {other:possessive} %s.</b>",
                    this, partner, hole));
            } else {
                c.write(this, Global.format(
                    "<b>{other:NAME-POSSESSIVE} devilish orfice does not let up, and "
                        + "{other:possessive} intense actions somehow force {self:name-do} to "
                        + "cum again instantly.</b>",
                    this, partner));
            }
            Optional<BodyPart> opponentHolePart = Global.pickRandom(c.getStance().getPartsFor(c, opponent, this));
            opponentHolePart.ifPresent(bodyPart -> partner.body.receiveCum(c, this, bodyPart));
        } else if (selfPart != null
            && selfPart.isType(CockPart.TYPE)
            && opponentPart != null
            && !opponentPart.isType("none")) {
            if (times == 1) {
                c.write(this, Global.format(
                    "<b>{self:NAME-POSSESSIVE} back arches as thick ropes of jizz fire "
                        + "from {self:possessive} dick and land on {other:name-possessive} "
                        + opponentPart.describe(opponent) + ".</b>",
                    this, opponent));
            } else {
                c.write(this, Global.format(
                    "<b>{other:SUBJECT-ACTION:expertly coax|expertly coaxes} yet another "
                        + "orgasm from {self:name-do}, leaving {self:direct-object} completely "
                        + "spent.</b>",
                    this, opponent));
            }
            opponent.body.receiveCum(c, this, opponentPart);
        } else {
            if (times == 1) {
                c.write(this, Global.format(
                    "<b>{self:SUBJECT-ACTION:shudder|shudders} as "
                        + "{other:subject-action:bring|brings} {self:direct-object} "
                        + "to a toe-curling climax.</b>",
                    this, opponent));
            } else {
                c.write(this, Global.format(
                    "<b>{other:SUBJECT-ACTION:expertly coax|expertly coaxes} yet another "
                        + "orgasm from {self:name-do}, leaving {self:direct-object} completely "
                        + "spent.</b>",
                    this, opponent));
            }
        }
        if (opponent.has(Trait.mindcontroller) && cloned == 0) {
            MindControl.Result res = new MindControl.Result(this, opponent, c.getStance());
            String message = res.getDescription();
            if (res.hasSucceeded()) {
                if (opponent.has(Trait.EyeOpener) && outfit.has(ClothingTrait.harpoonDildo)) {
                    message += "Below, the vibrations of the dildo reach a powerful crescendo,"
                        + " and your eyes open wide in shock, a perfect target for "
                        + " what's coming next.";
                    addict(c, AddictionType.MIND_CONTROL, opponent, Addiction.LOW_INCREASE);
                } else if (opponent.has(Trait.EyeOpener) && outfit.has(ClothingTrait.harpoonOnahole)) {
                    message += "The warm sheath around your dick suddenly tightens, pulling incredibly"
                        + ", almost painfully tight around the shaft. At the same time, it starts"
                        + " vibrating powerfully. The combined assault causes your eyes to open"
                        + " wide and defenseless.";
                    addict(c, AddictionType.MIND_CONTROL, opponent, Addiction.LOW_INCREASE);
                }
                message += "While your senses are overwhelmed by your violent orgasm, the deep pools of Mara's eyes"
                    + " swirl and dance. You helplessly stare at the intricate movements and feel a strong"
                    + " pressure on your mind as you do. When your orgasm dies down, so do the dancing patterns."
                    + " With a satisfied smirk, Mara tells you to lift an arm. Before you have even processed"
                    + " her words, you discover that your right arm is sticking straight up into the air. This"
                    + " is probably really bad.";
                addict(c, AddictionType.MIND_CONTROL, opponent, Addiction.MED_INCREASE);
            }
            c.write(this, message);
        }
    }

    public String getRandomLineFor(String lineType, Combat c, Character target) {
        return "";
    }

    /**Helper method for resolving what happens after orgasm for the opponent. Helps write dynamic text to the GUI.
     * @param c
     * The combat that this method requires. 
     * @param opponent
     * The opponent that is making this orgasm happen.
     * @param selfPart
     * The part that is orgasming. Important for fluid mechanics and other traits and features.
     * @param opponentPart
     * The opopnent's part that is orgasming.
     * 
     * */
    private void resolvePostOrgasmForOpponent(Combat c, Character opponent, BodyPart selfPart, BodyPart opponentPart) {
        if (selfPart != null && opponentPart != null) {
            selfPart.onOrgasmWith(c, this, opponent, opponentPart, true);
            opponentPart.onOrgasmWith(c, opponent, this, selfPart, false);
        }
        body.onOrgasm(c, this, opponent);

        if (opponent.has(Trait.erophage)) {
            c.write(Global.capitalizeFirstLetter("<b>" +
                opponent.subjectAction("flush", "flushes")
                + " as the feedback from " + nameOrPossessivePronoun() + " orgasm feeds "
                + opponent.possessiveAdjective() + " divine power.</b>"));
            opponent.add(c, new Alluring(opponent, 5));
            opponent.buildMojo(c, 100);
            if (c.getStance().inserted(this) && opponent.has(Trait.divinity)) {
                opponent.add(c, new DivineCharge(opponent, 1));
            }
        }
        if (opponent.has(Trait.sexualmomentum)) {
            c.write(Global.capitalizeFirstLetter("<b>"
               + opponent.subjectAction("are more composed", "seems more composed")
                + " as " + nameOrPossessivePronoun() + " forced orgasm goes straight to "
                + opponent.possessiveAdjective() + " ego.</b>"));
            opponent.restoreWillpower(c, 10 + Global.random(10));
        }
    }

    public void loseWillpower(Combat c, int i) {
        loseWillpower(c, i, 0, false, "");
    }

    public void loseWillpower(Combat c, int i, boolean primary) {
        loseWillpower(c, i, 0, primary, "");
    }
    
    /**Processes willpower loss for this character.
     * 
     * @param c
     * The combat required for this method.
     * @param i
     * The base value of willpower loss.
     * @param extra
     * 
     * @param primary
     * indicates if this is primary.
     * @param source
     * The source of the willpower loss.
     *
     * */
    public void loseWillpower(Combat c, int i, int extra, boolean primary, String source) {
        int amt = i + extra;
        String reduced = "";
        if (has(Trait.strongwilled) && primary) {
            amt = amt * 2 / 3 + 1;
            reduced += " (Strong-willed)";
        }
        if (is(Stsflag.feral) && primary) {
            amt = amt / 2;
            reduced += " (Feral)";
        }
        int old = willpower.get();
        willpower.exhaust(amt);
        if (c != null) {
            c.writeSystemMessage(String.format(
                            "%s lost <font color='rgb(220,130,40)'>%s<font color='white'> willpower" + reduced + "%s.",
                            Global.capitalizeFirstLetter(subject()), extra == 0 ? Integer.toString(amt) : i + "+" + extra + " (" + amt + ")",
                            source), true);
        } else if (human()) {
            Global.gui().systemMessage(String
                            .format("%s lost <font color='rgb(220,130,40)'>%d<font color='white'> willpower" + reduced
                                            + "%s.", subject(), amt, source));
        }
    }

    public void restoreWillpower(Combat c, int i) {
        willpower.recover(i);
        c.writeSystemMessage(String.format("%s regained <font color='rgb(181,230,30)'>%d<font color='white'> willpower.", subject(), i), true);
    }

    /**Helper method that Handles the inserted?
     * 
     * @param c
     * The Combat that this method requires.
     * 
     * */
    private void handleInserted(Combat c) {
        List<Character> partners = c.getStance().getAllPartners(c, this);
        partners.forEach(opponent -> {
            Iterator<BodyPart> selfOrganIt;
            Iterator<BodyPart> otherOrganIt;
            selfOrganIt = c.getStance().getPartsFor(c, this, opponent).iterator();
            otherOrganIt = c.getStance().getPartsFor(c, opponent, this).iterator();
            if (selfOrganIt.hasNext() && otherOrganIt.hasNext()) {
                BodyPart selfOrgan = selfOrganIt.next();
                BodyPart otherOrgan = otherOrganIt.next();
                if (has(Trait.energydrain) && selfOrgan != null && otherOrgan != null
                                && selfOrgan.isErogenous() && otherOrgan.isErogenous()) {
                    c.write(this, Global.format(
                                    "{self:NAME-POSSESSIVE} body glows purple as {other:subject-action:feel|feels} {other:possessive} very spirit drained into {self:possessive} "
                                                    + selfOrgan.describe(this) + " through your connection.",
                                    this, opponent));
                    int m = Global.random(5) + 5;
                    opponent.drain(c, this, (int) this.modifyDamage(DamageType.drain, opponent, m));
                }
                body.tickHolding(c, opponent, selfOrgan, otherOrgan);
            }
        });
    }

    public void endOfCombatRound(Combat c, Character opponent) {
        dropStatus(c, opponent);
        tick(c);
        status.forEach(s -> s.tick(c));
        List<String> removed = new ArrayList<>();
        for (String s : cooldowns.keySet()) {
            if (cooldowns.get(s) <= 1) {
                removed.add(s);
            } else {
                cooldowns.put(s, cooldowns.get(s) - 1);
            }
        }
        for (String s : removed) {
            cooldowns.remove(s);
        }
        handleInserted(c);
        if (outfit.has(ClothingTrait.tentacleSuit)) {
            c.write(this, Global.format("The tentacle suit squirms against {self:name-possessive} body.", this,
                            opponent));
            if (hasBreasts()) {
                TentaclePart.pleasureWithTentacles(c, this, 5, body.getRandomBreasts());
            }
            TentaclePart.pleasureWithTentacles(c, this, 5, body.getSkin());
        }
        if (outfit.has(ClothingTrait.tentacleUnderwear)) {
            String undieName = "underwear";
            if (hasPussy()) {
                undieName = "panties";
            }
            c.write(this, Global.format("The tentacle " + undieName + " squirms against {self:name-possessive} crotch.",
                            this, opponent));
            if (hasDick()) {
                TentaclePart.pleasureWithTentacles(c, this, 5, body.getRandomCock());
                body.pleasure(null, null, body.getRandomCock(), 5, c);
            }
            if (hasBalls()) {
                TentaclePart.pleasureWithTentacles(c, this, 5, body.getRandomBalls());
            }
            if (hasPussy()) {
                TentaclePart.pleasureWithTentacles(c, this, 5, body.getRandomPussy());
            }
            TentaclePart.pleasureWithTentacles(c, this, 5, body.getRandomAss());
        }
        if (outfit.has(ClothingTrait.harpoonDildo)) {
            if (!hasPussy()) {
                c.write(Global.format("Since {self:name-possessive} pussy is now gone, the dildo that was stuck inside of it falls"
                                + " to the ground. {other:SUBJECT-ACTION:reel|reels} it back into its slot on"
                                + " {other:possessive} arm device.", this, opponent));
            } else {
                int damage = 5;
                if (opponent.has(Trait.pussyhandler)) {
                    damage += 2;
                }
                if (opponent.has(Trait.yank)) {
                    damage += 3;
                }
                if (opponent.has(Trait.conducivetoy)) {
                    damage += 3;
                }
                if (opponent.has(Trait.intensesuction)) {
                    damage += 3;
                }

                c.write(Global.format("{other:NAME-POSSESSIVE} harpoon dildo is still stuck in {self:name-possessive}"
                                + " {self:body-part:pussy}, vibrating against {self:possessive} walls.", this, opponent));
                body.pleasure(opponent, ToysPart.dildo, body.getRandomPussy(), damage, c);
            }
        }
        if (outfit.has(ClothingTrait.harpoonOnahole)) {
            if (!hasDick()) {
                c.write(Global.format("Since {self:name-possessive} dick is now gone, the onahole that was stuck onto it falls"
                                + " to the ground. {other:SUBJECT-ACTION:reel|reels} it back into its slot on"
                                + " {other:possessive} arm device.", this, opponent));
            } else {
                int damage = 5;
                if (opponent.has(Trait.dickhandler)) {
                    damage += 2;
                }
                if (opponent.has(Trait.yank)) {
                    damage += 3;
                }
                if (opponent.has(Trait.conducivetoy)) {
                    damage += 3;
                }
                if (opponent.has(Trait.intensesuction)) {
                    damage += 3;
                }
                
                c.write(Global.format("{other:NAME-POSSESSIVE} harpoon onahole is still stuck on {self:name-possessive}"
                                + " {self:body-part:cock}, vibrating against {self:possessive} shaft.", this, opponent));
                body.pleasure(opponent, ToysPart.onahole, body.getRandomCock(), damage, c);
            }
        }
        if (getPure(Attribute.Animism) >= 4 && getArousal().percent() >= 50 && !is(Stsflag.feral)) {
            add(c, new Feral(this));
        }
        
        if (opponent.has(Trait.temptingass) && !is(Stsflag.frenzied)) {
            int chance = 20;
            chance += Math.max(0, Math.min(15, opponent.get(Attribute.Seduction) - get(Attribute.Seduction)));
            if (is(Stsflag.feral))
                chance += 10;
            if (is(Stsflag.charmed) || opponent.is(Stsflag.alluring))
                chance += 5;
            if (has(Trait.assmaster) || has(Trait.analFanatic))
                chance += 5;
            Optional<BodyFetish> fetish = body.getFetish(AssPart.TYPE);
            if (fetish.isPresent() && opponent.has(Trait.bewitchingbottom)) {
                chance += 20 * fetish.get().magnitude;
            }
            if (chance >= Global.random(100)) {
                AssFuck fuck = new AssFuck(this);
                if (fuck.requirements(c, opponent) && fuck.usable(c, opponent)) {
                    c.write(opponent,
                                    Global.format("<b>The look of {other:name-possessive} ass,"
                                                    + " so easily within {self:possessive} reach, causes"
                                                    + " {self:subject} to involuntarily switch to autopilot."
                                                    + " {self:SUBJECT} simply {self:action:NEED|NEEDS} that ass.</b>",
                                    this, opponent));
                    add(c, new Frenzied(this, 1));
                }
            }
        }

        pleasured = false;
        var opponentAssistants = new ArrayList<>(c.assistantsOf(opponent));
        Collections.shuffle(opponentAssistants);
        var randomOpponentPetOptional = opponentAssistants.stream().findFirst();
        if (!isPet() && randomOpponentPetOptional.isPresent()) {
            var pet = randomOpponentPetOptional.get().getCharacter();
            boolean weakenBetter = modifyDamage(DamageType.physical, pet, 100) / pet.getStamina().remaining() 
                            > 100 / pet.getStamina().remaining();
            if (canAct() && c.getStance().mobile(this) && pet.roll(this, c, 20)) {
                c.write(this, Global.format("<b>{self:SUBJECT-ACTION:turn} {self:possessive} attention"
                                + " on {other:name-do}</b>", this, pet));
                if (weakenBetter) {
                    c.write(Global.format("{self:SUBJECT-ACTION:focus|focuses} {self:possessive} attentions on {other:name-do}, "
                                    + "thoroughly exhausting {other:direct-object} in a game of cat and mouse.<br/>", this, pet));
                    pet.weaken(c, (int) modifyDamage(DamageType.physical, pet, Global.random(10, 20)));
                } else {
                    c.write(Global.format("{self:SUBJECT-ACTION:focus|focuses} {self:possessive} attentions on {other:name-do}, "
                                    + "harassing and toying with {other:possessive} body as much as {self:pronoun} can.<br/>", this, pet));
                    pet.body.pleasure(this, body.getRandomHands(), pet.body.getRandomGenital(), Global.random(10, 20), c);
                }
            }
        }

        if (has(Trait.apostles)) {
            Apostles.eachCombatRound(c, this, opponent);
        }

        if (has(Trait.Rut) && Global.random(100) < ((getArousal().percent() - 25) / 2) && !is(Stsflag.frenzied)) {
            c.write(this, Global.format("<b>{self:NAME-POSSESSIVE} eyes dilate and {self:possessive} body flushes as {self:pronoun-action:descend|descends} into a mating frenzy!</b>", this, opponent));
            add(c, new Frenzied(this, 3, true));
        }
    }

    public String orgasmLiner(Combat c, Character target) {         //FIXME: This could be an astract method. Eclipse just doesn't like you changing them by adding args after you first sign them.- DSM
        return "";
    }

    public String makeOrgasmLiner(Combat c, Character target) {    //FIXME: This could be an astract method. Eclipse just doesn't like you changing them by adding args after you first sign them.- DSM
        return "";
    }

    private int getOrgasmWillpowerLoss() {
        return 25;
    }

    public abstract void emote(Emotion emo, int amt);

    public void learn(Skill copy) {
        skills.addIfAbsent(copy.copy(this));
    }

    public void notifyTravel(Area dest, String message) { }

    public void endOfMatchRound() {
        regen();
        tick(null);
        status.forEach(Status::afterMatchRound);
        if (has(Trait.Confident)) {
            willpower.recover(10);
            mojo.deplete(5);
        } else {
            willpower.recover(5);
            mojo.deplete(10);
        }
        if (has(Trait.exhibitionist) && mostlyNude()) {
            mojo.build(2);
        }
        dropStatus(null, null);
        if (has(Trait.QuickRecovery)) {
            heal(null, Global.random(4, 7), " (Quick Recovery)");
        }
        update();
    }

    public void gain(Item item) {
        gain(item, 1);
    }

    public void remove(Item item) {
        gain(item, -1);
    }

    public void gain(Clothing item) {
        closet.add(item);
        update();
    }

    public void gain(Item item, int q) {
        int amt = 0;
        if (inventory.containsKey(item)) {
            amt = count(item);
        }
        inventory.put(item, Math.max(0, amt + q));
        update();
    }

    public boolean has(Item item) {
        return has(item, 1);
    }

    public boolean has(Item item, int quantity) {
        return inventory.containsKey(item) && inventory.get(item) >= quantity;
    }

    public void unequipAllClothing() {
        closet.addAll(outfitPlan);
        outfitPlan.clear();
        change();
    }

    public boolean has(Clothing item) {
        return closet.contains(item) || outfit.getEquipped().contains(item);
    }

    public void consume(Item item, int quantity) {
        consume(item, quantity, true);
    }

    public void consume(Item item, int quantity, boolean canBeResourceful) {
        if (canBeResourceful && has(Trait.resourceful) && Global.random(5) == 0) {
            quantity--;
        }
        if (inventory.containsKey(item)) {
            gain(item, -quantity);
        }
    }

    public int count(Item item) {
        if (inventory.containsKey(item)) {
            return inventory.get(item);
        }
        return 0;
    }

    public void chargeBattery() {
        int power = count(Item.Battery);
        if (power < 20) {
            gain(Item.Battery, 20 - power);
        }
    }

    /**Performs the tasks associated with finishing a match. temporary traits are removed while meters are reset. 
     * 
     * */
    public void finishMatch() {
        Global.gui().clearImage();
        change();
        clearStatus();
        temporaryAddedTraits.clear();
        temporaryRemovedTraits.clear();
        body.purge(null);
        getStamina().renew();
        getArousal().renew();
        getMojo().renew();
    }

    public void setTrophy(Item trophy) {
        this.trophy = trophy;
    }

    public Item getTrophy() {
        return trophy;
    }

    public abstract String challenge(Character other);

    public int lvlBonus(Character opponent) {
        if (opponent.getProgression().getLevel() > getProgression().getLevel()) {
            return 12 * (opponent.getProgression().getLevel() - getProgression().getLevel());
        } else {
            return 0;
        }
    }

    public int getVictoryXP(Character opponent) {
        return 25 + lvlBonus(opponent);
    }

    public int getAssistXP(Character opponent) {
        return 18 + lvlBonus(opponent);
    }

    public int getDefeatXP(Character opponent) {
        return 18 + lvlBonus(opponent);
    }

    /**Gets the attraction of this character to another.*/
    public int getAttraction(Character other) {
        if (other == null) {
            System.err.println("Other is null");
            Thread.dumpStack();
            return 0;
        }
        if (attractions.containsKey(other.getType())) {
            return attractions.get(other.getType());
        } else {
            return 0;
        }
    }

    /**Gains attraction value x to a given other character.
     * @param other
     * The character to gain attraction with.
     *  @param x  
     *  the amount of attraction to gain.
     * */
    public void gainAttraction(Character other, int x) {
        if (other == null) {
            System.err.println("Other is null");
            Thread.dumpStack();
            return;
        }
        if (attractions.containsKey(other.getType())) {
            attractions.put(other.getType(), attractions.get(other.getType()) + x);
        } else {
            attractions.put(other.getType(), x);
        }
    }

    public Map<String, Integer> getAffections() {
        return Collections.unmodifiableMap(affections);
    }

    public int getAffection(Character other) {
        if (other == null) {
            System.err.println("Other is null");
            Thread.dumpStack();
            return 0;
        }

        if (affections.containsKey(other.getType())) {
            return affections.get(other.getType());
        } else {
            return 0;
        }
    }
    
    
    public void gainAffection(Character other, int x) {
        if (other == null) {
            System.err.println("Other is null");
            Thread.dumpStack();
            return;
        }
        if (other == this) {
            //skip narcissism.
            return;
        }

        if (other.has(Trait.affectionate) && Global.random(2) == 0) {
            x += 1;
        }
        if (affections.containsKey(other.getType())) {
            affections.put(other.getType(), affections.get(other.getType()) + x);
        } else {
            affections.put(other.getType(), x);
        }
    }
    /**outputs the evasion bonus as a result of traits and status effects that affect it.*/
    public int evasionBonus() {
        int ac = 0;
        for (Status s : getStatuses()) {
            ac += s.evade();
        }
        if (has(Trait.clairvoyance)) {
            ac += 5;
        }
        if (has(Trait.FeralAgility) && is(Stsflag.feral)) {
            ac += 5;
        }
        return ac;
    }

    private Collection<Status> getStatuses() {
        return status;
    }

    /**outputs the counter chance as a result of traits and status effects that affect it.*/
    public int counterChance(Combat c, Character opponent, Skill skill) {
        int counter = 3;
        // subtract some counter chance if the opponent is more cunning than you.
        // 1% decreased counter chance per 5 points of cunning over you.
        counter += Math.min(0, get(Attribute.Cunning) - opponent.get(Attribute.Cunning)) / 5;
        // increase counter chance by perception difference
        counter += get(Attribute.Perception) - opponent.get(Attribute.Perception);
        // 1% increased counter chance per 2 speed over your opponent.
        counter += getSpeedDifference(opponent) / 2;
        for (Status s : getStatuses()) {
            counter += s.counter();
        }
        if (has(Trait.clairvoyance)) {
            counter += 3;
        }
        if (has(Trait.aikidoNovice)) {
            counter += 3;
        }
        if (has(Trait.fakeout)) {
            counter += 3;
        }
        if (opponent.is(Stsflag.countered)) {
            counter -= 10;
        }
        if (has(Trait.FeralAgility) && is(Stsflag.feral)) {
            counter += 5;
        }
        // Maximum counter chance is 3 + 5 + 2 + 3 + 3 + 3 + 5 = 24, which is super hard to achieve.
        // I guess you also get some more counter with certain statuses effects like water form.
        // Counters should be pretty rare.
        return Math.max(0, counter);
    }

    private int getSpeedDifference(Character opponent) {
        return Math.min(Math.max(get(Attribute.Speed) - opponent.get(Attribute.Speed), -5), 5);
    }
    
    /**Determines and returns the chace to hit, depending on the given accuracy and differences in levels, as well as traits.*/
    public int getChanceToHit(Character attacker, Combat c, int accuracy) {
        int hitDiff = attacker.getSpeedDifference(this) + (attacker.get(Attribute.Perception) - get(
                        Attribute.Perception));
        int levelDiff = Math.min(attacker.getProgression().getLevel() - getProgression().getLevel(), 5);
        levelDiff = Math.max(levelDiff, -5);

        // with no level or hit differences and an default accuracy of 80, 80%
        // hit rate
        // each level the attacker is below the target will reduce this by 2%,
        // to a maximum of 10%
        // each point in accuracy of skill affects changes the hit chance by 1%
        // each point in speed and perception will increase hit by 5%
        int chanceToHit = 2 * levelDiff + accuracy + 5 * (hitDiff - evasionBonus());
        if (has(Trait.hawkeye)) {
            chanceToHit += 5;
        }
        return chanceToHit;
    }

    /**Used by many resolve functions, this method returns true if the attacker hits with a given accuracy.*/
    public boolean roll(Character attacker, Combat c, int accuracy) {
        int attackroll = Global.random(100);
        int chanceToHit = getChanceToHit(attacker, c, accuracy);

        return attackroll < chanceToHit;
    }

    /**Determines the Difficulty Class for knocking someone down.*/
    public int knockdownDC() {
        int dc = 10 + getStamina().get() / 10 + getStamina().percent() / 5;
        if (is(Stsflag.braced)) {
            dc += getStatus(Stsflag.braced).value();
        }
        if (has(Trait.stabilized)) {
            dc += 12 + 3 * Math.sqrt(get(Attribute.Science));
        }
        if (has(ClothingTrait.heels) && !has(Trait.proheels)) {
            dc -= 7;
        }
        if (has(ClothingTrait.highheels) && !has(Trait.proheels)) {
            dc -= 8;
        }
        if (has(ClothingTrait.higherheels) && !has(Trait.proheels)) {
            dc -= 10;
        }
        if (bound()) {
            dc/=2;
        }
        if (stunned()) {
            dc/=4;
        }
        return dc;
    }

    public abstract void counterattack(Character target, Tactics type, Combat c);

    public void clearStatus() {
        status.removeIf(status -> !status.flags().contains(Stsflag.permanent));
    }

    public Status getStatus(Stsflag flag) {
        return getStatusStreamWithFlag(flag).findFirst().orElse(null);
    }

    // terrible code? who me? nahhhhh.
    @SuppressWarnings("unchecked")
    public <T> Collection<T> getStatusOfClass(Class<T> clazz) {
        return status.stream().filter(s -> s.getClass().isInstance(clazz)).map(s -> (T)s).collect(Collectors.toList());
    }

    public Collection<InsertedStatus> getInsertedStatus() {
        return getStatusOfClass(InsertedStatus.class);
    }

    
    /**Returns an Integer representing the value of prize for --defeating?-- this character. The prize depends on the rank of the characcter.*/
    public Integer prize() {
        if (getProgression().getRank() >= 2) {
            return 500;
        } else if (getProgression().getRank() == 1) {
            return 200;
        } else {
            return 50;
        }
    }


    /**Processes teh end of the battle for this character. */
    public void endofbattle(Combat c) {
        for (Status s : status) {
            if (!s.lingering() && !s.flags().contains(Stsflag.permanent)) {
                removelist.add(s);
            }
        }
        cooldowns.clear();
        dropStatus(null, null);
        orgasms = 0;
        update();
        if (has(ClothingTrait.heels)) {
            setFlag("heelsTraining", getFlag("heelsTraining") + 1);
        }
        if (has(ClothingTrait.highheels)) {
            setFlag("heelsTraining", getFlag("heelsTraining") + 1);
        }
        if (has(ClothingTrait.higherheels)) {
            setFlag("heelsTraining", getFlag("heelsTraining") + 1);
        }
        if (is(Stsflag.disguised) || has(Trait.slime)) {
            purge(c);
        }
        if (has(ClothingTrait.harpoonDildo)) {
            outfit.unequip(Clothing.getByID("harpoondildo"));
        }
        if (has(ClothingTrait.harpoonOnahole)) {
            outfit.unequip(Clothing.getByID("harpoononahole"));
        }
    }

    public void setFlag(String string, int i) {
        flags.put(string, i);
    }

    public int getFlag(String string) {
        if (flags.containsKey(string)) {
            return flags.get(string);
        }
        return 0;
    }

    public boolean canSpend(int mojo) {
        int cost = mojo;
        for (Status s : getStatuses()) {
            cost += s.spendmojo(mojo);
        }
        return getMojo().get() >= cost;
    }

    public Map<Item, Integer> getInventory() {
        return inventory;
    }

    public ArrayList<String> listStatus() {
        ArrayList<String> result = new ArrayList<>();
        for (Status s : getStatuses()) {
            result.add(s.toString());
        }
        return result;
    }

    /**Dumps stats to the GUi.
     * */
    public String dumpstats(boolean notableOnly) {
        StringBuilder b = new StringBuilder();
        b.append("<b>");
        b.append(getTrueName() + ": Level " + getProgression().getLevel() + "; ");
        for (Attribute a : att.keySet()) {
            b.append(a.name() + " " + att.get(a) + ", ");
        }
        b.append("</b>");
        b.append("<br/>Max Stamina " + stamina.max() + ", Max Arousal " + arousal.max() + ", Max Mojo " + mojo.max()
                        + ", Max Willpower " + willpower.max() + ".");
        b.append("<br/>");
        if (human()) {
            // ALWAYS GET JUDGED BY ANGEL. lol.
            body.describeBodyText(b, Global.getCharacterByType("Angel"), notableOnly);
        } else {
            body.describeBodyText(b, Global.getPlayer(), notableOnly);
        }
        if (getTraits().size() > 0) {
            b.append("<br/>Traits:<br/>");
            List<Trait> traits = new ArrayList<>(getTraits());
            traits.sort((first, second) -> first.toString().compareTo(second.toString()));
            for (Trait t : traits) {

                    b.append(t + ": " + t.getDesc());
                    b.append("<br/>");

            }
        }
        b.append("</p>");

        return b.toString();
    }

    public String toString() {
        return getType();
    }

    /***/
    public float getOtherFitness(Combat c, Character other) {
        float fit = 0;
        // Urgency marks
        float arousalMod = 1.0f;
        float staminaMod = 1.0f;
        float mojoMod = 1.0f;
        float usum = arousalMod + staminaMod + mojoMod;
        int escape = other.getEscape(c, this);
        if (escape > 1) {
            fit += 8 * Math.log(escape);
        } else if (escape < -1) {
            fit += -8 * Math.log(-escape);
        }
        int totalAtts = 0;
        for (Attribute attribute : att.keySet()) {
            totalAtts += att.get(attribute);
        }
        fit += Math.sqrt(totalAtts) * 5;

        // what an average piece of clothing should be worth in fitness
        double topFitness = 8.0;
        double bottomFitness = 6.0;
        // If I'm horny, I want the other guy's clothing off, so I put more
        // fitness in them
        if (getMood() == Emotion.horny) {
            topFitness += 6;
            bottomFitness += 8;
            // If I'm horny, I want to make the opponent cum asap, put more
            // emphasis on arousal
            arousalMod = 2.0f;
        }

        // check body parts based on my preferences
        if (other.hasDick()) {
            fit -= (dickPreference() - 3) * 4;
        }
        if (other.hasPussy()) {
            fit -= (pussyPreference() - 3) * 4;
        }

        fit += c.assistantsOf(other).stream().mapToDouble(Assistant::getFitness).sum();

        fit += other.outfit.getFitness(c, bottomFitness, topFitness);
        fit += other.body.getCharismaBonus(c, this);
        // Extreme situations
        if (other.arousal.isAtUnfavorableExtreme()) {
            fit -= 50;
        }
        // will power empty is a loss waiting to happen
        if (other.willpower.isAtUnfavorableExtreme()) {
            fit -= 100;
        }
        if (other.stamina.isAtUnfavorableExtreme()) {
            fit -= staminaMod * 3;
        }
        fit += other.getWillpower().getReal() * 5.33f;
        // Short-term: Arousal
        fit += arousalMod / usum * 100.0f * (other.getArousal().max() - other.getArousal().get()) / Math
                        .min(100, other.getArousal().max());
        // Mid-term: Stamina
        fit += staminaMod / usum * 50.0f * (1 - Math
                        .exp(-((float) other.getStamina().get()) / Math.min(other.getStamina().max(), 100.0f)));
        // Long term: Mojo
        fit += mojoMod / usum * 50.0f * (1 - Math
                        .exp(-((float) other.getMojo().get()) / Math.min(other.getMojo().max(), 40.0f)));
        for (Status status : other.getStatuses()) {
            fit += status.fitnessModifier();
        }
        // hack to make the AI favor making the opponent cum
        fit -= 100 * other.orgasms;
        // special case where if you lost, you are super super unfit.
        if (other.orgasmed && other.getWillpower().isAtUnfavorableExtreme()) {
            fit -= 1000;
        }
        return fit;
    }

    public float getFitness(Combat c) {

        float fit = 0;
        // Urgency marks
        float arousalMod = 1.0f;
        float staminaMod = 2.0f;
        float mojoMod = 1.0f;
        float usum = arousalMod + staminaMod + mojoMod;
        Character other = c.getOpponentCharacter(this);

        int totalAtts = 0;
        for (Attribute attribute : att.keySet()) {
            totalAtts += att.get(attribute);
        }
        fit += Math.sqrt(totalAtts) * 5;
        // Always important: Position

        fit += c.assistantsOf(this).stream().mapToDouble(Assistant::getFitness).sum();

        int escape = getEscape(c, other);
        if (escape > 1) {
            fit += 8 * Math.log(escape);
        } else if (escape < -1) {
            fit += -8 * Math.log(-escape);
        }
        // what an average piece of clothing should be worth in fitness
        double topFitness = 4.0;
        double bottomFitness = 4.0;
        // If I'm horny, I don't care about my clothing, so I put more less
        // fitness in them
        if (getMood() == Emotion.horny || is(Stsflag.feral)) {
            topFitness = .5;
            bottomFitness = .5;
            // If I'm horny, I put less importance on my own arousal
            arousalMod = .7f;
        }
        fit += outfit.getFitness(c, bottomFitness, topFitness);
        fit += body.getCharismaBonus(c, other);
        if (c.getStance().inserted()) { // If we are fucking...
            // ...we need to see if that's beneficial to us.
            fit += body.penetrationFitnessModifier(this, other, c.getStance().inserted(this),
                            c.getStance().anallyPenetrated(c));
        }
        if (hasDick()) {
            fit += (dickPreference() - 3) * 4;
        }

        if (hasPussy()) {
            fit += (pussyPreference() - 3) * 4;
        }
        if (has(Trait.pheromones)) {
            fit += 5 * getPheromonePower();
            fit += 15 * getPheromonesChance(c) * (2 + getPheromonePower());
        }

        // Also somewhat of a factor: Inventory (so we don't
        // just use it without thinking)
        for (Item item : inventory.keySet()) {
            fit += (float) item.getPrice() / 10;
        }
        // Extreme situations
        if (arousal.isAtUnfavorableExtreme()) {
            fit -= 100;
        }
        if (stamina.isAtUnfavorableExtreme()) {
            fit -= staminaMod * 3;
        }
        fit += getWillpower().getReal() * 5.3f;
        // Short-term: Arousal
        fit += arousalMod / usum * 100.0f * (getArousal().max() - getArousal().get())
                        / Math.min(100, getArousal().max());
        // Mid-term: Stamina
        fit += staminaMod / usum * 50.0f
                        * (1 - Math.exp(-((float) getStamina().get()) / Math.min(getStamina().max(), 100.0f)));
        // Long term: Mojo
        fit += mojoMod / usum * 50.0f * (1 - Math.exp(-((float) getMojo().get()) / Math.min(getMojo().max(), 40.0f)));
        for (Status status : getStatuses()) {
            fit += status.fitnessModifier();
        }

        if (this instanceof NPC) {
            NPC me = (NPC) this;
            AiModifiers mods = me.ai.getAiModifiers();
            fit += mods.modPosition(c.getStance().enumerate()) * 6;
            fit += status.stream().flatMap(s -> s.flags().stream()).mapToDouble(mods::modSelfStatus).sum();
            fit += c.getOpponentCharacter(this).status.stream().flatMap(s -> s.flags().stream())
                            .mapToDouble(mods::modOpponentStatus).sum();
        }
        // hack to make the AI favor making the opponent cum
        fit -= 100 * orgasms;
        // special case where if you lost, you are super super unfit.
        if (orgasmed && getWillpower().isAtUnfavorableExtreme()) {
            fit -= 1000;
        }
        return fit;
    }

    public String nameOrPossessivePronoun() {
        return getName() + "'s";
    }

    public double getExposure(ClothingSlot slot) {
        return outfit.getExposure(slot);
    }

    public double getExposure() {
        return outfit.getExposure();
    }

    public abstract String getPortrait();

    public void modMoney(int i) {
        setMoney((int) (money + Math.round(i * Global.moneyRate)));
    }

    public void setMoney(int i) {
        money = i;
        update();
    }

    public String pronoun() {
        return getGrammar().subject().pronoun();
    }

    public Emotion getMood() {
        return Emotion.confident;
    }

    public String possessiveAdjective() {
        return getGrammar().possessiveAdjective();
    }
    
    public String possessivePronoun() {
        return getGrammar().possessivePronoun();
    }

    public String objectPronoun() {
        return getGrammar().object().pronoun();
    }
    
    public String reflexivePronoun() {
        return getGrammar().reflexivePronoun();
    }

    public boolean useFemalePronouns() {
        return hasPussy()
                || !hasDick()
                || (body.getRandomBreasts().getSize().compareTo(Size.min()) > 0 && body.getFace().getFemininity(this) > 0)
                || (body.getFace().getFemininity(this) >= 1.5)
                || (human() && Global.checkFlag(Flag.PCFemalePronounsOnly))
                || (!human() && Global.checkFlag(Flag.NPCFemalePronounsOnly));
    }

    public String nameDirectObject() {
        return getGrammar().object().defaultNoun();
    }

    public boolean clothingFuckable(BodyPart part) {
        if (part.isType(StraponPart.TYPE)) {
            return true;
        }
        if (part.isType(CockPart.TYPE)) {
            return outfit.slotEmptyOrMeetsCondition(ClothingSlot.bottom,
                            (article) -> (!article.is(ClothingTrait.armored) && !article.is(ClothingTrait.bulky)
                                            && !article.is(ClothingTrait.persistent)));
        } else if (part.isType(PussyPart.TYPE) || part.isType(AssPart.TYPE)) {
            return outfit.slotEmptyOrMeetsCondition(ClothingSlot.bottom, (article) -> {
                return article.is(ClothingTrait.skimpy) || article.is(ClothingTrait.open)
                                || article.is(ClothingTrait.flexible);
            });
        } else {
            return false;
        }
    }

    public double pussyPreference() {
        return 11 - Global.getValue(Flag.malePref);
    }

    public double dickPreference() {
        return Global.getValue(Flag.malePref);
    }

    public boolean wary() {
        return hasStatus(Stsflag.wary);
    }

    public void gain(Combat c, Item item) {
        if (c != null) {
            c.write(Global.format("<b>{self:subject-action:have|has} gained " + item.pre() + item.getName() + "</b>",
                            this, this));
        }
        gain(item, 1);
    }

    public String temptLiner(Combat c, Character target) {
        if (c.getStance().sub(this)) {
            return Global.format("{self:SUBJECT-ACTION:try} to entice {other:name-do} by wiggling suggestively in {other:possessive} grip.", this, target);
        }
        return Global.format("{self:SUBJECT-ACTION:pat} {self:possessive} groin and {self:action:promise} {self:pronoun-action:will} show {other:direct-object} a REAL good time.", this, target);
    }

    public String action(String firstPerson, String thirdPerson) {
        return thirdPerson;
    }

    public String action(String verb) {
        return action(verb, ProseUtils.getThirdPersonFromFirstPerson(verb));
    }

    public void addCooldown(Skill skill) {
        if (skill.getCooldown() <= 0) {
            return;
        }
        if (cooldowns.containsKey(skill.toString())) {
            cooldowns.put(skill.toString(), cooldowns.get(skill.toString()) + skill.getCooldown());
        } else {
            cooldowns.put(skill.toString(), skill.getCooldown());
        }
    }

    public boolean cooldownAvailable(Skill s) {
        boolean cooledDown = true;
        if (cooldowns.containsKey(s.toString()) && cooldowns.get(s.toString()) > 0) {
            cooledDown = false;
        }
        return cooledDown;
    }

    public Integer getCooldown(Skill s) {
        if (cooldowns.containsKey(s.toString()) && cooldowns.get(s.toString()) > 0) {
            return cooldowns.get(s.toString());
        } else {
            return 0;
        }
    }

    public boolean checkLoss(Combat c) {
        return (orgasmed || c.getTimer() > 150) && willpower.isAtUnfavorableExtreme();
    }

    public boolean isCustomNPC() {
        return custom;
    }

    public String recruitLiner() {
        return "";
    }

    public int stripDifficulty(Character other) {
        if (outfit.has(ClothingTrait.tentacleSuit) || outfit.has(ClothingTrait.tentacleUnderwear)) {
            return other.get(Attribute.Science) + 20;
        }
        if (outfit.has(ClothingTrait.harpoonDildo) || outfit.has(ClothingTrait.harpoonOnahole)) {
            int diff = 20;
            if (other.has(Trait.yank)) {
                diff += 5;
            }
            if (other.has(Trait.conducivetoy)) {
                diff += 5;
            }
            if (other.has(Trait.intensesuction)) {
                diff += 5;
            }
            return diff;
        }
        return 0;
    }

    public void drainWillpower(Combat c, Character drainer, int i) {
        int drained = i;
        int bonus = 0;

        for (Status s : getStatuses()) {
            bonus += s.drained(c, drained);
        }
        drained += bonus;
        if (drained >= willpower.get()) {
            drained = willpower.get();
        }
        drained = Math.max(1, drained);
        int restored = drained;
        if (c != null) {
            c.writeSystemMessage(
                            String.format("%s drained of <font color='rgb(220,130,40)'>%d<font color='white'> willpower<font color='white'> by %s",
                                            subjectWas(), drained, drainer.subject()), true);
        }
        willpower.exhaust(drained);
        drainer.willpower.recover(restored);
    }

    public void drainWillpowerAsMojo(Combat c, Character drainer, int i, float efficiency) {
        int drained = i;
        int bonus = 0;

        for (Status s : getStatuses()) {
            bonus += s.drained(c, drained);
        }
        drained += bonus;
        if (drained >= willpower.get()) {
            drained = willpower.get();
        }
        drained = Math.max(1, drained);
        int restored = Math.round(drained * efficiency);
        if (c != null) {
            c.writeSystemMessage(
                            String.format("%s drained of <font color='rgb(220,130,40)'>%d<font color='white'> willpower as <font color='rgb(100,162,240)'>%d<font color='white'> mojo by %s",
                                            subjectWas(), drained, restored, drainer.subject()), true);
        }
        willpower.exhaust(drained);
        drainer.mojo.build(restored);
    }

    public void drainStaminaAsMojo(Combat c, Character drainer, int i, float efficiency) {
        int drained = i;
        int bonus = 0;

        for (Status s : getStatuses()) {
            bonus += s.drained(c, drained);
        }
        drained += bonus;
        if (drained >= stamina.get()) {
            drained = stamina.get();
        }
        drained = Math.max(1, drained);
        int restored = Math.round(drained * efficiency);
        if (c != null) {
            c.writeSystemMessage(
                            String.format("%s drained of <font color='rgb(240,162,100)'>%d<font color='white'> stamina as <font color='rgb(100,162,240)'>%d<font color='white'> mojo by %s",
                                            subjectWas(), drained, restored, drainer.subject()), true);
        }
        stamina.exhaust(drained);
        drainer.mojo.build(restored);
    }

    public void drainMojo(Combat c, Character drainer, int i) {
        int drained = i;
        int bonus = 0;

        for (Status s : getStatuses()) {
            bonus += s.drained(c, drained);
        }
        drained += bonus;
        if (drained >= mojo.get()) {
            drained = mojo.get();
        }
        drained = Math.max(1, drained);
        if (c != null) {
            c.writeSystemMessage(
                            String.format("%s drained of <font color='rgb(0,162,240)'>%d<font color='white'> mojo by %s",
                                            subjectWas(), drained, drainer.subject()), true);
        }
        mojo.deplete(drained);
        drainer.mojo.build(drained);
    }

    // TODO: Rename this method; it has the same name as Observer's update(), which is a little
    // confusing given that this is an Observable.
    public void update() {
        setChanged();
        notifyObservers();
    }

    public Outfit getOutfit() {
        return outfit;
    }

    public boolean footAvailable() {
        Clothing article = outfit.getTopOfSlot(ClothingSlot.feet);
        return article == null || article.getLayer() < 2;
    }

    public boolean hasInsertable() {
        return hasDick() || has(Trait.strapped);
    }

    /**Checks if this character has any mods that would consider them demonic.
     * 
     *  FIXME: Shouldn't the Incubus Trait also exist and be added to this?
     *  
     * @return
     * Returns true if They have a demonic attributeModifier on their pussy or cock, or has the succubus trait.*/
    public boolean isDemonic() {
        return has(Trait.succubus) || body.getRandomPussy().moddedPartCountsAs(DemonicMod.TYPE)
            || body.getRandomCock().moddedPartCountsAs(IncubusCockMod.TYPE);
    }

    public int baseDisarm() {
        int disarm = 0;
        if (has(Trait.cautious)) {
            disarm += 5;
        }
        return disarm;
    }

    /**Helper method for getDamage() - modifies recoil pleasure damage. 
     * 
     * @return
     * Returns a floating decimal modifier.
     * */
    public float modRecoilPleasure(Combat c, float mt) {
        float total = mt;
        if (c.getStance().sub(this)) {
            total += get(Attribute.Submissive) / 2;
        }
        if (has(Trait.responsive)) {
            total += total / 2;
        }
        return total;
    }

    public boolean isPartProtected(BodyPart target) {
        return target.isType(HandsPart.TYPE) && has(ClothingTrait.nursegloves);
    }

    /**Removes temporary traits from this character. 
     * */
    public void purge(Combat c) {
        temporaryAddedTraits.clear();
        temporaryRemovedTraits.clear();
        status = status.stream().filter(s -> !s.flags().contains(Stsflag.purgable))
                        .collect(Collectors.toCollection(ArrayList::new));
        body.purge(c);
    }

    /**
     * applies bonuses and penalties for using an attribute.
     */
    public void usedAttribute(Attribute att, Combat c, double baseChance) {
        // divine recoil applies at 20% per magnitude
        if (att == Attribute.Divinity && Global.randomdouble() < baseChance) {
            add(c, new DivineRecoil(this, 1));
        }
    }

    /**
     * Attempts to knock down this character
     */
    public void knockdown(Combat c, Character other, Set<Attribute> attributes, int strength, int roll) {
        if (canKnockDown(c, other, attributes, strength, roll)) {
            add(c, new Falling(this));
        }
    }

    public int knockdownBonus() {
        return 0;
    }

    public boolean canKnockDown(Combat c, Character other, Set<Attribute> attributes, int strength, double roll) {
        return knockdownDC() < strength + (roll * 100) + attributes.stream().mapToInt(other::get).sum() + other
                        .knockdownBonus();
    }

    public boolean checkResists(ResistType type, Character other, double value, double roll) {
        switch (type) {
            case mental:
                return value < roll * 100;
            default:
                return false;
        }
    }

    /**
     * If true, count insertions by this character as voluntary
     */
    public final boolean canMakeOwnDecision() {
        return !is(Stsflag.charmed) && !is(Stsflag.lovestruck) && !is(Stsflag.frenzied);
    }

    public final String printStats() {
        return "Character{" + "name='" + name + '\'' + ", type=" + getType() + ", level=" + progression.getLevel() +
                ", xp=" + progression.getXp() + ", rank=" + progression.getRank() + ", money=" + money +
                ", att=" + att + ", stamina=" + stamina.max()
                        + ", arousal=" + arousal.max() + ", mojo=" + mojo.max() + ", willpower=" + willpower.max()
                        + ", outfit=" + outfit + ", traits=" + traits + ", inventory=" + inventory + ", flags=" + flags
                        + ", trophy=" + trophy + ", closet=" + closet + ", body=" + body + ", availableAttributePoints="
                        + availableAttributePoints + '}';
    }

    public int getMaxWillpowerPossible() {
        return Integer.MAX_VALUE;
    }

    public final boolean levelUpIfPossible(Combat c) {
        int req;
        boolean dinged = false;
        while (progression.canLevelUp()) {
            progression.levelUp();
            ding(c);
            dinged = true;
        }
        return dinged;
    }

    /**This character Makes preparations before a match starts. Called only by Match.Start()
     * 
     * */
    public void matchPrep(Match m) {
        if (has(Trait.RemoteControl)) {
            int currentCount = inventory.getOrDefault(Item.RemoteControl, 0);
            gain(Item.RemoteControl, 2 - currentCount + get(Attribute.Science) / 10);
        }
    }

    /**Compares many variables between this character and the given character. Returns true only if they are all the same.
     * 
     * @return
     * 
     * Returns true only if all values are the same. 
     * */
    public final boolean hasSameStats(Character character) {
        if (!name.equals(character.name)) {
            return false;
        }
        if (!getType().equals(character.getType())) {
            return false;
        }
        if (!progression.hasSameStats(character.progression)) {
            return false;
        }
        if (!(money == character.money)) {
            return false;
        }
        if (!att.equals(character.att)) {
            return false;
        }
        if (!(stamina.max() == character.stamina.max())) {
            return false;
        }
        if (!(arousal.max() == character.arousal.max())) {
            return false;
        }
        if (!(mojo.max() == character.mojo.max())) {
            return false;
        }
        if (!(willpower.max() == character.willpower.max())) {
            return false;
        }
        if (!outfit.equals(character.outfit)) {
            return false;
        }
        if (!(new HashSet<>(traits).equals(new HashSet<>(character.traits)))) {
            return false;
        }
        if (!inventory.equals(character.inventory)) {
            return false;
        }
        if (!flags.equals(character.flags)) {
            return false;
        }
        if (!trophy.equals(character.trophy)) {
            return false;
        }
        if (!closet.equals(character.closet)) {
            return false;
        }
        if (!body.equals(character.body)) {
            return false;
        }
        return availableAttributePoints == character.availableAttributePoints;

    }

    public final void flagStatus(Stsflag flag) {
        statusFlags.add(flag);
    }
    
    public final void unflagStatus(Stsflag flag) {
        statusFlags.remove(flag);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)                      //If it has the same memory address - DSM
            return true;
        if (o == null || !getClass().equals(o.getClass()))      //Becuse this is overridden at the character level this must be if it's a character... - DSM
            return false;                   
        if (o == Global.noneCharacter() || this == Global.noneCharacter())      
            return false;
        Character character = (Character) o;
        return getType().equals(character.getType()) && name.equals(character.name);
    }

    @Override public final int hashCode() {
        int result = getType().hashCode();
        return result * 31 + name.hashCode();
    }

    public final Growth getGrowth() {
        return growth;
    }

    public final void setGrowth(Growth growth) {
        this.growth = growth;
    }
    public final Collection<Skill> getSkills() {
        return skills;
    }
    
    /**Distributes points during levelup. Called by several classes.
     * 
     * @param preferredAttributes
     * A list of preferred attributes. 
     * 
     * 
     * */
    public final void distributePoints(List<PreferredAttribute> preferredAttributes) {
        if (availableAttributePoints <= 0) {
            return;
        }
        ArrayList<Attribute> avail = new ArrayList<Attribute>();
        Deque<PreferredAttribute> preferred = new ArrayDeque<PreferredAttribute>(preferredAttributes);
        for (Attribute a : att.keySet()) {
            if (Attribute.isTrainable(this, a) && (getPure(a) > 0 || Attribute.isBasic(this, a))) {
                avail.add(a);
            }
        }
        if (avail.size() == 0) {
            avail.add(Attribute.Cunning);
            avail.add(Attribute.Power);
            avail.add(Attribute.Seduction);
        }
        int noPrefAdded = 2;
        for (; availableAttributePoints > 0; availableAttributePoints--) {
            Attribute selected = null;
            // remove all the attributes that isn't in avail
            preferred = new ArrayDeque<>(preferred.stream()
                .filter(p -> {
                    Optional<Attribute> att = p.getPreferred(this);
                    return att.isPresent() && avail.contains(att.get());
                })
                .collect(Collectors.toList()));
            if (preferred.size() > 0) {
                if (noPrefAdded > 1) {
                    noPrefAdded = 0;
                    Optional<Attribute> pref = preferred.removeFirst()
                                                        .getPreferred(this);
                    if (pref.isPresent()) {
                        selected = pref.get();
                    }
                } else {
                    noPrefAdded += 1;
                }
            }

            if (selected == null) {
                selected = avail.get(Global.random(avail.size()));
            }
            mod(selected, 1);
            selected = null;
        }
    }

    public boolean isPetOf(Character other) {
        return false;
    }
    
    public boolean isPet() {
        return false;
    }

    public int getPetLimit() {
        return has(Trait.congregation) ? 2 : 1;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public final boolean hasStatusVariant(String variant) {
        return status.stream().anyMatch(s -> s.getVariant().equals(variant));
    }

    public final List<Addiction> getAddictions() {
        return getAdditionStream().collect(Collectors.toList());
    }

    public final List<Status> getPermanentStatuses() {
        return getStatusStreamWithFlag(Stsflag.permanent).collect(Collectors.toList());
    }

    public final Stream<Status> getStatusStreamWithFlag(Stsflag flag) {
        return status.stream().filter(status -> status.flags().contains(flag));
    }

    public final Stream<Addiction> getAdditionStream() {
        return status.stream().filter(status -> status instanceof Addiction).map(s -> (Addiction)s);
    }

    public final boolean hasAddiction(AddictionType type) {
        return getAdditionStream().anyMatch(a -> a.getType() == type);
    }

    public final Optional<Addiction> getAddiction(AddictionType type) {
        return getAdditionStream().filter(a -> a.getType() == type).findAny();
    }

    public final Optional<Addiction> getStrongestAddiction() {
        return getAdditionStream().max(Comparator.comparing(Addiction::getSeverity));
    }

    /**Processes addiction gain for this character. 
     * @param c
     * The combat required for this method.
     * 
     * @param type
     * The type of addiction being processed.
     * 
     * @param cause
     * The cuase of this addiction. 
     * 
     * @param mag
     * The magnitude to increase the addiction.
     * 
     * */
    private static final Set<AddictionType> NPC_ADDICTABLES = EnumSet.of(AddictionType.CORRUPTION);                     
    public final void addict(Combat c, AddictionType type, Character cause, float mag) {
        boolean dbg = false;
        if (!human() && !NPC_ADDICTABLES.contains(type)) {
            if (dbg) {
                System.out.printf("Skipping %s addiction on %s because it's not supported for NPCs", type.name(), getType());
            }
        }
        Optional<Addiction> addiction = getAddiction(type);
        if (addiction.isPresent() && Objects.equals(addiction.map(Addiction::getCause).orElse(null), cause)) {
            if (dbg) {
                System.out.printf("Aggravating %s on player by %.3f\n", type.name(), mag);
            }
            Addiction a = addiction.get();
            a.aggravate(c, mag);
            if (dbg) {
                System.out.printf("%s magnitude is now %.3f\n", a.getType()
                                                                 .name(),
                                a.getMagnitude());
            }
        } else {
            if (dbg) {
                System.out.printf("Creating initial %s on player with %.3f\n", type.name(), mag);
            }
            Addiction addict = type.build(this, cause, mag);
            add(c, addict);
            addict.describeInitial();
        }
    }

    /**The reverse of Character.addict(). this alleviates the addiction by type.
     * 
     * @param c
     * The combat required for this method.
     * 
     * @param type
     * The type of addiction being processed.
     * 
     * @param mag
     * The magnitude to decrease the addiction.
     * 
     * */
    public final void unaddict(Combat c, AddictionType type, float mag) {
        boolean dbg = false;
        if (dbg) {
            System.out.printf("Alleviating %s on player by %.3f\n", type.name(), mag);
        }
        Optional<Addiction> addiction = getAddiction(type);
        if (!addiction.isPresent()) {
            return;
        }
        Addiction addict = addiction.get();
        addict.alleviate(c, mag);
        if (addict.shouldRemove()) {
            if (dbg) {
                System.out.printf("Removing %s from player", type.name());
            }
            removeStatusImmediately(addict);
        }
    }

    /**Removes the given status from this character. Used by Addiction removal.*/
    public final void removeStatusImmediately(Status status) {
        this.status.remove(status);
    }

    /**Processes addiction
     * 
     * FIXME: This method currently has no hits in the cal lhierarchy - is this method unused or deprecated? - DSM
     * 
     *  * @param c
     * The combat required for this method.
     * 
     * @param type
     * The type of addiction being processed.
     * 
     * @param cause
     * The cuase of this addiction. 
     * 
     * @param mag
     * The magnitude to increase the addiction.
     * */
    public final void addictCombat(AddictionType type, Character cause, float mag, Combat c) {
        boolean dbg = false;
        Optional<Addiction> addiction = getAddiction(type);
        if (addiction.isPresent()) {
            if (dbg) {
                System.out.printf("Aggravating %s on player by %.3f (Combat vs %s)\n", type.name(), mag,
                                cause.getTrueName());
            }
            Addiction a = addiction.get();
            a.aggravateCombat(c, mag);
            if (dbg) {
                System.out.printf("%s magnitude is now %.3f\n", a.getType()
                                                                 .name(),
                                a.getMagnitude());
            }
        } else {
            if (dbg) {
                System.out.printf("Creating initial %s on player with %.3f (Combat vs %s)\n", type.name(), mag,
                                cause.getTrueName());
            }
            Addiction addict = type.build(this, cause, Addiction.LOW_THRESHOLD);
            addict.aggravateCombat(c, mag);
            add(c, addict);
        }
    }

    /**The reverse of Character.addict(). this alleviates the addiction by type. Called by many resolve() methods of skills.
     * 
     * @param c
     * The combat required for this method.
     * 
     * @param type
     * The type of addiction being processed.
     * 
     * @param mag
     * The magnitude to decrease the addiction.
     * 
     * */
    public final void unaddictCombat(AddictionType type, Character cause, float mag, Combat c) {
        boolean dbg = false;
        Optional<Addiction> addict = getAddiction(type);
        if (addict.isPresent()) {
            if (dbg) {
                System.out.printf("Alleviating %s on player by %.3f (Combat vs %s)\n", type.name(), mag,
                                cause.getTrueName());
            }
            addict.get().alleviateCombat(c, mag);
        }
    }

    public final Severity getAddictionSeverity(AddictionType type) {
        return getAddiction(type).map(Addiction::getSeverity).orElse(Severity.NONE);
    }

    public final boolean checkAddiction() {
        return getAdditionStream().anyMatch(a -> a.atLeast(Severity.LOW));
    }
    
    public final boolean checkAddiction(AddictionType type) {
        return getAddiction(type).map(Addiction::isActive).orElse(false);
    }
    
    public final boolean checkAddiction(AddictionType type, Character cause) {
        return getAddiction(type).map(addiction -> addiction.isActive() && addiction.wasCausedBy(cause)).orElse(false);
    }

    public String loserLiner(Combat c, Character target) {
        return Global.format("{self:SUBJECT-ACTION:try} seems dissatisfied with losing so badly.", this, target);
    }

    public String victoryLiner(Combat c, Character target) {
        return Global.format("{self:SUBJECT-ACTION:try} smiles in satisfaction with their victory.", this, target);
    }

    public int exercise(Exercise source) {
        int maximumStaminaForLevel = Configuration.getMaximumStaminaPossible(this);
        int gain = 1 + Global.random(2);
        if (has(Trait.fitnessNut)) {
            gain = gain + Global.random(2);
        }
        gain = Math.max(0,
            (Math.min(maximumStaminaForLevel, stamina.max() + gain) - stamina.max()));
        stamina.gain(gain);
        return gain;
    }

    public int porn(Porn source) {
        int maximumArousalForLevel = Configuration.getMaximumArousalPossible(this);
        int gain = 1 + Global.random(2);
        if (has(Trait.expertGoogler)) {
            gain = gain + Global.random(2);
        }
        gain = Math.max(0, Math.min(maximumArousalForLevel, arousal.max() + gain) -  arousal.max());
        arousal.gain(gain);
        return gain;
    }

    public void chooseLocateTarget(Map<Character, Runnable> potentialTargets, Runnable noneOption, String msg) {
        throw new UnsupportedOperationException("attempted to choose locate target");
    }

    public void leaveAction(Runnable callback) {
        throw new UnsupportedOperationException(String.format("attempted to leave locate action"));
    }

    public void chooseShopOption(Store shop, Collection<Loot> items,
        List<String> additionalChoices) {
        throw new UnsupportedOperationException(
            String.format("attempted to choose options in shop %s", shop.toString()));
    }

    // displayTexts and prices are expected to be 1:1
    public void chooseBodyShopOption(BodyShop shop, List<String> displayText,
        List<Integer> prices, List<String> additionalChoices) {
        throw new UnsupportedOperationException(
            String.format("attempted to access options from %s", shop.toString()));
    }

    public void nextCombat(Combat c) {
        // Can't be sure this isn't used at the moment
    }

    public void sceneNext(Scene s) {
        // Can't be sure this isn't used at the moment
    }

    public void chooseActivitySubchoices(Activity activity, List<String> choices) {
        // Can't be sure this isn't used at the moment
    }

    public Set<Action> getItemActions() {
        var res = new HashSet<Action>();
        var inv = getInventory();
        for (Item i: inv.keySet()) {
            if (inv.get(i) > 0) {
                switch (i) {
                    case Beer:
                        res.add(new UseBeer());
                    case Lubricant:
                        res.add(new UseLubricant());
                    case EnergyDrink:
                        res.add(new UseEnergyDrink());
                }
            }
        }
        return res;
    }

    public String masterOrMistress() {
        return useFemalePronouns() ? "mistress" : "master";
    }

    public Optional<ArmManager> getArmManager() {
        return Optional.empty();
    }

    public void orgasm() {
        if (has(Trait.insatiable)) {
            Insatiable.renewArousal(arousal);
        } else {
            arousal.renew();
        }
    }

    public Optional<Integer> getAttribute(Attribute a) {
        return Optional.ofNullable(att.get(a));
    }

    public abstract Person getGrammar();

    public void notifyCombatStart(Combat c, Character opponent) {}

    public void message(String message) { }

    public void sendVictoryMessage(Combat c, Result flag) {}

    public void sendDefeatMessage(Combat c, Result flag) {}

    public void sendDrawMessage(Combat c, Result flag) {}

    public abstract Intelligence makeIntelligence();

    public abstract Dialog makeDialog();

    public void notifyStanceImage(String path) {};
}
