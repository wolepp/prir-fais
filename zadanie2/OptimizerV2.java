import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class OptimizerV2 implements OptimizerInterface {
    private final List<List<Square>> squares = new ArrayList<>();
    private final HashSet<PawnHandler> pawnHandlers = new HashSet<>();
    private OptimizationChecker optimizationChecker;
    private BoardInterface board;
    private int boardSize;
    private int meetingPointCol;
    private int meetingPointRow;

    /* TODO: trzeba zrobić join na wszystkich wątkach i czekać aż zakończą działanie, dopiero wtedy
    wywołać optimizationDone
     */

    @Override
    public void setBoard(BoardInterface board) {
        this.board = board;
        initialize();
        startOptimization();
    }

    @Override
    public void suspend() {
        // TODO: Zaimplementować
    }

    @Override
    public void resume() {
        // TODO: Zaimplementować
    }

    private void initialize() {
        getAndSetBoardAttributes();
        setSquares();
        getAndSetPawnInterfaces();
        createOptimizationChecker();
        registerPawnsThreads();
    }

    private void getAndSetBoardAttributes() {
        boardSize = board.getSize();
        this.meetingPointCol = board.getMeetingPointCol();
        this.meetingPointRow = board.getMeetingPointRow();
    }

    private void setSquares() {
        for (int i = 0; i < boardSize; i++) {
            List<Square> row = new ArrayList<>();
            for (int j = 0; j < boardSize; j++) {
                row.add(new Square(i, j));
            }
            squares.add(row);
        }
    }

    private void getAndSetPawnInterfaces() {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                int row = j;
                int col = i;
                board.get(col, row).ifPresent(pawnInterface -> {
                    pawnHandlers.add(new PawnHandler(col, row, pawnInterface));
                    getSquare(col, row).isOccupied.set(true);
                    System.out.printf("Zajmuję %d x %d\n", col, row);
                });
            }
        }
    }

    private Square getSquare(int col, int row) {
        return squares.get(col).get(row);
    }

    private void createOptimizationChecker() {
        optimizationChecker = new OptimizationChecker();
    }

    private void registerPawnsThreads() {
        pawnHandlers.forEach(handler -> {
            System.out.printf("Rejestruję pionek %d\n", handler.pawnId);
            handler.pawnRef.registerThread(handler.thread);
        });
    }

    private void stopAllPawns() {
        System.out.println("main: wyłączam wszystkie wątki pionków");
        pawnHandlers.forEach(PawnHandler::stop);
        System.out.println("main: zwalniam wszystkie pola");
        squares.forEach(squareRow -> squareRow.forEach(Square::release));
    }

    private void startOptimization() {
        board.optimizationStart();
        optimizationChecker.thread.start();
        pawnHandlers.forEach(PawnHandler::start);
        try {
            optimizationChecker.thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        stopAllPawns();
        pawnHandlers.forEach(pawnHandler -> {
            try {
                pawnHandler.thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        board.optimizationDone();
    }


    ///////////////////////////////////////////////////////////////////////
    private enum Move {
        LEFT(-1, 0),
        RIGHT(1, 0),
        UP(0, 1),
        DOWN(0, -1);

        int col;
        int row;

        Move(int col, int row) {
            this.col = col;
            this.row = row;
        }
    }

    ///////////////////////////////////////////////////////////////////////
    private class OptimizationChecker {
        private final AtomicInteger blockedPawnsCounter;
        Square meetingPoint;
        Thread thread;

        public OptimizationChecker() {
            this.blockedPawnsCounter = new AtomicInteger(0);
            this.meetingPoint = getSquare(meetingPointCol, meetingPointRow);
            thread = new Thread(() -> {
                System.out.println("Tutaj optimization checker - zaczynam pracę");
                do {
                    try {
                        synchronized (this) {
                            System.out.println("Zaczynam czekanie");
                            this.wait();
                            System.out.println("Zostałem wybudzony - sprawdzam czy meeting point jest zajęty");
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (!isMeetingPointOccupied());
                System.out.println("Checker: Meeting point zajęty - kończę pracę");
            });
        }

        public void incrementBlockedPawnsCounter() {
            if (this.blockedPawnsCounter.incrementAndGet() == pawnHandlers.size()) {
                synchronized (this) {
                    this.notify();
                }
            }
        }

        public void decrementBlockedPawnsCounter() {
            this.blockedPawnsCounter.decrementAndGet();
        }

        private boolean isMeetingPointOccupied() {
            return meetingPoint.isOccupied();
        }
    }

    ///////////////////////////////////////////////////////////////////////
    private class PawnHandler {
        int col;
        int row;
        int pawnId;
        boolean running;
        Thread thread;
        PawnInterface pawnRef;

        public PawnHandler(int col, int row, PawnInterface pawnRef) {
            this.col = col;
            this.row = row;
            this.pawnRef = pawnRef;
            this.pawnId = pawnRef.getID();
            this.thread = new Thread(pawnRunnable());
        }

        public void stop() {
            this.running = false;
        }

        public void start() {
            this.thread.start();
        }

        private Runnable pawnRunnable() {
            return () -> {
                this.running = true;
                while (running && !isOnMeetingPoint()) {
                    makeMove();
                }
                optimizationChecker.incrementBlockedPawnsCounter();
            };
        }

        private void makeMove() {
            Move nextMove;
            Square oldSquare = getSquare(col, row);
            try {
                nextMove = getNextMove();
                System.out.printf("Ruszam się w %s\n", nextMove);
                col = col + nextMove.col;
                row = row + nextMove.row;
                getSquare(col, row).occupy();

                if (running) {
                    switch (nextMove) {
                        case LEFT -> pawnRef.moveLeft();
                        case RIGHT -> pawnRef.moveRight();
                        case DOWN -> pawnRef.moveDown();
                        case UP -> pawnRef.moveUp();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (running) {
                    oldSquare.release();
                }
            }
        }

        private Move getNextMove() throws Exception {
            Optional<Move> blockedMove = Optional.empty();

            if (meetingPointCol < this.col) {
                if (canMove(Move.LEFT)) {
                    System.out.println("Zwracam ruch w lewo");
                    return Move.LEFT;
                } else {
                    System.out.println("Następny ruch zablokowany w LEWO");
                    blockedMove = Optional.of(Move.LEFT);
                }
            }
            if (meetingPointCol > this.col) {
                if (canMove(Move.RIGHT)) {
                    System.out.println("Zwracam ruch w prawo");
                    return Move.RIGHT;
                } else {
                    System.out.println("Następny ruch zablokowany w PRAWO");
                    blockedMove = Optional.of(Move.RIGHT);
                }
            }
            if (meetingPointRow < this.row) {
                System.out.println("Zwracam ruch w dół");
                return Move.DOWN;
            }
            if (meetingPointRow > this.row) {
                System.out.println("Zwracam ruch w górę");
                return Move.UP;
            }
            System.out.printf("Zwracam zablokowany ruch w %s\n", blockedMove.get());
            return blockedMove.orElseThrow(() -> new Exception("Coś nie tak"));
        }

        private boolean canMove(Move direction) {
            int destCol = col + direction.col;
            int destRow = row + direction.row;
            return !getSquare(destCol, destRow).isOccupied();
        }

        private boolean isOnMeetingPoint() {
            return col == meetingPointCol && row == meetingPointRow;
        }
    }

    ///////////////////////////////////////////////////////////////////////
    private class Square {
        private final AtomicBoolean isOccupied;
        int col;
        int row;

        public Square(int col, int row) {
            this.isOccupied = new AtomicBoolean(false);
            this.col = col;
            this.row = row;
        }

        synchronized void occupy() throws InterruptedException {
            System.out.printf("Pole %dx%d Metoda occupy start\n", col, row);
            if (!isOccupied.compareAndSet(false, true)) {
                System.out.printf("Pole %dx%d jest zajęty, zwiększam counter i czekam\n", col, row);
                do {
                    optimizationChecker.incrementBlockedPawnsCounter();
                    this.wait();
                    System.out.printf("Pionek na %dx%d- zostałem wybudzony\n", col, row);
                } while(!isOccupied.compareAndSet(false, true));
                System.out.println("Zmniejszam counter");
                optimizationChecker.decrementBlockedPawnsCounter();
            } else {
                System.out.printf("Pole %dx%d wolne więc zostaję tutaj\n", col, row);
            }
        }

        synchronized void release() {
            if (isOccupied.compareAndSet(true, false)) {
                System.out.printf("Zwalniam pole %dx%d\n", col, row);
                this.notify();
            }
        }

        boolean isOccupied() {
            return isOccupied.get();
        }
    }

}
