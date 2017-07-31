package Bot;
import java.util.Stack;

public interface GetMoves {	
	public Stack<Move> getPlayerMoves(Board board, int time, int round, Move lastOpponentMove);
}
