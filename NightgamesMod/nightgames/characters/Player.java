package nightgames.characters;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import nightgames.actions.Action;
import nightgames.actions.Leap;
import nightgames.actions.Locate;
import nightgames.actions.Move;
import nightgames.actions.Shortcut;
import nightgames.areas.Area;
import nightgames.areas.Area.EncounterResult;
import nightgames.areas.Deployable;
import nightgames.characters.body.BodyPart;
import nightgames.characters.body.BreastsPart;
import nightgames.characters.body.BreastsPart.Size;
import nightgames.characters.body.CockMod;
import nightgames.characters.body.GenericBodyPart;
import nightgames.characters.body.TentaclePart;
import nightgames.characters.body.mods.GooeyMod;
import nightgames.characters.body.mods.SlimyCockMod;
import nightgames.combat.Combat;
import nightgames.combat.CombatSceneChoice;
import nightgames.combat.Result;
import nightgames.daytime.Activity;
import nightgames.daytime.BodyShop;
import nightgames.daytime.Exercise;
import nightgames.daytime.Porn;
import nightgames.daytime.Store;
import nightgames.global.Encs;
import nightgames.global.Flag;
import nightgames.global.Global;
import nightgames.global.Scene;
import nightgames.gui.commandpanel.CommandPanelOption;
import nightgames.gui.GUI;
import nightgames.items.Item;
import nightgames.items.Loot;
import nightgames.items.clothing.Clothing;
import nightgames.match.Encounter;
import nightgames.match.MatchType;
import nightgames.match.ftc.FTCMatch;
import nightgames.skills.Nothing;
import nightgames.skills.Skill;
import nightgames.skills.SkillGroup;
import nightgames.skills.Stage;
import nightgames.skills.Tactics;
import nightgames.skills.damage.DamageType;
import nightgames.stance.Behind;
import nightgames.stance.Neutral;
import nightgames.stance.Position;
import nightgames.start.PlayerConfiguration;
import nightgames.status.Enthralled;
import nightgames.status.PlayerSlimeDummy;
import nightgames.status.Status;
import nightgames.status.Stsflag;
import nightgames.trap.Trap;

public class Player extends Character {

    private GUI gui;
    int traitPoints;
    private int levelsToGain;
    private boolean skippedFeat;

    public Player(String name, GUI gui) {
        this(name, gui, CharacterSex.male, Optional.empty(), new ArrayList<>(), new HashMap<>());
    }

    // TODO(Ryplinn): This initialization pattern is very close to that of BasePersonality. I think it makes sense to make NPC the primary parent of characters instead of BasePersonality.
    public Player(String name, GUI gui, CharacterSex sex, Optional<PlayerConfiguration> config,
        List<Trait> pickedTraits,
                    Map<Attribute, Integer> selectedAttributes) {
        super(name, 1);
        this.gui = gui;
        initialGender = sex;
        levelsToGain = 0;
        applyBasicStats(this);
        setGrowth();

        body.makeGenitalOrgans(initialGender);

        config.ifPresent(this::applyConfigStats);
        finishCharacter(pickedTraits, selectedAttributes);
    }

    @Override
    public void finishClone(Character other) {
        super.finishClone(other);
    }

    public void applyBasicStats(Character self) {
        self.getStamina().setMax(80);
        self.getArousal().setMax(80);
        self.getWillpower().setMax(self.willpower.max());
        self.availableAttributePoints = 0;
        self.setTrophy(Item.PlayerTrophy);
    }

    private void applyConfigStats(PlayerConfiguration config) {
        config.apply(this);
    }

    private void finishCharacter(List<Trait> pickedTraits, Map<Attribute, Integer> selectedAttributes) {
        pickedTraits.forEach(this::addTraitDontSaveData);
        att.putAll(selectedAttributes);
        change();
        body.finishBody(initialGender);
    }

    public void setGrowth() {
        getGrowth().stamina = 20;
        getGrowth().arousal = 20;
        getGrowth().willpower = 10.4f;
        getGrowth().bonusStamina = 10;
        getGrowth().bonusArousal = 20;
        getGrowth().attributes = new int[]{2, 3, 3, 3};
    }

    public String describeStatus() {
        StringBuilder b = new StringBuilder();
        if (gui.combat != null && (gui.combat.p1.human() || gui.combat.p2.human())) {
            body.describeBodyText(b, gui.combat.getOpponent(this), false);
        } else {
            body.describeBodyText(b, Global.getCharacterByType("Angel"), false);
        }
        if (getTraits().size() > 0) {
            b.append("<br/>Traits:<br/>");
            List<Trait> traits = new ArrayList<>(getTraits());
            traits.removeIf(t -> t.isOverridden(this));
            traits.sort(Comparator.comparing(Trait::toString));
            b.append(traits.stream()
                           .map(Object::toString)
                           .collect(Collectors.joining(", ")));
        }
        if (status.size() > 0) {
            b.append("<br/><br/>Statuses:<br/>");
            List<Status> statuses = new ArrayList<>(status);
            statuses.sort(Comparator.comparing(status -> status.name));
            b.append(statuses.stream()
                             .map(s -> s.name)
                             .collect(Collectors.joining(", ")));
        }
        return b.toString();
    }

