package Bot;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;

public class GetMovesABSort implements GetMoves{
	
	private Evaluator evaluator;
	private GetSearchNumber getSearchDepth;
	private AscendingSorter ascendingSorter = new AscendingSorter();
	private DescendingSorter descendingSorter = new DescendingSorter();
	
	public GetMovesABSort(Evaluator evaluator, GetSearchNumber getSearchDepth){
		this.evaluator = evaluator;
		this.getSearchDepth = getSearchDepth;
	}
	
	private class Edge {
		public Move move;
		public int evaluation;
		
		public Edge(Move move, int evaluation){
			this.move = move;
			this.evaluation = evaluation;
		}
	}
	
	private class DescendingSorter implements Comparator<Edge>{		
		public int compare(Edge edge1, Edge edge2){
			return edge2.evaluation - edge1.evaluation;
		}
	}
	
	private class AscendingSorter implements Comparator<Edge>{		
		public int compare(Edge edge1, Edge edge2){
			return edge1.evaluation - edge2.evaluation;
		}
	}
	
	public void descendingSort(Move[] moves, Board board){
		Edge[] edges = new Edge[moves.length];
		
		for(int i = 0; i < moves.length; i++){
			Move move = moves[i];
			board.makePlayerMove(move);
			edges[i] = new Edge(move, evaluator.evaluate(board));
			board.undoPlayerMove(move);
		}
		
		Arrays.sort(edges, descendingSorter);
		for(int i = 0; i < moves.length; i++){
			moves[i] = edges[i].move;
		}
	}
	
	public void ascendingSort(Move[] moves, Board board){
		Edge[] edges = new Edge[moves.length];
		
		for(int i = 0; i < moves.length; i++){
			Move move = moves[i];
			board.makeOpponentMove(move);
			edges[i] = new Edge(move, evaluator.evaluate(board));
			board.undoOpponentMove(move);
		}
		
		Arrays.sort(edges, ascendingSorter);
		for(int i = 0; i < moves.length; i++){
			moves[i] = edges[i].move;
		}
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
	
	private pathEvaluation playerEvaluate(Board board, Evaluator eval, int recursions, int sortThreshold, pathEvaluation alpha, pathEvaluation beta){
		if(recursions <= 0) return new pathEvaluation(eval.evaluate(board), 0);
		else{
			pathEvaluation maxVal = new pathEvaluation(Integer.MIN_VALUE, recursions);
			Move[] legalMoves = board.getLegalMovesForPlayer();
			if(recursions > sortThreshold) descendingSort(legalMoves, board);
			for(Move move : legalMoves){
				board.makePlayerMove(move);
				pathEvaluation moveEvaluation = opponentEvaluate(board, eval, recursions - 1, sortThreshold, alpha, beta);
				board.undoPlayerMove(move);
				
				if(maxVal.compare(moveEvaluation) < 0) maxVal = moveEvaluation;
				if(alpha.compare(maxVal) < 0) alpha = maxVal;
				if(beta.compare(alpha) <= 0) break; //prune
			}
			return maxVal;
		}
	}
	
	private pathEvaluation opponentEvaluate(Board board, Evaluator eval, int recursions, int sortThreshold, pathEvaluation alpha, pathEvaluation beta){
		if(recursions <= 0) return new pathEvaluation(eval.evaluate(board), 0);
		else{
			pathEvaluation minVal = new pathEvaluation(Integer.MAX_VALUE, recursions);
			Move[] legalMoves = board.getLegalMovesForOpponent();
			if(recursions > sortThreshold) ascendingSort(legalMoves, board);
			for(Move move : legalMoves){
				board.makeOpponentMove(move);
				pathEvaluation moveEvaluation = playerEvaluate(board, eval, recursions - 1, sortThreshold, alpha, beta);
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

		int moveDepthToSearch = getSearchDepth.apply(time, round, 0);
		System.err.println("searching with depth " + Integer.toString(moveDepthToSearch));
		
		int recursions = 2 * moveDepthToSearch;
		
		pathEvaluation alpha = new pathEvaluation(Integer.MIN_VALUE, Integer.MAX_VALUE);
		pathEvaluation beta = new pathEvaluation(Integer.MAX_VALUE, Integer.MIN_VALUE);
		
		pathEvaluation maxVal = new pathEvaluation(Integer.MIN_VALUE, recursions);
		
		Move[] legalMoves = board.getLegalMovesForPlayer();
		descendingSort(legalMoves, board);
		
		bestMove = Move.UP; //default move
		for(Move move : legalMoves){
			if(!board.isLegalMoveForPlayer(move)) continue;
			board.makePlayerMove(move);
			pathEvaluation moveEvaluation = opponentEvaluate(board, this.evaluator, recursions - 1, recursions - 3, alpha, beta);
			board.undoPlayerMove(move);
			
			if(alpha.compare(maxVal) < 0) alpha = maxVal;
			if(moveEvaluation.compare(maxVal) > 0){
				maxVal = moveEvaluation;
				bestMove = move;
			}
		}
		
		Stack<Move> s = new Stack<>();
		s.addElement(bestMove);
		return s;
	}
}
