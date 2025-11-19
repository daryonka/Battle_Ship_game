package battleship.core;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Board {
    public static final int EMPTY = 0;
    public static final int SHIP = 1;

    int n;
    public int[][] grid;
    public boolean[][] shot;
    public List<Ship> ships = new ArrayList<>();
    Random rnd = new Random();
    int[] shipSizes;

    List<Point> aiTargets = new ArrayList<>();
    public Point lastHit = null;

    public Board(int n, int[] shipSizes) {
        this.n = n;
        this.shipSizes = shipSizes;
        grid = new int[n][n];
        shot = new boolean[n][n];
    }

    public void clear() {
        for (int i = 0; i < n; i++) Arrays.fill(grid[i], EMPTY);
        for (int i = 0; i < n; i++) Arrays.fill(shot[i], false);
        ships.clear();
    }

    public int getCell(int r, int c) { return grid[r][c];
    }
    public boolean isShot(int r, int c) { return shot[r][c];
    }

    public boolean shoot(int r, int c) {
        shot[r][c] = true;
        if (grid[r][c] == SHIP) {
            for (Ship s : ships) {
                if (s.contains(r, c)) {
                    s.hit(r, c);
                    if (!s.isSunk()) {
                        lastHit = new Point(r, c);
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

    public boolean canPlace(int row, int col, int size, boolean horiz) {
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

    public void placeShipsRandomly(int[] shipSizes) {
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
                clear();
                placeShipsRandomly(shipSizes);
                return;
            }
        }
    }

    public boolean allShipsSunk() {
        for (Ship s : ships) if (!s.isSunk()) return false;
        return true;
    }

    public Point nextAIMove() {
        // 1) Зібрати розміри кораблів, що ще не потоплені
        List<Integer> remaining = new ArrayList<>();
        for (Ship s : ships) {
            if (!s.isSunk()) remaining.add(s.getSize()); // ЗМІНА: Використовується getSize()
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
                if (shot[r][c]) continue;
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

    private List<Point> unresolvedHits() {
        List<Point> res = new ArrayList<>();
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                if (shot[r][c] && grid[r][c] == SHIP) {
                    for (Ship s : ships) {
                        if (s.contains(r, c)) {
                            boolean isShipSunk = s.isSunk();
                            if (!isShipSunk) {
                                res.add(new Point(r, c));
                                break;
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    private int[][] computeProbabilityGrid(List<Integer> remainingShipSizes, List<Point> unresolvedHits) {
        int[][] heat = new int[n][n];

        for (int size : remainingShipSizes) {
            // горизонтальні
            for (int r = 0; r < n; r++) {
                for (int c = 0; c + size - 1 < n; c++) {

                    boolean ok = true;
                    boolean coversUnresolved = unresolvedHits.isEmpty() ? true : false;
                    for (int k = 0; k < size; k++) {
                        int rr = r;
                        int cc = c + k;
                        if (shot[rr][cc] && grid[rr][cc] != SHIP) { ok = false;
                            break; }
                        if (shot[rr][cc] && grid[rr][cc] == SHIP) {

                            Ship sh = shipContaining(rr, cc);
                            if (sh != null && sh.isSunk()) { ok = false; break;
                            }
                        }
                        for (Point hp : unresolvedHits) {

                            if (hp.x == rr && hp.y == cc) { coversUnresolved = true;
                            }
                        }
                    }
                    if (!ok) continue;
                    if (!unresolvedHits.isEmpty() && !coversUnresolved) continue;
                    for (int k = 0; k < size; k++) {
                        heat[r][c + k]++;
                    }
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
                        if (shot[rr][cc] && grid[rr][cc] != SHIP) { ok = false; break;
                        }
                        if (shot[rr][cc] && grid[rr][cc] == SHIP) {
                            Ship sh = shipContaining(rr, cc);
                            if (sh != null && sh.isSunk()) { ok = false; break;
                            }
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
                }
            }
        }

        return heat;
    }

    private Ship shipContaining(int r, int c) {
        for (Ship s : ships) {
            if (s.contains(r, c)) return s;
        }
        return null;
    }
}