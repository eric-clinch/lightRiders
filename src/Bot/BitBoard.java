package Bot;

import java.util.ArrayList;

public class BitBoard implements Board {
	int playerRow, playerCol, opponentRow, opponentCol;
	long rows0To3, rows4To7, rows8To11, rows12To15, floodRows0To3, floodRows4To7, floodRows8To11, floodRows12To15;
	
	public BitBoard(String strBoard, int rows, int cols, String player, String opponent) {
		String[] splitted = strBoard.split(",");
		assert(splitted.length == rows * cols);
		
		this.playerRow = -1;
		this.playerCol = -1;
		this.opponentRow = -1;
		this.opponentCol = -1;
		
		for(int i = 0; i < 64; i++){
			String s = splitted[i];
			if(!s.equals(".")) rows0To3 |= (1L << i);
			if(s.equals(player)){
				assert(this.playerRow == -1 && this.playerCol == -1);
				this.playerRow = i / 16;
				this.playerCol = i % 16;
			} 
			if(s.equals(opponent)){
				assert(this.opponentRow == -1 && this.opponentCol == -1);
				this.opponentRow = i / 16;
				this.opponentCol = i % 16;
			}
		}
		for(int i = 64; i < 128; i++){
			String s = splitted[i];
			if(!s.equals(".")) rows4To7 |= (1L << i - 64);
			if(s.equals(player)){
				assert(this.playerRow == -1 && this.playerCol == -1);
				this.playerRow = i / 16;
				this.playerCol = i % 16;
			} 
			if(s.equals(opponent)){
				assert(this.opponentRow == -1 && this.opponentCol == -1);
				this.opponentRow = i / 16;
				this.opponentCol = i % 16;
			}
		}
		for(int i = 128; i < 192; i++){
			String s = splitted[i];
			if(!s.equals(".")) rows8To11 |= (1L << i - 128);
			if(s.equals(player)){
				assert(this.playerRow == -1 && this.playerCol == -1);
				this.playerRow = i / 16;
				this.playerCol = i % 16;
			} 
			if(s.equals(opponent)){
				assert(this.opponentRow == -1 && this.opponentCol == -1);
				this.opponentRow = i / 16;
				this.opponentCol = i % 16;
			}
		}
		for(int i = 192; i < 256; i++){
			String s = splitted[i];
			if(!s.equals(".")) rows12To15 |= (1L << i - 192);
			if(s.equals(player)){
				assert(this.playerRow == -1 && this.playerCol == -1);
				this.playerRow = i / 16;
				this.playerCol = i % 16;
			} 
			if(s.equals(opponent)){
				assert(this.opponentRow == -1 && this.opponentCol == -1);
				this.opponentRow = i / 16;
				this.opponentCol = i % 16;
			}
		}
	}
	
	public BitBoard(int rows, int cols, int playerRow, int playerCol, int opponentRow, int opponentCol){
		this.playerRow = playerRow;
		this.playerCol = playerCol;
		this.opponentRow = opponentRow;
		this.opponentCol = opponentCol;
		
		int playerSpot = 16 * playerRow + playerCol;
		if(playerSpot < 64) rows0To3 |= (1L << playerSpot);
		else if(playerSpot < 128) rows4To7 |= (1L << playerSpot - 64);
		else if(playerSpot < 192) rows8To11 |= (1L << playerSpot - 128);
		else rows12To15 |= (1L << playerSpot - 192);
		
		int opponentSpot = 16 * opponentRow + opponentCol;
		if(opponentSpot < 64) rows0To3 |= (1L << opponentSpot);
		else if(opponentSpot < 128) rows4To7 |= (1L << opponentSpot - 64);
		else if(opponentSpot < 192) rows8To11 |= (1L << opponentSpot - 128);
		else rows12To15 |= (1L << opponentSpot - 192);
	}
	
	private BitBoard(int playerRow, int playerCol, int opponentRow, int opponentCol, long rows0To3, long rows4To7, long rows8To11, long rows12To15, long floodRows0To3, long floodRows4To7, long floodRows8To11, long floodRows12To15){
		this.playerRow = playerRow;
		this.playerCol = playerCol;
		this.opponentRow = opponentRow;
		this.opponentCol = opponentCol;
		this.rows0To3 = rows0To3;
		this.rows4To7 = rows4To7;
		this.rows8To11 = rows8To11;
		this.rows12To15 = rows12To15;
		this.floodRows0To3 = floodRows0To3;
		this.floodRows4To7 = floodRows4To7;
		this.floodRows8To11 = floodRows8To11;
		this.floodRows12To15 = floodRows12To15;
	}
	
