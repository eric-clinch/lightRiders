package Bot;

public class Location {
	public int row;
	public int col;
	
	public Location(int row, int col){
		this.row = row;
		this.col = col;
	}
	
	public boolean equals(Location other){
		return other.row == row && other.col == col;
	}
	
	public int distance(Location other){
		return Math.abs(this.row - other.row) + Math.abs(this.col - other.col);
	}
}