    @Override
    public String describe(int per, Combat c) {
        String description = "<i>";
        for (Status s : status) {
            description = description + s.describe(c) + "<br/>";
        }
        description = description + "</i>";
        description = description + outfit.describe(this);
        if (per >= 5 && status.size() > 0) {
            description += "<br/>List of statuses:<br/><i>";
            description += status.stream().map(Status::toString).collect(Collectors.joining(", "));
            description += "</i><br/>";
        }
        description += Stage.describe(this);
        
        return description;
    }

    @Override
    public void victory(Combat c, Result flag) {
        if (has(Trait.slime)) {
            purge(c);
        }
        if (c.p1.human()) {
            c.p2.defeat(c, flag);

            if (Global.getButtslutQuest().isPresent()) {
                Global.getButtslutQuest().get().addPlayerWonPoint(c.p2);
            }
        } else {
            c.p1.defeat(c, flag);
            if (Global.getButtslutQuest().isPresent()) {
                Global.getButtslutQuest().get().addPlayerWonPoint(c.p1);
            }

        }
    }

    @Override
    public void defeat(Combat c, Result flag) {
        c.write("Bad thing");
        if (has(Trait.slime)) {
            purge(c);
        }
    }

    @Override
    public boolean act(Combat c) {
        Character target;
        if (c.p1 == this) {
            target = c.p2;
        } else {
            target = c.p1;
        }
        pickSkills(c, target);
        return true;
    }

    /**Adds skills to the GUI*/
    private void pickSkills(Combat c, Character target) {
        HashSet<Skill> available = new HashSet<>();
        HashSet<Skill> cds = new HashSet<>();
        for (Skill a : getSkills()) {
            if (Skill.isUsable(c, a)) {
                if (cooldownAvailable(a)) {
                    available.add(a);
                } else {
                    cds.add(a);
                }
            }
        }
        HashMap<Tactics, HashSet<Skill>> skillMap = new HashMap<>();
        Skill.filterAllowedSkills(c, available, this, target);
        if (available.size() == 0) {
            available.add(new Nothing(this));
        }
        available.addAll(cds);
        available.forEach(skill -> {
            if (!skillMap.containsKey(skill.type(c))) {
                skillMap.put(skill.type(c), new HashSet<>());
            }
            skillMap.get(skill.type(c)).add(skill);
        });
        ArrayList<SkillGroup> skillGroups = new ArrayList<>();
        skillMap.forEach((tactic, skills) -> skillGroups.add(new SkillGroup(tactic, skills)));

        gui.chooseSkills(c, target, skillGroups);
    }


    /**Overridden abstract method for determining if this character is human - meaning the player. 
     * TODO: Reccomend renaming to isHuman(), to make more meaningful name and easier to find.*/
    @Override
    public boolean human() {
        return true;
    }

    @Override
    public void draw(Combat c, Result flag) {
        if (has(Trait.slime)) {
            purge(c);
        }
        if (c.p1.human()) {
            c.p2.draw(c, flag);
        } else {
            c.p1.draw(c, flag);
        }
    }

    @Override
    public String bbLiner(Combat c, Character target) {
        return null;
    }

    @Override
    public String nakedLiner(Combat c, Character target) {
        return null;
    }

    @Override
    public String stunLiner(Combat c, Character target) {
        return null;
    }

    @Override
    public String taunt(Combat c, Character target) {
        return null;
    }

    private ActionListener encounterOption(Encounter enc, Character target, Encs choice) {
        return event -> {
            enc.parse(choice, Global.getPlayer(), target);
            Global.getMatch().resume();
        };
    }

    private void detect() {
        for (Area adjacent : location.adjacent) {
            if (adjacent.ping(get(Attribute.Perception))) {
                gui.message("You hear something in the <b>" + adjacent.name + "</b>.");
                adjacent.setPinged(true);
            }
        }
    }

    @Override
    public void faceOff(Character opponent, Encounter enc) {
        gui.message("You run into <b>" + opponent.nameDirectObject()
                        + "</b> and you both hesitate for a moment, deciding whether to attack or retreat.");
        assessOpponent(opponent);
        gui.message("<br/>");
        ArrayList<CommandPanelOption> options = new ArrayList<>();
        options.add(new CommandPanelOption("Fight",
            encounterOption(enc, opponent, Encs.fight)));
        options.add(new CommandPanelOption("Flee",
            encounterOption(enc, opponent, Encs.flee)));
        gui.presentOptions(options);
        Global.getMatch().pause();
    }

    private void assessOpponent(Character opponent) {
        String arousal;
        String stamina;
        if (opponent.state == State.webbed) {
            gui.message("She is naked and helpless.<br/>");
            return;
        }
        if (get(Attribute.Perception) >= 6) {
            gui.message("She is level " + opponent.getLevel());
        }
        if (get(Attribute.Perception) >= 8) {
            gui.message("Her Power is " + opponent.get(Attribute.Power) + ", her Cunning is "
                            + opponent.get(Attribute.Cunning) + ", and her Seduction is "
                            + opponent.get(Attribute.Seduction));
        }
        if (opponent.mostlyNude() || opponent.state == State.shower) {
            gui.message("She is completely naked.");
        } else {
            gui.message("She is dressed and ready to fight.");
        }
        if (get(Attribute.Perception) >= 4) {
            if (opponent.getArousal()
                        .percent() > 70) {
                arousal = "horny";
            } else if (opponent.getArousal()
                               .percent() > 30) {
                arousal = "slightly aroused";
            } else {
                arousal = "composed";
            }
            if (opponent.getStamina()
                        .percent() < 50) {
                stamina = "tired";
            } else {
                stamina = "eager";
            }
            gui.message("She looks " + stamina + " and " + arousal + ".");
        }
    }

