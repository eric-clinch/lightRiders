package Bot;

public class FloodEvaluator implements Evaluator {
	
	private int floodFillPlayerHelper(Board board, Location location){
		int res = 0;
		for(Move move : Move.ALLMOVES){
			Location newLocation = new Location(location.row + move.drow, location.col + move.dcol);
			if(newLocation.equals(board.getOpponentLocation())) return -1;
			if(board.floodable(newLocation.row, newLocation.col)){
				board.placeFlood(newLocation.row, newLocation.col);
				res++;
				int recursedRes = floodFillPlayerHelper(board, newLocation);
				
				if(recursedRes == -1) return -1;
				else res += recursedRes;
			}
		}
		return res;
	}
	
	private int floodFillPlayer(Board board){
		board = board.deepcopy();
		return floodFillPlayerHelper(board, board.getPlayerLocation());
	}
	
	private int scanFloodFillPlayerHelper(Board board, int row, int col, int opponentRow, int opponentCol){
		int res = 0;
		
		int col0 = col;
		int colMax = col;
		while(col0 < board.cols && board.floodable(row, col0)){
			board.placeFlood(row, col0);
			colMax = col0;
			res++;
			col0++;
			if(col0 == opponentCol && row == opponentRow) return -1;
		}
		
		col0 = col - 1;
		if(col0 == opponentCol && row == opponentRow) return -1;
		int colMin = col;
		while(col0 >= 0 && board.floodable(row, col0)){
			board.placeFlood(row, col0);
			colMin = col0;
			res++;
			col0--;
			if(col0 == opponentCol && row == opponentRow) return -1;
		}
		
		//scan above
		for(col0 = colMin; col0 <= colMax; col0++){
			if(row - 1 == opponentRow && col0 == opponentCol) return -1;
			if(row > 0 && board.floodable(row - 1, col0)){
				int recRes = scanFloodFillPlayerHelper(board, row - 1, col0, opponentRow, opponentCol);
				if(recRes == -1) return -1;
				res += recRes;
			}
		}
		
		//scan below
		for(col0 = colMin; col0 <= colMax; col0++){
			if(row + 1 == opponentRow && col0 == opponentCol) return -1;
			if(row < board.rows - 1 && board.floodable(row + 1, col0)){
				int recRes = scanFloodFillPlayerHelper(board, row + 1, col0, opponentRow, opponentCol);
				if(recRes == -1) return -1;
				res += recRes;
			}
		}
		
		return res;
	}
	
	private int scanFloodFillPlayer(Board board){		
		board = board.deepcopy();
		int res = 0;
		Location playerLocation = board.getPlayerLocation();
		Location opponentLocation = board.getOpponentLocation();
		int opponentRow = opponentLocation.row;
		int opponentCol = opponentLocation.col;
		for(Move move : Move.ALLMOVES){
			int newRow = playerLocation.row + move.drow;
			int newCol = playerLocation.col + move.dcol;
			if(newRow == opponentRow && newCol == opponentCol) return -1;
			if(board.floodable(newRow, newCol)){
				int recRes = scanFloodFillPlayerHelper(board, newRow, newCol, opponentRow, opponentCol);
				if(recRes == -1){
					return -1;
				}
				res += recRes;
			}
		}
		return res;
	}
	
	private int floodFillOpponentHelper(Board board, Location location){
		int res = 0;
		for(Move move : Move.ALLMOVES){
			Location newLocation = new Location(location.row + move.drow, location.col + move.dcol);
			if(newLocation.equals(board.getPlayerLocation())) return -1;
			if(board.floodable(newLocation.row, newLocation.col)){
				board.placeFlood(newLocation.row, newLocation.col);
				res++;
				int recursedRes = floodFillOpponentHelper(board, newLocation);
				
				if(recursedRes == -1) return -1;
				else res += recursedRes;
			}
		}
		return res;
	}
	
	private int floodFillOpponent(Board board){
		board = board.deepcopy();
		return floodFillOpponentHelper(board, board.getOpponentLocation());
	}
	
	private int scanFloodFillOpponentHelper(Board board, int row, int col, int playerRow, int playerCol){
		int res = 0;
//		assert(row != playerRow || col != playerCol);
//		assert(board.floodable(row, col));
		
		int col0 = col;
		int colMax = col;
		while(col0 < board.cols && board.floodable(row, col0)){
			board.placeFlood(row, col0);
			colMax = col0;
			res++;
			col0++;
			if(col0 == playerCol && row == playerRow) return -1;
		}
		
		col0 = col - 1;
		if(col0 == playerCol && row == playerRow) return -1;
		int colMin = col;
		while(col0 >= 0 && board.floodable(row, col0)){
			board.placeFlood(row, col0);
			colMin = col0;
			res++;
			col0--;
			if(col0 == playerCol && row == playerRow) return -1;
		}
		
		//scan above
		for(col0 = colMin; col0 <= colMax; col0++){
			if(row - 1 == playerRow && col0 == playerCol) return -1;
			if(row > 0 && board.floodable(row - 1, col0)){
				int recRes = scanFloodFillOpponentHelper(board, row - 1, col0, playerRow, playerCol);
				if(recRes == -1) return -1;
				res += recRes;
			}
		}
		
		//scan below
		for(col0 = colMin; col0 <= colMax; col0++){
			if(row + 1 == playerRow && col0 == playerCol) return -1;
			if(row < board.rows - 1 && board.floodable(row + 1, col0)){
				int recRes = scanFloodFillOpponentHelper(board, row + 1, col0, playerRow, playerCol);
				if(recRes == -1) return -1;
				res += recRes;
			}
		}
		return res;
	}
	
	private int scanFloodFillOpponent(Board board){
		
		board = board.deepcopy();
		int res = 0;
		Location playerLocation = board.getPlayerLocation();
		Location opponentLocation = board.getOpponentLocation();
		int playerRow = playerLocation.row;
		int playerCol = playerLocation.col;
		for(Move move : Move.ALLMOVES){
			int newRow = opponentLocation.row + move.drow;
			int newCol = opponentLocation.col + move.dcol;
			if(newRow == playerRow && newCol == playerCol) return -1;
			if(board.floodable(newRow, newCol)){
				int recRes = scanFloodFillOpponentHelper(board, newRow, newCol, playerRow, playerCol);
				if(recRes == -1){
					return -1;
				}
				res += recRes;
			}
		}
		return res;
	}
	
	public int getMaxValue(){
		return 256;
	}
	
	public int getPartitionedOffset(){
		return 512;
	}
	
	public int evaluate(Board board){
		int res = floodFillPlayer(board);
		if(res == -1) return 0;
		return res - floodFillOpponent(board);
	}
}
