package Bot;

public class Nash {
	public static class NashResult{
		public float nashValue;
		public float[] mixedStrategy;
	}
	
	private float[][] normalForm; //the game normal form matrix;
	private float[][] tableu; //the tableux form of the game, to be used for the simplex algorithm
	private int rows;
	private int cols;
	private int tableuRows;
	private int tableuCols;
	
	public Nash(float[][] normalForm){
		this.normalForm = normalForm;
		this.rows = normalForm.length;
		this.cols = normalForm[0].length;
		getTableu();
	}
	
	private void printTableu(){
		for(float[] row : tableu){
			for(float f : row){
				System.out.print(f + "  ");
			}
			System.out.println();
		}
	}
	
	private void getTableu(){
		tableuRows = cols + 2;
		tableuCols = rows + 2;
		tableu = new float[tableuRows][tableuCols];
		
		//fill the tableu with the negative tranpose of the normal form game
		for(int row = 0; row < cols; row++){
			for(int col = 0; col < rows; col++){
				tableu[row][col] = -normalForm[col][row];
			}
		}
		
		for(int row = 0; row < cols; row++) tableu[row][rows] = 1;
		for(int col = 0; col < rows; col++) tableu[cols][col] = 1;
		tableu[cols][rows] = 0;
		for(int row = 0; row < cols; row++) tableu[row][tableuCols - 1] = 0;
		for(int col = 0; col < rows; col++) tableu[tableuRows -1][col] = 0;
		tableu[tableuRows - 1][rows] = -1;
		tableu[cols][tableuCols - 1] = 1;
		tableu[tableuRows - 1][tableuCols - 1] = 0;
	}
	
	public NashResult getNashEquilibrium(Simplex simplex){
		NashResult res = new NashResult();
		
		Simplex.LinearProgramResult nashRes = simplex.solveNash(tableu);
		res.mixedStrategy = nashRes.boundedResult;
		res.nashValue = nashRes.boundValue;
		
		return res;
	}
	
	public static void test(String[] args){
		float[][] game = {
			      {2.0f, -70.0f, -16.0f},
			      {72.0f, 10.0f, 41.9999770f},
			      {28.0f, -22.0f, 9.999996f}
			      };
		Nash nash = new Nash(game);
		NashResult res = nash.getNashEquilibrium(new Simplex());
		System.out.println();
		for(float f : res.mixedStrategy) System.out.print(f + "  ");
		System.out.println();
		System.out.println(res.nashValue);
	}
}
