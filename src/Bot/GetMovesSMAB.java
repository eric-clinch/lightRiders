package Bot;

import java.util.Random;
import java.util.Stack;

public class GetMovesSMAB implements GetMoves{
	
	private Evaluator evaluator;
	private GetSearchNumber getBoardsToEvaluate;
	private Simplex simplex;
	private Random random;
	private int TIE_VALUE = 0;
	private int MIN_VALUE = -256;
	private int MAX_VALUE = 256;
	
	public GetMovesSMAB(Evaluator evaluator, GetSearchNumber getBoardsToEvaluate){
		this.evaluator = evaluator;
		this.getBoardsToEvaluate = getBoardsToEvaluate;
		this.simplex = new Simplex();
		this.random = new Random();
	}
	
	private int floodFillHelper(Board board, Location location){
		int res = 0;
		for(Move move : Move.ALLMOVES){
			Location newLocation = new Location(location.row + move.drow, location.col + move.dcol);
			if(newLocation.equals(board.getOpponentLocation())) return -1;
			if(board.floodable(newLocation.row, newLocation.col)){
				board.placeFlood(newLocation.row, newLocation.col);
				res++;
				int recursedRes = floodFillHelper(board, newLocation);
				
				if(recursedRes == -1) return -1;
				else res += recursedRes;
			}
		}
		return res;
	}
	
	private int floodFill(Board board){
		board = board.deepcopy();
		return floodFillHelper(board, board.getPlayerLocation());
	}
	
	private int getBestMoveFillHelper(Board board, int recursions){
		if(recursions <= 0) return floodFill(board);
		else{
			int maxSpots = Integer.MIN_VALUE;
			for(Move move : Move.ALLMOVES){
				if(!board.isLegalMoveForPlayer(move)) continue;
				board.makePlayerMove(move);
				int spots = getBestMoveFillHelper(board, recursions - 1);
				board.undoPlayerMove(move);
				maxSpots = Math.max(maxSpots, spots);
			}
			return maxSpots;
		}
	}
	
	private Move getBestMoveFill(Board board, int recursions){
		int maxSpots = Integer.MIN_VALUE;
		Move bestMove = Move.UP; //default move
		for(Move move : Move.ALLMOVES){
			if(!board.isLegalMoveForPlayer(move)) continue;
			board.makePlayerMove(move);
			int spots = getBestMoveFillHelper(board, recursions - 1);
			board.undoPlayerMove(move);
			if(spots > maxSpots){
				maxSpots = spots;
				bestMove = move;
			}
		}
		return bestMove;
	}
	
	private Stack<Move> backtrackFill(Board board, int spotsToFill){
		if(spotsToFill <= 0){
			System.err.println("backtracked to max depth");
			Stack<Move> s = new Stack<>();
			s.addElement(Move.UP);
			return s;
		}
		int maxMoves = Integer.MIN_VALUE;
		Stack<Move> bestRes = null;
		for(Move move : Move.ALLMOVES){
			if(!board.isLegalMoveForPlayer(move)) continue;
			board.makePlayerMove(move);
			Stack<Move> res = backtrackFill(board, spotsToFill - 1);
			board.undoPlayerMove(move);
			if(res.size() == spotsToFill){
				res.addElement(move);
				return res;
			}
			else if (bestRes == null || maxMoves < res.size()){
				maxMoves = res.size();
				res.addElement(move);
				bestRes = res;
			}
		}
		if(bestRes != null) return bestRes;
		else{
			bestRes = new Stack<>();
			bestRes.addElement(Move.UP);
			return bestRes;
		}
	}
	
