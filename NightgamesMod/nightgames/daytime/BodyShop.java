package nightgames.daytime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import nightgames.characters.Character;
import nightgames.characters.Trait;
import nightgames.characters.body.AssPart;
import nightgames.characters.body.Body;
import nightgames.characters.body.BodyPart;
import nightgames.characters.body.BreastsPart;
import nightgames.characters.body.CockPart;
import nightgames.characters.body.CockPart.Size;
import nightgames.characters.body.EarPart;
import nightgames.characters.body.EarsPart;
import nightgames.characters.body.GenericBodyPart;
import nightgames.characters.body.PussyPart;
import nightgames.characters.body.TailPart;
import nightgames.characters.body.mods.PartMod;
import nightgames.characters.body.mods.SecondPussyMod;
import nightgames.global.Flag;
import nightgames.global.Global;

public class BodyShop extends Activity {
    List<ShopSelection> selection;

    public BodyShop(Character player) {
        super("Body Shop", player);
        selection = new ArrayList<ShopSelection>();
        populateSelection();
    }

    abstract class ShopSelection {
        String choice;
        int price;

        ShopSelection(String choice, int price) {
            this.choice = choice;
            this.price = price;
        }

        abstract void buy(Character buyer);

        abstract boolean available(Character buyer);

        double priority(Character buyer) {
            return 5;
        }

        @Override
        public String toString() {
            return choice;
        }
    }

    interface CharacterRequirement {
        boolean isSatisfied(Character character);
    }

    private void addBodyPart(String name, final BodyPart part, final BodyPart normal, int growPrice,
                    int removePrice) {
        addBodyPart(name, part, normal, growPrice, removePrice, 5, false);
    }

    private void addBodyPart(String name, final BodyPart part, final BodyPart normal, int growPrice, int removePrice,
                    final int priority, final boolean onlyReplace) {
        selection.add(new ShopSelection(name, growPrice) {
            @Override
            void buy(Character buyer) {
                buyer.body.addReplace(part, 1);
            }

            @Override
            boolean available(Character buyer) {
                boolean possible = true;
                if (onlyReplace) {
                    possible = buyer.body.has(part.getType());
                }
                if (normal == null) {
                    return possible && !buyer.body.has(part.getType()); // never
                                                                        // available
                } else {
                    return possible && !buyer.body.contains(part);
                }
            }

            @Override
            double priority(Character buyer) {
                return priority;
            }
        });

        selection.add(new ShopSelection("Remove " + name, removePrice) {
            @Override
            void buy(Character buyer) {
                if (normal == null) {
                    buyer.body.removeOne(part.getType());
                } else {
                    buyer.body.remove(part);
                    buyer.body.addReplace(normal, 1);
                }
            }

            @Override
            boolean available(Character buyer) {
                if (normal == null) {
                    return buyer.body.has(part.getType());
                } else {
                    return buyer.body.contains(part);
                }
            }

            @Override
            double priority(Character buyer) {
                return 1;
            }
        });
    }

    private void addBodyPartMod(String name, final PartMod mod, final String partType, int growPrice, int removePrice,
                    final int priority) {
        selection.add(new ShopSelection(name, growPrice) {
            @Override
            void buy(Character buyer) {
                Body body = buyer.body;
                body.applyMod(partType, mod);
            }

            @Override
            boolean available(Character buyer) {
                return buyer.body.get(partType).stream().anyMatch(part -> !part.moddedPartCountsAs(
                    mod.getModType()));
            }

            @Override
            double priority(Character buyer) {
                return priority;
            }
        });

        selection.add(new ShopSelection("Remove " + name, removePrice) {
            @Override
            void buy(Character buyer) {
                buyer.body.removeMod(partType, mod);
            }

            @Override
            boolean available(Character buyer) {
                return buyer.body.get(partType).stream().anyMatch(part -> part.moddedPartCountsAs(
                    mod.getModType()));
            }

            @Override
            double priority(Character buyer) {
                return 1;
            }
        });
    }

