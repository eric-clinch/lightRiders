package Bot;
import java.util.Stack;

public class Bot {
	
	private static final int floodFillHelper(Board board, Location location){
		int res = 0;
		for(Move move : Move.ALLMOVES){
			Location newLocation = new Location(location.row + move.drow, location.col + move.dcol);
			if(newLocation.equals(board.getOpponentLocation())) return -1;
			if(board.floodable(newLocation.row, newLocation.col)){
				board.placeFlood(newLocation.row, newLocation.col);
				res++;
				int recursedRes = floodFillHelper(board, newLocation);
				
				if(recursedRes == -1) return -1;
				else res += recursedRes;
			}
		}
		return res;
	}
	
	private static final int floodFill(Board board){
		board = board.deepcopy();
		return floodFillHelper(board, board.getPlayerLocation());
	}
	
	private Stack<Move> moves;
	private GetMoves getMoves;
	private GetMovesEndGame getMovesEndGame;
	
	public Bot(GetMoves getMoves, GetMovesEndGame getMovesEndGame){
		this.moves = new Stack<>();
		this.getMoves = getMoves;
		this.getMovesEndGame = getMovesEndGame;
	}
	
	public final Move getMove(Board board, int time, int round, Move lastOpponentMove){
		if(moves.isEmpty()){
			int partitionSpace = floodFill(board);
			if(partitionSpace == -1) moves = getMoves.getPlayerMoves(board, time, round, lastOpponentMove); // the board is not partitioned
			else moves = getMovesEndGame.getPlayerMoves(board, time, round, lastOpponentMove, partitionSpace);
		}
		return moves.pop();
	}
}