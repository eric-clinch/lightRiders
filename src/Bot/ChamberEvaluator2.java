package Bot;

import java.util.ArrayList;

public class ChamberEvaluator2 implements Evaluator {
	
	private static int cols = Board.cols;
	
	public int getMaxValue(){
		return 256;
	}
	
	public int getPartitionedOffset(){
		return 512;
	}
	
	public static String getCellSizeStr(Cell cell){
//		String res = String.valueOf(cell.chamberSize);
//		while(res.length() < 3) res = "_" + res;
//		return res;
		return "__1";
	}
	
	public static String CellToStr(Cell cell){
		String startLetter;
		if(cell.cellType == CellType.PLAYER) startLetter = "P";
		else if(cell.cellType == CellType.OPPONENT) startLetter = "O";
		else if(cell.isArticulationPoint) startLetter = "A";
		else if(cell.cellType == CellType.UNCONTROLLED) startLetter = ",";
		else if(cell.cellType == CellType.WALL) return " XXXXXX";
		else if(cell.cellType == CellType.PLAYER_CONTROLLED) startLetter = "p";
		else if(cell.cellType == CellType.OPPONENT_CONTROLLED) startLetter = "o";
		else startLetter = "ERR";
		String endLetter = cell.battlefront ? "B" : " ";
		String endLetter2 = cell.visited ? "V" : " ";
		return " " + startLetter + getCellSizeStr(cell) + endLetter + endLetter2;
	}
	
	public static void printCellBoard(Cell[][] cellBoard){
		for(Cell[] row : cellBoard){
			for(Cell c : row){
				String cellStr = CellToStr(c);
//				if(cellStr.startsWith(" A") || cellStr.startsWith(" P") || cellStr.startsWith(" O")) System.err.print(cellStr + "   ");
				System.out.print(cellStr + "   ");
			}
			System.out.println();
		}
		System.out.println("\n\n");
	}
		
	public class CellsAvailable {
		public ArrayList<Cell> expandingCells;
		public ArrayList<Move> articulationMoves;
		public ArrayList<Move> controlledMoves;
		public boolean battleFront;
		
		public CellsAvailable(Cell[] board, int row, int col, CellType playerCellType, CellType opponentCellType, CellType opponentControlledCellType) {
			// initializing arrays with length 3 because there are generally at most 3 legal moves
			expandingCells = new ArrayList<Cell>(3);
			articulationMoves = new ArrayList<Move>(3);
			controlledMoves = new ArrayList<Move>(3);
			battleFront = false;
			for(Move move : Move.ALLMOVES) {
				int newRow = row + move.drow;
				if(newRow < 0 || newRow >= Board.rows) continue;
				int newCol = col + move.dcol;
				if(newCol < 0 || newCol >= Board.cols) continue;
				Cell newCell = board[newRow * cols + newCol]; //board access
				if(newCell.cellType == CellType.WALL) continue;
				else if(newCell.cellType == opponentCellType){
					battleFront = true;
				}
				else if(newCell.cellType == opponentControlledCellType) {
					battleFront = true;
					articulationMoves.add(move);
				}
				else if(newCell.cellType == playerCellType) articulationMoves.add(move);
				else{
					articulationMoves.add(move);
					controlledMoves.add(move);
					if(newCell.cellType == CellType.UNCONTROLLED) expandingCells.add(newCell);
				}
			}
		}
	}
	
	private static final int countHelper(Cell cell, Cell[] board, ArrayList<Cell> articulationPoints) {
		cell.visited = true;
		ArrayList<Move> searchMoves = cell.searchMoves;
		int searchMovesSize = searchMoves.size();
		int result = 256 + searchMovesSize;
		for(int i = 0; i < searchMovesSize; i++) {
			Move move = searchMoves.get(i);
			Cell newCell = board[(cell.row + move.drow) * cols + cell.col + move.dcol]; //board access
			if(!newCell.visited){
				if(newCell.isArticulationPoint) articulationPoints.add(newCell);
				else {
					result += countHelper(newCell, board, articulationPoints);
					cell.battlefront = cell.battlefront || newCell.battlefront;
				}
			}
		}
		return result;
	}
	