    private void addTraitMod(String name, String removeName, final Trait trait, int addPrice, int removePrice,
                    final CharacterRequirement requirement) {
        selection.add(new ShopSelection(name, addPrice) {
            @Override
            void buy(Character buyer) {
                buyer.add(trait);
            }

            @Override
            boolean available(Character buyer) {
                return !buyer.has(trait) && requirement.isSatisfied(buyer);
            }
        });

        selection.add(new ShopSelection(removeName, removePrice) {
            @Override
            void buy(Character buyer) {
                buyer.remove(trait);
            }

            @Override
            boolean available(Character buyer) {
                return buyer.has(trait);
            }

            @Override
            double priority(Character buyer) {
                return 1;
            }
        });
    }

    private void populateSelection() {
        CharacterRequirement noRequirement = character -> true;

        selection.add(new ShopSelection("Ass Expansion", 1500) {
            @Override
            void buy(Character buyer) {
                AssPart target = buyer.body.getAssBelow(AssPart.Size.max());
                assert target != null;
                target.changeSize(1);
            }

            @Override
            boolean available(Character buyer) {
                AssPart target = buyer.body.getAssBelow(AssPart.Size.max());
                return target != null;
            }

            @Override
            double priority(Character buyer) {
                return 10;
            }
        });

        selection.add(new ShopSelection("Ass Reduction", 1500) {
            @Override
            void buy(Character buyer) {
                AssPart target = buyer.body.getAssAbove(AssPart.Size.min());
                assert target != null;
                target.changeSize(-1);
            }

            @Override
            boolean available(Character buyer) {
                AssPart target = buyer.body.getAssAbove(AssPart.Size.min());
                return target != null;
            }

            @Override
            double priority(Character buyer) {
                return 10;
            }
        });

        selection.add(new ShopSelection("Breast Expansion", 1500) {
            @Override
            void buy(Character buyer) {
                BreastsPart target = buyer.body.getBreastsBelow(BreastsPart.Size.max());
                assert target != null;
                target.changeSize(1);
            }

            @Override
            boolean available(Character buyer) {
                BreastsPart target = buyer.body.getBreastsBelow(BreastsPart.Size.max());
                return target != null;
            }

            @Override
            double priority(Character buyer) {
                return 10;
            }
        });

        selection.add(new ShopSelection("Breast Reduction", 1500) {
            @Override
            void buy(Character buyer) {
                BreastsPart target = buyer.body.getBreastsAbove(BreastsPart.Size.min());
                assert target != null;
                target.changeSize(-1);
            }

            @Override
            boolean available(Character buyer) {
                BreastsPart target = buyer.body.getBreastsAbove(BreastsPart.Size.min());
                return target != null;
            }

            @Override
            double priority(Character buyer) {
                return 5;
            }
        });

        selection.add(new ShopSelection("Grow Cock", 2500) {
            @Override
            void buy(Character buyer) {
                buyer.body.addReplace(new CockPart(Size.Tiny), 1);
            }

            @Override
            boolean available(Character buyer) {
                return !buyer.hasDick();
            }

            @Override
            double priority(Character buyer) {
                return buyer.dickPreference();
            }
        });

        selection.add(new ShopSelection("Remove Cock", 2500) {
            @Override
            void buy(Character buyer) {
                buyer.body.removeAll(CockPart.TYPE);
                buyer.body.removeAll("balls");
            }

            @Override
            boolean available(Character buyer) {
                return buyer.hasDick();
            }

            @Override
            double priority(Character buyer) {
                return Math.max(0, buyer.pussyPreference() - 7);
            }
        });

        selection.add(new ShopSelection("Remove Pussy", 2500) {
            @Override
            void buy(Character buyer) {
                buyer.body.removeAll(PussyPart.TYPE);
            }

            @Override
            boolean available(Character buyer) {
                return buyer.hasPussy();
            }

            @Override
            double priority(Character buyer) {
                return Math.max(0, buyer.dickPreference() - 7);
            }
        });

        selection.add(new ShopSelection("Grow Balls", 1000) {
            @Override
            void buy(Character buyer) {
                buyer.body.addReplace(new GenericBodyPart("balls", 0, 1.0, 1.5, "balls", ""), 1);
            }

            @Override
            boolean available(Character buyer) {
                return !buyer.hasBalls() && buyer.hasDick();
            }

            @Override
            double priority(Character buyer) {
                return Math.max(0, 4 - buyer.dickPreference());
            }
        });

        selection.add(new ShopSelection("Remove Balls", 1000) {
            @Override
            void buy(Character buyer) {
                buyer.body.removeAll("balls");
            }

            @Override
            boolean available(Character buyer) {
                return buyer.hasBalls();
            }

            @Override
            double priority(Character buyer) {
                return Math.max(0, buyer.pussyPreference() - 5);
            }
        });

        selection.add(new ShopSelection("Remove Wings", 1000) {
            @Override
            void buy(Character buyer) {
                buyer.body.removeAll("wings");
            }

            @Override
            boolean available(Character buyer) {
                return buyer.body.has("wings");
            }

            @Override
            double priority(Character buyer) {
                return 0;
            }
        });

        selection.add(new ShopSelection("Remove Tail", 1000) {
            @Override
            void buy(Character buyer) {
                buyer.body.removeAll(TailPart.TYPE);
            }

            @Override
            boolean available(Character buyer) {
                return buyer.body.has(TailPart.TYPE);
            }

            @Override
            double priority(Character buyer) {
                return 0;
            }
        });

        selection.add(new ShopSelection("Restore Ears", 1000) {
            @Override
            void buy(Character buyer) {
                buyer.body.removeAll(EarPart.TYPE);
                buyer.body.add(new EarsPart());
            }

            @Override
            boolean available(Character buyer) {
                return !buyer.body.getRandom(EarPart.TYPE).getMods().isEmpty();
            }

            @Override
            double priority(Character buyer) {
                return 0;
            }
        });
        selection.add(new ShopSelection("Grow Pussy", 2500) {
            @Override
            void buy(Character buyer) {
                buyer.body.addReplace(new PussyPart(), 1);
            }

            @Override
            boolean available(Character buyer) {
                return !buyer.hasPussy();
            }

            @Override
            double priority(Character buyer) {
                return buyer.pussyPreference();
            }
        });

        selection.add(new ShopSelection("Cock Expansion", 1500) {
            @Override
            void buy(Character buyer) {
                CockPart target = buyer.body.getCockBelow(Size.max());
                assert target != null;
                target.changeSize(1);
            }

            @Override
            boolean available(Character buyer) {
                CockPart target = buyer.body.getCockBelow(Size.max());
                return target != null;
            }

            @Override
            double priority(Character buyer) {
                CockPart part = buyer.body.getRandomCock();
                if (part != null) {
                    return Size.Big.compareTo(part.getSize()) > 0 ? 10 : 3;
                }
                return 0;
            }
        });

        selection.add(new ShopSelection("Cock Reduction", 1500) {
            @Override
            void buy(Character buyer) {
                CockPart target = buyer.body.getCockAbove(Size.min());
                assert target != null;
                target.changeSize(-1);
            }

            @Override
            boolean available(Character buyer) {
                CockPart target = buyer.body.getCockAbove(Size.min());
                return target != null;
            }

            @Override
            double priority(Character buyer) {
                CockPart part = buyer.body.getRandomCock();
                if (part != null) {
                    return Size.Small.compareTo(part.getSize()) < 0 ? 3 : 0;
                }
                return 0;
            }
        });

        selection.add(new ShopSelection("Restore Cock", 1500) {
            @Override
            void buy(Character buyer) {
                CockPart target = buyer.body.getRandomCock();
                assert target != null;
                buyer.body.remove(target);
                buyer.body.addReplace(target.withoutAllMods(), 1);
            }

            @Override
            boolean available(Character buyer) {
                Optional<BodyPart> optTarget =
                                buyer.body.get(CockPart.TYPE).stream().filter(c -> !c.isGeneric(buyer)).findAny();
                return optTarget.isPresent();
            }

            @Override
            double priority(Character buyer) {
                return 0;
            }
        });

        selection.add(new ShopSelection("Restore Pussy", 1500) {
            @Override
            void buy(Character buyer) {
                PussyPart target = buyer.body.getRandomPussy();
                assert target != null;
                buyer.body.remove(target);
                buyer.body.addReplace(new PussyPart(), 1);
            }

            @Override
            boolean available(Character buyer) {
                Optional<BodyPart> optTarget =
                                buyer.body.get(PussyPart.TYPE).stream().filter(c -> !c.isGeneric(buyer)).findAny();
                return optTarget.isPresent();
            }

            @Override
            double priority(Character buyer) {
                return 0;
            }
        });

        addTraitMod("Laced Juices", "Remove L.Juices", Trait.lacedjuices, 1000, 1000,
                        noRequirement);
        addTraitMod("Permanent Lactation", "Stop Lactating", Trait.lactating, 1000, 1000,
                        noRequirement);
        addTraitMod("Pheromones", "Remove Pheromones", Trait.augmentedPheromones, 1500, 1500,
                        noRequirement);
        addBodyPart("Fused Boots",
                        new GenericBodyPart("Fused Boots",
                                        "{self:name-possessive} legs are wrapped in a shiny black material that look fused on.",
                                        .3, 1.5, .7, true, Body.FEET, ""),
                        new GenericBodyPart("feet", 0, 1, 1, Body.FEET, ""), 1000, 1000);
        addBodyPartMod("Anal Pussy", new SecondPussyMod(), AssPart.TYPE, 2000, 2000, 0);
        addBodyPart("Fused Gloves",
                        new GenericBodyPart("Fused Gloves",
                                        "{self:name-possessive} arms and hands are wrapped in a shiny black material that look fused on.",
                                        .2, 1.5, .7, true, Body.HANDS, ""),
                        new GenericBodyPart("hands", 0, 1, 1, Body.HANDS, ""), 1000, 1000);
    }