	public boolean isLegalMoveForPlayer(Move move){
		int newRow = playerRow + move.drow;
		int newCol = playerCol + move.dcol;
		if(newRow < 0 || newRow >= 16 || newCol < 0 || newCol >= 16) return false;
		int newSpot = newRow * 16 + newCol;
		if(newSpot < 64) return ((rows0To3 >> newSpot) & 1) == 0;
		else if(newSpot < 128) return ((rows4To7 >> newSpot - 64) & 1) == 0;
		else if(newSpot < 192) return ((rows8To11 >> newSpot - 128) & 1) == 0;
		else return ((rows12To15 >> newSpot - 192) & 1) == 0;
	}
	
	public boolean isLegalMoveForOpponent(Move move){
		int newRow = opponentRow + move.drow;
		int newCol = opponentCol + move.dcol;
		if(newRow < 0 || newRow >= 16 || newCol < 0 || newCol >= 16) return false;
		int newSpot = newRow * 16 + newCol;
		if(newSpot < 64) return ((rows0To3 >> newSpot) & 1) == 0;
		else if(newSpot < 128) return ((rows4To7 >> newSpot - 64) & 1) == 0;
		else if(newSpot < 192) return ((rows8To11 >> newSpot - 128) & 1) == 0;
		else return ((rows12To15 >> newSpot - 192) & 1) == 0;
	}
	
	public Move[] getLegalMovesForPlayer(){
		ArrayList<Move> legalMoves = new ArrayList<>(4);
		for(Move m : Move.ALLMOVES){
			if(isLegalMoveForPlayer(m)) legalMoves.add(m);
		}
		Move[] res = new Move[legalMoves.size()];
		return legalMoves.toArray(res);
	}
	
	public Move[] getLegalMovesForOpponent(){
		ArrayList<Move> legalMoves = new ArrayList<>(4);
		for(Move m : Move.ALLMOVES){
			if(isLegalMoveForOpponent(m)) legalMoves.add(m);
		}
		Move[] res = new Move[legalMoves.size()];
		return legalMoves.toArray(res);
	}
	
	public void makePlayerMove(Move move){
		playerRow += move.drow;
		playerCol += move.dcol;
		int spot = playerRow * 16 + playerCol;
		if(spot < 64) rows0To3 |= 1L << spot;
		else if(spot < 128) rows4To7 |= 1L << spot - 64;
		else if(spot < 192) rows8To11 |= 1L << spot - 128;
		else rows12To15 |= 1L << spot - 192;
	}
	
	public void undoPlayerMove(Move move){
		int spot = playerRow * 16 + playerCol;
		if(spot < 64) rows0To3 &= ~(1L << spot);
		else if(spot < 128) rows4To7 &= ~(1L << spot - 64);
		else if(spot < 192) rows8To11 &= ~(1L << spot - 128);
		else rows12To15 &= ~(1L << spot - 192);
		playerRow -= move.drow;
		playerCol -= move.dcol;
	}
	
	public void makeOpponentMove(Move move){
		opponentRow += move.drow;
		opponentCol += move.dcol;
		int spot = opponentRow * 16 + opponentCol;
		if(spot < 64) rows0To3 |= 1L << spot;
		else if(spot < 128) rows4To7 |= 1L << spot - 64;
		else if(spot < 192) rows8To11 |= 1L << spot - 128;
		else rows12To15 |= 1L << spot - 192;
	}
	
	public void undoOpponentMove(Move move){
		int spot = opponentRow * 16 + opponentCol;
		if(spot < 64) rows0To3 &= ~(1L << spot);
		else if(spot < 128) rows4To7 &= ~(1L << spot - 64);
		else if(spot < 192) rows8To11 &= ~(1L << spot - 128);
		else rows12To15 &= ~(1L << spot - 192);
		opponentRow -= move.drow;
		opponentCol -= move.dcol;
	}
	
	public Location getPlayerLocation(){
		return new Location(playerRow, playerCol);
	}
	
	public Location getOpponentLocation(){
		return new Location(opponentRow, opponentCol);
	}
	