    @Override
    public void spy(Character opponent, Encounter enc) {
        gui.message("You spot <b>" + opponent.nameDirectObject()
                        + "</b> but she hasn't seen you yet. You could probably catch her off guard, or you could remain hidden and hope she doesn't notice you.");
        assessOpponent(opponent);
        gui.message("<br/>");

        ArrayList<CommandPanelOption> options = new ArrayList<>();
        options.add(new CommandPanelOption("Fight",
            encounterOption(enc, opponent, Encs.fight)));
        options.add(new CommandPanelOption("Flee",
            encounterOption(enc, opponent, Encs.flee)));
        gui.presentOptions(options);

        Global.getMatch().pause();
    }

    @Override
    public void move() {
        System.out.println("move called");
        List<Action> actionChoices = new ArrayList<>();
        List<CommandPanelOption> optionChoices = new ArrayList<>();
        if (state == State.combat) {
            if (!location.fight.battle()) {
                Global.getMatch().resume();
            }
        } else if (busy > 0) {
            busy--;
        } else if (this.is(Stsflag.enthralled)) {
            Character master;
            master = ((Enthralled) getStatus(Stsflag.enthralled)).master;
            if (master != null) {
                Move compelled = findPath(master.location());
                gui.message("You feel an irresistible compulsion to head to the <b>" + master.location().name + "</b>");
                if (compelled != null) {
                    actionChoices.add(compelled);
                }
            }
        } else if (state == State.shower || state == State.lostclothes) {
            bathe();
        } else if (state == State.crafting) {
            craft();
        } else if (state == State.searching) {
            search();
        } else if (state == State.resupplying) {
            resupply();
        } else if (state == State.webbed) {
            gui.message("You eventually manage to get an arm free, which you then use to extract yourself from the trap.");
            state = State.ready;
        } else if (state == State.masturbating) {
            masturbate();
        } else {
            if (Global.checkFlag(Flag.FTC)) {
                Character holder = ((FTCMatch) Global.getMatch()).getFlagHolder();
                if (holder != null && !holder.human()) {
                    gui.message("<b>" + holder.subject() + " currently holds the Flag.</b></br>");
                }
            }
            gui.message(location.description + "<br/><br/>");
            for (Deployable trap : location.env) {
                if (trap.owner() == this) {
                    gui.message("You've set a " + trap.toString() + " here.");
                }
            }
            if (state == State.inTree) {
                gui.message("You are hiding in a tree, waiting to drop down on an unwitting foe.");
            } else if (state == State.inBushes) {
                gui.message("You are hiding in dense bushes, waiting for someone to pass by.");
            } else if (state == State.inPass) {
                gui.message("You are hiding in an alcove in the pass.");
            } else if (state == State.hidden) {
                gui.message("You have found a hiding spot and are waiting for someone to pounce upon.");
            }
            detect();
            EncounterResult result = location.encounter(this);
            optionChoices.addAll(result.options);
            if (!result.exclusive) {
                if (!allowedActions().isEmpty()) {
                    actionChoices.addAll(allowedActions());
                } else {
                    List<Action> possibleActions = new ArrayList<>();
                    if (Global.getMatch().canMoveOutOfCombat(this)) {
                        possibleActions.addAll(location.possibleActions());
                    }
                    possibleActions.addAll(getItemActions());
                    possibleActions.addAll(Global.getMatch().getAvailableActions(this));
                    for (Action act : possibleActions) {
                        if (act.usable(this) 
                            && Global.getMatch().getCondition().allowAction(act, this, Global.getMatch())) {
                            actionChoices.add(act);
                        }
                    }
                }
            }
        }
        optionChoices.addAll(actionChoices.stream()
            .map(action -> new CommandPanelOption(
                action.toString(),
                event -> {
                    action.execute(this);
                    if (!action.freeAction()) {
                        Global.getMatch().resume();
                    }
                })).collect(Collectors.toList()));

        if (!optionChoices.isEmpty()) {
            // Otherwise someone else is going to provide choices
            gui.presentOptions(optionChoices);
            Global.getMatch().pause();
        }
    }

    @Override
    public void ding(Combat c) {
        levelsToGain += 1;
        if (levelsToGain == 1) {
            actuallyDing();
            if (cloned == 0) {
                if (c != null) {
                    c.pause();
                }
                handleLevelUp();
            }
        }
    }

