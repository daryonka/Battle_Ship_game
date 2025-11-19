package battleship.app;

import battleship.ui.BattleshipGame;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(BattleshipGame::new);
    }
}