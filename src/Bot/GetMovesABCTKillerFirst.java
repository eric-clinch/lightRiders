package Bot;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;

public class GetMovesABCTKillerFirst implements GetMoves{
	
	private final Evaluator evaluator;
	private final GetSearchNumber getSearchDepth;
	private static final AscendingChildrenSorter ascendingChildrenSorter = new AscendingChildrenSorter();
	private static final DescendingChildrenSorter descendingChildrenSorter = new DescendingChildrenSorter();
	private final AscendingMoveSorter ascendingMoveSorter = new AscendingMoveSorter();
	private final DescendingMoveSorter descendingMoveSorter = new DescendingMoveSorter();
	private static final int sortThreshold = 3;
	private static final int moveSortThreshold = 3;
	public CacheTree currentTree;
	
	public GetMovesABCTKillerFirst(Evaluator evaluator, GetSearchNumber getSearchDepth){
		this.evaluator = evaluator;
		this.getSearchDepth = getSearchDepth;
		this.currentTree = null;
	}
	
	private static final class CacheTreeChild {
		public Move move;
		public CacheTree cacheTree;
		
		public CacheTreeChild(Move move, CacheTree cacheTree){
			this.move = move;
			this.cacheTree = cacheTree;
		}
	}
	
	private static final class CacheTreeEdge {
		public CacheTreeChild cacheTreeChild;
		public PathEvaluation evaluation;
		
		public CacheTreeEdge(CacheTreeChild cacheTreeChild, PathEvaluation evaluation){
			this.cacheTreeChild = cacheTreeChild;
			this.evaluation = evaluation;
		}
	}

	
	private static final class CacheTree {
		public CacheTreeChild[] children;
		
		public CacheTree(CacheTreeChild[] children) {
			this.children = children;
		}
		
		public CacheTree getChild(Move move){
			for(CacheTreeChild child : children){
				if(child.move == move) return child.cacheTree;
			}
			assert(false);
			return null;
		}
	}
	
	private static final class Edge {
		public Move move;
		public int evaluation;
		
		public Edge(Move move, int evaluation){
			this.move = move;
			this.evaluation = evaluation;
		}
	}
	
	private static final class DescendingMoveSorter implements Comparator<Edge>{		
		public int compare(Edge edge1, Edge edge2){
			return edge2.evaluation - edge1.evaluation;
		}
	}
	
	private static final class AscendingMoveSorter implements Comparator<Edge>{		
		public int compare(Edge edge1, Edge edge2){
			return edge1.evaluation - edge2.evaluation;
		}
	}
	
	public final void descendingSortMoves(Move[] moves, Board board){
		Edge[] edges = new Edge[moves.length];
		
		for(int i = 0; i < moves.length; i++){
			Move move = moves[i];
			board.makePlayerMove(move);
			edges[i] = new Edge(move, evaluator.evaluate(board));
			board.undoPlayerMove(move);
		}
		
		Arrays.sort(edges, descendingMoveSorter);
		for(int i = 0; i < moves.length; i++){
			moves[i] = edges[i].move;
		}
	}
	
	public final void ascendingSortMoves(Move[] moves, Board board){
		Edge[] edges = new Edge[moves.length];
		
		for(int i = 0; i < moves.length; i++){
			Move move = moves[i];
			board.makeOpponentMove(move);
			edges[i] = new Edge(move, evaluator.evaluate(board));
			board.undoOpponentMove(move);
		}
		
		Arrays.sort(edges, ascendingMoveSorter);
		for(int i = 0; i < moves.length; i++){
			moves[i] = edges[i].move;
		}
	}
	
	private static final class DescendingChildrenSorter implements Comparator<CacheTreeEdge>{		
		public int compare(CacheTreeEdge edge1, CacheTreeEdge edge2){
			return edge2.evaluation.compare(edge1.evaluation);
		}
	}
	
	private static final class AscendingChildrenSorter implements Comparator<CacheTreeEdge>{		
		public int compare(CacheTreeEdge edge1, CacheTreeEdge edge2){
			return edge1.evaluation.compare(edge2.evaluation);
		}
	}
	
	public static final CacheTreeChild[] descendingSortChildren(CacheTreeEdge[] edges){
		Arrays.sort(edges, descendingChildrenSorter);
		int len = edges.length;
		CacheTreeChild[] res = new CacheTreeChild[len];
		for(int i = 0; i < len; i++){
			res[i] = edges[i].cacheTreeChild;
		}
		return res;
	}
	