    public void handleLevelUp() {
        if (availableAttributePoints > 0) {
            gui.message(this, availableAttributePoints + " Attribute Points remain.</br>");

            List<CommandPanelOption> options = att.keySet().stream()
                .filter(a -> Attribute.isTrainable(this, a) && getPure(a) > 0)
                .map(this::optionToSelectAttribute)
                .collect(Collectors.toList());
            options.add(optionToSelectAttribute(Attribute.Willpower));
            gui.presentOptions(options);

            if (Global.getMatch() != null) {
                Global.getMatch().pause();
            }
        } else if (traitPoints > 0 && !skippedFeat) {
            gui.message(this, "You've earned a new perk. Select one below.</br>");

            List<CommandPanelOption> options = Global.getFeats(this).stream()
                .filter(t -> !has(t))
                .map(this::optionToSelectTrait)
                .collect(Collectors.toList());
            options.add(optionToSelectTrait(null));
            gui.presentOptions(options);
        } else {
            skippedFeat = false;
            Global.gainSkills(this);
            levelsToGain -= 1;
            if (getLevelsToGain() > 0) {
                actuallyDing();
                handleLevelUp();
            } else {
                if (gui.combat != null) {
                    gui.combat.resume();
                } else if (Global.getMatch() != null) {
                    Global.getMatch().resume();
                } else if (Global.day != null) {
                    Global.getDay().plan();
                } else {
                    MatchType.NORMAL.runPrematch();
                }
            }
        }
    }

    private CommandPanelOption optionToSelectAttribute(Attribute a) {
        return new CommandPanelOption(a.name(), event -> increaseAttribute(a));
    }

    private CommandPanelOption optionToSelectTrait(Trait t) {
        CommandPanelOption o = new CommandPanelOption("Skip",
            "Save the trait point for later.",
            event -> {
                skipFeat();
                handleLevelUp();
            });
        if (t != null) {
            o = new CommandPanelOption(t.toString(), t.getDesc(), event -> grantTrait(t));
        }
        return o;
    }

    private void skipFeat() {
        skippedFeat = true;
    }

    private void increaseAttribute(Attribute attribute) {
        if (availableAttributePoints < 1) {
            throw new RuntimeException("attempted to increase attributes with no points remaining");
        }
        mod(attribute, 1);
        availableAttributePoints -= 1;
        handleLevelUp();
    }

    private void grantTrait(Trait trait) {
        if (traitPoints < 1) {
            throw new RuntimeException("attempted to grant trait without trait points");
        }
        gui.message(this, "Gained feat: " + trait.toString());
        add(trait);
        Global.gainSkills(this);
        traitPoints -= 1;
        handleLevelUp();
    }

    private void actuallyDing() {
        level += 1;
        getStamina().gain(getGrowth().stamina);
        getArousal().gain(getGrowth().arousal);
        availableAttributePoints += getGrowth().attributes[Math.min(rank, getGrowth().attributes.length-1)];
        gui.message(this, "You've gained a Level!<br/>Select which attributes to increase.");
        if (getLevel() % 3 == 0 && level < 10 || (getLevel() + 1) % 2 == 0 && level > 10) {
            traitPoints += 1;
        }
    }

    @Override
    public int getMaxWillpowerPossible() {
        return 50 + getLevel() * 5 - get(Attribute.Submissive) * 2;
    }

    @Override
    public void flee(Area location2) {
        Area[] adjacent = location2.adjacent.toArray(new Area[location2.adjacent.size()]);
        Area destination = adjacent[Global.random(adjacent.length)];
        gui.message("You dash away and escape into the <b>" + destination.name + ".</b>");
        travel(destination);
        location2.endEncounter();
    }

    @Override
    public void bathe() {
        status.removeIf(s -> s.flags().contains(Stsflag.purgable));
        stamina.fill();
        if (location.name.equals("Showers")) {
            gui.message("You let the hot water wash away your exhaustion and soon you're back to peak condition.");
        }
        if (location.name.equals("Pool")) {
            gui.message("The hot water soothes and relaxes your muscles. You feel a bit exposed, skinny-dipping in such an open area. You decide it's time to get moving.");
        }
        if (state == State.lostclothes) {
            gui.message("Your clothes aren't where you left them. Someone must have come by and taken them.");
        }
        state = State.ready;
    }

    @Override
    public void craft() {
        int roll = Global.random(10);
        gui.message("You spend some time crafting some potions with the equipment.");
        if (check(Attribute.Cunning, 25)) {
            if (roll == 9) {
                gain(Item.Aphrodisiac);
                gain(Item.DisSol);
            } else if (roll >= 5) {
                gain(Item.Aphrodisiac);
            } else {
                gain(Item.Lubricant);
                gain(Item.Sedative);
            }
        } else if (check(Attribute.Cunning, 20)) {
            if (roll == 9) {
                gain(Item.Aphrodisiac);
            } else if (roll >= 7) {
                gain(Item.DisSol);
            } else if (roll >= 5) {
                gain(Item.Lubricant);
            } else if (roll >= 3) {
                gain(Item.Sedative);
            } else {
                gain(Item.EnergyDrink);
            }
        } else if (check(Attribute.Cunning, 15)) {
            if (roll == 9) {
                gain(Item.Aphrodisiac);
            } else if (roll >= 8) {
                gain(Item.DisSol);
            } else if (roll >= 7) {
                gain(Item.Lubricant);
            } else if (roll >= 6) {
                gain(Item.EnergyDrink);
            } else {
                gui.message("Your concoction turns a sickly color and releases a foul smelling smoke. You trash it before you do any more damage.");
            }
        } else {
            if (roll >= 7) {
                gain(Item.Lubricant);
            } else if (roll >= 5) {
                gain(Item.Sedative);
            } else {
                gui.message("Your concoction turns a sickly color and releases a foul smelling smoke. You trash it before you do any more damage.");
            }
        }
        state = State.ready;
    }

