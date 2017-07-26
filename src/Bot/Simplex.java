package Bot;

public class Simplex {
	public static class LinearProgramResult {
		public static enum ResultType{
			BOUNDED, INFEASIBLE, UNBOUNDED, MAX_UNBOUNDED, MIN_UNBOUNDED;
		}
		
		public float[] boundedResult;
		public float boundValue;
		public ResultType resultType;
	}

	
	private enum VarType{
		SLACK, VARIABLE
	}
	
	private class Variable{
		private VarType varType;
		private int index;
		
		public Variable(VarType varType, int index){
			this.varType = varType;
			this.index = index;
		}
	}
	
	private static float EPSILON = .00001f;
	private static float NEGATIVE_EPSILON = -.00001f;
	private float[][] table;
	private int rows, cols;
	private Variable[] left, top;
	private int numResultVariables;
	
	public LinearProgramResult min(float[][] table){
		this.table = table;
		this.rows = table.length;
		assert(rows > 0);
		this.cols = table[0].length;
		assert(cols > 0);
		this.left = new Variable[rows - 1];
		for(int i = 0; i < rows - 1; i++) left[i] = new Variable(VarType.VARIABLE, i);
		this.numResultVariables = rows - 1;
		this.top = new Variable[cols - 1];
		for(int i = 0; i < cols - 1; i++) top[i] = new Variable(VarType.SLACK, i);
		
		LinearProgramResult res = simplex();
		if(res.resultType == LinearProgramResult.ResultType.BOUNDED) getBoundedResultMin(res);
		else if(res.resultType == LinearProgramResult.ResultType.MIN_UNBOUNDED) res.resultType = LinearProgramResult.ResultType.UNBOUNDED;
		else if(res.resultType == LinearProgramResult.ResultType.MAX_UNBOUNDED) res.resultType = LinearProgramResult.ResultType.INFEASIBLE;
		return res;
	}
	
	public LinearProgramResult max(float[][] table){
		this.table = table;
		this.rows = table.length;
		assert(rows > 0);
		this.cols = table[0].length;
		assert(cols > 0);
		this.left = new Variable[rows - 1];
		for(int i = 0; i < rows - 1; i++) left[i] = new Variable(VarType.SLACK, i);
		this.top = new Variable[cols - 1];
		for(int i = 0; i < cols - 1; i++) top[i] = new Variable(VarType.VARIABLE, i);
		this.numResultVariables = cols - 1;
		
		LinearProgramResult res = simplex();
		if(res.resultType == LinearProgramResult.ResultType.BOUNDED) getBoundedResultMax(res);
		else if(res.resultType == LinearProgramResult.ResultType.MAX_UNBOUNDED) res.resultType = LinearProgramResult.ResultType.INFEASIBLE;
		else if(res.resultType == LinearProgramResult.ResultType.MIN_UNBOUNDED) res.resultType = LinearProgramResult.ResultType.UNBOUNDED;
		return res;
	}
	
	private void printBoard(){
		System.out.println();
		for(float[] row : table){
			for(float x : row){
				System.out.print(x);
				System.out.print("   ");
			}
			System.out.println();
		}
	}
	
	//implementation of the algorithm described at https://www.math.ucla.edu/~tom/LP.pdf
	private LinearProgramResult simplex(){
		LinearProgramResult res = new LinearProgramResult();
		
		boolean running = true;
		while(running){
//			printBoard();
			assert(rows == table.length && cols == table[0].length);
			Location pivotLocation = new Location(-1, -1);
			
			//case 1 for max simplex
			if(rightColumnIsNonnegative()){
//				System.out.println("rightColumnIsNonnegative");
				boolean solved = true;
				int bottomRow = rows - 1;
				for(int col = 0; col < cols - 1; col++){
					if(table[bottomRow][col] < NEGATIVE_EPSILON){
						solved = false;
						int pivotRow = getCase1MaxPivotRow(col);
						if(pivotRow >= 0){
							pivotLocation = new Location(pivotRow, col);
							break;
						}
					}
				}
				if(solved){
					res.resultType = LinearProgramResult.ResultType.BOUNDED;
					return res;
				}
				else if(pivotLocation.row < 0){
					//the maximum problem is unbounded feasible
					res.resultType = LinearProgramResult.ResultType.MAX_UNBOUNDED;
					return res;
				}
			}
			
			//case 1 for min simplex
			else if(bottomRowIsNonpositive()){
//				System.out.println("bottomRowIsNonpositive");
				int rightCol = cols - 1;
				for(int row = 0; row < rows - 1; row++){
					if(table[row][rightCol] < NEGATIVE_EPSILON){
						int pivotCol = getCase1MinPivotCol(row);
						if(pivotCol >= 0){
							pivotLocation = new Location(row, pivotCol);
							break;
						}
					}
				}
				if(pivotLocation.row < 0){
					//the minimum problem is unbounded feasible
					res.resultType = LinearProgramResult.ResultType.MIN_UNBOUNDED;
					return res;
				}
			}
			
			//case 2
			else{
				pivotLocation = getCase2PivotLocation();
				if(pivotLocation.row < 0){
					//the problem is infeasible
					res.resultType = LinearProgramResult.ResultType.INFEASIBLE;
					return res;
				}
			}
			
//			System.out.println("pivot: " + pivotLocation.row + " " + pivotLocation.col);
			pivot(pivotLocation.row, pivotLocation.col);
		}
		
		return res;
	}
	
