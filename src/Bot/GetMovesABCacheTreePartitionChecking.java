package Bot;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;

public class GetMovesABCacheTreePartitionChecking implements GetMoves{
	
	private Evaluator evaluator;
	private GetSearchNumber getSearchDepth;
	private AscendingSorter ascendingSorter = new AscendingSorter();
	private DescendingSorter descendingSorter = new DescendingSorter();
	private int sortThreshold;
	private int maxEvaluationValue;
	private int partitionedEvaluationOffset;
	public CacheTree currentTree;
	
	public GetMovesABCacheTreePartitionChecking(Evaluator evaluator, GetSearchNumber getSearchDepth){
		this.evaluator = evaluator;
		this.getSearchDepth = getSearchDepth;
		this.sortThreshold = 3;
		this.currentTree = null;
		this.maxEvaluationValue = evaluator.getMaxValue();
		this.partitionedEvaluationOffset = evaluator.getPartitionedOffset();
	}
	
	private class CacheTreeChild {
		public Move move;
		public CacheTree cacheTree;
		
		public CacheTreeChild(Move move, CacheTree cacheTree){
			this.move = move;
			this.cacheTree = cacheTree;
		}
	}
	
	private class CacheTreeEdge {
		public CacheTreeChild cacheTreeChild;
		public PathEvaluation evaluation;
		
		public CacheTreeEdge(CacheTreeChild cacheTreeChild, PathEvaluation evaluation){
			this.cacheTreeChild = cacheTreeChild;
			this.evaluation = evaluation;
		}
	}

	
	private class CacheTree {
		public CacheTreeChild[] children;
		public Integer partitionedEvaluation;
		
		public CacheTree(CacheTreeChild[] children, Integer partitionedEvaluation) {
			this.children = children;
			this.partitionedEvaluation = partitionedEvaluation;
		}
		
		public CacheTree getChild(Move move){
			for(CacheTreeChild child : children){
				if(child.move == move) return child.cacheTree;
			}
			assert(false);
			return null;
		}
	}
	
	private class DescendingSorter implements Comparator<CacheTreeEdge>{		
		public int compare(CacheTreeEdge edge1, CacheTreeEdge edge2){
			return edge2.evaluation.compare(edge1.evaluation);
		}
	}
	
	private class AscendingSorter implements Comparator<CacheTreeEdge>{		
		public int compare(CacheTreeEdge edge1, CacheTreeEdge edge2){
			return edge1.evaluation.compare(edge2.evaluation);
		}
	}
	
	public CacheTreeChild[] descendingSort(CacheTreeEdge[] edges){
		Arrays.sort(edges, descendingSorter);
		int len = edges.length;
		CacheTreeChild[] res = new CacheTreeChild[len];
		for(int i = 0; i < len; i++){
			res[i] = edges[i].cacheTreeChild;
		}
		return res;
	}
	
	public CacheTreeChild[] ascendingSort(CacheTreeEdge[] edges){
		Arrays.sort(edges, ascendingSorter);
		int len = edges.length;
		CacheTreeChild[] res = new CacheTreeChild[len];
		for(int i = 0; i < len; i++){
			res[i] = edges[i].cacheTreeChild;
		}
		return res;
	}
	
	public boolean isArticulationPoint(Move[] legalMoves){
		if(legalMoves.length != 2) return false;
		Move move0 = legalMoves[0];
		Move move1 = legalMoves[1];
		return move0.drow == move1.drow || move0.dcol == move1.dcol;
	}
	
	private class PathEvaluation{
		int evaluation;
		int searchHeight; //represents how far from the bottom of the search tree the evaluation
		
		public PathEvaluation(int evaluation, int searchHeight){
			this.evaluation = evaluation;
			this.searchHeight = searchHeight;
		}
		
		public int compare(PathEvaluation other){
			if(this.evaluation > other.evaluation) return 1;
			else if(this.evaluation < other.evaluation) return -1;
			
			else if(this.searchHeight > other.searchHeight) return 1;
			else if(this.searchHeight < other.searchHeight) return -1;
			return 0;
		}
	}
	