    @Override
    public void search() {
        int roll = Global.random(10);
        switch (roll) {
            case 9:
                gain(Item.Tripwire);
                gain(Item.Tripwire);
                break;
            case 8:
                gain(Item.ZipTie);
                gain(Item.ZipTie);
                gain(Item.ZipTie);
                break;
            case 7:
                gain(Item.Phone);
                break;
            case 6:
                gain(Item.Rope);
                break;
            case 5:
                gain(Item.Spring);
                break;
            default:
                gui.message("You don't find anything useful.");
        }
        state = State.ready;
    }

    @Override
    public void masturbate() {
        gui.message("You hurriedly stroke yourself off, eager to finish before someone catches you. After what seems like an eternity, you ejaculate into a tissue and "
                        + "throw it in the trash. Looks like you got away with it.");
        arousal.empty();
        state = State.ready;
    }

    @Override
    public void showerScene(Character target, Encounter encounter) {
        if (target.location().name.equals("Showers")) {
            gui.message("You hear running water coming from the first floor showers. There shouldn't be any residents on this floor right now, so it's likely one "
                            + "of your opponents. You peek inside and sure enough, <b>" + target.subject()
                            + "</b> is taking a shower and looking quite vulnerable. Do you take advantage "
                            + "of her carelessness?");
        } else if (target.location().name.equals("Pool")) {
            gui.message("You stumble upon <b>" + target.nameDirectObject()
                            + "</b> skinny dipping in the pool. She hasn't noticed you yet. It would be pretty easy to catch her off-guard.");
        }
        assessOpponent(target);
        gui.message("<br/>");

        ArrayList<CommandPanelOption> options = new ArrayList<>();
        options.add(new CommandPanelOption("Surprise Her",
            encounterOption(encounter, target, Encs.showerattack)));
        if (!target.mostlyNude()) {
            options.add(new CommandPanelOption("Steal Clothes",
                encounterOption(encounter, target, Encs.stealclothes)));
        }
        if (has(Item.Aphrodisiac)) {
            options.add(new CommandPanelOption("Use Aphrodisiac",
                encounterOption(encounter, target, Encs.aphrodisiactrick)));
        }
        options.add(new CommandPanelOption("Do Nothing",
            encounterOption(encounter, target, Encs.wait)));
        gui.presentOptions(options);
        Global.getMatch().pause();
    }

    @Override
    public List<CommandPanelOption> intervene(Encounter enc, Character p1, Character p2) {
        gui.message("You find <b>" + p1.getName() + "</b> and <b>" + p2.getName()
                        + "</b> fighting too intensely to notice your arrival. If you intervene now, it'll essentially decide the winner.");
        gui.message("Then again, you could just wait and see which one of them comes out on top. It'd be entertaining,"
                        + " at the very least.");

        ArrayList<CommandPanelOption> options = new ArrayList<>();
        options.add(new CommandPanelOption("Help " + p1.getName(),
            event -> enc.intrude(Global.getPlayer(), p1)));
        options.add(new CommandPanelOption("Help " + p2.getName(),
            event -> enc.intrude(Global.getPlayer(), p2)));
        options.add(new CommandPanelOption("Watch them fight",
            event -> enc.watch()));
        Global.getMatch().pause();
        return options;
    }

    @Override
    public void intervene3p(Combat c, Character target, Character assist) {
        gainXP(getAssistXP(target));
        target.defeated(this);
        assist.gainAttraction(this, 1);
        c.write("You take your time, approaching " + target.getName() + " and " + assist.getName() + " stealthily. "
                        + assist.getName() + " notices you first and before her reaction "
                        + "gives you away, you quickly lunge and grab " + target.getName()
                        + " from behind. She freezes in surprise for just a second, but that's all you need to "
                        + "restrain her arms and leave her completely helpless. Both your hands are occupied holding her, so you focus on kissing and licking the "
                        + "sensitive nape of her neck.<br/><br/>");
    }

    @Override
    public void victory3p(Combat c, Character target, Character assist) {
        gainXP(getVictoryXP(target));
        target.gainXP(target.getDefeatXP(this));
        target.arousal.empty();
        if (target.has(Trait.insatiable)) {
            target.arousal.restore((int) (arousal.max() * .2));
        }
        dress(c);
        target.undress(c);
        gainTrophy(c, target);
        target.defeated(this);
        if (target.hasDick()) {
            c.write(String.format(
                            "You position yourself between %s's legs, gently "
                                            + "forcing them open with your knees. %s dick stands erect, fully "
                                            + "exposed and ready for attention. You grip the needy member and "
                                            + "start jerking it with a practiced hand. %s moans softly, but seems"
                                            + " to be able to handle this level of stimulation. You need to turn "
                                            + "up the heat some more. Well, if you weren't prepared to suck a cock"
                                            + " or two, you may have joined the wrong competition. You take just "
                                            + "the glans into your mouth, attacking the most senstitive area with "
                                            + "your tongue. %s lets out a gasp and shudders. That's a more promising "
                                            + "reaction.<br/><br/>You continue your oral assault until you hear a breathy "
                                            + "moan, <i>\"I'm gonna cum!\"</i> You hastily remove %s dick out of "
                                            + "your mouth and pump it rapidly. %s shoots %s load into the air, barely "
                                            + "missing you.", target.getName(),
                            Global.capitalizeFirstLetter(target.possessiveAdjective()), target.getName(),
                            Global.capitalizeFirstLetter(target.pronoun()), target.possessiveAdjective(), target.getName(),
                            target.possessiveAdjective()));
        } else {
            c.write(target.nameOrPossessivePronoun()
                            + " arms are firmly pinned, so she tries to kick you ineffectually. You catch her ankles and slowly begin kissing and licking your way "
                            + "up her legs while gently, but firmly, forcing them apart. By the time you reach her inner thighs, she's given up trying to resist. Since you no "
                            + "longer need to hold her legs, you can focus on her flooded pussy. You pump two fingers in and out of her while licking and sucking her clit. In no "
                            + "time at all, she's trembling and moaning in orgasm.");
        }
        gainAttraction(target, 1);
        target.gainAttraction(this, 1);
    }

