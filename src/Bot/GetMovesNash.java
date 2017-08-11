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
	
	public Stack<Move> getPlayerMoves(Board board, int time, int round, Move lastOpponentMove){
		System.err.print("round " + round + " ");
		Move bestMove = Move.UP;

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
		
		Stack<Move> s = new Stack<>();
		s.addElement(bestMove);
		return s;
	}
}