	private PathEvaluation playerEvaluation(Board board, CacheTree thisTree, int depth, int maxDepth, PathEvaluation alpha, PathEvaluation beta){
		assert(thisTree != null);
		assert(depth % 2 == 0);
		if(thisTree.partitionedEvaluation != null){
			return new PathEvaluation(thisTree.partitionedEvaluation, depth);
		}
		if(depth == maxDepth) return new PathEvaluation(evaluator.evaluate(board), depth);
		
		PathEvaluation maxVal = new PathEvaluation(Integer.MIN_VALUE, depth);
		CacheTreeEdge[] edges = null;
		boolean shouldSort = depth == sortThreshold || depth == sortThreshold - 1;
		
		if(thisTree.children == null){
			Move[] legalMoves = board.getLegalMovesForPlayer();
			int legalMovesLength = legalMoves.length;
			CacheTreeChild[] children = new CacheTreeChild[legalMovesLength];
			if(shouldSort) edges = new CacheTreeEdge[legalMovesLength];
			
			for(int i = 0; i < legalMovesLength; i++){
				Move move = legalMoves[i];
				board.makePlayerMove(move);
				CacheTreeEdge edge = makeOpponentEdge(board, depth + 1, maxDepth, alpha, beta, move);
				board.undoPlayerMove(move);
				
				PathEvaluation moveEvaluation = edge.evaluation;
				if(shouldSort) edges[i] = edge;
				children[i] = edge.cacheTreeChild;
				if(maxVal.compare(moveEvaluation) < 0){
					maxVal = moveEvaluation;
					if(alpha.compare(maxVal) < 0){
						alpha = maxVal;
						if(beta.compare(alpha) <= 0){
							for(int j = i + 1; j < legalMovesLength; j++) children[j] = new CacheTreeChild(legalMoves[j], null);
							if(shouldSort) for(int j = i + 1; j < legalMovesLength; j++) edges[j] = new CacheTreeEdge(children[j], new PathEvaluation(Integer.MIN_VALUE, depth));
							break; // prune
						}
					}
				}
			}
			thisTree.children = children;
		} else {
			CacheTreeChild[] children = thisTree.children;
			int numChildren = thisTree.children.length;
			if(shouldSort) edges = new CacheTreeEdge[numChildren];
			for(int i = 0; i < numChildren; i++){
				CacheTreeChild child = children[i];
				Move move = child.move;
				PathEvaluation moveEvaluation;
				
				if(child.cacheTree == null){
					board.makePlayerMove(move);
					CacheTreeEdge edge = makeOpponentEdge(board, depth + 1, maxDepth, alpha, beta, move);
					board.undoPlayerMove(move);
					
					moveEvaluation = edge.evaluation;
					child.cacheTree = edge.cacheTreeChild.cacheTree;
					if(shouldSort) edges[i] = edge;
				} else{
					assert(child.cacheTree != null);
					board.makePlayerMove(move);
					moveEvaluation = opponentEvaluation(board, child.cacheTree, depth + 1, maxDepth, alpha, beta);
					board.undoPlayerMove(move);
					if(shouldSort) edges[i] = new CacheTreeEdge(child, moveEvaluation);
				}
				
				if(maxVal.compare(moveEvaluation) < 0){
					maxVal = moveEvaluation;
					if(alpha.compare(maxVal) < 0){
						alpha = maxVal;
						if(beta.compare(alpha) <= 0) {
							if(shouldSort) for(int j = i + 1; j < numChildren; j++) edges[j] = new CacheTreeEdge(children[j], new PathEvaluation(Integer.MIN_VALUE, depth));
							break; // prune
						}
					}
				}
			}
		}
		if(shouldSort) thisTree.children = descendingSort(edges);
		return maxVal;
	}
	
