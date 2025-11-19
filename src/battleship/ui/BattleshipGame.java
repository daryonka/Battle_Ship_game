package battleship.ui;

import battleship.core.Board;
import battleship.core.Ship;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.util.Random;

public class BattleshipGame extends JFrame {
    private static final int SIZE = 10;
    private static final int[] SHIP_SIZES = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};

    private JPanel mainPanel;
    private JPanel setupPanel;
    private JPanel boardsPanel;
    private JPanel playerBoardPanel;
    private JPanel enemyBoardPanel;
    private CellButton[][] playerButtons = new CellButton[SIZE][SIZE];
    private CellButton[][] enemyButtons = new CellButton[SIZE][SIZE];

    private JLabel centerMessage = new JLabel(" ", SwingConstants.CENTER);
    private JLabel statusLabel = new JLabel("Розстановка: оберіть режим", SwingConstants.CENTER);
    private JButton rotateButton = new JButton("Обертати (гориз/верт)");
    private JButton autoButton = new JButton("Авто-розстановка");
    private JButton manualButton = new JButton("Ручна розстановка");
    private JButton startButton = new JButton("Старт");
    private JButton restartButton = new JButton("Перезапуск");

    private Board playerBoard = new Board(SIZE, SHIP_SIZES);
    private Board enemyBoard = new Board(SIZE, SHIP_SIZES);
    private boolean placingShipsMode = false;
    private boolean shipsPlaced = false;
    private int currentShipIndex = 0;
    private boolean horizontalPlacement = true;
    private boolean playerTurn = true;

    private Random rnd = new Random();

    public BattleshipGame() {
        super("Морський бій");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(20, 40, 70));
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        statusLabel.setForeground(Color.WHITE);
        top.add(statusLabel, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBackground(new Color(10, 25, 45));

        boardsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        boardsPanel.setOpaque(false);

        playerBoardPanel = createBoardPanel(playerButtons, "Ваше поле", true);
        enemyBoardPanel = createBoardPanel(enemyButtons, "Поле ворога", false);
        boardsPanel.add(playerBoardPanel);

        mainPanel.add(boardsPanel, BorderLayout.CENTER);
        centerMessage.setFont(new Font("SansSerif", Font.BOLD, 18));
        centerMessage.setForeground(Color.RED);
        mainPanel.add(centerMessage, BorderLayout.SOUTH);
        add(mainPanel, BorderLayout.CENTER);

        setupPanel = new JPanel();
        setupPanel.setLayout(new BoxLayout(setupPanel, BoxLayout.Y_AXIS));
        setupPanel.setBackground(new Color(30, 50, 80));
        setupPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel choose = new JLabel("Виберіть режим розстановки:");
        choose.setForeground(Color.WHITE);
        choose.setFont(new Font("SansSerif", Font.BOLD, 14));
        choose.setAlignmentX(CENTER_ALIGNMENT);

        manualButton.setAlignmentX(CENTER_ALIGNMENT);
        autoButton.setAlignmentX(CENTER_ALIGNMENT);
        rotateButton.setAlignmentX(CENTER_ALIGNMENT);
        startButton.setAlignmentX(CENTER_ALIGNMENT);
        restartButton.setAlignmentX(CENTER_ALIGNMENT);

        rotateButton.setEnabled(false);
        startButton.setEnabled(false);
        restartButton.setEnabled(true);

        manualButton.addActionListener(e -> beginManualPlacement());
        autoButton.addActionListener(e -> doAutoPlacementPlayer());
        rotateButton.addActionListener(e -> toggleOrientation());
        startButton.addActionListener(e -> onStartGame());
        restartButton.addActionListener(e -> resetFull());

        setupPanel.add(choose);
        setupPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        setupPanel.add(manualButton);
        setupPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        setupPanel.add(autoButton);
        setupPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        setupPanel.add(rotateButton);
        setupPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        setupPanel.add(startButton);
        setupPanel.add(Box.createVerticalGlue());
        setupPanel.add(restartButton);

        add(setupPanel, BorderLayout.EAST);
        resetFull();
        setVisible(true);
    }

    private void resetButtons(CellButton[][] buttons) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                buttons[r][c].setBackground(new Color(50, 90, 130));
                buttons[r][c].setEnabled(false);
            }
        }
    }

    private void resetFull() {
        playerBoard.clear();
        enemyBoard.clear();
        removeEnemyBoardIfPresent();

        resetButtons(playerButtons);
        resetButtons(enemyButtons);

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                playerButtons[r][c].setEnabled(true);
            }
        }

        placingShipsMode = false;
        shipsPlaced = false;
        currentShipIndex = 0;
        horizontalPlacement = true;
        playerTurn = true;
        centerMessage.setText(" ");

        rotateButton.setEnabled(false);
        startButton.setEnabled(false);
        manualButton.setEnabled(true);
        autoButton.setEnabled(true);

        statusLabel.setText("Розстановка: оберіть режим");
        updateButtons();
    }

    private JPanel createBoardPanel(CellButton[][] buttons, String title, boolean isPlayer) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(18, 35, 60));
        JLabel lbl = new JLabel(title, SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 16));
        lbl.setForeground(Color.WHITE);
        panel.add(lbl, BorderLayout.NORTH);
        JPanel grid = new JPanel(new GridLayout(SIZE + 1, SIZE + 1));
        grid.setBackground(new Color(10, 25, 45));

        grid.add(new JLabel(""));
        for (int c = 0; c < SIZE; c++) {
            JLabel l = new JLabel(String.valueOf((char) ('A' + c)), SwingConstants.CENTER);
            l.setForeground(Color.WHITE);
            grid.add(l);
        }
        for (int r = 0; r < SIZE; r++) {
            JLabel l = new JLabel(String.valueOf(r + 1), SwingConstants.CENTER);
            l.setForeground(Color.WHITE);
            grid.add(l);
            for (int c = 0; c < SIZE; c++) {
                CellButton btn = new CellButton(r, c);
                btn.setPreferredSize(new Dimension(42, 42));
                btn.setBorder(new LineBorder(Color.DARK_GRAY));
                buttons[r][c] = btn;
                if (isPlayer) {
                    btn.addActionListener(e -> onPlayerBoardClick(btn));
                    btn.setEnabled(true);
                } else {
                    btn.addActionListener(e -> onEnemyBoardClick(btn));
                    btn.setEnabled(false);
                }
                grid.add(btn);
            }
        }

        panel.add(grid, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(450, 450));
        return panel;
    }

    private void beginManualPlacement() {
        playerBoard.clear();
        shipsPlaced = false;
        placingShipsMode = true;
        currentShipIndex = 0;
        horizontalPlacement = true;
        rotateButton.setEnabled(true);
        startButton.setEnabled(false);
        manualButton.setEnabled(false);
        autoButton.setEnabled(false);
        centerMessage.setText(" ");
        statusLabel.setText("Ручна розстановка: ставте корабель розміром " + SHIP_SIZES[currentShipIndex]);

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                playerButtons[r][c].setEnabled(true);
            }
        }
        updateButtons();
    }

    private void doAutoPlacementPlayer() {
        playerBoard.clear();
        playerBoard.placeShipsRandomly(SHIP_SIZES);
        placingShipsMode = false;
        shipsPlaced = true;
        rotateButton.setEnabled(false);
        startButton.setEnabled(true);
        manualButton.setEnabled(true);
        autoButton.setEnabled(true);
        statusLabel.setText("Кораблі розставлені автоматично - натисніть Старт");

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                playerButtons[r][c].setEnabled(false);
            }
        }
        updateButtons();
    }

    private void onPlayerBoardClick(CellButton btn) {
        if (!placingShipsMode && !shipsPlaced) {
            centerMessage.setText("Виберіть режим розстановки");
            centerMessage.setForeground(Color.RED);
            return;
        }

        centerMessage.setText(" ");
        centerMessage.setForeground(Color.RED);

        if (!placingShipsMode) return;
        centerMessage.setText(" ");
        int row = btn.row;
        int col = btn.col;
        int size = SHIP_SIZES[currentShipIndex];
        if (!playerBoard.canPlace(row, col, size, horizontalPlacement)) {
            centerMessage.setText("Не можна поставити корабель! Змініть місце.");
            return;
        }
        Ship s = new Ship(size);
        for (int k = 0; k < size; k++) {
            int r = row + (horizontalPlacement ? 0 : k);
            int c = col + (horizontalPlacement ? k : 0);
            playerBoard.grid[r][c] = Board.SHIP;
            s.addCell(r, c);
        }
        playerBoard.ships.add(s);
        currentShipIndex++;
        if (currentShipIndex >= SHIP_SIZES.length) {
            placingShipsMode = false;
            shipsPlaced = true;
            rotateButton.setEnabled(false);
            startButton.setEnabled(true);
            statusLabel.setText("Розстановка завершена - натисніть Старт");

            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    playerButtons[r][c].setEnabled(false);
                }
            }

        } else {
            statusLabel.setText("Розставте корабель розміром " + SHIP_SIZES[currentShipIndex] + (horizontalPlacement ? " (горизонтально)" : " (вертикально)"));
        }
        updateButtons();
    }

    private void toggleOrientation() {
        horizontalPlacement = !horizontalPlacement;
        if (placingShipsMode) {
            statusLabel.setText("Розстановка: корабель розміром " + SHIP_SIZES[currentShipIndex] + (horizontalPlacement ? " (гориз)" : " (верт)"));
        }
    }

    private void onStartGame() {
        if (!isEnemyPanelPresent()) {
            boardsPanel.add(enemyBoardPanel);
            boardsPanel.revalidate();
            boardsPanel.repaint();
        }

        if (enemyBoard.ships.isEmpty()) enemyBoard.placeShipsRandomly(SHIP_SIZES);
        manualButton.setEnabled(false);
        autoButton.setEnabled(false);
        rotateButton.setEnabled(false);
        startButton.setEnabled(false);

        for (int r = 0; r < SIZE; r++) for (int c = 0; c < SIZE; c++) playerButtons[r][c].setEnabled(false);

        setEnemyFieldEnabled(true);
        playerTurn = true;
        updateStatusTurn();
        updateButtons();
    }

    private boolean isEnemyPanelPresent() {
        for (Component c : boardsPanel.getComponents()) if (c == enemyBoardPanel) return true;
        return false;
    }

    private void removeEnemyBoardIfPresent() {
        if (isEnemyPanelPresent()) boardsPanel.remove(enemyBoardPanel);
        boardsPanel.revalidate();
        boardsPanel.repaint();
    }

    private void setEnemyFieldEnabled(boolean val) {
        for (int r = 0; r < SIZE; r++) for (int c = 0; c < SIZE; c++) enemyButtons[r][c].setEnabled(val);
    }

    private void onEnemyBoardClick(CellButton btn) {
        if (!playerTurn) {
            return;
        }
        centerMessage.setText(" ");

        if (!shipsPlaced) return;
        if (!playerTurn) return; // Цей рядок буде досягнутий тільки якщо playerTurn = true

        int r = btn.row;
        int c = btn.col;

        if (enemyBoard.isShot(r, c)) {
            btn.setEnabled(false);
            return;
        }

        boolean hit = enemyBoard.shoot(r, c);

        btn.setEnabled(false);

        if (hit) {
            statusLabel.setText("Попадання! Ваш хід продовжується");
        } else {
            statusLabel.setText("Промах - хід ворога");
            playerTurn = false;
        }
        updateButtons();
        markSunkShip(enemyBoard, enemyButtons);
        updateStatusTurn();
        if (enemyBoard.allShipsSunk()) {
            gameOver(true);
            return;
        }

        if (!playerTurn) {
            Timer t = new Timer(3000, e -> enemyMakeMove());
            t.setRepeats(false);
            t.start();
        }
    }

    private void markSunkShip(Board board, CellButton[][] buttons) {
        for (Ship s : board.ships) {
            if (s.isSunk()) {
                for (Point p : s.cells) {
                    buttons[p.x][p.y].setBackground(Color.BLACK);
                }

                for (Point p : s.cells) {
                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            int nr = p.x + dr, nc = p.y + dc;
                            if (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE) {
                                if (!board.isShot(nr, nc)) {
                                    board.shot[nr][nc] = true;
                                    buttons[nr][nc].setBackground(Color.WHITE);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void enemyMakeMove() {
        SwingUtilities.invokeLater(() -> {
            Point p = playerBoard.nextAIMove();
            if (p == null) {
                outer:
                for (int r = 0; r < SIZE; r++) {
                    for (int c = 0; c < SIZE; c++) {
                        if (!playerBoard.isShot(r, c)) {
                            p = new Point(r, c);
                            break outer;
                        }
                    }
                }
                if (p == null) return;
            }

            boolean hit = playerBoard.shoot(p.x, p.y);

            updateButtons();
            markSunkShip(playerBoard, playerButtons);
            updateStatusTurn();

            if (hit) {
                playerBoard.lastHit = p;
                statusLabel.setText("Ворог влучив! Він ходить ще.");
                if (playerBoard.allShipsSunk()) {
                    gameOver(false);
                    return;
                }
                Timer t = new Timer(800, ev -> {
                    ((Timer) ev.getSource()).stop();
                    enemyMakeMove();
                });
                t.setRepeats(false);
                t.start();
            } else {
                statusLabel.setText("Ворог промахнувся - ваш хід");
                playerBoard.lastHit = null;
                playerTurn = true;
                updateButtons();
                markSunkShip(playerBoard, playerButtons);
                updateStatusTurn();
                if (playerBoard.allShipsSunk()) {
                    gameOver(false);
                }
            }
        });
    }


    private void updateStatusTurn() {
        if (playerTurn) {
            statusLabel.setText("Ваш хід");
            playerBoardPanel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));
            if (isEnemyPanelPresent()) enemyBoardPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        } else {
            statusLabel.setText("Хід ворога...");
            playerBoardPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
            if (isEnemyPanelPresent()) enemyBoardPanel.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 3));
        }
    }

    private void gameOver(boolean playerWon) {
        setEnemyFieldEnabled(false);
        String msg = playerWon ? "Ви перемогли!" : "Ви програли.";
        JOptionPane.showMessageDialog(this, msg, "Результат гри", JOptionPane.INFORMATION_MESSAGE);
        statusLabel.setText(msg);
    }

    private void updateButtons() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                CellButton pb = playerButtons[r][c];
                CellButton eb = enemyButtons[r][c];

                int pv = playerBoard.getCell(r, c);
                int ev = enemyBoard.getCell(r, c);

                // Оновлення поля гравця
                if (pb.getBackground().equals(Color.BLACK)) continue;
                if (pv == Board.SHIP && !playerBoard.isShot(r, c)) pb.setBackground(new Color(0, 120, 160));
                else if (playerBoard.isShot(r, c) && pv == Board.SHIP) pb.setBackground(Color.RED);
                else if (playerBoard.isShot(r, c)) pb.setBackground(Color.WHITE);
                else pb.setBackground(new Color(50, 90, 130));

                // Оновлення поля ворога
                if (eb.getBackground().equals(Color.BLACK)) continue;
                if (enemyBoard.isShot(r, c)) {
                    if (ev == Board.SHIP) eb.setBackground(Color.RED);
                    else eb.setBackground(Color.WHITE);

                    eb.setEnabled(false);

                } else {
                    eb.setBackground(new Color(50, 90, 130));
                    if (playerTurn && shipsPlaced) {
                        eb.setEnabled(true);
                    } else {
                        eb.setEnabled(false);
                    }
                }
            }
        }
    }
}