	private float getNewAlpha(float[][] P, float[][] O, float alpha, int a, int b, int m, int n){
//		System.out.println("O: ");
//		for(float[] row : O){
//			for(float f : row) System.out.print(f + "   ");
//			System.out.println();
//		}
//		System.out.println();
//		
//		System.out.println("P: ");
//		for(float[] row : P){
//			for(float f : row) System.out.print(f + "   ");
//			System.out.println();
//		}
//		System.out.println();
		
		assert(a < m && b < n);
		int tableuRows = n + 1;
		int tableuCols = m + 1;
		float[][] tableu = new float[tableuRows][tableuCols];
		float negAlpha = -alpha;
		
		for(int col = 0; col < tableuCols; col++) tableu[0][col] = 1;
		
		for(int tableuRow = 1; tableuRow <= b; tableuRow++){
			int PCol = tableuRow - 1;
			for(int tableuCol = 0; tableuCol < a; tableuCol++) tableu[tableuRow][tableuCol] = -P[tableuCol][PCol];			
			for(int tableuCol = a; tableuCol < m - 1; tableuCol++) tableu[tableuRow][tableuCol] = -P[tableuCol + 1][PCol];		
			tableu[tableuRow][m - 1] = negAlpha;
			tableu[tableuRow][m] = -O[a][PCol];
		}
		for(int tableuRow = b + 1; tableuRow < n; tableuRow++){
			for(int tableuCol = 0; tableuCol < a; tableuCol++) tableu[tableuRow][tableuCol] = -P[tableuCol][tableuRow];
			for(int tableuCol = a; tableuCol < m - 1; tableuCol++) tableu[tableuRow][tableuCol] = -P[tableuCol + 1][tableuRow];
			tableu[tableuRow][m - 1] = negAlpha;
			tableu[tableuRow][m] = -O[a][tableuRow];
		}
		
		for(int PRow = 0; PRow < a; PRow++) tableu[n][PRow] = -P[PRow][b];
		for(int PRow = a + 1; PRow < m; PRow++) tableu[n][PRow - 1] = -P[PRow][b];
		tableu[n][m - 1] = negAlpha;
		
//		for(float[] row : tableu){
//			for(float f : row) System.out.print(f + "   ");
//			System.out.println();
//		}
		
		Simplex.LinearProgramResult res = simplex.getAlpha(tableu);
		if(res.resultType == Simplex.LinearProgramResult.ResultType.BOUNDED) return res.boundValue;
		else return MIN_VALUE - 1;
	}
	
	private float getNewBeta(float[][] P, float[][] O, float beta, int a, int b, int m, int n){
		assert(a < m && b < n);
		int tableuRows = n + 1;
		int tableuCols = m + 1;
		float[][] tableu = new float[tableuRows][tableuCols];
		float negBeta = -beta;
		for(int tableuRow = 0; tableuRow < b; tableuRow++){
			tableu[tableuRow][0] = 1;
			for(int tableuCol = 1; tableuCol <= a; tableuCol++) tableu[tableuRow][tableuCol] = -O[tableuCol - 1][tableuRow];
			for(int tableuCol = a + 1; tableuCol < m; tableuCol++) tableu[tableuRow][tableuCol] = -O[tableuCol][tableuRow];
			tableu[tableuRow][m] = P[a][tableuRow];
		}
		for(int tableuRow = b; tableuRow < n - 1; tableuRow++){
			tableu[tableuRow][0] = 1;
			for(int tableuCol = 1; tableuCol <= a; tableuCol++) tableu[tableuRow][tableuCol] = -O[tableuCol - 1][tableuRow + 1];
			for(int tableuCol = a + 1; tableuCol < m; tableuCol++) tableu[tableuRow][tableuCol] = -O[tableuCol][tableuRow + 1];
			tableu[tableuRow][m] = P[a][tableuRow + 1];
		}
		
		int tableuRow = n - 1;
		tableu[tableuRow][0] = 1;
		for(int tableuCol = 1; tableuCol < m; tableuCol++) tableu[tableuRow][tableuCol] = negBeta;
		tableu[tableuRow][m] = beta;
		
		tableu[n][0] = -1f;
		for(int tableuCol = 1; tableuCol <= b; tableuCol++) tableu[n][tableuCol] = -P[tableuCol - 1][b];
		for(int tableuCol = b + 1; tableuCol < m; tableuCol++) tableu[n][tableuCol] = -P[tableuCol][b];
		tableu[n][m] = 0f;
		
		for(float[] row : tableu){
			for(float f : row) System.out.print(f + "   ");
			System.out.println();
		}
		
		Simplex.LinearProgramResult res = simplex.getBeta(tableu);
		if(res.resultType == Simplex.LinearProgramResult.ResultType.BOUNDED) return res.boundValue;
		else return MAX_VALUE + 1;
	}
	
    public static float[][] removeRow(float[][] matrix, int removedRow){
        int rows = matrix.length;
        int cols = matrix[0].length;
        float[][] res = new float[rows - 1][cols];
        System.arraycopy(matrix, 0, res, 0, removedRow);
        System.arraycopy(matrix, removedRow + 1, res, removedRow, rows - removedRow - 1);
        return res;
    }

    public static float[][] removeCol(float[][] matrix, int removedCol){
        int rows = matrix.length;
        int cols = matrix[0].length;
        float[][] res = new float[rows][cols - 1];
        int lenghtOfRightSide = cols - removedCol - 1;
        for(int row = 0; row < rows; row++){
            System.arraycopy(matrix[row], 0, res[row], 0, removedCol);
            System.arraycopy(matrix[row], removedCol + 1, res[row], removedCol, lenghtOfRightSide);
        }
        return res;
    }
    
