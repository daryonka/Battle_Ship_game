package battleship.core;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

public class Ship {
    private int size; // Залишається private
    public Set<Point> cells = new HashSet<>();
    private Set<Point> hits = new HashSet<>();

    public Ship(int size) { this.size = size;
    }
    public void addCell(int r, int c) { cells.add(new Point(r, c));
    }
    public boolean contains(int r, int c) { return cells.contains(new Point(r, c));
    }
    public void hit(int r, int c) { hits.add(new Point(r, c));
    }
    public boolean isSunk() { return hits.size() == size;
    }

    public int getSize() {
        return size;
    }
}