import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

class Coordinates {
    public int x;
    public int y;

    public Coordinates(int new_x, int new_y) {
        x = new_x;
        y = new_y;
    }

    public Coordinates() {
        this(0, 0);
    }

    public Coordinates(Coordinates copy) {
        this(copy.x, copy.y);
    }

    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    public Coordinates copy() {
        return new Coordinates(this);
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Coordinates))
            return false;
        Coordinates other = (Coordinates) o;
        return (other.x == x && other.y == y);
    }

    public int hashCode() {
        return Objects.hash(x, y);
    }
}

enum Move {
    LEFT(0, -1),
    RIGHT(0, 1),
    DOWN(-1, 0),
    UP(1, 0);

    public final int x;
    public final int y;

    Move(int new_x, int new_y) {
        x = new_x;
        y = new_y;
    }
}

public class Board implements BoardInterface {
    class Pawn implements PawnInterface {
        private final int m_id;
        private final AtomicReference<Thread> m_my_thread
                = new AtomicReference<Thread>();
        private final Coordinates m_coords = new Coordinates();

        public Pawn(int x, int y) {
            m_id = m_id_generator.incrementAndGet();
            m_coords.x = x;
            m_coords.y = y;
        }

        private void delay(int time) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
            }
        }

        public int getID() {
            return m_id;
        }

        public void registerThread(Thread thread) {
            m_my_thread.set(thread);
        }

        private void check() {
            if (Thread.currentThread() != m_my_thread.get())
                throw new RuntimeException("Pionek poruszony złym pionkiem");
            if (!m_running.get())
                throw new RuntimeException("Nie wywołano optimizationStart");
        }

        private void makeMove(Move move) {
            check();
            // System.out.println("Poruszamy " + m_id + " " + move);
            final int new_x = m_coords.x + move.x;
            final int new_y = m_coords.y + move.y;
            final int old_x = m_coords.x;
            final int old_y = m_coords.y;

            if (!m_array.compareAndSet(index(new_x, new_y), 0, m_id))
                throw new RuntimeException(
                        "Pole (" + new_x + ", " + new_y + ") nie jest puste");
            //
            m_changed.set(true);
            m_map.put(new Coordinates(new_x, new_y), this);
            m_coords.x = new_x;
            m_coords.y = new_y;
            // delay(100 + (int)(Math.random() * 2000));
            m_map.remove(new Coordinates(old_x, old_y));

            if (!m_array.compareAndSet(index(old_x, old_y), m_id, 0))
                throw new RuntimeException(
                        "Pole (" + old_x + ", " + old_y
                                + ") zostało zajęte przez kogoś innego");
            m_changed.set(true);
        }

        public int moveLeft() {
            makeMove(Move.LEFT);
            return m_coords.y;
        }

        public int moveRight() {
            makeMove(Move.RIGHT);
            return m_coords.y;
        }

        public int moveUp() {
            makeMove(Move.UP);
            return m_coords.x;
        }

        public int moveDown() {
            makeMove(Move.DOWN);
            return m_coords.x;
        }
    }

    // koniec klasy Pawn

    private final int m_size;
    private final int m_middlepoint_row;
    private final int m_middlepoint_col;
    private final AtomicBoolean m_running = new AtomicBoolean(false);
    private final AtomicIntegerArray m_array;
    private final CountDownLatch m_start_signal = new CountDownLatch(1);
    private final AtomicBoolean m_changed = new AtomicBoolean(false);
    private final ConcurrentHashMap<Coordinates, Pawn> m_map
            = new ConcurrentHashMap<Coordinates, Pawn>();

    private final AtomicInteger m_id_generator = new AtomicInteger();

    public static void main(String[] args) {
        OptimizerInterface optimizer = new Optimizer();
        final int N = 9;
        Board board = new Board(
                N,
                ThreadLocalRandom.current().nextInt(N),
                ThreadLocalRandom.current().nextInt(N));

        {
            final Integer[] indices = new Integer[N * N];
            Arrays.setAll(indices, i -> i);
            Collections.shuffle(Arrays.asList(indices));
            for (int i = ThreadLocalRandom.current().nextInt(1, N * N); i > 0; --i) {
                final int x = indices[i];
                board.addPawn(x / N, x % N);
            }
        }

        board.print();
        {
            Thread p = new Thread(() -> {
                board.waitForStart();
                while (board.m_running.get()) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                    if (board.m_changed.getAndSet(false))
                        board.print();
                }
            });
            p.setDaemon(true);
            p.start();
        }

        optimizer.setBoard(board);

        board.waitUntillDone();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
        }
        board.print();
    }

    public Board(int N) {
        this(N, (int) Math.floor((N - 1) / 2));
    }

    public Board(int N, int middlepoint) {
        this(N, middlepoint, middlepoint);
    }

    @SuppressWarnings("unchecked")
    public Board(int N, int middlepoint_row, int middlepoint_col) {
        System.out.println(
                "Board: " + N + ", (" + middlepoint_row + ", " + middlepoint_col
                        + ")");
        m_size = N;
        m_middlepoint_row = middlepoint_row;
        m_middlepoint_col = middlepoint_col;
        m_array = new AtomicIntegerArray(m_size * m_size);
    }

    public int getSize() {
        return m_size;
    }

    public int getMeetingPointCol() {
        return m_middlepoint_col;
    }

    public int getMeetingPointRow() {
        return m_middlepoint_row;
    }

    public void addPawn(int x, int y) {
        // System.out.println("Adding pawn on (" + x + ", " + y + ")");
        Pawn tmp = new Pawn(x, y);
        if (!m_array.compareAndSet(index(x, y), 0, tmp.getID())) {
            throw new RuntimeException(
                    "Pionek już znajduje się na pozycji (" + x + ", " + y + ")");
        }
        m_map.put(new Coordinates(x, y), tmp);
    }

    public Optional<PawnInterface> get(int col, int row) {
        synchronized (m_array) {
            final int id = m_array.get(index(row, col));
            if (id == 0) {
                return Optional.empty();
            } else {
                return Optional.of(m_map.get(new Coordinates(row, col)));
            }
        }
    }

    private int index(int x, int y) {
        return m_size * x + y;
    }

    public void optimizationDone() {
        m_running.set(false);
        synchronized (m_running) {
            m_running.notifyAll();
        }
        System.out.println("Optimization done");
    }

    public void optimizationStart() {
        m_running.set(true);
        m_start_signal.countDown();
        System.out.println("Starting optimization");
    }

    public void waitForStart() {
        try {
            m_start_signal.await();
        } catch (InterruptedException e) {
        }
    }

    public void waitUntillDone() {
        synchronized (m_running) {
            while (m_running.get()) {
                try {
                    m_running.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void print() {
        String linia
                = String.join("----", Collections.nCopies(m_size + 1, "+"));
        for (int n_row = m_size - 1; n_row >= 0; --n_row) {
            System.out.println(linia);
            for (int n_col = 0; n_col < m_size; ++n_col) {
                int id = m_array.get(index(n_row, n_col));
                Pawn x;
                if (id == 0)
                    System.out.print("|    ");
                else if (
                        (x = m_map.get(new Coordinates(n_row, n_col))) != null
                                && x.m_my_thread.get() != null
                                && x.m_my_thread.get().getState()
                                == Thread.State.TERMINATED)
                    System.out.format("| %2d ", id);
                else
                    System.out.format("|*%2d*", id);
            }
            System.out.println("|");
        }
        System.out.println(linia);
    }
}