    public static Move[] removeElement(Move[] list, int index){
    	Move[] res = new Move[list.length - 1];
    	System.arraycopy(list, 0, res, 0, index);
    	System.arraycopy(list, index + 1, res, index, res.length - index);
    	return res;
    }
	
	private float evaluateBoard(Board board, float alpha, float beta, int recursions){
		if(recursions <= 0) return evaluator.evaluate(board);
		
		Move[] legalMovesForPlayer = board.getLegalMovesForPlayer();
		int numMovesForPlayer = legalMovesForPlayer.length;
		Move[] legalMovesForOpponent = board.getLegalMovesForOpponent();
		int numMovesForOpponent = legalMovesForOpponent.length;
		
		if(numMovesForPlayer == 0 && numMovesForOpponent == 0) return TIE_VALUE;
		else if(numMovesForPlayer == 0) return MIN_VALUE;
		else if(numMovesForOpponent == 0) return MAX_VALUE;
		
		//the pessimistic bound for each move
		float[][] O = new float[numMovesForPlayer][numMovesForOpponent];
		for(int row = 0; row < numMovesForPlayer; row++) for(int col = 0; col < numMovesForOpponent; col++) O[row][col] = MAX_VALUE;
		
		//the optimistic bound for each move
		float[][] P = new float[numMovesForPlayer][numMovesForOpponent];
		for(int row = 0; row < numMovesForPlayer; row++) for(int col = 0; col < numMovesForOpponent; col++) P[row][col] = MIN_VALUE;
		
        Location cell = new Location(0, 0);
        boolean traversingRow = true; //if false, then traversing column
        while(cell.row < numMovesForPlayer && cell.col < numMovesForOpponent){
        	boolean rowShouldBeRemoved = false;
        	boolean colShouldBeRemoved = false;
            assert(O[cell.row][cell.col] == MAX_VALUE && P[cell.row][cell.col] == MIN_VALUE);
            
//            System.out.println("row: " + cell.row + " col: " + cell.col + " depth : " + recursions);
//    		System.out.println("O: ");
//    		for(float[] row : O){
//    			for(float f : row) System.out.print(f + "   ");
//    			System.out.println();
//    		}
//    		System.out.println();
//    		
//    		System.out.println("P: ");
//    		for(float[] row : P){
//    			for(float f : row) System.out.print(f + "   ");
//    			System.out.println();
//    		}
//    		System.out.println();
            
            float newAlpha = Math.min(cell.row, cell.col) > 0 ? getNewAlpha(P, O, alpha, cell.row, cell.col, numMovesForPlayer, numMovesForOpponent) : MIN_VALUE - 1;
            float newBeta = Math.min(cell.row, cell.col) > 0 ? getNewAlpha(P, O, beta, cell.row, cell.col, numMovesForPlayer, numMovesForOpponent) : MAX_VALUE + 1;
            
//            System.out.println("alpha: " + alpha + " newAlpha: " + newAlpha);
//            System.out.println("beta: " + beta + " newBeta: " + newBeta);
            
            Move playerMove = legalMovesForPlayer[cell.row];
			Move opponentMove = legalMovesForOpponent[cell.col];
			board.makePlayerMove(playerMove);
			board.makeOpponentMove(opponentMove);
			
            if(newAlpha >= newBeta){
            	float value = evaluateBoard(board, newAlpha, newAlpha + 0.0001f, recursions - 1);
            	if(value <= newAlpha) rowShouldBeRemoved = true;
            	else colShouldBeRemoved = true;
            } else{
            	float value = evaluateBoard(board, newAlpha, newBeta, recursions - 1);
//            	System.out.println("value: " + value + "\n");
            	if(value <= newAlpha) rowShouldBeRemoved = true;
            	else if(value >= newBeta) colShouldBeRemoved = true;
            	else O[cell.row][cell.col] = P[cell.row][cell.col] = value;
            }
			
			board.undoOpponentMove(opponentMove);
			board.undoPlayerMove(playerMove);

            if(rowShouldBeRemoved){
            	assert(!colShouldBeRemoved);
                O = removeRow(O, cell.row);
                P = removeRow(P, cell.row);
                legalMovesForPlayer = removeElement(legalMovesForPlayer, cell.row);
                numMovesForPlayer--;
                if(cell.row >= numMovesForPlayer){
                	cell.col++;
                    cell.row = cell.col;
                    traversingRow = true;
                }
                if(traversingRow) cell.col = cell.row;
            } else if(colShouldBeRemoved){
                O = removeCol(O, cell.col);
                P = removeCol(P, cell.col);
                legalMovesForOpponent = removeElement(legalMovesForOpponent, cell.col); 
                numMovesForOpponent--;
                if(cell.col >= numMovesForOpponent){
                	cell.col = cell.row;
                    cell.row++;
                    traversingRow = false;
                }
                if(!traversingRow) cell.row = cell.col + 1;
            } else{
                if(traversingRow){
                	cell.col++;
                    if(cell.col >= numMovesForOpponent){
                    	cell.col = cell.row;
                        cell.row++;
                        traversingRow = false;
                    }
                } else{
                	cell.row++;
                    if(cell.row >= numMovesForPlayer){
                    	cell.col++;
                        cell.row = cell.col;
                        traversingRow = true;
                    }
                }
            }
        }
        
		try{
			if(O.length == 0 || O[0].length == 0) return alpha;
			Nash nash = new Nash(O);
			Nash.NashResult nashResult = nash.getNashEquilibrium(simplex);
			return nashResult.nashValue;
		} catch(Exception e){
			System.out.println("errored board:");
			for(int i = 0; i < O.length; i++){
				float[] row = O[i];
				for(float f : row) System.out.print(f + "   ");
				System.out.println(BotStarter.MoveToStr(legalMovesForPlayer[i]));
			}
			e.printStackTrace();
			Move error = legalMovesForPlayer[10]; //make an illegal reference to create an error
			return 0;
		}
	}
	
