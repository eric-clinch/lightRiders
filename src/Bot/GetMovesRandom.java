package Bot;
import java.util.Random;
import java.util.Stack;

public class GetMovesRandom implements GetMoves {
	public Stack<Move> getPlayerMoves(Board board, int time, int round, Move lastOpponentMove){
		Move[] legalMoves = board.getLegalMovesForPlayer();
		Move m;
		if(legalMoves.length == 0) m = Move.UP;
		else{
			Random r = new Random();
			int randInt = r.nextInt(legalMoves.length);
			m = legalMoves[randInt];
		}
		Stack<Move> s = new Stack<>();
		s.push(m);
		return s;
	}
}
