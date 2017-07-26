package Bot;
import java.util.Stack;

public class Bot {
	
	private Stack<Move> moves;
	private GetMoves gms;
	
	public Bot(GetMoves gms){
		this.moves = new Stack<>();
		this.gms = gms;
	}
	
	public Move getMove(Board board, int time, int round){
		if(moves.isEmpty()) moves = gms.getPlayerMoves(board, time, round);
		return moves.pop();
	}
}