	public static final CacheTreeChild[] ascendingSortChildren(CacheTreeEdge[] edges){
		Arrays.sort(edges, ascendingChildrenSorter);
		int len = edges.length;
		CacheTreeChild[] res = new CacheTreeChild[len];
		for(int i = 0; i < len; i++){
			res[i] = edges[i].cacheTreeChild;
		}
		return res;
	}
	
	public static final void placeFirst(Object[] array, int putFirst){
		if(putFirst == 0) return;
		Object temp = array[putFirst];
		array[putFirst] = array[0];
		array[0] = temp;
	}
	
	public static void placeFirstIfExists(Object[] xs, Object x){
	    Object movingItem = xs[0];
	    if(movingItem == x) return;
	    Object temp = movingItem;
	    int len = xs.length;
	    for(int i = 1; i < len; i++){
	        temp = xs[i];
	        if(temp == x){
	            xs[0] = x;
	            xs[i] = movingItem;
	            return;
	        } else {
	            xs[i] = movingItem;
	            movingItem = temp;
	        }
	    }
	    xs[0] = movingItem; // this only happens if x is not in the list
	}
	
	public static void placeChildFirstIfExists(CacheTreeChild[] children, Move move){
		CacheTreeChild movingItem = children[0];
	    if(movingItem.move == move) return;
	    CacheTreeChild temp = movingItem;
	    int len = children.length;
	    for(int i = 1; i < len; i++){
	        temp = children[i];
	        if(temp.move == move){
	            children[0] = temp;
	            children[i] = movingItem;
	            return;
	        } else {
	            children[i] = movingItem;
	            movingItem = temp;
	        }
	    }
	    children[0] = movingItem; // this only happens if x is not in the list
	}
	
	private static final class PathEvaluation{
		int evaluation;
		int searchHeight; //represents how far from the bottom of the search tree the evaluation
		Move killerMove;
		
		public PathEvaluation(int evaluation, int searchHeight){
			this.evaluation = evaluation;
			this.searchHeight = searchHeight;
			this.killerMove = null;
		}
		
		public int compare(PathEvaluation other){
			if(this.evaluation > other.evaluation) return 1;
			else if(this.evaluation < other.evaluation) return -1;
			
			else if(this.searchHeight > other.searchHeight) return 1;
			else if(this.searchHeight < other.searchHeight) return -1;
			return 0;
		}
	}
	