	public static final int count(Cell cell, Cell[] board, ArrayList<Cell> articulationPoints) {
		cell.visited = true;
		ArrayList<Move> searchMoves = cell.searchMoves;
		int searchMovesSize = searchMoves.size();
		int result = 256 + searchMovesSize;
		for(int i = 0; i < searchMovesSize; i++) {
			Move move = searchMoves.get(i);
			Cell newCell = board[(cell.row + move.drow) * cols + cell.col + move.dcol];
			if(!newCell.visited){
				if(newCell.isArticulationPoint) articulationPoints.add(newCell);
				else{
					result += countHelper(newCell, board, articulationPoints);
					cell.battlefront = cell.battlefront || newCell.battlefront;
				}
			}
		}
		
		int maxSize = result;
		boolean maxSizeIsBattlefront = cell.battlefront;
		int numArticulationPoints = articulationPoints.size();
		for(int i = 0; i < numArticulationPoints; i++) {
			Cell articulationCell = articulationPoints.get(i);
			if(articulationCell.visited) continue;
			int chamberSize = count(articulationCell, board, new ArrayList<Cell>());
			int size;
			if(articulationCell.battlefront) {
				size = chamberSize + articulationCell.layer - cell.layer; // articulationCell.layer - cell.layer is the distance from the starting cell to the exit of this chamber
				if(size > maxSize){
					maxSize = size;
					maxSizeIsBattlefront = true;
				}
			}
			else{
				size = chamberSize + result;
				if(size > maxSize){
					maxSize = size;
					maxSizeIsBattlefront = cell.battlefront;
				}
			}
		}
		
		cell.battlefront = maxSizeIsBattlefront;
		return maxSize;
	}
	
	private static final boolean isArticulationPoint(ArrayList<Move> legalMoves) {
		if(legalMoves.size() != 2) return false;
		Move move0 = legalMoves.get(0);
		Move move1 = legalMoves.get(1);
		return move0.drow == move1.drow || move0.dcol == move1.dcol;
	}
		
	public final int evaluate(Board board) {
		Cell[] cellBoard = board.getCellBoard();
		Cell[] cellQueue = new Cell[board.rows * board.cols];
		
		Location playerLocation = board.getPlayerLocation();
		int playerRow = playerLocation.row;
		int playerCol = playerLocation.col;
		Cell playerCell = cellBoard[playerRow * cols + playerCol];
		playerCell.cellType = CellType.PLAYER;
		int playerQueueStart = 0;
		int playerQueueEnd = 1;
		cellQueue[playerQueueStart] = playerCell;
		
		Location opponentLocation = board.getOpponentLocation();
		int opponentRow = opponentLocation.row;
		int opponentCol = opponentLocation.col;
		Cell opponentCell = cellBoard[opponentRow * cols + opponentCol];
		opponentCell.cellType = CellType.OPPONENT;
		int opponentQueueStart = 1;
		int opponentQueueEnd = 2;
		cellQueue[opponentQueueStart] = opponentCell;
		
		int queueEnd = 2;
		
		int layer = 0;
		while(playerQueueStart < playerQueueEnd || opponentQueueStart < opponentQueueEnd){
			while(playerQueueStart < playerQueueEnd){
				Cell cell = cellQueue[playerQueueStart];
				cell.layer = layer;
				CellsAvailable cellsAvailable = new CellsAvailable(cellBoard, cell.row, cell.col, CellType.PLAYER, CellType.OPPONENT, CellType.OPPONENT_CONTROLLED);
				cell.battlefront = cellsAvailable.battleFront;
				cell.searchMoves = cellsAvailable.controlledMoves;
				cell.isArticulationPoint = isArticulationPoint(cellsAvailable.articulationMoves);
				ArrayList<Cell> expandingCells = cellsAvailable.expandingCells;
				int expandingCellsSize = expandingCells.size();
				for(int i = 0; i < expandingCellsSize; i++){
					Cell expandingCell = expandingCells.get(i);
					expandingCell.cellType = CellType.PLAYER_CONTROLLED;
					cellQueue[queueEnd] = expandingCell;
					queueEnd++;
				}
				playerQueueStart++;
			}
			playerQueueStart = opponentQueueEnd;
			playerQueueEnd = queueEnd;
			
			while(opponentQueueStart < opponentQueueEnd){
				Cell cell = cellQueue[opponentQueueStart];
				cell.layer = layer;
				CellsAvailable cellsAvailable = new CellsAvailable(cellBoard, cell.row, cell.col, CellType.OPPONENT, CellType.PLAYER, CellType.PLAYER_CONTROLLED);
				cell.battlefront = cellsAvailable.battleFront;
				cell.searchMoves = cellsAvailable.controlledMoves;
				cell.isArticulationPoint = isArticulationPoint(cellsAvailable.articulationMoves);
				ArrayList<Cell> expandingCells = cellsAvailable.expandingCells;
				int expandingCellsSize = expandingCells.size();
				for(int i = 0; i < expandingCellsSize; i++){
					Cell expandingCell = expandingCells.get(i);
					expandingCell.cellType = CellType.OPPONENT_CONTROLLED;
					cellQueue[queueEnd] = expandingCell;
					queueEnd++;
				}
				opponentQueueStart++;
			}
			opponentQueueStart = playerQueueEnd;
			opponentQueueEnd = queueEnd;
			
			layer++;
		}
		
		int res = count(playerCell, cellBoard, new ArrayList<Cell>()) - count(opponentCell, cellBoard, new ArrayList<Cell>());
//		printCellBoard(cellBoard);
		return res;
	}
	
