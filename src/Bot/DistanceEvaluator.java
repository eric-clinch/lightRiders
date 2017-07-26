package Bot;

public class DistanceEvaluator implements Evaluator {
	
	private class distancesAndConnections{
		public int distanceSum;
		public int connections;
		
		public distancesAndConnections(int distanceSum, int connections){
			this.distanceSum = distanceSum;
			this.connections = connections;
		}
		
		public void add(distancesAndConnections other){
			this.distanceSum += other.distanceSum;
			this.connections += other.connections;
		}
	}
	
	private distancesAndConnections scanFloodFillPlayerHelper(Board board, int row, int col){
		distancesAndConnections res = new distancesAndConnections(0, 0);
		assert(board.floodable(row,  col));
		Location playerLocation = board.getPlayerLocation();
		
		int col0 = col;
		int colMax = col;
		while(col0 < board.cols && board.floodable(row, col0)){
			board.placeFlood(row, col0);
			colMax = col0;
			res.connections++;
			res.distanceSum += Math.abs(playerLocation.row - row) + Math.abs(playerLocation.col - col0);
			col0++;
		}
		
		col0 = col - 1;
		int colMin = col;
		while(col0 >= 0 && board.floodable(row, col0)){
			board.placeFlood(row, col0);
			colMin = col0;
			res.connections++;
			res.distanceSum += Math.abs(playerLocation.row - row) + Math.abs(playerLocation.col - col0);
			col0--;
		}
		
		//scan above
		for(col0 = colMin; col0 <= colMax; col0++){
			if(row > 0 && board.floodable(row - 1, col0)) res.add(scanFloodFillPlayerHelper(board, row - 1, col0));
		}
		
		//scan below
		for(col0 = colMin; col0 <= colMax; col0++){
			if(row < board.rows - 1 && board.floodable(row + 1, col0)) res.add(scanFloodFillPlayerHelper(board, row + 1, col0));
		}
		
		return res;
	}
	
	private distancesAndConnections scanFloodFillPlayer(Board board){
		board = board.deepcopy();
		distancesAndConnections res = new distancesAndConnections(0, 0);
		Location playerLocation = board.getPlayerLocation();
		for(Move move : Move.ALLMOVES){
			if(board.floodable(playerLocation.row + move.drow, playerLocation.col + move.dcol)){
				res.add(scanFloodFillPlayerHelper(board, playerLocation.row + move.drow, playerLocation.col + move.dcol));
			}
		}		
		return res;
	}
	
	private distancesAndConnections scanFloodFillOpponentHelper(Board board, int row, int col){
		distancesAndConnections res = new distancesAndConnections(0, 0);
		assert(board.floodable(row,  col));
		Location opponentLocation = board.getOpponentLocation();
		
		int col0 = col;
		int colMax = col;
		while(col0 < board.cols && board.floodable(row, col0)){
			board.placeFlood(row, col0);
			colMax = col0;
			res.connections++;
			res.distanceSum += Math.abs(opponentLocation.row - row) + Math.abs(opponentLocation.col - col0);
			col0++;
		}
		
		col0 = col - 1;
		int colMin = col;
		while(col0 >= 0 && board.floodable(row, col0)){
			board.placeFlood(row, col0);
			colMin = col0;
			res.connections++;
			res.distanceSum += Math.abs(opponentLocation.row - row) + Math.abs(opponentLocation.col - col0);
			col0--;
		}
		
		//scan above
		for(col0 = colMin; col0 <= colMax; col0++){
			if(row > 0 && board.floodable(row - 1, col0)) res.add(scanFloodFillOpponentHelper(board, row - 1, col0));
		}
		
		//scan below
		for(col0 = colMin; col0 <= colMax; col0++){
			if(row < board.rows - 1 && board.floodable(row + 1, col0)) res.add(scanFloodFillOpponentHelper(board, row + 1, col0));
		}
		
		return res;
	}
	
	private distancesAndConnections scanFloodFillOpponent(Board board){		
		board = board.deepcopy();
		distancesAndConnections res = new distancesAndConnections(0, 0);
		Location opponentLocation = board.getOpponentLocation();
		for(Move move : Move.ALLMOVES){
			if(board.floodable(opponentLocation.row + move.drow, opponentLocation.col + move.dcol)){
				res.add(scanFloodFillOpponentHelper(board, opponentLocation.row + move.drow, opponentLocation.col + move.dcol));
			}
		}		
		return res;
	}
	
	public int evaluate(Board board){
		int res = 0;
		int coefficient = board.rows * board.cols;
		distancesAndConnections playerDistancesAndConnections = scanFloodFillPlayer(board);
		distancesAndConnections opponentDistancesAndConnections = scanFloodFillOpponent(board);
		res += coefficient * (playerDistancesAndConnections.connections - opponentDistancesAndConnections.connections);
		res += opponentDistancesAndConnections.distanceSum - playerDistancesAndConnections.distanceSum;
		return res;
	}
}
