package Bot;

import java.util.Stack;

public class GetMovesAlphaBeta implements GetMoves{
	
	private Evaluator evaluator;
	GetSearchNumber getSearchDepth;
	
	public GetMovesAlphaBeta(Evaluator evaluator, GetSearchNumber getSearchDepth){
		this.evaluator = evaluator;
		this.getSearchDepth = getSearchDepth;
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
			
			//a smaller searchHeight is better, as it means it got farther down the tree
			else if(this.searchHeight < other.searchHeight) return 1;
			else if(this.searchHeight > other.searchHeight) return -1;
			return 0;
		}
	}
	
	private pathEvaluation playerEvaluate(Board board, Evaluator eval, int recursions, pathEvaluation alpha, pathEvaluation beta){
		if(recursions <= 0) return new pathEvaluation(eval.evaluate(board), 0);
		else{
			pathEvaluation maxVal = new pathEvaluation(Integer.MIN_VALUE, recursions);
			for(Move move : Move.ALLMOVES){
				if(!board.isLegalMoveForPlayer(move)) continue;
				board.makePlayerMove(move);
				pathEvaluation moveEvaluation = opponentEvaluate(board, eval, recursions - 1, alpha, beta);
				board.undoPlayerMove(move);
				
				if(maxVal.compare(moveEvaluation) < 0) maxVal = moveEvaluation;
				if(alpha.compare(maxVal) < 0) alpha = maxVal;
				if(beta.compare(alpha) <= 0) break; //prune
			}
			return maxVal;
		}
	}
	
	private pathEvaluation opponentEvaluate(Board board, Evaluator eval, int recursions, pathEvaluation alpha, pathEvaluation beta){
		if(recursions <= 0) return new pathEvaluation(eval.evaluate(board), 0);
		else{
			pathEvaluation minVal = new pathEvaluation(Integer.MAX_VALUE, recursions);
			for(Move move : Move.ALLMOVES){
				if(!board.isLegalMoveForOpponent(move)) continue;
				board.makeOpponentMove(move);
				pathEvaluation moveEvaluation = playerEvaluate(board, eval, recursions - 1, alpha, beta);
				board.undoOpponentMove(move);
				
				if(minVal.compare(moveEvaluation) > 0) minVal = moveEvaluation;
				if(beta.compare(minVal) > 0) beta = minVal;
				if(beta.compare(alpha) <= 0) break; //prune
			}
			return minVal;
		}
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
	
	public Stack<Move> getPlayerMoves(Board board, int time, int round, Move lastOpponentMove){
//		System.err.print("round " + round + " ");
		Move bestMove;
		
		int floodFillResult = floodFill(board);
		if(floodFillResult != -1){
			if(floodFillResult < 30){
				System.err.println("backtrack filling with depth " + Integer.toString(floodFillResult));
				return backtrackFill(board, floodFillResult);
			}
			int fillRecursions = 12;
			System.err.println("filling with depth " + Integer.toString(fillRecursions));
			bestMove = getBestMoveFill(board, fillRecursions);
		}
		else{
			int moveDepthToSearch = getSearchDepth.apply(time, round, 0);
//			System.err.println("searching with depth " + Integer.toString(moveDepthToSearch));
			
			int recursions = 2 * moveDepthToSearch;
			
			pathEvaluation alpha = new pathEvaluation(Integer.MIN_VALUE, Integer.MAX_VALUE);
			pathEvaluation beta = new pathEvaluation(Integer.MAX_VALUE, Integer.MIN_VALUE);
			
			pathEvaluation maxVal = new pathEvaluation(Integer.MIN_VALUE, recursions);
			bestMove = Move.UP; //default move
			for(Move move : Move.ALLMOVES){
				if(!board.isLegalMoveForPlayer(move)) continue;
				board.makePlayerMove(move);
				pathEvaluation moveEvaluation = opponentEvaluate(board, this.evaluator, recursions - 1, alpha, beta);
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