	public Stack<Move> getPlayerMoves(Board board, int time, int round){
		System.err.print("round " + round + " ");
		Move bestMove = Move.UP;
		
		int floodFillResult = floodFill(board);
		if(floodFillResult != -1){
			if(floodFillResult < 30){
				System.err.println("backtrack filling with depth " + Integer.toString(floodFillResult));
				return backtrackFill(board, floodFillResult);
			}
			int fillRecursions = 15;
			System.err.println("filling with depth " + Integer.toString(fillRecursions));
			bestMove = getBestMoveFill(board, fillRecursions);
		}
		else{
			int boardsToEvaluate = getBoardsToEvaluate.apply(time, round, 0);
			System.err.println("searching with depth " + Integer.toString(boardsToEvaluate));
			
			Move[] legalMovesForPlayer = board.getLegalMovesForPlayer();
			Move[] legalMovesForOpponent = board.getLegalMovesForOpponent();
			
			if(legalMovesForPlayer.length > 0) bestMove = legalMovesForPlayer[0];
			if(legalMovesForPlayer.length > 0 && legalMovesForOpponent.length > 0){
				float[][] gameMatrix = new float[legalMovesForPlayer.length][legalMovesForOpponent.length];
				for(int i = 0; i < legalMovesForPlayer.length; i++){
					for(int j = 0; j < legalMovesForOpponent.length; j++){
						Move playerMove = legalMovesForPlayer[i];
						Move opponentMove = legalMovesForOpponent[j];
						board.makePlayerMove(playerMove);
						board.makeOpponentMove(opponentMove);
						gameMatrix[i][j] = evaluateBoard(board, MIN_VALUE, MAX_VALUE, boardsToEvaluate - 1);
						board.undoOpponentMove(opponentMove);
						board.undoPlayerMove(playerMove);
					}
				}

				Nash nash = new Nash(gameMatrix);
				Nash.NashResult nashResult = nash.getNashEquilibrium(simplex);
				float randomFloat = random.nextFloat();
				
				for(int i = 0; i < gameMatrix.length; i++){
					float[] row = gameMatrix[i];
					for(float f : row) System.out.print(f + "   ");
					System.out.println(BotStarter.MoveToStr(legalMovesForPlayer[i]));
				}
				System.out.println("board result:");
				for(float f : nashResult.mixedStrategy) System.out.print(f + "   ");
				System.out.println();
				System.out.print(nashResult.nashValue + " ");
				System.out.print("random value: " + randomFloat + " ");
				
				for(int i = 0; i < legalMovesForPlayer.length; i++){
					float f = nashResult.mixedStrategy[i];
					randomFloat -= f;
					if(randomFloat < 0.0f){
						bestMove = legalMovesForPlayer[i];
						System.out.println("move chosen: " + i + "\n");
						break;
					}
				}		
			}
		}
		
		Stack<Move> s = new Stack<>();
		s.addElement(bestMove);
		return s;
	}
}