	private final PathEvaluation playerEvaluation(Board board, CacheTree thisTree, int depth, int maxDepth, PathEvaluation alpha, PathEvaluation beta, Move startMove){
		assert(thisTree != null);
		assert(depth % 2 == 0);
		if(depth == maxDepth) return new PathEvaluation(evaluator.evaluate(board), depth);
		
		PathEvaluation maxVal = new PathEvaluation(Integer.MIN_VALUE, depth);
		CacheTreeEdge[] edges = null;
		Move killerMove = startMove;
		Move childrenKillerMove = null;
		
		boolean shouldSort = depth == sortThreshold || depth == sortThreshold - 1;
		
		if(thisTree.children == null){
			Move[] legalMoves = board.getLegalMovesForPlayer();
			int legalMovesLength = legalMoves.length;
			CacheTreeChild[] children = new CacheTreeChild[legalMovesLength];
			if(legalMovesLength == 0){
				thisTree.children = children;
				if(board.getLegalMovesForOpponent().length == 0) maxVal.evaluation = 0; // tie
				maxVal.killerMove = killerMove;
				return maxVal;
			}
			if(killerMove != null && legalMovesLength > 0) placeFirstIfExists(legalMoves, killerMove);
			if(depth < moveSortThreshold) descendingSortMoves(legalMoves, board);
			if(shouldSort) edges = new CacheTreeEdge[legalMovesLength];
			
			for(int i = 0; i < legalMovesLength; i++){
				Move move = legalMoves[i];
				board.makePlayerMove(move);
				CacheTreeEdge edge = makeOpponentEdge(board, depth + 1, maxDepth, alpha, beta, move, childrenKillerMove);
				board.undoPlayerMove(move);
				
				PathEvaluation moveEvaluation = edge.evaluation;
				childrenKillerMove = moveEvaluation.killerMove;
				if(shouldSort) edges[i] = edge;
				children[i] = edge.cacheTreeChild;
				if(maxVal.compare(moveEvaluation) < 0){
					maxVal = moveEvaluation;
					if(alpha.compare(maxVal) < 0){
						alpha = maxVal;
						if(beta.compare(alpha) <= 0){
							for(int j = i + 1; j < legalMovesLength; j++) children[j] = new CacheTreeChild(legalMoves[j], null);
							placeFirst(children, i); // place the killer move first
							killerMove = move;
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
			if(numChildren == 0){
				if(board.getLegalMovesForOpponent().length == 0) maxVal.evaluation = 0; // tie
				maxVal.killerMove = killerMove;
				return maxVal;
			}
			if(killerMove != null && numChildren > 0) placeChildFirstIfExists(children, killerMove);
			if(shouldSort) edges = new CacheTreeEdge[numChildren];
			for(int i = 0; i < numChildren; i++){
				CacheTreeChild child = children[i];
				Move move = child.move;
				PathEvaluation moveEvaluation;
				
				if(child.cacheTree == null){
					board.makePlayerMove(move);
					CacheTreeEdge edge = makeOpponentEdge(board, depth + 1, maxDepth, alpha, beta, move, childrenKillerMove);
					board.undoPlayerMove(move);
					
					moveEvaluation = edge.evaluation;
					childrenKillerMove = moveEvaluation.killerMove;
					child.cacheTree = edge.cacheTreeChild.cacheTree;
					if(shouldSort) edges[i] = edge;
				} else{
					board.makePlayerMove(move);
					moveEvaluation = opponentEvaluation(board, child.cacheTree, depth + 1, maxDepth, alpha, beta, childrenKillerMove);
					board.undoPlayerMove(move);
					childrenKillerMove = moveEvaluation.killerMove;
					if(shouldSort) edges[i] = new CacheTreeEdge(child, moveEvaluation);
				}
				
				if(maxVal.compare(moveEvaluation) < 0){
					maxVal = moveEvaluation;
					if(alpha.compare(maxVal) < 0){
						alpha = maxVal;
						if(beta.compare(alpha) <= 0) {
							placeFirst(children, i); // place the killer move first
							killerMove = move;
							if(shouldSort) for(int j = i + 1; j < numChildren; j++) edges[j] = new CacheTreeEdge(children[j], new PathEvaluation(Integer.MIN_VALUE, depth));
							break; // prune
						}
					}
				}
			}
		}
		if(shouldSort) thisTree.children = descendingSortChildren(edges);
		maxVal.killerMove = killerMove;
		return maxVal;
	}
	
	private final PathEvaluation opponentEvaluation(Board board, CacheTree thisTree, int depth, int maxDepth, PathEvaluation alpha, PathEvaluation beta, Move startMove){
		if(depth == maxDepth) return new PathEvaluation(evaluator.evaluate(board), depth);
		
		PathEvaluation minVal = new PathEvaluation(Integer.MAX_VALUE, depth);
		CacheTreeEdge[] edges = null;
		Move killerMove = startMove;
		Move childrenKillerMove = null;
		
		boolean shouldSort = depth == sortThreshold || depth == sortThreshold - 1;
		
		if(thisTree.children == null){
			Move[] legalMoves = board.getLegalMovesForOpponent();
			int legalMovesLength = legalMoves.length;
			CacheTreeChild[] children = new CacheTreeChild[legalMovesLength];
			if(killerMove != null && legalMovesLength > 0) placeFirstIfExists(legalMoves, killerMove);
			if(depth < moveSortThreshold) ascendingSortMoves(legalMoves, board);
			if(shouldSort) edges = new CacheTreeEdge[legalMovesLength];
			for(int i = 0; i < legalMovesLength; i++){
				Move move = legalMoves[i];
				board.makeOpponentMove(move);
				CacheTreeEdge edge = makePlayerEdge(board, depth + 1, maxDepth, alpha, beta, move, childrenKillerMove);
				board.undoOpponentMove(move);
				
				PathEvaluation moveEvaluation = edge.evaluation;
				childrenKillerMove = moveEvaluation.killerMove;
				children[i] = edge.cacheTreeChild;
				if(shouldSort) edges[i] = edge;
				if(minVal.compare(moveEvaluation) > 0){
					minVal = moveEvaluation;
					if(beta.compare(minVal) > 0){
						beta = minVal;
						if(beta.compare(alpha) <= 0){
							for(int j = i + 1; j < legalMovesLength; j++) children[j] = new CacheTreeChild(legalMoves[j], null);
							placeFirst(children, i); // place the killer move first
							killerMove = move;
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
			if(killerMove != null && numChildren > 0) placeChildFirstIfExists(children, killerMove);
			if(shouldSort) edges = new CacheTreeEdge[numChildren];
			
			for(int i = 0; i < numChildren; i++){
				CacheTreeChild child = children[i];
				Move move = child.move;
				PathEvaluation moveEvaluation;
				
				if(child.cacheTree == null){
					board.makeOpponentMove(move);
					CacheTreeEdge edge = makePlayerEdge(board, depth + 1, maxDepth, alpha, beta, move, childrenKillerMove);
					board.undoOpponentMove(move);
					
					child.cacheTree = edge.cacheTreeChild.cacheTree;
					moveEvaluation = edge.evaluation;
					childrenKillerMove = moveEvaluation.killerMove;
					if(shouldSort) edges[i] = edge;
				} else{
					board.makeOpponentMove(move);
					moveEvaluation = playerEvaluation(board, child.cacheTree, depth + 1, maxDepth, alpha, beta, childrenKillerMove);
					board.undoOpponentMove(move);
					childrenKillerMove = moveEvaluation.killerMove;
					if(shouldSort) edges[i] = new CacheTreeEdge(child, moveEvaluation);
				}
				
				if(minVal.compare(moveEvaluation) > 0){
					minVal = moveEvaluation;
					if(beta.compare(minVal) > 0){
						beta = minVal;
						if(beta.compare(alpha) <= 0) {
							placeFirst(children, i); // place the killer move first
							killerMove = move;
							if(shouldSort) for(int j = i + 1; j < numChildren; j++) edges[j] = new CacheTreeEdge(children[j], new PathEvaluation(Integer.MAX_VALUE, depth));
							break; // prune
						}
					}
				}
			}
		}
		if(shouldSort) thisTree.children = ascendingSortChildren(edges);
		minVal.killerMove = killerMove;
		return minVal;
	}
	
	private final CacheTreeEdge makePlayerEdge(Board board, int depth, int maxDepth, PathEvaluation alpha, PathEvaluation beta, Move lastMove, Move startMove){
		if(depth == maxDepth){
			CacheTree tree = new CacheTree(null);
			CacheTreeChild child = new CacheTreeChild(lastMove, tree);
			PathEvaluation evaluation = new PathEvaluation(evaluator.evaluate(board), depth);
			return new CacheTreeEdge(child, evaluation); 
		}

		CacheTreeEdge[] edges = null;
		PathEvaluation maxVal = new PathEvaluation(Integer.MIN_VALUE, depth);
		
		Move[] legalMoves = board.getLegalMovesForPlayer();
		int legalMovesLength = legalMoves.length;
		if(legalMovesLength == 0 && board.getLegalMovesForOpponent().length == 0) maxVal.evaluation = 0; // tie
		Move killerMove = startMove;
		if(killerMove != null && legalMovesLength > 0) placeFirstIfExists(legalMoves, killerMove);
		Move childrenKillerMove = null;
		if(depth < moveSortThreshold) descendingSortMoves(legalMoves, board);
		CacheTreeChild[] children = new CacheTreeChild[legalMovesLength];
		
		boolean shouldSort = depth == sortThreshold || depth == sortThreshold - 1;
		if(shouldSort) edges = new CacheTreeEdge[legalMovesLength];
		
		for(int i = 0; i < legalMovesLength; i++){
			Move move = legalMoves[i];
			board.makePlayerMove(move);
			CacheTreeEdge edge = makeOpponentEdge(board, depth + 1, maxDepth, alpha, beta, move, childrenKillerMove);
			board.undoPlayerMove(move);
			
			PathEvaluation moveEvaluation = edge.evaluation;
			childrenKillerMove = moveEvaluation.killerMove;
			children[i] = edge.cacheTreeChild;
			if(shouldSort) edges[i] = edge;
			
			if(maxVal.compare(moveEvaluation) < 0){
				maxVal = moveEvaluation;
				if(alpha.compare(maxVal) < 0){
					alpha = maxVal;
					if(beta.compare(alpha) <= 0){
						for(int j = i + 1; j < legalMovesLength; j++) children[j] = new CacheTreeChild(legalMoves[j], null);
						placeFirst(children, i); // place the killer move first
						killerMove = move;
						if(shouldSort) for(int j = i + 1; j < legalMovesLength; j++) edges[j] = new CacheTreeEdge(children[j], new PathEvaluation(Integer.MIN_VALUE, depth));
						break; // prune
					}
				}
			}
		}
		for(CacheTreeChild c : children) assert(c.move != null);
		if(shouldSort) children = descendingSortChildren(edges);
		CacheTree tree = new CacheTree(children);
		CacheTreeChild child = new CacheTreeChild(lastMove, tree);
		maxVal.killerMove = killerMove;
		return new CacheTreeEdge(child, maxVal); 
	}
	
	private final CacheTreeEdge makeOpponentEdge(Board board, int depth, int maxDepth, PathEvaluation alpha, PathEvaluation beta, Move lastMove, Move startMove){
		if(depth == maxDepth){
			CacheTree tree = new CacheTree(null);
			CacheTreeChild child = new CacheTreeChild(lastMove, tree);
			PathEvaluation evaluation = new PathEvaluation(evaluator.evaluate(board), depth);
			return new CacheTreeEdge(child, evaluation); 
		}
		
		CacheTreeEdge[] edges = null;
		PathEvaluation minVal = new PathEvaluation(Integer.MAX_VALUE, depth);
		
		Move[] legalMoves = board.getLegalMovesForOpponent();
		int legalMovesLength = legalMoves.length;
		Move killerMove = lastMove;
		if(killerMove != null && legalMovesLength > 0) placeFirstIfExists(legalMoves, killerMove);
		Move childrenKillerMove = null;
		if(depth < moveSortThreshold) ascendingSortMoves(legalMoves, board);
		CacheTreeChild[] children = new CacheTreeChild[legalMovesLength];
		
		boolean shouldSort = depth == sortThreshold || depth == sortThreshold - 1;
		if(shouldSort) edges = new CacheTreeEdge[legalMovesLength];
		
		for(int i = 0; i < legalMovesLength; i++){
			Move move = legalMoves[i];
			board.makeOpponentMove(move);
			CacheTreeEdge edge = makePlayerEdge(board, depth + 1, maxDepth, alpha, beta, move, childrenKillerMove);
			board.undoOpponentMove(move);
			
			PathEvaluation moveEvaluation = edge.evaluation;
			childrenKillerMove = moveEvaluation.killerMove;
			children[i] = edge.cacheTreeChild;
			if(shouldSort) edges[i] = edge;
			
			if(minVal.compare(moveEvaluation) > 0){
				minVal = moveEvaluation;
				if(beta.compare(minVal) > 0){
					beta = minVal;
					if(beta.compare(alpha) <= 0){ 
						for(int j = i + 1; j < legalMovesLength; j++) children[j] = new CacheTreeChild(legalMoves[j], null);
						placeFirst(children, i); // place the killer move first
						killerMove = move;
						if(shouldSort) for(int j = i + 1; j < legalMovesLength; j++) edges[j] = new CacheTreeEdge(children[j], new PathEvaluation(Integer.MAX_VALUE, depth));
						break; // prune
					}
				}
			}
		}
		for(CacheTreeChild c : children) assert(c.move != null);
		if(shouldSort) children = ascendingSortChildren(edges);
		CacheTree tree = new CacheTree(children);
		CacheTreeChild child = new CacheTreeChild(lastMove, tree);
		minVal.killerMove = killerMove;
		return new CacheTreeEdge(child, minVal);
	}
	
	public final Stack<Move> getPlayerMoves(Board board, int time, int round, Move lastOpponentMove){
		System.err.print("round " + round + " ");
		Move bestMove;

		int moveDepthToSearch = getSearchDepth.apply(time, round, 0);
		System.err.println("searching with depth " + Integer.toString(moveDepthToSearch));
		
		int recursions = 2 * moveDepthToSearch;
		
		PathEvaluation alpha = new PathEvaluation(Integer.MIN_VALUE, Integer.MIN_VALUE);
		PathEvaluation beta = new PathEvaluation(Integer.MAX_VALUE, Integer.MAX_VALUE);
		
		Move childrenKillerMove = null;
		
		if(currentTree == null){
			bestMove = Move.UP;
			PathEvaluation bestEval = new PathEvaluation(Integer.MIN_VALUE, Integer.MIN_VALUE);
			CacheTree bestChildTree = null;
			Move[] legalMoves = board.getLegalMovesForPlayer();
			descendingSortMoves(legalMoves, board);
			for(Move move : legalMoves){
				board.makePlayerMove(move);
				CacheTreeEdge edge = makeOpponentEdge(board, 1, recursions, alpha, beta, null, childrenKillerMove);
				board.undoPlayerMove(move);
				
				PathEvaluation eval = edge.evaluation;
				childrenKillerMove = eval.killerMove;
				
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
					eval = opponentEvaluation(board, subtree, 1, recursions, alpha, beta, childrenKillerMove);
					board.undoPlayerMove(move);
				} else {
					board.makePlayerMove(move);
					CacheTreeEdge edge = makeOpponentEdge(board, 1, recursions, alpha, beta, null, childrenKillerMove);
					board.undoPlayerMove(move);
					subtree = edge.cacheTreeChild.cacheTree;
					eval = edge.evaluation;
				}
				childrenKillerMove = eval.killerMove;
				
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