	private PathEvaluation opponentEvaluation(Board board, CacheTree thisTree, int depth, int maxDepth, PathEvaluation alpha, PathEvaluation beta){
		assert(thisTree != null);
		assert(depth % 2 == 1);
		if(thisTree.partitionedEvaluation != null){
			return new PathEvaluation(thisTree.partitionedEvaluation, depth);
		}
		if(depth == maxDepth) return new PathEvaluation(evaluator.evaluate(board), depth);
		
		PathEvaluation minVal = new PathEvaluation(Integer.MAX_VALUE, depth);
		CacheTreeEdge[] edges = null;
		boolean shouldSort = depth == sortThreshold || depth == sortThreshold - 1;
		
		if(thisTree.children == null){
			Move[] legalMoves = board.getLegalMovesForOpponent();
			int legalMovesLength = legalMoves.length;
			if(shouldSort) edges = new CacheTreeEdge[legalMovesLength];
			CacheTreeChild[] children = new CacheTreeChild[legalMovesLength];
			for(int i = 0; i < legalMovesLength; i++){
				Move move = legalMoves[i];
				board.makeOpponentMove(move);
				CacheTreeEdge edge = makePlayerEdge(board, depth + 1, maxDepth, alpha, beta, move);
				board.undoOpponentMove(move);
				
				PathEvaluation moveEvaluation = edge.evaluation;
				children[i] = edge.cacheTreeChild;
				if(shouldSort) edges[i] = edge;
				if(minVal.compare(moveEvaluation) > 0){
					minVal = moveEvaluation;
					if(beta.compare(minVal) > 0){
						beta = minVal;
						if(beta.compare(alpha) <= 0){
							for(int j = i + 1; j < legalMovesLength; j++) children[j] = new CacheTreeChild(legalMoves[j], null);
							if(shouldSort) for(int j = i + 1; j < legalMovesLength; j++) edges[j] = new CacheTreeEdge(children[j], new PathEvaluation(Integer.MAX_VALUE, depth));
							break; // prune
						}
					}
				}
			}
			thisTree.children = children;
		} else {
			CacheTreeChild[] children = thisTree.children;
			int numChildren = thisTree.children.length;
			if(shouldSort) edges = new CacheTreeEdge[numChildren];
			
			for(int i = 0; i < numChildren; i++){
				CacheTreeChild child = children[i];
				Move move = child.move;
				PathEvaluation moveEvaluation;
				
				if(child.cacheTree == null){
					board.makeOpponentMove(move);
					CacheTreeEdge edge = makePlayerEdge(board, depth + 1, maxDepth, alpha, beta, move);
					board.undoOpponentMove(move);
					
					child.cacheTree = edge.cacheTreeChild.cacheTree;
					moveEvaluation = edge.evaluation;
					if(shouldSort) edges[i] = edge;
				} else{
					board.makeOpponentMove(move);
					assert(child.cacheTree != null);
					moveEvaluation = playerEvaluation(board, child.cacheTree, depth + 1, maxDepth, alpha, beta);
					board.undoOpponentMove(move);
					if(shouldSort) edges[i] = new CacheTreeEdge(child, moveEvaluation);
				}
				
				if(minVal.compare(moveEvaluation) > 0){
					minVal = moveEvaluation;
					if(beta.compare(minVal) > 0){
						beta = minVal;
						if(beta.compare(alpha) <= 0) {
							if(shouldSort) for(int j = i + 1; j < numChildren; j++) edges[j] = new CacheTreeEdge(children[j], new PathEvaluation(Integer.MAX_VALUE, depth));
							break; // prune
						}
					}
				}
			}
		}
		if(shouldSort) thisTree.children = ascendingSort(edges);
		return minVal;
	}
	
