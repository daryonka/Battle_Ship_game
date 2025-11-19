import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class BattleshipGame extends JFrame {
    private static final int SIZE = 10;
    // Набір кораблів
    private static final int[] SHIP_SIZES = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};

    // UI
    private JPanel mainPanel;
    private JPanel setupPanel; // панель праворуч з вибором
    private JPanel boardsPanel; // сюди будемо додавати/прибирати друге поле
    private JPanel playerBoardPanel;
    private JPanel enemyBoardPanel;
    private CellButton[][] playerButtons = new CellButton[SIZE][SIZE];
    private CellButton[][] enemyButtons = new CellButton[SIZE][SIZE];

    private JLabel centerMessage = new JLabel(" ", SwingConstants.CENTER); // червоне повідомлення при помилці
    private JLabel statusLabel = new JLabel("Розстановка: оберіть режим", SwingConstants.CENTER);
    private JButton rotateButton = new JButton("Обертати (гориз/верт)");
    private JButton autoButton = new JButton("Авто-розстановка");
    private JButton manualButton = new JButton("Ручна розстановка");
    private JButton startButton = new JButton("Старт");
    private JButton restartButton = new JButton("Перезапуск");

    // Логіка
    private Board playerBoard = new Board(SIZE, SHIP_SIZES);
    private Board enemyBoard = new Board(SIZE, SHIP_SIZES);
    private boolean placingShipsMode = false; // true, якщо зараз ручна розстановка
    private boolean shipsPlaced = false; // чи гравець завершив розстановку

    private int currentShipIndex = 0;
    private boolean horizontalPlacement = true;

    private boolean playerTurn = true; // чий зараз хід

    private Random rnd = new Random();

    public BattleshipGame() {
        super("Морський бій");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // TOP статус
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(new Color(20, 40, 70));
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        statusLabel.setForeground(Color.WHITE);
        top.add(statusLabel, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // CENTER: основна область: ліворуч поле(тільки одне під час розстановки), праворуч панель налаштувань
        mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBackground(new Color(10, 25, 45));

        // boardsPanel: сюди будемо додавати обидва поля після старту
        boardsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        boardsPanel.setOpaque(false);

        playerBoardPanel = createBoardPanel(playerButtons, "Ваше поле", true);
        enemyBoardPanel = createBoardPanel(enemyButtons, "Поле ворога", false);

        // Спочатку додаємо лише поле гравця
        boardsPanel.add(playerBoardPanel);
        // enemyBoardPanel додамо після натискання Start

        mainPanel.add(boardsPanel, BorderLayout.CENTER);

        // centerMessage у центрі над полем
        centerMessage.setFont(new Font("SansSerif", Font.BOLD, 18));
        centerMessage.setForeground(Color.RED);
        mainPanel.add(centerMessage, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        // Праворуч панель вибору режиму розстановки
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

        // Готово
        resetFull();
        setVisible(true);
    }

    private void resetFull() {
        // Очистити все і повернутися в початковий стан
        playerBoard.clear();
        enemyBoard.clear();
        removeEnemyBoardIfPresent();

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
                } else {
                    btn.addActionListener(e -> onEnemyBoardClick(btn));
                    btn.setEnabled(false); // ворог спочатку неактивний
                }
                grid.add(btn);
            }
        }

        panel.add(grid, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(450, 450));
        return panel;
    }

    private void beginManualPlacement() {
        // enable manual placement flow
        playerBoard.clear();
        shipsPlaced = false;
        placingShipsMode = true;
        currentShipIndex = 0;
        horizontalPlacement = true;
        rotateButton.setEnabled(true);
        startButton.setEnabled(false); // активується лише після розстановки всіх кораблів
        manualButton.setEnabled(false);
        autoButton.setEnabled(false);
        centerMessage.setText(" ");
        statusLabel.setText("Ручна розстановка: ставте корабель розміром " + SHIP_SIZES[currentShipIndex]);
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
        updateButtons();
    }

    private void onPlayerBoardClick(CellButton btn) {
        // якщо розстановку ще не вибрано, показуємо червоне повідомлення
        if (!placingShipsMode && !shipsPlaced) {
            centerMessage.setText("Виберіть режим розстановки");
            centerMessage.setForeground(Color.RED);
            return;
        }

        centerMessage.setText(" ");
        centerMessage.setForeground(Color.RED); // лишається червоним

        if (!placingShipsMode) return;
        centerMessage.setText(" ");
        int row = btn.row;
        int col = btn.col;
        int size = SHIP_SIZES[currentShipIndex];
        if (!playerBoard.canPlace(row, col, size, horizontalPlacement)) {
            centerMessage.setText("Не можна поставити корабель! Змініть місце.");
            return;
        }
        // place
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
        // Додаємо поле ворога, якщо ще не додане
        if (!isEnemyPanelPresent()) {
            boardsPanel.add(enemyBoardPanel);
            boardsPanel.revalidate();
            boardsPanel.repaint();
        }

        // Для ворога розставляємо автоматично
        if (enemyBoard.ships.isEmpty()) enemyBoard.placeShipsRandomly(SHIP_SIZES);

        // Блокуємо кнопки розстановки
        manualButton.setEnabled(false);
        autoButton.setEnabled(false);
        rotateButton.setEnabled(false);
        startButton.setEnabled(false);

        // Активуємо поле ворога для стрільби
        setEnemyFieldEnabled(true);

        // Статус: чи ваш хід
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
        // якщо зараз хід комп'ютера - повідомлення
        if (!playerTurn) {
            centerMessage.setText("Зараз хід ворога! Дочекайтесь своєї черги.");
            centerMessage.setForeground(Color.RED);
            return;
        }
        centerMessage.setText(" "); // очищаємо повідомлення, якщо хід гравця

        if (!shipsPlaced) return; // не можна стріляти поки не розставлені кораблі
        if (!playerTurn) return; // не ваш хід

        int r = btn.row;
        int c = btn.col;
        if (enemyBoard.isShot(r, c)) return;

        boolean hit = enemyBoard.shoot(r, c);
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
            // ход ворога після невеликої паузи
            Timer t = new Timer(3000, e -> enemyMakeMove());
            t.setRepeats(false);
            t.start();
        }
    }

    private void markSunkShip(Board board, CellButton[][] buttons) {
        for (Ship s : board.ships) {
            if (s.isSunk()) {
                // 1. фарбуємо клітинки корабля у чорний
                for (Point p : s.cells) {
                    buttons[p.x][p.y].setBackground(Color.BLACK);
                }

                // 2. ставимо навколо білі клітини
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
        // Всі зміни інтерфейсу робимо у EDT
        SwingUtilities.invokeLater(() -> {
            // Отримуємо хід AI на основі поля гравця
            Point p = playerBoard.nextAIMove();
            if (p == null) {
                // Невірна ситуація, fallback до першої вільної клітини
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

            // Виконуємо постріл
            boolean hit = playerBoard.shoot(p.x, p.y);

            // Негайно відобразити постріл на UI
            updateButtons();
            markSunkShip(playerBoard, playerButtons);
            updateStatusTurn();

            if (hit) {
                // Запам'ятати останнє попадання (вже робиться в shoot)
                playerBoard.lastHit = p;
                statusLabel.setText("Ворог влучив! Він ходить ще.");
                // Якщо гра не закінчилась - повторний хід через невелику паузу
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
                // Промах - передаємо хід гравцю
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

                // якщо ця клітинка належить вже потопленому кораблю — залишаємо чорним
                if (playerBoard.isPartOfSunkShip(r, c)) {
                    pb.setBackground(Color.BLACK);
                } else {
                    // гравець бачить свої кораблі; якщо по клітинці вже стріляли і там корабель — червоне
                    if (pv == Board.SHIP && playerBoard.isShot(r, c)) pb.setBackground(Color.RED);
                    else if (pv == Board.SHIP && !playerBoard.isShot(r, c)) pb.setBackground(new Color(0, 120, 160));
                    else if (playerBoard.isShot(r, c)) pb.setBackground(Color.WHITE);
                    else pb.setBackground(new Color(50, 90, 130));
                }

                // ENEMY buttons
                if (enemyBoard.isPartOfSunkShip(r, c)) {
                    // якщо цей корабель потоплено - чорний (щоб не перефарбовувати потоплений у червоне)
                    eb.setBackground(Color.BLACK);
                } else {
                    if (enemyBoard.isShot(r, c)) {
                        if (ev == Board.SHIP) eb.setBackground(Color.RED);
                        else eb.setBackground(Color.WHITE);
                    } else {
                        eb.setBackground(new Color(50, 90, 130));
                    }
                }
            }
        }
    }


    // Логіка полів\кораблів
    static class Board {
        static final int EMPTY = 0;
        static final int SHIP = 1;

        int n;
        int[][] grid;
        boolean[][] shot;
        List<Ship> ships = new ArrayList<>();
        Random rnd = new Random();
        int[] shipSizes;

        List<Point> aiTargets = new ArrayList<>(); // сусідні цілі після попадання
        Point lastHit = null; // останнє попадання для AI

        Board(int n, int[] shipSizes) {
            this.n = n;
            this.shipSizes = shipSizes;
            grid = new int[n][n];
            shot = new boolean[n][n];
        }

        void clear() {
            for (int i = 0; i < n; i++) Arrays.fill(grid[i], EMPTY);
            for (int i = 0; i < n; i++) Arrays.fill(shot[i], false);
            ships.clear();
        }

        int getCell(int r, int c) { return grid[r][c]; }
        boolean isShot(int r, int c) { return shot[r][c]; }

        boolean shoot(int r, int c) {
            shot[r][c] = true;
            if (grid[r][c] == SHIP) {
                for (Ship s : ships) {
                    if (s.contains(r, c)) {
                        s.hit(r, c);
                        if (!s.isSunk()) {
                            lastHit = new Point(r, c); // запам'ятовуємо останнє попадання
                        } else {
                            aiTargets.clear();
                            lastHit = null;
                        }
                    }
                }
                return true;
            }
            return false;
        }

        boolean canPlace(int row, int col, int size, boolean horiz) {
            if (horiz && col + size > n) return false;
            if (!horiz && row + size > n) return false;
            for (int k = 0; k < size; k++) {
                int r = row + (horiz ? 0 : k);
                int c = col + (horiz ? k : 0);
                if (grid[r][c] == SHIP) return false;
                for (int dr = -1; dr <= 1; dr++)
                    for (int dc = -1; dc <= 1; dc++) {
                        int nr = r + dr;
                        int nc = c + dc;
                        if (nr >= 0 && nr < n && nc >= 0 && nc < n)
                            if (grid[nr][nc] == SHIP) return false;
                    }
            }
            return true;
        }

        void placeShipsRandomly(int[] shipSizes) {
            for (int size : shipSizes) {
                boolean placed = false;
                int attempts = 0;
                while (!placed && attempts < 5000) {
                    attempts++;
                    boolean horiz = rnd.nextBoolean();
                    int row = rnd.nextInt(n);
                    int col = rnd.nextInt(n);
                    if (canPlace(row, col, size, horiz)) {
                        Ship s = new Ship(size);
                        for (int k = 0; k < size; k++) {
                            int r = row + (horiz ? 0 : k);
                            int c = col + (horiz ? k : 0);
                            grid[r][c] = SHIP;
                            s.addCell(r, c);
                        }
                        ships.add(s);
                        placed = true;
                    }
                }
                if (!placed) {
                    // якщо не вдалось розмістити - очистити і почати заново
                    clear();
                    placeShipsRandomly(shipSizes);
                    return;
                }
            }
        }

        boolean allShipsSunk() {
            for (Ship s : ships) if (!s.isSunk()) return false;
            return true;
        }

        Point nextAIMove() {
            // 1) Зібрати розміри кораблів, що ще не потоплені
            List<Integer> remaining = new ArrayList<>();
            for (Ship s : ships) {
                if (!s.isSunk()) remaining.add(s.size);
            }
            // Якщо немає інформації про власні кораблі (наприклад перед початком),
            // використаємо стандартний набір (shipSizes)
            if (remaining.isEmpty()) {
                for (int sz : shipSizes) remaining.add(sz);
            }

            // 2) Знайти всі "неприсвоєні" попадання (х* на полі - попадання в корабель, який ще не потоплений)
            List<Point> unresolvedHits = unresolvedHits();

            // 3) Побудувати матрицю ймовірностей (цілісні лічильники)
            int[][] heat = computeProbabilityGrid(remaining, unresolvedHits);

            // 4) Вибрати клітину з максимальною вагою, яка ще не простріляна
            int bestR = -1, bestC = -1;
            int bestVal = -1;
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {
                    if (shot[r][c]) continue; // пропускаємо вже простріляні
                    if (heat[r][c] > bestVal) {
                        bestVal = heat[r][c];
                        bestR = r; bestC = c;
                    }
                }
            }

            // 5) Якщо нічого не знайдено (bestVal == 0 або -1), fallback:
            if (bestVal <= 0) {
                // шахматний варіант (для ефективності проти 2-3 клітинних кораблів)
                List<Point> cand = new ArrayList<>();
                for (int r = 0; r < n; r++)
                    for (int c = 0; c < n; c++)
                        if (!shot[r][c] && (r + c) % 2 == 0) cand.add(new Point(r, c));
                if (!cand.isEmpty()) return cand.get(rnd.nextInt(cand.size()));
                // інакше будь-яка вільна
                List<Point> all = new ArrayList<>();
                for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) if (!shot[r][c]) all.add(new Point(r, c));
                return all.get(rnd.nextInt(all.size()));
            }

            return new Point(bestR, bestC);
        }

        /**
         * Збирає всі попадання по клітинках, що належать кораблям, які ще не потоплені.
         * Це допомагає прив'язувати ймовірності до областей з невитісненими попаданнями.
         */
        private List<Point> unresolvedHits() {
            List<Point> res = new ArrayList<>();
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {
                    if (shot[r][c] && grid[r][c] == SHIP) {
                        // знайти корабель, якому належить ця клітинка
                        for (Ship s : ships) {
                            if (s.contains(r, c) && !s.isSunk()) {
                                res.add(new Point(r, c));
                                break;
                            }
                        }
                    }
                }
            }
            return res;
        }

        private int[][] computeProbabilityGrid(List<Integer> remainingShipSizes, List<Point> unresolvedHits) {
            int[][] heat = new int[n][n];
            int totalPlacements = 0;

            for (int size : remainingShipSizes) {
                // горизонтальні
                for (int r = 0; r < n; r++) {
                    for (int c = 0; c + size - 1 < n; c++) {
                        boolean ok = true;
                        boolean coversUnresolved = unresolvedHits.isEmpty() ? true : false;
                        for (int k = 0; k < size; k++) {
                            int rr = r;
                            int cc = c + k;
                            // якщо в цій клітинці є промах - розміщення неможливе
                            if (shot[rr][cc] && grid[rr][cc] != SHIP) { ok = false; break; }
                            // не дозволяємо розміщувати поверх потоплених кораблів (вони вже відомі)
                            if (shot[rr][cc] && grid[rr][cc] == SHIP) {
                                // якщо ця клітинка належить вже потопленому кораблю - забороняємо
                                Ship sh = shipContaining(rr, cc);
                                if (sh != null && sh.isSunk()) { ok = false; break; }
                            }
                            // перевірка на наявність незакритого попадання в цьому розміщенні
                            for (Point hp : unresolvedHits) {
                                if (hp.x == rr && hp.y == cc) { coversUnresolved = true; }
                            }
                        }
                        if (!ok) continue;
                        if (!unresolvedHits.isEmpty() && !coversUnresolved) continue;
                        // додати в heat
                        for (int k = 0; k < size; k++) {
                            heat[r][c + k]++;
                        }
                        totalPlacements++;
                    }
                }
                // вертикальні
                for (int r = 0; r + size - 1 < n; r++) {
                    for (int c = 0; c < n; c++) {
                        boolean ok = true;
                        boolean coversUnresolved = unresolvedHits.isEmpty() ? true : false;
                        for (int k = 0; k < size; k++) {
                            int rr = r + k;
                            int cc = c;
                            if (shot[rr][cc] && grid[rr][cc] != SHIP) { ok = false; break; }
                            if (shot[rr][cc] && grid[rr][cc] == SHIP) {
                                Ship sh = shipContaining(rr, cc);
                                if (sh != null && sh.isSunk()) { ok = false; break; }
                            }
                            for (Point hp : unresolvedHits) {
                                if (hp.x == rr && hp.y == cc) { coversUnresolved = true; }
                            }
                        }
                        if (!ok) continue;
                        if (!unresolvedHits.isEmpty() && !coversUnresolved) continue;
                        for (int k = 0; k < size; k++) {
                            heat[r + k][c]++;
                        }
                        totalPlacements++;
                    }
                }
            } // кінець по всіх кораблях

            return heat;
        }

        // Знаходить корабель, що містить вказану клітинку (або null)
        private Ship shipContaining(int r, int c) {
            for (Ship s : ships) {
                if (s.contains(r, c)) return s;
            }
            return null;
        }
        // всередині static class Board
        public boolean isPartOfSunkShip(int r, int c) {
            Ship sh = shipContaining(r, c);
            return sh != null && sh.isSunk();
        }
    }


    static class Ship {
        private int size;
        private Set<Point> cells = new HashSet<>();
        private Set<Point> hits = new HashSet<>();

        Ship(int size) { this.size = size; }
        void addCell(int r, int c) { cells.add(new Point(r, c)); }
        boolean contains(int r, int c) { return cells.contains(new Point(r, c)); }
        void hit(int r, int c) { hits.add(new Point(r, c)); }
        boolean isSunk() { return hits.size() == size; }
    }

    static class CellButton extends JButton {
        final int row, col;

        CellButton(int row, int col) {
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BattleshipGame::new);
    }
}
