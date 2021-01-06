import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Optimizer implements OptimizerInterface {
    private final HashSet<PawnHandler> pawns = new HashSet<>();
    private final HashMap<Integer, Thread> threads = new HashMap<>();
    private final List<List<Square>> squares = new ArrayList<>();
    private BoardInterface board;
    private int boardSize;
    private int meetingPointCol;
    private int meetingPointRow;

    @Override
    public void setBoard(BoardInterface board) {
        this.board = board;
        setBoardAttributes();
        initialize();
        optimize();
    }

    @Override
    public void suspend() {

    }

    @Override
    public void resume() {

    }

    private Square getSquare(int col, int row) {
        return squares.get(col).get(row);
    }

    private boolean isMeetingPointOccupied() {
        return getSquare(meetingPointCol, meetingPointRow).isOccupied.get();
    }

    private void setBoardAttributes() {
        this.boardSize = board.getSize();
        this.meetingPointCol = board.getMeetingPointCol();
        this.meetingPointRow = board.getMeetingPointRow();
    }

    private void initialize() {
        populateSquares();
        getPawnsInterfaces();
        createAndRegisterThreadsForPawns();
    }

    private void populateSquares() {
        for (int i = 0; i < boardSize; i++) {
            List<Square> row = new ArrayList<>();
            for (int j = 0; j < boardSize; j++) {
                row.add(new Square());
            }
            squares.add(row);
        }
    }

    private void optimize() {
        board.optimizationStart();
        runAllPawnsThreads();
        joinAllPawnsThreads();
        board.optimizationDone();
    }

    private void runAllPawnsThreads() {
        threads.forEach((id, thread) -> thread.start());
    }

    private void joinAllPawnsThreads() {
        threads.forEach((id, thread) -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void getPawnsInterfaces() {
        for (int col = 0; col < boardSize; col++) {
            for (int row = 0; row < boardSize; row++) {
                int finalCol = col;
                int finalRow = row;
                board.get(col, row).ifPresent(pawnInterface -> {
                    pawns.add(new PawnHandler(finalCol, finalRow, pawnInterface));
                    squares.get(finalCol).get(finalRow).isOccupied.set(true);
                });
            }
        }
    }

    private void threadFunction(PawnHandler handler) {
        while (!isMeetingPointOccupied()) {
            handler.makeMove();
        }
    }

    private void createAndRegisterThreadsForPawns() {
        pawns.forEach(pawn -> {
            int pawnId = pawn.pawnId;
            Thread thread = new Thread(() -> threadFunction(pawn));
            pawn.pawnRef.registerThread(thread);
            threads.put(pawnId, thread);
        });
    }

    //---------------------------
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

    //---------------------------
    private class Square {
        private final AtomicBoolean isOccupied = new AtomicBoolean(false);

        Square() {
        }

        void occupyOrWait() {
            synchronized (this) {
                if (this.isNotOccupied()) {
                    this.isOccupied.set(true);
                } else {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        void release() {
            this.isOccupied.set(false);
            synchronized (this) {
                this.notify();
            }
        }

        boolean isNotOccupied() {
            return !this.isOccupied.get();
        }
    }

    //---------------------------
    private class PawnHandler {
        int col;
        int row;
        int pawnId;
        PawnInterface pawnRef;

        PawnHandler(int col, int row, PawnInterface pawnRef) {
            this.col = col;
            this.row = row;
            this.pawnRef = pawnRef;
            this.pawnId = pawnRef.getID();
        }

        public void makeMove() {
            int oldCol = col;
            int oldRow = row;

            Optional<Move> nextMove = getNextMoveWithLock();
            nextMove.ifPresent(move -> {
                switch (move) {
                    case UP -> this.row = pawnRef.moveUp();
                    case DOWN -> this.row = pawnRef.moveDown();
                    case LEFT -> this.col = pawnRef.moveLeft();
                    case RIGHT -> this.col = pawnRef.moveRight();
                }
            getSquare(oldCol, oldRow).release();

            });
        }

        private Optional<Move> getNextMoveWithLock() {
            Optional<Move> blockedMove = Optional.empty();
            if (meetingPointCol < this.col) {
                if (canMove(Move.LEFT)) {
                    getSquare(col + Move.LEFT.col, row + Move.LEFT.row).occupyOrWait();
                    return Optional.of(Move.LEFT);
                } else {
                    blockedMove = Optional.of(Move.LEFT);
                }
            }
            if (meetingPointCol > this.col) {
                if (canMove(Move.RIGHT)) {
                    getSquare(col + Move.RIGHT.col, row + Move.RIGHT.row).occupyOrWait();
                    return Optional.of(Move.RIGHT);
                } else {
                    blockedMove = Optional.of(Move.RIGHT);
                }
            }
            if (meetingPointRow < this.row && canMove(Move.DOWN)) {
                getSquare(col + Move.DOWN.col, row + Move.DOWN.row).occupyOrWait();
                return Optional.of(Move.DOWN);
            }
            if (meetingPointRow > this.row && canMove(Move.UP)) {
                getSquare(col + Move.UP.col, row + Move.UP.row).occupyOrWait();
                return Optional.of(Move.UP);
            }
            blockedMove.ifPresent(move -> getSquare(col + move.col, row + move.row).occupyOrWait());
            return blockedMove;
        }

        private boolean canMove(Move direction) {
            return getSquare(col + direction.col, row + direction.row).isNotOccupied();
        }

    }
}
