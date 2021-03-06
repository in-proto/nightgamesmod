package nightgames.daytime;

import java.util.ArrayList;
import java.util.Map;

import java.util.Set;
import nightgames.characters.Character;
import nightgames.global.Flag;
import nightgames.global.Global;
import nightgames.items.Item;
import nightgames.items.Loot;

public class Bookstore extends Store {
    public Bookstore(Character player) {
        super("Bookstore", player);
        add(Item.EnergyDrink);
        add(Item.ZipTie);
        add(Item.Phone);
    }

    @Override
    public boolean known() {
        return Global.checkFlag(Flag.basicStores);
    }

    @Override
    public void visit(String choice) {
        Global.gui().clearText();
        if (choice.equals("Start")) {
            acted = false;
        }
        if (choice.equals("Leave")) {
            done(acted);
            return;
        }
        checkSale(choice);
        if (player.human()) {
            Global.gui().message(
                            "In addition to textbooks, the campus bookstore sells assorted items for everyday use.");
            Map<Item, Integer> MyInventory = this.player.getInventory();
            for (Item i : stock.keySet()) {
                if (MyInventory.get(i) == null || MyInventory.get(i) == 0) {
                    Global.gui().message(i.getName() + ": $" + i.getPrice());
                } else {
                    Global.gui().message(
                                    i.getName() + ": $" + i.getPrice() + " (you have: " + MyInventory.get(i) + ")");
                }
            }
            Global.gui().message("You have : $" + player.money + " to spend.");

            Set<Loot> purchasableItems = getGoods();
            ArrayList<String> choices = new ArrayList<>();
            choices.add("Leave");
            player.chooseShopOption(this, purchasableItems, choices);
        }
    }

    @Override
    public void shop(Character npc, int budget) {
        int remaining = budget;
        int bored = 0;
        while (remaining > 25 && bored < 5) {
            for (Item i : stock.keySet()) {
                if (remaining > i.getPrice() && !npc.has(i, 10)) {
                    npc.gain(i);
                    npc.money -= i.getPrice();
                    remaining -= i.getPrice();
                } else {
                    bored++;
                }
            }
        }
    }
}