    @Override
    public void gain(Item item) {
        gui.message("<b>You've gained " + item.pre() + item.getName() + ".</b>");
        super.gain(item);
    }

    @Override
    public String challenge(Character other) {
        return null;
    }

    @Override
    public void promptTrap(Encounter enc, Character target, Trap trap) {
        gui.message("Do you want to take the opportunity to ambush <b>" + target.getName() + "</b>?");
        assessOpponent(target);
        gui.message("<br/>");

        ArrayList<CommandPanelOption> options = new ArrayList<>();
        options.add(new CommandPanelOption("Attack " + target.getName(), event -> {
            enc.parse(Encs.capitalize, Global.getPlayer(), target, trap);
            Global.getMatch().resume();
        }));
        options.add(new CommandPanelOption("Wait",
            encounterOption(enc, target, Encs.wait)));
        gui.presentOptions(options);
        Global.getMatch().pause();
    }

    @Override
    public void afterParty() {
    }

    @Override
    public void counterattack(Character target, Tactics type, Combat c) {
        switch (type) {
            case damage:
                c.write(this, "You dodge " + target.getName()
                                + "'s slow attack and hit her sensitive tit to stagger her.");
                target.pain(c, target, 4 + Math.min(Global.random(get(Attribute.Power)), 20));
                break;
            case pleasure:
                if (!target.crotchAvailable() || !target.hasPussy()) {
                    c.write(this, "You pull " + target.getName()
                                    + " off balance and lick her sensitive ear. She trembles as you nibble on her earlobe.");
                    target.body.pleasure(this, body.getRandom("tongue"), target.body.getRandom("ears"),
                                    4 + Math.min(Global.random(get(Attribute.Seduction)), 20), c);
                } else {
                    c.write(this, "You pull " + target.getName() + " to you and rub your thigh against her girl parts.");
                    target.body.pleasure(this, body.getRandom("feet"), target.body.getRandomPussy(),
                                    4 + Math.min(Global.random(get(Attribute.Seduction)), 20), c);
                }
                break;
            case fucking:
                if (c.getStance()
                     .sub(this)) {
                    Position reverse = c.getStance()
                                        .reverse(c, true);
                    if (reverse != c.getStance() && !BodyPart.hasOnlyType(reverse.bottomParts(), "strapon")) {
                        c.setStance(reverse, this, false);
                    } else {
                        c.write(this, Global.format(
                                        "{self:NAME-POSSESSIVE} quick wits find a gap in {other:name-possessive} hold and {self:action:slip|slips} away.",
                                        this, target));
                        c.setStance(new Neutral(this, c.getOpponent(this)), this, true);
                    }
                } else {
                    target.body.pleasure(this, body.getRandom("hands"), target.body.getRandomBreasts(),
                                    4 + Math.min(Global.random(get(Attribute.Seduction)), 20), c);
                    c.write(this, Global.format(
                                    "{self:SUBJECT-ACTION:pinch|pinches} {other:possessive} nipples with {self:possessive} hands as {other:subject-action:try|tries} to fuck {self:direct-object}. "
                                                    + "While {other:subject-action:yelp|yelps} with surprise, {self:subject-action:take|takes} the chance to pleasure {other:possessive} body.",
                                    this, target));
                }
                break;
            case stripping:
                Clothing clothes = target.stripRandom(c);
                if (clothes != null) {
                    c.write(this, "You manage to catch " + target.possessiveAdjective()
                                    + " hands groping your clothing, and in a swift motion you strip off her "
                                    + clothes.getName() + " instead.");
                } else {
                    c.write(this, "You manage to dodge " + target.possessiveAdjective()
                                    + " groping hands and give a retaliating slap in return.");
                    target.pain(c, target, 4 + Math.min(Global.random(get(Attribute.Power)), 20));
                }
                break;
            case positioning:
                if (c.getStance()
                     .dom(this)) {
                    c.write(this, "You outmanuever " + target.getName() + " and you exhausted her from the struggle.");
                    target.weaken(c, (int) this.modifyDamage(DamageType.stance, target, 15));
                } else {
                    c.write(this, target.getName()
                                    + " loses her balance while grappling with you. Before she can fall to the floor, you catch her from behind and hold her up.");
                    c.setStance(new Behind(this, target));
                }
                break;
            default:
                c.write(this, "You manage to dodge " + target.possessiveAdjective()
                                + " attack and give a retaliating slap in return.");
                target.pain(c, target, 4 + Math.min(Global.random(get(Attribute.Power)), 20));
        }
    }

