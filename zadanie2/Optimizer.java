import java.util.HashMap;
import java.util.HashSet;

public class Optimizer implements OptimizerInterface {
    private final HashSet<PawnInterface> pawns = new HashSet<>();
    private final HashMap<Integer, Thread> threads = new HashMap<>();
    private BoardInterface board;
    private int boardSize;

    @Override
    public void setBoard(BoardInterface board) {
        this.board = board;
        boardSize = board.getSize();
        initialize();
        optimize();
    }

    @Override
    public void suspend() {

    }

    @Override
    public void resume() {

    }

    private void initialize() {
        getPawnsInterfaces();
        createAndRegisterThreadsForPawns();
    }

    private void optimize() {
        runAllPawnsThreads();
        joinAllPawnsThreads();
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
                board.get(col, row).ifPresent(pawns::add);
            }
        }
    }

    private void threadFunction(int pawnId) {
        System.out.printf("Tutaj pionek nr %d\n", pawnId);
    }

    private void createAndRegisterThreadsForPawns() {
        pawns.forEach(pawn -> {
            int pawnId = pawn.getID();
            Thread thread = new Thread(() -> threadFunction(pawnId));
            pawn.registerThread(thread);
            threads.put(pawnId, thread);
        });
    }
}