    @Override
    public boolean known() {

        return Global.checkFlag(Flag.bodyShop);
    }

    private void displaySelection() {
        Global.gui().message("You have :$" + player.money + " to spend.");
        List<ShopSelection> available = selection.stream()
            .filter(s -> s.available(player) && player.money >= s.price)
            .collect(Collectors.toList());
        ArrayList<String> displayTexts = new ArrayList<>();
        ArrayList<Integer> prices = new ArrayList<>();
        for (ShopSelection s : available) {
            displayTexts.add(s.choice);
            prices.add(s.price);
            Global.gui().message(s.choice + ": $" + s.price);
        }
        ArrayList<String> additionalChoices = new ArrayList<>();
        additionalChoices.add("Leave");
        player.chooseBodyShopOption(this, displayTexts, prices, additionalChoices);
    }

    @Override
    public void visit(String choice) {
        if (choice.equals("Start")) {
            Global.gui().clearText();
            Global.gui().message(
                            "While wondering why you're even here, you walk into the rundown shack named \"The Body Shop\". The proprietor looks at you strangely then mutely points to the sign.");
            displaySelection();
            return;
        }
        for (ShopSelection s : selection) {
            if (s.choice.equals(choice)) {
                Global.gui().message("<br/>You've selected " + s.choice
                                + ". While wondering if this was such a great idea, you follow the proprietor into the back room...");
                s.buy(player);
                player.money -= s.price;
                done(true);
                return;
            }
        }
        Global.gui().message(
                        "<br/>You have some second thoughts about letting some stranger play with your body. You think up some excuse and quickly leave the shack.");
        done(false);
    }

    @Override
    public void shop(Character npc, int budget) {
        int chance = 100;
        while (budget > 0) {
            if (Global.random(100) > chance) {
                break;
            }
            chance /= 3;
            List<ShopSelection> avail = new ArrayList<ShopSelection>();
            for (int i = 0; i < 10; i++) {
                avail.add(new ShopSelection("none" + i, 0) {
                    @Override
                    void buy(Character buyer) {

                    }

                    @Override
                    boolean available(Character buyer) {
                        return true;
                    }
                });
            }
            for (ShopSelection s : selection) {
                if (s.available(npc) && budget >= s.price) {
                    for (int i = 0; i < s.priority(npc); i++) {
                        avail.add(s);
                    }
                }
            }

            if (avail.size() == 0) {
                return;
            }
            int randomindex = Global.random(avail.size());
            ShopSelection choice = avail.get(randomindex);
            npc.money -= choice.price;
            budget -= choice.price;
            choice.buy(npc);
        }
    }
}
