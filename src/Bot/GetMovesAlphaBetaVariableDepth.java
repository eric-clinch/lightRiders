package Bot;

import java.util.Stack;

public class GetMovesAlphaBetaVariableDepth implements GetMoves{
	
	private Evaluator evaluator;
	GetSearchNumber getBoardsToEvaluate;
	
	public GetMovesAlphaBetaVariableDepth(Evaluator evaluator, GetSearchNumber getBoardsToEvaluate){
		this.evaluator = evaluator;
		this.getBoardsToEvaluate = getBoardsToEvaluate;
	}
	
	private class pathEvaluation{
		int evaluation;
		int searchHeight; //represents how far from the bottom of the search tree the evaluation
		
		public pathEvaluation(int evaluation, int searchHeight){
			this.evaluation = evaluation;
			this.searchHeight = searchHeight;
		}
		
		public int compare(pathEvaluation other){
			//a greater evaluation is better
			if(this.evaluation > other.evaluation) return 1;
			else if(this.evaluation < other.evaluation) return -1;
			
			//a larger searchHeight is better, as it means it got farther down the tree
			else if(this.searchHeight > other.searchHeight) return 1;
			else if(this.searchHeight < other.searchHeight) return -1;
			return 0;
		}
	}
	
	private pathEvaluation playerEvaluate(Board board, Evaluator eval, int boardsToEvaluate, int recursionLevel, pathEvaluation alpha, pathEvaluation beta){
		if(boardsToEvaluate <= 0) return new pathEvaluation(eval.evaluate(board), recursionLevel);
		else{
			pathEvaluation maxVal = new pathEvaluation(Integer.MIN_VALUE, recursionLevel);
			Move[] legalMoves = board.getLegalMovesForPlayer();
			if(legalMoves.length == 0) return maxVal;
			int boardsToEvaluatePerMove = boardsToEvaluate / legalMoves.length;
			for(Move move : legalMoves){
				board.makePlayerMove(move);
				pathEvaluation moveEvaluation = opponentEvaluate(board, eval, boardsToEvaluatePerMove, recursionLevel + 1, alpha, beta);
				board.undoPlayerMove(move);
				
				if(maxVal.compare(moveEvaluation) < 0) maxVal = moveEvaluation;
				if(alpha.compare(maxVal) < 0) alpha = maxVal;
				if(beta.compare(alpha) <= 0) break; //prune
			}
			return maxVal;
		}
	}
	
	private pathEvaluation opponentEvaluate(Board board, Evaluator eval, int boardsToEvaluate, int recursionLevel, pathEvaluation alpha, pathEvaluation beta){
		if(boardsToEvaluate <= 0) return new pathEvaluation(eval.evaluate(board), recursionLevel);
		else{
			pathEvaluation minVal = new pathEvaluation(Integer.MAX_VALUE, recursionLevel);
			Move[] legalMoves = board.getLegalMovesForOpponent();
			if(legalMoves.length == 0) return minVal;
			int boardsToEvaluatePerMove = boardsToEvaluate / legalMoves.length;
			for(Move move : legalMoves){
				board.makeOpponentMove(move);
				pathEvaluation moveEvaluation = playerEvaluate(board, eval, boardsToEvaluatePerMove, recursionLevel + 1, alpha, beta);
				board.undoOpponentMove(move);
				
				if(minVal.compare(moveEvaluation) > 0) minVal = moveEvaluation;
				if(beta.compare(minVal) > 0) beta = minVal;
				if(beta.compare(alpha) <= 0) break; //prune
			}
			return minVal;
		}
	}
	
	public Stack<Move> getPlayerMoves(Board board, int time, int round, Move lastOpponentMove){
		System.err.print("round " + round + " ");
		Move bestMove;

		int boardsToEvaluate = getBoardsToEvaluate.apply(time, round, 0);
		System.err.println("searching with depth " + Integer.toString(boardsToEvaluate));
		
		pathEvaluation alpha = new pathEvaluation(Integer.MIN_VALUE, Integer.MAX_VALUE);
		pathEvaluation beta = new pathEvaluation(Integer.MAX_VALUE, Integer.MIN_VALUE);
		
		pathEvaluation maxVal = new pathEvaluation(Integer.MIN_VALUE, 0);
		bestMove = Move.UP; //default move
		Move[] legalMoves = board.getLegalMovesForPlayer();
		if(legalMoves.length > 0){
			int boardsToEvaluatePerMove = boardsToEvaluate / legalMoves.length;
			for(Move move : legalMoves){
				board.makePlayerMove(move);
				pathEvaluation moveEvaluation = opponentEvaluate(board, this.evaluator, boardsToEvaluatePerMove, 1, alpha, beta);
				board.undoPlayerMove(move);
				
				if(alpha.compare(maxVal) < 0) alpha = maxVal;
				if(moveEvaluation.compare(maxVal) > 0){
					maxVal = moveEvaluation;
					bestMove = move;
				}
			}	
		}
		
		Stack<Move> s = new Stack<>();
		s.addElement(bestMove);
		return s;
	}
}
