package nightgames.gui;

import nightgames.characters.Player;
import nightgames.global.Global;
import nightgames.global.Time;

import javax.swing.*;
import java.awt.*;

class GUIPlayerBio {
    private static JLabel labelForString(String s) {
            JLabel l = new JLabel(s);
            l.setHorizontalAlignment(SwingConstants.LEFT);
            l.setFont(new Font("Sylfaen", 1, 15));
            l.setForeground(GUIColors.textColorLight);
            return l;
        }

    private JPanel panel;
    private Player player;
    private JLabel name;
    private JLabel level;
    private JLabel xp;
    private JLabel location;
    private JLabel time;
    private JLabel cash;

    // These live here because their status options are on the
    // bio panel and they are otherwise independent.
    private GUIPlayerInventory inventory;
    private GUIPlayerAttributes attributes;

    GUIPlayerBio(Player player, JPanel statusTarget, GUI refreshTarget) {
        this.player = player;

        panel = new JPanel();
        panel.setLayout(new GridLayout(2, 0, 0, 0));
        panel.setBackground(GUIColors.bgDark);

        name = labelForString(player.getTrueName());
        panel.add(name);

        level = labelForString("Lvl: " + player.getProgression().getLevel());
        player.getProgression()
                .getLevelProperty()
                .addPropertyChangeListener((oldValue, newValue) ->
                        SwingUtilities.invokeLater(() ->
                                level.setText("Lvl: " + newValue)));
        panel.add(level);

        xp = labelForString("XP: " + player.getProgression().getXp());
        player.getProgression()
                .getXPProperty()
                .addPropertyChangeListener((oldValue, newValue) ->
                        SwingUtilities.invokeLater(() ->
                                xp.setText("XP: " + newValue)));
        panel.add(xp);

        attributes = new GUIPlayerAttributes(player, statusTarget, refreshTarget);
        panel.add(attributes.getButton());

        location = new JLabel();
        location.setFont(new Font("Sylfaen", 1, 16));
        location.setForeground(GUIColors.textColorLight);

        panel.add(location);

        time = labelForString("");
        panel.add(time);

        cash = labelForString("");
        panel.add(cash);

        inventory = new GUIPlayerInventory(player);
        panel.add(inventory.getButton());
    }

    JPanel getPanel() {
        return panel;
    }

    void refresh() {
        location.setText(player.location().name);
        cash.setText("$" + player.money);

        refreshTime();
        displayStatus();
        inventory.refresh();
    }

    private void refreshTime() {
        String timeText;
        String textColor = "rgb(0, 0, 0)";

        // We may be in between setting NIGHT and building the Match object
        if (Global.getTime() == Time.NIGHT) {
            // yup... silverbard pls :D
            if (Global.getMatch() == null) {
                timeText = "9:50 pm";
            } else if (Global.getMatch().getHour() >= 12) {
                timeText = Global.getMatch().getTime() + " am";
            } else {
                timeText = Global.getMatch().getTime() + " pm";
            }
            textColor = "rgb(51, 101, 202)";
        } else if (Global.getTime() == Time.DAY) { // not updating correctly during daytime
            if (Global.getDay() != null) {
                timeText = Global.getDay().getTime();
            } else {
                timeText = "10:00 am";
            }
            textColor = "rgb(253, 184, 19)";
        } else {
            System.err.println("Unknown time of day: " + Global.getTime());
            timeText = "";
        }
        time.setText(String.format("<html>Day %d - <font color='%s'>%s</font></html>", Global.getDate(), textColor, timeText));
    }

     void displayStatus() {
         attributes.displayStatus();
     }
}