	private CacheTreeEdge makePlayerEdge(Board board, int depth, int maxDepth, PathEvaluation alpha, PathEvaluation beta, Move lastMove){
		assert(depth % 2 == 0);
		if(depth == maxDepth){
			int evaluation = evaluator.evaluate(board);
			boolean isPartitioned = evaluation > maxEvaluationValue;
			if(isPartitioned){
				evaluation -= partitionedEvaluationOffset;
				evaluation *= 256;
			}
			PathEvaluation pathEvaluation = new PathEvaluation(evaluation, depth);
			CacheTree tree = new CacheTree(null, isPartitioned ? evaluation : null);
			CacheTreeChild child = new CacheTreeChild(lastMove, tree);
			return new CacheTreeEdge(child, pathEvaluation); 
		}
		
		boolean shouldSort = depth == sortThreshold || depth == sortThreshold - 1;
		CacheTreeEdge[] edges = null;
		
		PathEvaluation maxVal = new PathEvaluation(Integer.MIN_VALUE, depth);
		Move[] legalMoves = board.getLegalMovesForPlayer();
		int legalMovesLength = legalMoves.length;
		CacheTreeChild[] children = new CacheTreeChild[legalMovesLength];
		if(shouldSort) edges = new CacheTreeEdge[legalMovesLength];
		for(int i = 0; i < legalMovesLength; i++){
			Move move = legalMoves[i];
			board.makePlayerMove(move);
			CacheTreeEdge edge = makeOpponentEdge(board, depth + 1, maxDepth, alpha, beta, move);
			board.undoPlayerMove(move);
			
			PathEvaluation moveEvaluation = edge.evaluation;
			children[i] = edge.cacheTreeChild;
			if(shouldSort) edges[i] = edge;
			
			if(maxVal.compare(moveEvaluation) < 0){
				maxVal = moveEvaluation;
				if(alpha.compare(maxVal) < 0){
					alpha = maxVal;
					if(beta.compare(alpha) <= 0){
						for(int j = i + 1; j < legalMovesLength; j++) children[j] = new CacheTreeChild(legalMoves[j], null);
						if(shouldSort) for(int j = i + 1; j < legalMovesLength; j++) edges[j] = new CacheTreeEdge(children[j], new PathEvaluation(Integer.MIN_VALUE, depth));
						break; // prune
					}
				}
			}
		}
		for(CacheTreeChild c : children) assert(c.move != null);
		if(shouldSort) children = descendingSort(edges);
		CacheTree tree = new CacheTree(children, null);
		CacheTreeChild child = new CacheTreeChild(lastMove, tree);
		return new CacheTreeEdge(child, maxVal); 
	}
	
	private CacheTreeEdge makeOpponentEdge(Board board, int depth, int maxDepth, PathEvaluation alpha, PathEvaluation beta, Move lastMove){
		assert(depth % 2 == 1);
		if(depth == maxDepth){
			assert(false);
			int evaluation = evaluator.evaluate(board);
			boolean isPartitioned = evaluation > maxEvaluationValue;
			if(isPartitioned){
				evaluation -= partitionedEvaluationOffset;
				evaluation *= 256;
			}
			PathEvaluation pathEvaluation = new PathEvaluation(evaluation, depth);
			CacheTree tree = new CacheTree(null, isPartitioned ? evaluation : null);
			CacheTreeChild child = new CacheTreeChild(lastMove, tree);
			return new CacheTreeEdge(child, pathEvaluation); 
		}
		
		boolean shouldSort = depth == sortThreshold || depth == sortThreshold - 1;
		CacheTreeEdge[] edges = null;
		
		PathEvaluation minVal = new PathEvaluation(Integer.MAX_VALUE, depth);
		Move[] legalMoves = board.getLegalMovesForOpponent();
		int legalMovesLength = legalMoves.length;
		CacheTreeChild[] children = new CacheTreeChild[legalMovesLength];
		if(shouldSort) edges = new CacheTreeEdge[legalMovesLength];
		for(int i = 0; i < legalMovesLength; i++){
			Move move = legalMoves[i];
			board.makeOpponentMove(move);
			CacheTreeEdge edge = makePlayerEdge(board, depth + 1, maxDepth, alpha, beta, move);
			board.undoOpponentMove(move);
			
			PathEvaluation moveEvaluation = edge.evaluation;
			children[i] = edge.cacheTreeChild;
			if(shouldSort) edges[i] = edge;
			
			if(minVal.compare(moveEvaluation) > 0){
				minVal = moveEvaluation;
				if(beta.compare(minVal) > 0){
					beta = minVal;
					if(beta.compare(alpha) <= 0){ 
						for(int j = i + 1; j < legalMovesLength; j++) children[j] = new CacheTreeChild(legalMoves[j], null);
						if(shouldSort) for(int j = i + 1; j < legalMovesLength; j++) edges[j] = new CacheTreeEdge(children[j], new PathEvaluation(Integer.MAX_VALUE, depth));
						break; // prune
					}
				}
			}
		}
		for(CacheTreeChild c : children) assert(c.move != null);
		if(shouldSort) children = ascendingSort(edges);
		CacheTree tree = new CacheTree(children, null);
		CacheTreeChild child = new CacheTreeChild(lastMove, tree);
		return new CacheTreeEdge(child, minVal); 
	}
	