	public static void test(String[] args){
//		String strBoard = ".,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,x,x,x,x,.,.,.,.,.,.,.,.,.,.,.,.,x,x,x,x,x,x,x,x,x,x,x,x,.,.,.,.,x,x,x,x,x,x,x,x,x,x,x,x,x,.,.,.,x,x,x,x,x,x,x,.,x,x,x,x,x,.,.,.,x,x,x,x,x,x,x,x,x,x,x,x,x,.,.,.,x,x,x,x,x,x,.,x,.,.,x,x,x,.,.,.,x,x,x,x,x,x,.,x,.,.,x,x,x,.,.,.,x,x,x,x,.,.,.,x,.,.,x,x,x,x,.,0,x,x,x,x,.,.,.,x,.,.,x,x,x,x,.,.,.,x,x,x,.,.,.,x,.,.,x,x,x,x,.,.,.,x,x,x,.,.,.,x,x,x,x,x,x,x,.,.,.,x,x,x,x,x,x,x,x,x,x,x,x,x,.,.,.,x,x,x,x,x,x,x,x,x,x,x,x,x,.,.,.,.,.,.,.,.,.,.,.,1,x,x,x,x,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.";
//		String strBoard = ".,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,x,x,x,.,.,.,.,.,.,.,.,.,.,.,.,.,x,x,x,x,x,x,x,.,.,.,.,.,.,.,x,x,x,x,.,x,.,.,x,.,.,.,.,.,.,.,x,x,x,x,.,x,.,.,x,.,.,.,.,.,.,.,x,x,.,.,.,x,.,.,x,.,.,.,.,.,.,.,x,x,.,.,.,x,.,.,x,.,.,.,.,.,.,.,x,x,.,.,.,x,.,.,x,x,.,.,.,.,.,.,x,x,.,.,.,x,x,x,x,x,.,.,.,.,.,.,x,x,0,.,.,1,x,x,x,x,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.";
//		String strBoard = "x,x,x,x,x,x,x,x,.,.,.,.,.,.,x,x,x,x,x,.,x,x,x,x,0,x,x,x,x,x,x,1,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,.,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,.,x,x,x,x,x,x,x,x,x,x,x,x,x,x,x,.,x,x,x,x,x,x,x,.,.,x,x,x,x,x,x,.,.,x,x,x,x,x,x,.,.,x,x,x,x,x,x,.,.,x,x,x,x,x,x,.,.,x,x,x,x,x,x,.,.,.,x,x,x,x,x,.,.,x,x,x,x,x,x,.,.,.,.,.,.,x,x,x,.,x,x,x,x,.,.,.,.,.,.,.,.,x,x,x,x,x,x,x,x,.,.,.,.,.,.,.,.,.,x,x,x,x,x,x,x,.,.,.,.,.,.,.,.,.,x,x,.,x,x,x,x,x,x,.,.,.,.,.,.,.,.,.,.,.,.,x,x,x,x,.,.,.,.,.,.,.,.,.,.,.,.,.,.,x,x,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.";
		String strBoard = ".,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,x,x,x,x,x,0,.,.,.,.,.,.,.,.,.,.,x,x,x,x,x,x,x,x,x,x,.,.,.,.,.,.,x,x,x,x,x,x,x,.,x,x,x,x,.,.,.,.,x,x,x,x,x,x,x,x,x,.,x,x,.,.,.,.,x,x,x,x,x,x,x,x,.,x,x,x,.,.,.,.,x,x,x,x,x,x,x,x,x,x,x,x,.,.,.,.,x,x,x,x,.,x,x,x,x,.,x,x,.,.,.,.,x,x,x,.,x,x,x,x,x,x,x,x,.,.,.,.,x,x,x,.,x,x,x,x,x,x,x,x,.,.,.,.,.,.,.,.,.,x,x,x,x,x,x,x,.,.,.,.,x,x,x,x,x,x,x,x,x,x,x,x,.,.,.,.,x,x,x,x,x,x,x,x,x,1,.,.,.,.,.,.,x,x,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.,.";
		Board board = (Board) new PieceBoard(strBoard, 16, 16, "0", "1");
		System.out.println(board.toString());
		ChamberEvaluator evaluator = new ChamberEvaluator();
		evaluator.evaluate(board);
	}
}
