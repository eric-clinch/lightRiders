package Bot;

import java.util.Stack;

public interface GetMovesEndGame {
	public Stack<Move> getPlayerMoves(Board board, int time, int round, Move lastOpponentMove, int partitionSpace);
}