	public Stack<Move> getPlayerMoves(Board board, int time, int round, Move lastOpponentMove){
		System.err.print("round " + round + " ");
		Move bestMove;
		
		int moveDepthToSearch = getSearchDepth.apply(time, round, 0);
		System.err.println("searching with depth " + Integer.toString(moveDepthToSearch));
		
		int recursions = 2 * moveDepthToSearch;
		
		PathEvaluation alpha = new PathEvaluation(Integer.MIN_VALUE, Integer.MIN_VALUE);
		PathEvaluation beta = new PathEvaluation(Integer.MAX_VALUE, Integer.MAX_VALUE);
		
		if(currentTree == null){
			bestMove = Move.UP;
			PathEvaluation bestEval = new PathEvaluation(Integer.MIN_VALUE, Integer.MIN_VALUE);
			CacheTree bestChildTree = null;
			Move[] legalMoves = board.getLegalMovesForPlayer();
			for(Move move : legalMoves){
				board.makePlayerMove(move);
				CacheTreeEdge edge = makeOpponentEdge(board, 1, recursions, alpha, beta, null);
				board.undoPlayerMove(move);
				
				PathEvaluation eval = edge.evaluation;
				if(bestEval.compare(eval) < 0){
					bestEval = eval;
					bestMove = move;
					bestChildTree = edge.cacheTreeChild.cacheTree;
				}
			}
			currentTree = bestChildTree;
		} else {
			currentTree = currentTree.getChild(lastOpponentMove);
			PathEvaluation bestEval = new PathEvaluation(Integer.MIN_VALUE, Integer.MIN_VALUE);
			bestMove = null;
			CacheTree bestSubtree = null;
			for(CacheTreeChild child : currentTree.children){
				Move move = child.move;
				CacheTree subtree = child.cacheTree;
				PathEvaluation eval;
				if(subtree != null){
					board.makePlayerMove(move);
					eval = opponentEvaluation(board, subtree, 1, recursions, alpha, beta);
					board.undoPlayerMove(move);
				} else {
					board.makePlayerMove(move);
					CacheTreeEdge edge = makeOpponentEdge(board, 1, recursions, alpha, beta, null);
					board.undoPlayerMove(move);
					subtree = edge.cacheTreeChild.cacheTree;
					eval = edge.evaluation;
				}
				
				if(bestEval.compare(eval) < 0){
					bestEval = eval;
					bestMove = move;
					bestSubtree = subtree;
					if(alpha.compare(bestEval) < 0){
						alpha = bestEval;
					}
				}
			}
			if(bestMove == null) bestMove = Move.UP;
			currentTree = bestSubtree;
		}
		
		Stack<Move> s = new Stack<>();
		s.addElement(bestMove);
		return s;
	}
}
