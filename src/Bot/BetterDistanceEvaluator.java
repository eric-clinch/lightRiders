package Bot;

public class BetterDistanceEvaluator implements Evaluator {
	
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
	
	private distancesAndConnections queueFloodFill(Board board, int startRow, int startCol){
		board = board.deepcopy();
		
		int distanceSum = 0;
		int connections = 0;
		
		int queueLength = Board.cols * Board.rows;
		int[] rowQueue = new int[queueLength];
		int[] colQueue = new int[queueLength];
		int[] pathQueue = new int[queueLength];
		int queueStart = 0;
		
		rowQueue[queueStart] = startRow;
		colQueue[queueStart] = startCol;
		pathQueue[queueStart] = 0;
		int queueEnd = 1;
		
		while(queueStart < queueEnd){
			int row = rowQueue[queueStart];
			int col = colQueue[queueStart];
			int pathLength = pathQueue[queueStart];
			for(Move move : Move.ALLMOVES){
				int newRow = row + move.drow;
				int newCol = col + move.dcol;
				if(board.floodable(newRow, newCol)){
					board.placeFlood(newRow, newCol);
					int newPathLength = pathLength + 1;
					rowQueue[queueEnd] = newRow;
					colQueue[queueEnd] = newCol;
					pathQueue[queueEnd] = newPathLength;
					distanceSum += newPathLength * newPathLength;
					connections++;
					queueEnd++;
				}
			}
			queueStart++;
		}
		
		return new distancesAndConnections(distanceSum, connections);
	}
	
	public int evaluate(Board board){
		int res = 0;
		int coefficient = Board.rows * Board.cols;
		coefficient *= coefficient;
		
		Location playerLocation = board.getPlayerLocation();
		Location opponentLocation = board.getOpponentLocation();
		
		distancesAndConnections playerDistancesAndConnections = queueFloodFill(board, playerLocation.row, playerLocation.col);
		distancesAndConnections opponentDistancesAndConnections = queueFloodFill(board, opponentLocation.row, opponentLocation.col);

		res += coefficient * (playerDistancesAndConnections.connections - opponentDistancesAndConnections.connections);
		res += opponentDistancesAndConnections.distanceSum - playerDistancesAndConnections.distanceSum;
		return res;
	}
}
