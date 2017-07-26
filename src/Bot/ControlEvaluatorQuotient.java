package Bot;

public class ControlEvaluatorQuotient implements Evaluator {
	
	private int queueFloodFill(Board board){
		board = board.deepcopy();
		
		int queueLength = Board.cols * Board.rows;
		
		int playerControl = 1;
		Location playerLocation = board.getPlayerLocation();
		int[] playerRowQueue = new int[queueLength];
		int[] playerColQueue = new int[queueLength];
		int playerQueueStart = 0;
		playerRowQueue[playerQueueStart] = playerLocation.row;
		playerColQueue[playerQueueStart] = playerLocation.col;
		int playerQueueEnd = 1;
		
		int opponentControl = 1;
		Location opponentLocation = board.getOpponentLocation();
		int[] opponentRowQueue = new int[queueLength];
		int[] opponentColQueue = new int[queueLength];
		int opponentQueueStart = 0;
		opponentRowQueue[opponentQueueStart] = opponentLocation.row;
		opponentColQueue[opponentQueueStart] = opponentLocation.col;
		int opponentQueueEnd = 1;
		
		while(playerQueueStart < playerQueueEnd || opponentQueueStart < opponentQueueEnd){
			int tempQueueEnd = playerQueueEnd;
			while(playerQueueStart < playerQueueEnd){
				int row = playerRowQueue[playerQueueStart];
				int col = playerColQueue[playerQueueStart];
				for(Move move : Move.ALLMOVES){
					int newRow = row + move.drow;
					int newCol = col + move.dcol;
					if(board.floodable(newRow,  newCol)){
						board.placeFlood(newRow, newCol);
						playerRowQueue[tempQueueEnd] = newRow;
						playerColQueue[tempQueueEnd] = newCol;
						playerControl++;
						tempQueueEnd++;
					}
				}
				playerQueueStart++;
			}
			playerQueueEnd = tempQueueEnd;
			
			tempQueueEnd = opponentQueueEnd;
			while(opponentQueueStart < opponentQueueEnd){
				int row = opponentRowQueue[opponentQueueStart];
				int col = opponentColQueue[opponentQueueStart];
				for(Move move : Move.ALLMOVES){
					int newRow = row + move.drow;
					int newCol = col + move.dcol;
					if(board.floodable(newRow,  newCol)){
						board.placeFlood(newRow, newCol);
						opponentRowQueue[tempQueueEnd] = newRow;
						opponentColQueue[tempQueueEnd] = newCol;
						opponentControl++;
						tempQueueEnd++;
					}
				}
				opponentQueueStart++;
			}
			opponentQueueEnd = tempQueueEnd;
		}
		
		return opponentControl == 0 ? Integer.MAX_VALUE : (playerControl << 10) / opponentControl;
	}
	
	public int evaluate(Board board){
		return queueFloodFill(board);
	}
}
