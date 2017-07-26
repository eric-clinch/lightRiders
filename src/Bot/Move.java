package Bot;
public class Move {
	public static final Move UP = new Move(-1, 0);
	public static final Move DOWN = new Move(1, 0);
	public static final Move LEFT = new Move(0, -1);
	public static final Move RIGHT = new Move(0, 1);
	public static final Move[] ALLMOVES = {UP, DOWN, LEFT, RIGHT};
	
	public int drow;
	public int dcol;
	
	private Move(int drow, int dcol){
		this.drow = drow;
		this.dcol = dcol;
	}
}
