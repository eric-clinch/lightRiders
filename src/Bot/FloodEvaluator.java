package Bot;

public class FloodEvaluator implements Evaluator {
	
	public int getMaxValue(){
		return 256;
	}
	
	public int getPartitionedOffset(){
		return 512;
	}
	
	private int floodFillHelper(Board board, Location location){
		int res = 0;
		for(Move move : Move.ALLMOVES){
			Location newLocation = new Location(location.row + move.drow, location.col + move.dcol);
			if(board.floodable(newLocation.row, newLocation.col)){
				board.placeFlood(newLocation.row, newLocation.col);
				res++;
				int recursedRes = floodFillHelper(board, newLocation);
				res += recursedRes;
			}
		}
		return res;
	}
	
	public int evaluate(Board board){
		board = board.deepcopy();
		return floodFillHelper(board, board.getPlayerLocation());
	}
}
