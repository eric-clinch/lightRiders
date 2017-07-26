package Bot;

import java.util.Random;
import java.util.Stack;

public class GetMovesNash implements GetMoves{
	
	private Evaluator evaluator;
	private GetSearchNumber getBoardsToEvaluate;
	private Simplex simplex;
	private Random random;
	private int TIE_VALUE = 0;
	private int MIN_VALUE = -1000;
	private int MAX_VALUE = 1000;
	
	public GetMovesNash(Evaluator evaluator, GetSearchNumber getBoardsToEvaluate){
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
	
	public float evaluateBoard(Board board, int recursions){
		if(recursions <= 0) return evaluator.evaluate(board);
		
		Move[] legalMovesForPlayer = board.getLegalMovesForPlayer();
		Move[] legalMovesForOpponent = board.getLegalMovesForOpponent();
		
		if(legalMovesForPlayer.length == 0 && legalMovesForOpponent.length == 0) return TIE_VALUE;
		else if(legalMovesForPlayer.length == 0) return MIN_VALUE;
		else if(legalMovesForOpponent.length == 0) return MAX_VALUE;
		
		float[][] gameMatrix = new float[legalMovesForPlayer.length][legalMovesForOpponent.length];
		for(int i = 0; i < legalMovesForPlayer.length; i++){
			for(int j = 0; j < legalMovesForOpponent.length; j++){
				Move playerMove = legalMovesForPlayer[i];
				Move opponentMove = legalMovesForOpponent[j];
				board.makePlayerMove(playerMove);
				board.makeOpponentMove(opponentMove);
				gameMatrix[i][j] = evaluateBoard(board, recursions - 1);
				board.undoOpponentMove(opponentMove);
				board.undoPlayerMove(playerMove);
			}
		}

		try{
			Nash nash = new Nash(gameMatrix);
			Nash.NashResult nashResult = nash.getNashEquilibrium(simplex);
			return nashResult.nashValue;
		} catch(Exception e){
			System.out.println("errored board:");
			for(int i = 0; i < gameMatrix.length; i++){
				float[] row = gameMatrix[i];
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
						gameMatrix[i][j] = evaluateBoard(board, boardsToEvaluate - 1);
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
