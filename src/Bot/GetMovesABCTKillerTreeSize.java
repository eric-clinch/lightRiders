package Bot;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;

public class GetMovesABCTKillerTreeSize implements GetMoves{
	
	private final Evaluator evaluator;
	private final GetSearchNumberWithTreeSize getSearchDepth;
	private static final AscendingChildrenSorter ascendingChildrenSorter = new AscendingChildrenSorter();
	private static final DescendingChildrenSorter descendingChildrenSorter = new DescendingChildrenSorter();
	private final AscendingMoveSorter ascendingMoveSorter = new AscendingMoveSorter();
	private final DescendingMoveSorter descendingMoveSorter = new DescendingMoveSorter();
	private static final int sortThreshold = 3;
	private static final int moveSortThreshold = 3;
	public CacheTree currentTree;
	
	public GetMovesABCTKillerTreeSize(Evaluator evaluator, GetSearchNumberWithTreeSize getSearchDepth){
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
		
		public final CacheTree getChild(Move move){
			for(CacheTreeChild child : children){
				if(child.move == move) return child.cacheTree;
			}
			assert(false);
			return null;
		}
		
		public final int getSize(){
			if(children == null) return 1;
			
			int leafs = 0;
			for(CacheTreeChild child : children){
				CacheTree childTree = child.cacheTree;
				if(childTree != null){ 
					leafs += child.cacheTree.getSize();
				}
			}
			return leafs;
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
	
	private final class DescendingMoveSorter implements Comparator<Edge>{		
		public int compare(Edge edge1, Edge edge2){
			return edge2.evaluation - edge1.evaluation;
		}
	}
	
	private final class AscendingMoveSorter implements Comparator<Edge>{		
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
		for(int i = putFirst; i > 0; i--){
			array[i] = array[i - 1];
		}
		array[0] = temp;
	}
	
	private static final class PathEvaluation{
		int evaluation;
		int searchHeight; //represents how far from the bottom of the search tree the evaluation
		
		public PathEvaluation(int evaluation, int searchHeight){
			this.evaluation = evaluation;
			this.searchHeight = searchHeight;
		}
		
		public final int compare(PathEvaluation other){
			if(this.evaluation > other.evaluation) return 1;
			else if(this.evaluation < other.evaluation) return -1;
			
			else if(this.searchHeight > other.searchHeight) return 1;
			else if(this.searchHeight < other.searchHeight) return -1;
			return 0;
		}
	}
	
	private final PathEvaluation playerEvaluation(Board board, CacheTree thisTree, int depth, int maxDepth, PathEvaluation alpha, PathEvaluation beta){
		assert(thisTree != null);
		assert(depth % 2 == 0);
		if(depth == maxDepth) return new PathEvaluation(evaluator.evaluate(board), depth);
		
		PathEvaluation maxVal = new PathEvaluation(Integer.MIN_VALUE, depth);
		CacheTreeEdge[] edges = null;
		boolean shouldSort = depth == sortThreshold || depth == sortThreshold - 1;
		
		if(thisTree.children == null){
			Move[] legalMoves = board.getLegalMovesForPlayer();
			if(depth < moveSortThreshold) descendingSortMoves(legalMoves, board);
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
							placeFirst(children, i); // place the killer move first
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
							placeFirst(children, i); // place the killer move first
							if(shouldSort) for(int j = i + 1; j < numChildren; j++) edges[j] = new CacheTreeEdge(children[j], new PathEvaluation(Integer.MIN_VALUE, depth));
							break; // prune
						}
					}
				}
			}
		}
		if(shouldSort) thisTree.children = descendingSortChildren(edges);
		return maxVal;
	}
	
	private final PathEvaluation opponentEvaluation(Board board, CacheTree thisTree, int depth, int maxDepth, PathEvaluation alpha, PathEvaluation beta){
		assert(thisTree != null);
		assert(depth % 2 == 1);
		if(depth == maxDepth) return new PathEvaluation(evaluator.evaluate(board), depth);
		
		PathEvaluation minVal = new PathEvaluation(Integer.MAX_VALUE, depth);
		CacheTreeEdge[] edges = null;
		boolean shouldSort = depth == sortThreshold || depth == sortThreshold - 1;
		
		if(thisTree.children == null){
			Move[] legalMoves = board.getLegalMovesForOpponent();
			if(depth < moveSortThreshold) ascendingSortMoves(legalMoves, board);
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
							placeFirst(children, i); // place the killer move first
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
							placeFirst(children, i); // place the killer move first
							if(shouldSort) for(int j = i + 1; j < numChildren; j++) edges[j] = new CacheTreeEdge(children[j], new PathEvaluation(Integer.MAX_VALUE, depth));
							break; // prune
						}
					}
				}
			}
		}
		if(shouldSort) thisTree.children = ascendingSortChildren(edges);
		return minVal;
	}
	
	private final CacheTreeEdge makePlayerEdge(Board board, int depth, int maxDepth, PathEvaluation alpha, PathEvaluation beta, Move lastMove){
		assert(depth % 2 == 0);
		if(depth == maxDepth){
			CacheTree tree = new CacheTree(null);
			CacheTreeChild child = new CacheTreeChild(lastMove, tree);
			PathEvaluation evaluation = new PathEvaluation(evaluator.evaluate(board), depth);
			return new CacheTreeEdge(child, evaluation); 
		}
		
		boolean shouldSort = depth == sortThreshold || depth == sortThreshold - 1;
		CacheTreeEdge[] edges = null;
		
		PathEvaluation maxVal = new PathEvaluation(Integer.MIN_VALUE, depth);
		Move[] legalMoves = board.getLegalMovesForPlayer();
		if(depth < moveSortThreshold) descendingSortMoves(legalMoves, board);
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
						placeFirst(children, i); // place the killer move first
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
		return new CacheTreeEdge(child, maxVal); 
	}
	
	private final CacheTreeEdge makeOpponentEdge(Board board, int depth, int maxDepth, PathEvaluation alpha, PathEvaluation beta, Move lastMove){
		assert(depth % 2 == 1);
		if(depth == maxDepth){
			CacheTree tree = new CacheTree(null);
			CacheTreeChild child = new CacheTreeChild(lastMove, tree);
			PathEvaluation evaluation = new PathEvaluation(evaluator.evaluate(board), depth);
			return new CacheTreeEdge(child, evaluation); 
		}
		
		boolean shouldSort = depth == sortThreshold || depth == sortThreshold - 1;
		CacheTreeEdge[] edges = null;
		
		PathEvaluation minVal = new PathEvaluation(Integer.MAX_VALUE, depth);
		Move[] legalMoves = board.getLegalMovesForOpponent();
		if(depth < moveSortThreshold) ascendingSortMoves(legalMoves, board);
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
						placeFirst(children, i); // place the killer move first
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
		return new CacheTreeEdge(child, minVal); 
	}
	
	public final Stack<Move> getPlayerMoves(Board board, int time, int round, Move lastOpponentMove){
		System.err.print("round " + round + " ");
		Move bestMove;

		PathEvaluation alpha = new PathEvaluation(Integer.MIN_VALUE, Integer.MIN_VALUE);
		PathEvaluation beta = new PathEvaluation(Integer.MAX_VALUE, Integer.MAX_VALUE);
		
		if(currentTree == null){
			int moveDepthToSearch = getSearchDepth.apply(time, round, 0);
			System.err.println("searching with depth " + Integer.toString(moveDepthToSearch));
			int recursions = 2 * moveDepthToSearch;
			
			bestMove = Move.UP;
			PathEvaluation bestEval = new PathEvaluation(Integer.MIN_VALUE, Integer.MIN_VALUE);
			CacheTree bestChildTree = null;
			Move[] legalMoves = board.getLegalMovesForPlayer();
			descendingSortMoves(legalMoves, board);
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
			getSearchDepth.setTreeLeafs(currentTree.getSize());
			
			int moveDepthToSearch = getSearchDepth.apply(time, round, 0);
			System.err.println("searching with depth " + Integer.toString(moveDepthToSearch));
			int recursions = 2 * moveDepthToSearch;
			
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