	private void getBoundedResultMax(LinearProgramResult res){
		float[] boundedResult = new float[numResultVariables];
		int leftLen = left.length;
		for(int i = 0; i < leftLen; i++){
			Variable var = left[i];
			if(var.varType == VarType.VARIABLE) boundedResult[var.index] = table[i][cols - 1];
		}
		int topLen = top.length;
		for(int i = 0; i < topLen; i++){
			Variable var = top [i];
			if(var.varType == VarType.VARIABLE) boundedResult[var.index] = 0;
		}
		res.boundedResult = boundedResult;
		res.boundValue = table[rows - 1][cols - 1];
	}
	
	private void getBoundedResultMin(LinearProgramResult res){
		float[] boundedResult = new float[numResultVariables];
		int topLen = top.length;
		for(int i = 0; i < topLen; i++){
			Variable var = top [i];
			if(var.varType == VarType.VARIABLE) boundedResult[var.index] = table[rows - 1][i];
		}
		int leftLen = left.length;
		for(int i = 0; i < leftLen; i++){
			Variable var = left[i];
			if(var.varType == VarType.VARIABLE) boundedResult[var.index] = 0;
		}
		res.boundedResult = boundedResult;
		res.boundValue = table[rows - 1][cols - 1];
	}
	
	private int getCase1MaxPivotRow(int col){
		float smallestRatio = Float.MAX_VALUE;
		int pivotRow = -1;
		for(int row = 0; row < rows - 1; row++){
			float a = table[row][col];
			if(a > EPSILON){
				float ratio = table[row][cols - 1] / a;
				if(ratio < smallestRatio){
					smallestRatio = ratio;
					pivotRow = row;
				}
			}
		}
		return pivotRow;
	}
	
	private int getCase1MinPivotCol(int row){
		float smallestRatio = Float.MAX_VALUE;
		int pivotCol = -1;
		for(int col = 0; col < cols - 1; col++){
			float a = table[row][col];
			if(a < NEGATIVE_EPSILON){
				float ratio = Math.abs(table[rows - 1][col] / a);
				if(ratio < smallestRatio){
					smallestRatio = ratio;
					pivotCol = col;
				}
			}
		}
		return pivotCol;
	}
	
	private Location getCase2PivotLocation(){
//		System.out.println();
//		for(float[] row : table){
//			for(float f : row) System.out.print(f + "   ");
//			System.out.println();
//		}
//		System.out.println();
		
		int k = -1;
		int lastCol = cols - 1;
		int lastRow = rows - 1;
		for(int row = 0; row < lastRow; row++){
			if(table[row][lastCol] < NEGATIVE_EPSILON){
				k = row;
				break;
			}
		}
		assert(k >= 0);
		
		int pivotCol = -1;
		for(int col = 0; col < lastCol; col++){
			if(table[k][col] < NEGATIVE_EPSILON){
				pivotCol = col;
				break;
			}
		}
		if(pivotCol < 0) return new Location(-1, -1); //no negative values, so no location to pivot on
		
		int pivotRow = k;
		float smallestRatio = table[pivotRow][lastCol] / table[pivotRow][pivotCol];
		for(int row = 0; row < lastRow; row++){
			float val = table[row][pivotCol];
			if(val > EPSILON){
				float ratio = table[row][lastCol] / val;
				if(ratio < smallestRatio){
					pivotRow = row;
					smallestRatio = ratio;
				}
			}
		}
		return new Location(pivotRow, pivotCol);
	}
	
	private boolean bottomRowIsNonpositive(){
		int row = rows - 1;
		for(int col = 0; col < cols - 1; col++){
			if(table[row][col] > EPSILON) return false;
		}
		return true;
	}
	
	private boolean rightColumnIsNonnegative(){
		int col = cols - 1;
		for(int row = 0; row < rows - 1; row++){
			if(table[row][col] < NEGATIVE_EPSILON) return false;
		}
		return true;
	}
	