    @Override
    public void eot(Combat c, Character opponent) {
        super.eot(c, opponent);
        if (has(Trait.RawSexuality)) {
            c.write(this, Global.format("{self:NAME-POSSESSIVE} raw sexuality turns both of you on.", this, opponent));
            temptNoSkillNoSource(c, opponent, arousal.max() / 25);
            opponent.temptNoSkillNoSource(c, this, opponent.arousal.max() / 25);
        }
        if (has(Trait.slime)) {
            if (hasPussy() && !body.getRandomPussy().moddedPartCountsAs(GooeyMod.TYPE)) {
                body.temporaryAddOrReplacePartWithType(body.getRandomPussy().applyMod(GooeyMod.INSTANCE), 999);
                c.write(this, 
                                Global.format("{self:NAME-POSSESSIVE} %s turned back into a gooey pussy.",
                                                this, opponent, body.getRandomPussy()));
            }
            if (hasDick() && !body.getRandomCock().moddedPartCountsAs(SlimyCockMod.TYPE)) {
                body.temporaryAddOrReplacePartWithType(body.getRandomCock().applyMod(CockMod.slimy), 999);
                c.write(this, 
                                Global.format("{self:NAME-POSSESSIVE} %s turned back into a gooey pussy.",
                                                this, opponent, body.getRandomPussy()));
            }
        }
    }

    @Override
    public String nameDirectObject() {
        return "you";
    }
    
    @Override
    public boolean add(Trait t) {
        if (t==Trait.nymphomania) {mod(Attribute.Nymphomania, 1);}
        return super.add(t);
    }

    @Override
    public String subjectAction(String verb, String pluralverb) {
        return subject() + " " + verb;
    }

    @Override
    public String nameOrPossessivePronoun() {
        return "your";
    }

    @Override
    public String possessiveAdjective() {
        return "your";
    }
    
    @Override
    public String possessivePronoun() {
        return "yours";
    }

    @Override
    public String reflectivePronoun() {
        return "yourself";
    }

    @Override
    public String subject() {
        return "you";
    }

    @Override
    public String subjectWas() {
        return subject() + " were";
    }

    @Override
    public String pronoun() {
        return "you";
    }

    @Override
    public String directObject() {
        return "you";
    }

    @Override
    public void emote(Emotion emo, int amt) {

    }

    @Override
    public String getPortrait(Combat c) {
        return null;
    }

    @Override
    public String action(String firstPerson, String thirdPerson) {
        return firstPerson;
    }

