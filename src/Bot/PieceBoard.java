package Bot;
import java.util.ArrayList;
import java.util.Arrays;

public class PieceBoard implements Board{
	private enum Piece {
		EMPTY, WALL, PLAYER, OPPONENT, FLOODED
	}
	
	private Piece[][] board;
	public int rows;
	public int cols;
	private int playerRow;
	private int playerCol;
	private int opponentRow;
	private int opponentCol;
	
	
	private Piece strToPiece(String s, String player, String opponent) {
		if(s.equals(player)) return Piece.PLAYER;
		else if(s.equals(opponent)) return Piece.OPPONENT;
		else if(s.equals("x")) return Piece.WALL;
		else if(s.equals(".")) return Piece.EMPTY;
		System.err.println("unable to parse character " + s);
		return Piece.EMPTY;
	}
	
	public PieceBoard(String strBoard, int rows, int cols, String player, String opponent) {
		this.rows = rows;
		this.cols = cols;
		
		String[] splitted = strBoard.split(",");
		assert(splitted.length == rows * cols);
		
		this.board = new Piece[rows][cols];
		this.playerRow = -1;
		this.playerCol = -1;
		this.opponentRow = -1;
		this.opponentCol = -1;
		for(int i = 0; i < rows; i++){
			for(int j = 0; j < cols; j++){
				int index = i * cols + j;
				String s = splitted[index];
				this.board[i][j] = strToPiece(s, player, opponent);
				if(s.equals(player)){
					assert(this.playerRow == -1 && this.playerCol == -1);
					this.playerRow = i;
					this.playerCol = j;
				}
				if(s.equals(opponent)){
					assert(this.opponentRow == -1 && this.opponentCol == -1);
					this.opponentRow = i;
					this.opponentCol = j;
				}
			}
		}
	}
	
	public PieceBoard(int rows, int cols, int playerRow, int playerCol, int opponentRow, int opponentCol){
		this.rows = rows;
		this.cols = cols;
		this.playerRow = playerRow;
		this.playerCol = playerCol;
		this.opponentRow = opponentRow;
		this.opponentCol = opponentCol;
		
		this.board = new Piece[rows][cols];
		for(int i = 0; i < rows; i++){
			for(int j = 0; j < cols; j++){
				this.board[i][j] = Piece.EMPTY;
			}
		}
		this.board[playerRow][playerCol] = Piece.PLAYER;
		this.board[opponentRow][opponentCol] = Piece.OPPONENT;
	}
	
	private PieceBoard(Piece[][] board, int playerRow, int playerCol, int opponentRow, int opponentCol){
		this.rows = board.length;
		this.cols = board[0].length;
		this.playerRow = playerRow;
		this.playerCol = playerCol;
		this.opponentRow = opponentRow;
		this.opponentCol = opponentCol;
		this.board = board;
		assert(board[playerRow][playerCol] == Piece.PLAYER);
		assert(board[opponentRow][opponentCol] == Piece.OPPONENT);
	}
	
	public boolean isLegalMoveForPlayer(Move move){
		int newRow = playerRow + move.drow;
		int newCol = playerCol + move.dcol;
		if(newRow < 0 || newRow >= rows || newCol < 0 || newCol >= cols || board[newRow][newCol] != Piece.EMPTY) return false;
		return true;
	}
	
	public boolean isLegalMoveForOpponent(Move move){
		int newRow = opponentRow + move.drow;
		int newCol = opponentCol + move.dcol;
		if(newRow < 0 || newRow >= rows || newCol < 0 || newCol >= cols || board[newRow][newCol] != Piece.EMPTY) return false;
		return true;
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
		int newRow = playerRow + move.drow;
		int newCol = playerCol + move.dcol;
		board[newRow][newCol] = Piece.PLAYER;
		board[playerRow][playerCol] = Piece.WALL;
		playerRow = newRow;
		playerCol = newCol;
	}
	
	public void undoPlayerMove(Move move){
		int oldRow = playerRow - move.drow;
		int oldCol = playerCol - move.dcol;
		board[oldRow][oldCol] = Piece.PLAYER;
		board[playerRow][playerCol] = Piece.EMPTY;
		playerRow = oldRow;
		playerCol = oldCol;
	}
	
	public void makeOpponentMove(Move move){
		int newRow = opponentRow + move.drow;
		int newCol = opponentCol + move.dcol;
		board[newRow][newCol] = Piece.OPPONENT;
		board[opponentRow][opponentCol] = Piece.WALL;
		opponentRow = newRow;
		opponentCol = newCol;
	}
	
	public void undoOpponentMove(Move move){
		int oldRow = opponentRow - move.drow;
		int oldCol = opponentCol - move.dcol;
		board[oldRow][oldCol] = Piece.OPPONENT;
		board[opponentRow][opponentCol] = Piece.EMPTY;
		opponentRow = oldRow;
		opponentCol = oldCol;
	}
	
	public Location getPlayerLocation(){
		return new Location(playerRow, playerCol);
	}
	
	public Location getOpponentLocation(){
		return new Location(opponentRow, opponentCol);
	}
	
	public void placeFlood(int row, int col){
		this.board[row][col] = Piece.FLOODED;
	}
	
	public boolean floodable(int row, int col){
		return (row >= 0 && row < rows &&
				col >= 0 && col < cols &&
				board[row][col] == Piece.EMPTY);
	}
	
	public boolean isFlooded(int row, int col){
		return board[row][col] == Piece.FLOODED;
	}
	
	public int isAvailable(int row, int col){
		return (row >= 0 && row < rows &&
				col >= 0 && col < cols &&
				board[row][col] == Piece.EMPTY) ? 1 : 0;
	}
	
	public PieceBoard deepcopy(){
		final Piece[][] result = new Piece[board.length][];
		for(int i = 0; i < board.length; i++){
			result[i] = Arrays.copyOf(board[i], board.length);
		}
		return new PieceBoard(result, playerRow, playerCol, opponentRow, opponentCol);
	}
	
	private char PieceToChar(Piece piece){
		if(piece == Piece.EMPTY) return '.';
		if(piece == Piece.FLOODED) return 'i';
		if(piece == Piece.OPPONENT) return '1';
		if(piece == Piece.PLAYER) return '0';
		assert(piece == Piece.WALL);
		return 'x';
	}
	
	public String toString(){
		String res = "";
		for(Piece[] row : this.board){
			for(Piece piece : row){
				res = res + PieceToChar(piece) + ' ';
			}
			res = res + '\n';
		}
		return res;
	}
	
	public PieceBoard toOpponentBoard(){
		PieceBoard copied = this.deepcopy();
		Piece[][] newBoard = copied.board;
		newBoard[playerRow][playerCol] = Piece.OPPONENT;
		newBoard[opponentRow][opponentCol] = Piece.PLAYER;
		
		return new PieceBoard(newBoard, opponentRow, opponentCol, playerRow, playerCol);
	}
	
	private CellType getCellType(int row, int col){
		Piece p = board[row][col];
		if(p == Piece.EMPTY) return CellType.UNCONTROLLED;
		else if(p == Piece.WALL) return CellType.WALL;
		else if(p == Piece.PLAYER) return CellType.PLAYER;
		else return CellType.OPPONENT;
	}
	
	public Cell[] getCellBoard(){
		Cell[] res = new Cell[rows * cols];
		for(int i = 0; i < rows; i++){
			int x = i * cols;
			for(int j = 0; j < cols; j++){
				res[x + j] = new Cell(getCellType(i, j), i, j);
			}
		}
		return res;
	}
}