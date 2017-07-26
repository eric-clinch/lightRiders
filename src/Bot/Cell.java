package Bot;

import java.util.ArrayList;

public class Cell {
	public CellType cellType;
	public int row;
	public int col;
	public boolean isArticulationPoint;
	public boolean battlefront;
	public int layer;
	public ArrayList<Move> searchMoves;
	public boolean visited;
	
	public Cell(CellType cellType, int row, int col){
		this.cellType = cellType;
		this.row = row;
		this.col = col;
		this.isArticulationPoint = false;
		this.visited = false;
	}
}
