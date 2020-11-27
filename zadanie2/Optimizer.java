import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;

public class Optimizer implements OptimizerInterface {
    private BoardInterface board;
    private int boardSize;
    private HashSet<PawnInterface> pawns;
    private HashMap<Integer, Thread> threads;

    @Override
    public void setBoard(BoardInterface board) {
        this.board = board;
        boardSize = board.getSize();
        initialize();
    }

    @Override
    public void suspend() {

    }

    @Override
    public void resume() {

    }

    private void initialize() {
        getPawnsInterfaces();
        createThreadsForPawns();
    }

    private void getPawnsInterfaces() {
        for (int col = 0; col < boardSize; col++) {
            for (int row = 0; row < boardSize; row++) {
                board.get(col, row).ifPresent(pawn -> pawns.add(pawn));
            }
        }
    }

    private void createThreadsForPawns() {
        pawns.forEach(pawn -> {
            int pawnId = pawn.getID();
            threads.put(pawnId, new Thread(() -> {
                System.out.printf("Tutej pionek nr %d%n", pawnId);
            }));
        });
    }
}
