package Bot;

public interface Board {
	public int rows = 16;
	public int cols = 16;
	public boolean isLegalMoveForPlayer(Move move);
	public boolean isLegalMoveForOpponent(Move move);
	public Move[] getLegalMovesForPlayer();
	public Move[] getLegalMovesForOpponent();
	public void makePlayerMove(Move move);
	public void undoPlayerMove(Move move);
	public void makeOpponentMove(Move move);
	public void undoOpponentMove(Move move);
	public Location getPlayerLocation();
	public Location getOpponentLocation();
	public boolean floodable(int row, int col);
	public void placeFlood(int row, int col);
	public boolean isFlooded(int row, int col);
	public int isAvailable(int row, int col);
	public Board deepcopy();
	public String toString();
	public Board toOpponentBoard();
	public Cell[] getCellBoard();
}