    @Override
    public Meter getWillpower() {
        return willpower;
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean resist3p(Combat c, Character target, Character assist) {
        return has(Trait.cursed);
    }

    @Override
    protected void resolveOrgasm(Combat c, Character opponent, BodyPart selfPart, BodyPart opponentPart, int times,
                    int totalTimes) {
        super.resolveOrgasm(c, opponent, selfPart, opponentPart, times, totalTimes);
        if (has(Trait.slimification) && times == totalTimes && getWillpower().percent() < 60
            && !has(Trait.slime)) {
            c.write(this, Global.format(
                "A powerful shiver runs through your entire body. Oh boy, you know where this"
                    + " is headed... Sure enough, you look down to see your skin seemingly <i>melt</i>,"
                    + " turning a translucent blue. You legs fuse together and collapse into a puddle."
                    + " It only takes a few seconds for you to regain some semblance of control over"
                    + " your amorphous body, but you're not going to switch back to your human"
                    + " form before this fight is over...", this, opponent));
            nudify();
            purge(c);
            addTemporaryTrait(Trait.slime, 999);
            add(c, new PlayerSlimeDummy(this));
            if (hasPussy() && !body.getRandomPussy().moddedPartCountsAs(GooeyMod.TYPE)) {
                body.temporaryAddOrReplacePartWithType(
                    body.getRandomPussy().applyMod(GooeyMod.INSTANCE), 999);
                body.temporaryAddOrReplacePartWithType(
                    new TentaclePart("slime filaments", "pussy", "slime", 0.0, 1.0, 1.0), 999);
            }
            if (hasDick() && !body.getRandomCock().moddedPartCountsAs(SlimyCockMod.TYPE)) {
                body.temporaryAddOrReplacePartWithType(body.getRandomCock().applyMod(CockMod.slimy),
                    999);
            }
            BreastsPart part = body.getBreastsBelow(BreastsPart.Size.max());
            if (part != null
                && body.getRandomBreasts().getSize() != Size.min()) {
                body.temporaryAddOrReplacePartWithType(part.upgrade(), 999);
            }
            body.temporaryAddOrReplacePartWithType(
                new GenericBodyPart("gooey skin", .6, 1.5, .8, "skin", ""), 999);
            body.temporaryAddOrReplacePartWithType(
                new TentaclePart("slime pseudopod", "back", "slime", 0.0, 1.0, 1.0), 999);
            addTemporaryTrait(Trait.Sneaky, 999);
            addTemporaryTrait(Trait.shameless, 999);
            addTemporaryTrait(Trait.lactating, 999);
            addTemporaryTrait(Trait.addictivefluids, 999);
            addTemporaryTrait(Trait.autonomousPussy, 999);
            addTemporaryTrait(Trait.enthrallingjuices, 999);
            addTemporaryTrait(Trait.energydrain, 999);
            addTemporaryTrait(Trait.desensitized, 999);
            addTemporaryTrait(Trait.steady, 999);
            addTemporaryTrait(Trait.strongwilled, 999);
        }
    }

    private int getLevelsToGain() {
        return levelsToGain;
    }

    @Override
    public int exercise(Exercise source) {
        int gain = super.exercise(source);
        gui.clearText();
        ArrayList<CommandPanelOption> options = new ArrayList<>();
        CommandPanelOption o = new CommandPanelOption("Next",
            event -> {
                source.done(true);
                gui.clearText();
            });
        options.add(o);
        gui.presentOptions(options);
        source.showScene(source.pickScene(gain));
        if (gain > 0) {
            Global.gui().message("<b>Your maximum stamina has increased by " + gain + ".</b>");
        }
        return gain;
    }

    @Override
    public int porn(Porn source) {
        int gain = super.porn(source);
        gui.clearText();
        ArrayList<CommandPanelOption> options = new ArrayList<>();
        CommandPanelOption o = new CommandPanelOption("Next",
            event -> {
                source.done(true);
                gui.clearText();
            });
        options.add(o);
        gui.presentOptions(options);
        source.showScene(source.pickScene(gain));
        if (gain > 0) {
            Global.gui().message("<b>Your maximum arousal has increased by " + gain + ".</b>");
        }
        return gain;
    }

    @Override
    public void chooseLocateTarget(Locate action, Collection<Character> potentialTargets) {
        List<CommandPanelOption> options = potentialTargets.stream()
            .map(character -> new CommandPanelOption(character.getTrueName(),
                event -> action.handleEvent(this, getTrueName())))
            .collect(Collectors.toList());
        options.add(new CommandPanelOption("Leave",
            event -> action.handleEvent(this, "Leave")));
        gui.presentOptions(options);
    }

    @Override
    public void leaveAction(Locate action) {
        ArrayList<CommandPanelOption> options = new ArrayList<>();
        options.add(new CommandPanelOption("Leave",
            event -> action.handleEvent(this, "Leave")));
        gui.presentOptions(options);
    }

    @Override
    public void chooseShopOption(Store shop, Collection<Loot> items,
        List<String> additionalChoices) {
        List<CommandPanelOption> options = items.stream()
            .map(item -> new CommandPanelOption(
                Global.capitalizeFirstLetter(item.getName()),
                item.getDesc(),
                event -> shop.visit(item.getName())))
            .collect(Collectors.toList());
        options.addAll(additionalChoices.stream()
            .map(choice -> newActivitySubchoice(shop, choice))
            .collect(Collectors.toList()));
        gui.presentOptions(options);
    }

    @Override
    public void sceneNext(Scene source) {
        ArrayList<CommandPanelOption> options = new ArrayList<>();
        options.add(new CommandPanelOption(
            "Next",
            event -> source.respond("Next")
        ));
        gui.presentOptions(options);
    }

    // displayTexts and prices are expected to be 1:1
    @Override
    public void chooseBodyShopOption(BodyShop shop, List<String> displayTexts,
        List<Integer> prices, List<String> additionalChoices) {
        assert displayTexts.size() == prices.size();
        ArrayList<CommandPanelOption> options = new ArrayList<>();
        for (int i = 0; i < displayTexts.size(); i++) {
            final String displayText = displayTexts.get(i);
            options.add(new CommandPanelOption(
                displayText,
                "Price: $" + prices.get(i),
                event -> shop.visit(displayText)));
        }
        options.addAll(additionalChoices.stream()
            .map(choice -> newActivitySubchoice(shop, choice))
            .collect(Collectors.toList()));
        gui.presentOptions(options);
    }

    @Override
    public void nextCombat(Combat c) {
        Global.getMatch().pause();
        ArrayList<CommandPanelOption> options = new ArrayList<>();
        options.add(new CommandPanelOption("Next", event -> {
            c.resume();
        }));
        gui.presentOptions(options);
    }

    public void chooseActivity(List<Activity> activities) {
        gui.presentOptions(activities.stream()
            .map(activity -> new CommandPanelOption(activity.toString(),
                event -> activity.visit("Start")))
            .collect(Collectors.toList()));
    }

    public void chooseCombatScene(Combat c, Character npc, List<CombatSceneChoice> choices) {
        gui.presentOptions(choices.stream()
            .map(choice -> new CommandPanelOption(choice.getChoice(), event -> {
                c.write("<br/>");
                choice.choose(c, npc);
                c.updateMessage();
                nextCombat(c);
            }))
            .collect(Collectors.toList()));
    }

    @Override
    public void chooseActivitySubchoices(Activity activity, List<String> choices) {
        gui.presentOptions(choices.stream().map(
            choice -> newActivitySubchoice(activity, choice))
            .collect(Collectors.toList()));
    }

    private static CommandPanelOption newActivitySubchoice(Activity activity, String choice) {
        return new CommandPanelOption(choice, event -> activity.visit(choice));
    }
}