	public boolean floodable(int row, int col){
		int spot = row * 16 + col;
		if(row < 0 || row >= 16 || col < 0 || col >= 16) return false;
		if(spot < 64) return ((floodRows0To3 >> spot) & 1) == 0 && ((rows0To3 >> spot) & 1) == 0;
		else if(spot < 128) return ((floodRows4To7 >> spot - 64) & 1) == 0 && ((rows4To7 >> spot - 64) & 1) == 0;
		else if(spot < 192) return ((floodRows8To11 >> spot - 128) & 1) == 0 && ((rows8To11 >> spot - 128) & 1) == 0;
		else return ((floodRows12To15 >> spot - 192) & 1) == 0 && ((rows12To15 >> spot - 192) & 1) == 0;
	}
	
	public void placeFlood(int row, int col){
		int spot = row * 16 + col;
		if(spot < 64) floodRows0To3 |= 1L << spot;
		else if(spot < 128) floodRows4To7 |= 1L << spot - 64;
		else if(spot < 192) floodRows8To11 |= 1L << spot - 128;
		else floodRows12To15 |= 1L << spot - 192;
	}
	
	public boolean isFlooded(int row, int col){
		int spot = row * 16 + col;
		if(spot < 64) return ((floodRows0To3 >> spot) & 1) == 1;
		else if(spot < 128) return ((floodRows4To7 >> spot - 64) & 1) == 1;
		else if(spot < 192) return ((floodRows8To11 >> spot - 128) & 1) == 1;
		else return ((floodRows12To15 >> spot - 192) & 1) == 1;
	}
	
	public BitBoard deepcopy(){
		return new BitBoard(playerRow, playerCol, opponentRow, opponentCol, rows0To3, rows4To7, rows8To11, rows12To15, floodRows0To3, floodRows4To7, floodRows8To11, floodRows12To15);
	}
	
	private String locationToString(int row, int col){
		if(row == playerRow && col == playerCol) return "0";
		if(row == opponentRow && col == opponentCol) return "1";
		int x = 16 * row + col;
		if(x < 64){
			return ((rows0To3 >> x) & 1L) == 1L ? "x" : ".";
		} else if(x < 128){
			return ((rows4To7 >> x - 64) & 1L) == 1L ? "x" : ".";
		} else if(x < 192){
			return ((rows8To11 >> x - 128) & 1L) == 1L ? "x" : ".";
		} else{
			return ((rows12To15 >> x - 192) & 1L) == 1L ? "x" : ".";
		}
	}
	
	public String toString(){
		String res = "";
		for(int i = 0; i < 16; i++){
			for(int j = 0; j < 16; j++){
				res = res + locationToString(i, j) + " ";
			}
			res = res + "\n";
		}
		return res;
	}
	
	public BitBoard toOpponentBoard(){
		return new BitBoard(opponentRow, opponentCol, playerRow, playerCol, rows0To3, rows4To7, rows8To11, rows12To15, floodRows0To3, floodRows4To7, floodRows8To11, floodRows12To15);
	}

	public Cell[] getCellBoard(){
		Cell[] res = new Cell[256];
		
		int spot = 0;
		for(int row = 0; row < 4; row++){
			for(int col = 0; col < 16; col++){
				if(((rows0To3 >> spot) & 1) == 1) res[spot] = new Cell(CellType.WALL, row, col);
				else res[spot] = new Cell(CellType.UNCONTROLLED, row, col);
				spot++;
			}
		}
		for(int row = 4; row < 8; row++){
			for(int col = 0; col < 16; col++){
				if(((rows4To7 >> spot - 64) & 1) == 1) res[spot] = new Cell(CellType.WALL, row, col);
				else res[spot] = new Cell(CellType.UNCONTROLLED, row, col);
				spot++;
			}
		}
		for(int row = 8; row < 12; row++){
			for(int col = 0; col < 16; col++){
				if(((rows8To11 >> spot - 128) & 1) == 1) res[spot] = new Cell(CellType.WALL, row, col);
				else res[spot] = new Cell(CellType.UNCONTROLLED, row, col);
				spot++;
			}
		}
		for(int row = 12; row < 16; row++){
			for(int col = 0; col < 16; col++){
				if(((rows12To15 >> spot - 192) & 1) == 1) res[spot] = new Cell(CellType.WALL, row, col);
				else res[spot] = new Cell(CellType.UNCONTROLLED, row, col);
				spot++;
			}
		}
		
		return res;
	}
}