    private void pivot(int pivotRow, int pivotCol){
    	Variable temp = left[pivotRow];
    	left[pivotRow] = top[pivotCol];
    	top[pivotCol] = temp;
    	
        float pivotValue = table[pivotRow][pivotCol];
        float inversed = 1 / pivotValue;
        table[pivotRow][pivotCol] = inversed;
        
        for(int col = 0; col < cols; col++){
        	if(col != pivotCol) table[pivotRow][col] = table[pivotRow][col] * inversed;
        }
        
        for(int row = 0; row < rows; row++){
        	if(row != pivotRow)	table[row][pivotCol] = - table[row][pivotCol] * inversed;
        }
        
        for(int row = 0; row < rows; row++){
        	if(row == pivotRow) continue;
        	for(int col = 0; col < cols; col++){
        		if(col != pivotCol){
        			table[row][col] = table[row][col] + table[pivotRow][col] * table[row][pivotCol] * pivotValue;
        		}
        	}
        }
        
    }
    
    //this method really shouldn't be part of the library, but is a hack
    //that I'm using so I don't have to implement the General Simplex algorithm
    public LinearProgramResult solveNash(float[][] table){ 	
    	this.table = table;
		this.rows = table.length;
		assert(rows > 0);
		this.cols = table[0].length;
		assert(cols > 0);
		this.left = new Variable[rows - 1];
		for(int i = 0; i < rows - 1; i++) left[i] = new Variable(VarType.SLACK, i);
		this.top = new Variable[cols - 1];
		for(int i = 0; i < cols - 1; i++) top[i] = new Variable(VarType.VARIABLE, i);
		this.numResultVariables = cols - 2;
		
//		printBoard();
		pivot(0, cols - 2);
		removeFirstRow();
//		printBoard();
		pivot(rows - 2, 0);
		removeFirstCol();
//		printBoard();
    	
		LinearProgramResult res = simplex();
		if(res.resultType == LinearProgramResult.ResultType.BOUNDED) getBoundedResultMax(res);
		return res;
    }
    
    public LinearProgramResult getAlpha(float[][] table){
    	this.table = table;
		this.rows = table.length;
		assert(rows > 0);
		this.cols = table[0].length;
		assert(cols > 0);
		this.left = new Variable[rows - 1];
		for(int i = 0; i < rows - 1; i++) left[i] = new Variable(VarType.SLACK, i);
		this.top = new Variable[cols - 1];
		for(int i = 0; i < cols - 1; i++) top[i] = new Variable(VarType.VARIABLE, i);
		this.numResultVariables = cols - 1;
		
//		printBoard();
		pivot(0, 0);
		removeFirstCol();
//		printBoard();
    	
		LinearProgramResult res = simplex();
		if(res.resultType == LinearProgramResult.ResultType.BOUNDED) getBoundedResultMax(res);
		return res;
    }
    
    public LinearProgramResult getBeta(float[][] table){
    	this.table = table;
		this.rows = table.length;
		assert(rows > 0);
		this.cols = table[0].length;
		assert(cols > 0);
		this.left = new Variable[rows - 1];
		for(int i = 0; i < rows - 1; i++) left[i] = new Variable(VarType.VARIABLE, i);
		this.top = new Variable[cols - 1];
		for(int i = 0; i < cols - 1; i++) top[i] = new Variable(VarType.SLACK, i);
		this.numResultVariables = cols - 1;
		
//		printBoard();
		pivot(0, 0);
		removeFirstRow();
//		printBoard();
    	
		LinearProgramResult res = simplex();
		if(res.resultType == LinearProgramResult.ResultType.BOUNDED) getBoundedResultMin(res);
		return res;
    }
    
    private void removeFirstRow(){
    	rows--;
    	Variable[] tempLeft = left;
    	left = new Variable[rows-1];
    	System.arraycopy(tempLeft, 1, left, 0, rows - 1);
    	float[][] tempTable = table;
    	table = new float[rows][cols];
    	for(int i = 0; i < rows; i++) System.arraycopy(tempTable[i+1], 0, table[i], 0, cols);
    }
    
    private void removeFirstCol(){
    	cols--;
    	Variable[] tempTop = top;
    	top = new Variable[cols-1];
    	System.arraycopy(tempTop, 1, top, 0, cols - 1);
    	float[][] tempTable = table;
    	table = new float[rows][cols];
    	for(int i = 0; i < rows; i++) System.arraycopy(tempTable[i], 1, table[i], 0, cols);
    }
    
    public static void test (String[] args){
    	Simplex simplex = new Simplex();
    	float[][] table = {
					      {1.0f, 1.0f, 1.0f, 1.0f},
					      {256.0f, 256.0f, 256.0f, -256.0f},
					      {256.0f, 256.0f, 256.0f, -256.0f},
					      {256.0f,  256.0f, 256.0f, 0.0f}
					      };
    	LinearProgramResult res = simplex.getAlpha(table);
//    	for(float f : res.boundedResult) System.out.print(f + " ");
    }
}
