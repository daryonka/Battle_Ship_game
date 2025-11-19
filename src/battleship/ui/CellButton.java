package battleship.ui;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Insets;

public class CellButton extends JButton {
    public final int row, col;
    public CellButton(int row, int col) {
        this.row = row;
        this.col = col;
        setMargin(new Insets(0,0,0,0));
        setFocusPainted(false);
        setContentAreaFilled(true);
        setOpaque(true);
        setBackground(new Color(50, 90, 130));
        setForeground(Color.WHITE);
    }
}