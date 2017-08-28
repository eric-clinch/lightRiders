package Bot;

import java.util.ArrayList;
import java.util.Random;

public class simulateGame {
	
	public static void printBoards(ArrayList<Board> boards){
		for(Board board : boards){
			System.out.println(board.toString());
		}
	}
	
	public class simulationResult{
		
		public ArrayList<Board> boards;
		public int winner;
		public ArrayList<Move> bot0Moves;
		public ArrayList<Move> bot1Moves;
		public boolean bot0TimedOut;
		public boolean bot1TimedOut;
		
		public simulationResult(ArrayList<Board> boards, ArrayList<Move> bot0Moves, ArrayList<Move> bot1Moves, int winner, boolean bot0TimedOut, boolean bot1TimedOut){
			this.boards = boards;
			this.bot0Moves = bot0Moves;
			this.bot1Moves = bot1Moves;
			this.winner = winner;
			this.bot0TimedOut = bot0TimedOut;
			this.bot1TimedOut = bot1TimedOut;
		}
	}
	
	public simulationResult playMatch(Bot bot0, Bot bot1, int bot0StartRow, int bot0StartCol){
		ArrayList<Board> boards = new ArrayList<Board>();
		ArrayList<Move> bot0Moves = new ArrayList<Move>();
		ArrayList<Move> bot1Moves = new ArrayList<Move>();
		
		long timePerMovePlayer = 200; // milliseconds
		long timePerMoveOpponent = 200;
		double timeFudge = .5; //used for matching the speed of the competition servers
		int playerRow = bot0StartRow;
		int playerCol = bot0StartCol;
		long playerTime = 10000;
		int opponentRow = playerRow;
		int opponentCol = Board.cols - playerCol - 1;
		long opponentTime = 10000;
		int round = 0;
		Board board = new PieceBoard(16, 16, playerRow, playerCol, opponentRow, opponentCol);
		boards.add(board.deepcopy());
		
		long time = System.currentTimeMillis();
		Move bot0Move = bot0.getMove(board, (int) playerTime, round, Move.UP);
		Move lastBot0Move = bot0Move;
		long elapsed = (long) (timeFudge * (double) (System.currentTimeMillis() - time));
		playerTime -= elapsed;
		System.out.println(round + " bot0 time: " + playerTime);
		
		time = System.currentTimeMillis();
		Move bot1Move = bot1.getMove(board.toOpponentBoard(), (int) opponentTime, round, Move.UP);
		Move lastBot1Move = bot1Move;
		elapsed = (long) (timeFudge * (double) (System.currentTimeMillis() - time));
		opponentTime -= elapsed;
		System.out.println(round + " bot1 time: " + opponentTime);
		
		int bot0Faster = 0;
		int bot1Faster = 0;
		
		while(board.isLegalMoveForPlayer(bot0Move) && board.isLegalMoveForOpponent(bot1Move)){			
			board.makePlayerMove(bot0Move);
			board.makeOpponentMove(bot1Move);
			playerTime = Math.min(playerTime + timePerMovePlayer, 10000);
			opponentTime = Math.min(opponentTime + timePerMoveOpponent, 10000);
			
			round++;
			time = System.currentTimeMillis();
			bot0Move = bot0.getMove(board, (int) playerTime, round, lastBot1Move);
			elapsed = (long) (timeFudge * (double) (System.currentTimeMillis() - time));
			playerTime -= elapsed;
			System.out.println(round + " bot0 time: " + playerTime);
			
			time = System.currentTimeMillis();
			bot1Move = bot1.getMove(board.toOpponentBoard(), (int) opponentTime, round, lastBot0Move);
			elapsed = (long) (timeFudge * (double) (System.currentTimeMillis() - time));
			opponentTime -= elapsed;
			System.out.println(round + " bot1 time: " + opponentTime);
			
			boards.add(board.deepcopy());
			bot0Moves.add(bot0Move);
			bot1Moves.add(bot1Move);
			lastBot0Move = bot0Move;
			lastBot1Move = bot1Move;
			
			if(playerTime > opponentTime) bot0Faster++;
			else bot1Faster++;
			
			if(playerTime < 0 || opponentTime < 0){
				int winner;
				if(playerTime < 0 && opponentTime < 0) winner = -1;
				else if(playerTime < 0) winner = 1;
				else winner = 0;
				return new simulationResult(boards, bot0Moves, bot1Moves, winner, playerTime < 0, opponentTime < 0);
			}
		}
		
		System.out.println(bot0Faster + " : " + bot1Faster);
		
		int winner;
		if(!board.isLegalMoveForPlayer(bot0Move) && !board.isLegalMoveForOpponent(bot1Move)) winner = -1; //tie
		else if(!board.isLegalMoveForOpponent(bot1Move)) winner = 0;
		else winner = 1;
		
		return new simulationResult(boards, bot0Moves, bot1Moves, winner, false, false);
	}
	
	// has the two bots returned by botFactory() play a match in every legal starting position and prints the results of these matches
	public static void playTournament() {
		int gamesPlayed = 0;
		int gamesWonByBot0 = 0;
		int gamesWonByBot1 = 0;
		int gamesTimedOutByBot0 = 0;
		int gamesTimedOutByBot1 = 0;
		int gamesTied = 0;
		for(int row = 1; row < 15; row++) for (int col = 1; col < 7; col++){
			Bot[] bots = botFactory();
			Bot bot0 = bots[0];
			Bot bot1 = bots[1];
			simulationResult res = (new simulateGame()).playMatch(bot0, bot1, row, col);
			
			if(res.winner == -1) gamesTied++;
			else if(res.winner == 1) gamesWonByBot1++;
			else gamesWonByBot0++;
			if(res.bot0TimedOut) gamesTimedOutByBot0++;
			if(res.bot1TimedOut) gamesTimedOutByBot1++;
			gamesPlayed++;
			
			//play the mirrored match
			bots = botFactory();
			bot0 = bots[0];
			bot1 = bots[1];
			res = (new simulateGame()).playMatch(bot1, bot0, row, col);
			
			if(res.winner == -1) gamesTied++;
			else if(res.winner == 0) gamesWonByBot1++;
			else gamesWonByBot0++;
			if(res.bot0TimedOut) gamesTimedOutByBot1++;
			if(res.bot1TimedOut) gamesTimedOutByBot0++;
			gamesPlayed++;
		}
		System.out.println("Games played: " + gamesPlayed);
		System.out.println("Games won by bot 0: " + gamesWonByBot0);
		System.out.println("Games timed out by bot 0: " + gamesTimedOutByBot0);
		System.out.println("Games won by bot 1: " + gamesWonByBot1);
		System.out.println("Games timed out by bot 1: " + gamesTimedOutByBot1);
		System.out.println("Games tied: " + gamesTied);
	}
	
	// used for fine tuning parameterized strategies
	// has various instances of a parameterized bot play against a control bot
	public static void playVariationTournament() {
		ArrayList<tournamentResult> results = new ArrayList<tournamentResult>();
		Random random = new Random();
		for(int i = 2290; i <= 2710; i+=30){
			int threshold = i;
			int gamesToPlay = 100;
			int gamesWonByBot1 = 0;
			int gamesWonByBot0 = 0;
			int gamesTied = 0;
			int gamesTimedOutByBot0 = 0;
			int gamesTimedOutByBot1 = 0;
			for(int round = 0; round < gamesToPlay; round++){
				Bot bot0 = new Bot(new GetMovesABCTKillerPrune(new ChamberEvaluator(), new bot0Depth(), 81), new GetMovesEndGameBacktrack(new FloodEndGameEvaluator(), 14));
				Bot bot1 = new Bot(new GetMovesABCTKillerFirst(new ChamberEvaluator(), new bot0Depth()), new GetMovesEndGameBacktrack(new FloodEndGameEvaluator(), 14));
				int row = random.nextInt(Board.rows - 2) + 1;
				int col = random.nextInt((Board.cols / 2) - 2) + 1;
				
				if(round % 2 == 0){
					simulationResult res = (new simulateGame()).playMatch(bot0, bot1, row, col);
					
					if(res.winner == -1) gamesTied++;
					else if(res.winner == 1) gamesWonByBot1++;
					else gamesWonByBot0++;
					if(res.bot0TimedOut) gamesTimedOutByBot0++;
					if(res.bot1TimedOut) gamesTimedOutByBot1++;
				} else {
					simulationResult res = (new simulateGame()).playMatch(bot1, bot0, row, col);
					
					if(res.winner == -1) gamesTied++;
					else if(res.winner == 0) gamesWonByBot1++;
					else gamesWonByBot0++;
					if(res.bot0TimedOut) gamesTimedOutByBot1++;
					if(res.bot1TimedOut) gamesTimedOutByBot0++;
				}
			}
			tournamentResult tres = new tournamentResult(gamesToPlay, gamesWonByBot0, gamesWonByBot1, gamesTimedOutByBot0, gamesTimedOutByBot1, gamesTied, threshold);
			results.add(tres);
		}
		for(tournamentResult tres : results){
			System.out.println("Games played: " + tres.gamesPlayed + " bot 0: " + tres.gamesWonByPlayer0 + " bot 0 timeouts: " + tres.gamesTimedOutByPlayer0 + " bot 1: " + tres.gamesWonByPlayer1 + " bot 1 timeouts: " + tres.gamesTimedOutByPlayer1 + " tied: " + tres.gamesTied + " threshold: " + tres.parameter);;
		}
	}
	
	// play a single match and print the results of the match
	public static void playAndShowGame() {
		Bot[] bots = botFactory();
		Bot bot0 = bots[0];
		Bot bot1 = bots[1];
		
		Random random = new Random();
		int row = random.nextInt(Board.rows - 2) + 1;
		int col = random.nextInt((Board.cols / 2) - 2) + 1;
		
		simulationResult res = (new simulateGame()).playMatch(bot0, bot1, row, col);
		ArrayList<Board> boards = res.boards;
		
		printBoards(boards);
	}
	
	// returns the two bots I am currently testing
	public static Bot[] botFactory(){
		Bot bot0 = new Bot(new GetMovesABCTKillerFirst(new ChamberEvaluator(), new bot0Depth()), new GetMovesEndGameBacktrack(new FloodEndGameEvaluator(), 14));
		Bot bot1 = new Bot(new GetMovesABCTKillerFirst(new ChamberEvaluator2(), new bot0Depth()), new GetMovesEndGameBacktrack(new FloodEndGameEvaluator(), 14));
		
		Bot[] res = {bot0, bot1};
		return res;
	}
	
	public static void main(String[] args) {
//		playAndShowGame();
//		playVariationTournament();
		playTournament();
	}
	
	private static class tournamentResult {
		
		public int gamesPlayed;
		public int gamesWonByPlayer0;
		public int gamesWonByPlayer1;
		public int gamesTimedOutByPlayer0;
		public int gamesTimedOutByPlayer1;
		public int gamesTied;
		public int parameter;
		
		public tournamentResult(int gamesPlayed, int gamesWonByPlayer0, int gamesWonByPlayer1, int timedOutPlayer0, int timedOutPlayer1, int gamesTied, int parameter){
			this.gamesPlayed = gamesPlayed;
			this.gamesWonByPlayer0 = gamesWonByPlayer0;
			this.gamesWonByPlayer1 = gamesWonByPlayer1;
			this.gamesTimedOutByPlayer0 = timedOutPlayer0;
			this.gamesTimedOutByPlayer1 = timedOutPlayer1;
			this.gamesTied = gamesTied;
			this.parameter = parameter;
		}
	}
	
	private static class adaptiveBotDepth implements GetSearchNumber {
		
		private int previousNumber = 3;
		private int previousTime = 10000;
		private int previousTimeLost;
		private int increaseDepthThreshold;
		
		public adaptiveBotDepth(int increaseDepthThreshold){
			this.increaseDepthThreshold = increaseDepthThreshold;
			this.previousTimeLost = -increaseDepthThreshold / 2;
		}
		
		public  int apply(int time, int rounds, int numOfMoveCombinations){
			int timeLost = previousTime - time;
			
			int res = previousNumber;
			if(timeLost > 1000) res -= 2;
			else if(timeLost > 0 && time < 1500) res -= 2;
			else if(timeLost > 600) res--;
			else if(timeLost > 0 && time < 3000) res--;
			else if(timeLost + previousTimeLost < -increaseDepthThreshold || time >= 10000){
				res++;
			}
			
			previousTimeLost = timeLost;
			previousNumber = res;
			previousTime = time;
			return res;
		}
	}
	
	private static class adaptiveAverageBotDepth implements GetSearchNumber {
		
		private int previousNumber = 3;
		private int previousTime = 10000;
		private int averageWeightedTimeLost = 0;
		private int increaseDepthThreshold;
		
		public adaptiveAverageBotDepth(int increaseDepthThreshold){
			this.increaseDepthThreshold = increaseDepthThreshold;
		}
		
		public  int apply(int time, int rounds, int numOfMoveCombinations){
			int timeLost = previousTime - time;
			averageWeightedTimeLost = (3 * averageWeightedTimeLost / 4) + timeLost / 4;
//			System.out.println("previous time: " + previousTime + " time: " + time);
//			System.out.println("time lost average: " + averageWeightedTimeLost);
//			System.out.println("time lost: " + timeLost);
			
			int res = previousNumber;
			if(timeLost > 1000) res -= 2;
			else if(timeLost > 0 && time < 1500) res -= 2;
			else if(timeLost > 600) res--;
			else if(timeLost > 0 && time < 3000) res--;
			else if(averageWeightedTimeLost < -increaseDepthThreshold || time >= 10000){
				res++;
			}
			
			previousNumber = res;
			previousTime = time;
			return res;
		}
	}
	
	private static class constantBotDepth implements GetSearchNumber {
		
		private int constant;
		
		public constantBotDepth(int constant){
			this.constant = constant;
		}
		
		public  int apply(int time, int rounds, int numOfMoveCombinations){
			return constant;
		}
	}
	
	private static class constantBotDepthWithTreeSize implements GetSearchNumberWithTreeSize {
		
		private int constant;
		private int leafs;
		private int previousTime = 10000;
		
		public constantBotDepthWithTreeSize(int constant){
			this.constant = constant;
		}
		
		public void setTreeLeafs(int treeLeafs){
			this.leafs = treeLeafs;
		}
		
		public  int apply(int time, int rounds, int numOfMoveCombinations){
			int timeOnLastMove = (time == 10000) ? 0 : 200 + previousTime - time;
			previousTime = time;
			System.err.println();
			System.out.flush();
			System.err.flush();
			System.err.println("time on round " + (rounds - 1) + " : " + timeOnLastMove);
			System.err.println("round " + rounds + " leafs: " + leafs);
			System.err.flush();
			return constant;
		}
	}
	
	private static class AdaptiveDepthWithTreeSize implements GetSearchNumberWithTreeSize {

		private int leafs;
		private int previousTime = 10000;
		private int previousDepth = 3;
		
		public void setTreeLeafs(int treeLeafs){
			this.leafs = treeLeafs;
		}
		
		public  int apply(int time, int rounds, int numOfMoveCombinations){
			int timeLost = previousTime - time;
			previousTime = time;
			
//			int timeOnLastMove = (time == 10000) ? 0 : 200 + previousTime - time;
//			previousTime = time;
//			System.err.println();
//			System.out.flush();
//			System.err.flush();
//			System.err.println("time on round " + (rounds - 1) + " : " + timeOnLastMove);
//			System.err.println("round " + rounds + " leafs: " + leafs);
//			System.err.flush();
			
			int res = previousDepth;
			if(time < 1000 || timeLost > 600) res--;
			else if(time < 3000 && timeLost > 0) res--;
			else if (leafs > 2590) res--;
			else if (leafs < 160) res += 2;
			else if(leafs < 960) res++;
			
			res = Math.max(res, 3);
			previousDepth = res;
			return res;
		}
	}

	private static class AdaptiveDepthWithTreeSizeParameterized implements GetSearchNumberWithTreeSize {

		private int leafs;
		private int previousTime = 10000;
		private int previousDepth = 3;
		private int lowerLeafThreshold;
		private int doubleLowerLeafThreshold;
		private int upperLeafThreshold;
		
		public AdaptiveDepthWithTreeSizeParameterized(int lowerLeafThreshold, int upperLeafThreshold){
			this.lowerLeafThreshold = lowerLeafThreshold;
			this.doubleLowerLeafThreshold = lowerLeafThreshold / 6;
			this.upperLeafThreshold = upperLeafThreshold;
		}
		
		public void setTreeLeafs(int treeLeafs){
			this.leafs = treeLeafs;
		}
		
		public  int apply(int time, int rounds, int numOfMoveCombinations){
			int timeLost = previousTime - time;
			previousTime = time;
			
//			int timeOnLastMove = (time == 10000) ? 0 : 200 + previousTime - time;
//			previousTime = time;
//			System.err.println();
//			System.out.flush();
//			System.err.flush();
//			System.err.println("time on round " + (rounds - 1) + " : " + timeOnLastMove);
//			System.err.println("round " + rounds + " leafs: " + leafs);
//			System.err.flush();
			
			int res = previousDepth;
			if(time < 1000 || timeLost > 600) res--;
			else if(time < 3000 && timeLost > 0) res--;
			else if (leafs > upperLeafThreshold) res--;
			else if (leafs < doubleLowerLeafThreshold) res += 2;
			else if(leafs < lowerLeafThreshold) res++;
			
			res = Math.max(res, 3);
			previousDepth = res;
			return res;
		}
	}
	
	private static class bot0Depth implements GetSearchNumber {
		private int previousNumber = 3;
		private int previousTime = 10000;
		private int previousTimeLost = -135;
		
		public  int apply(int time, int rounds, int numOfMoveCombinations){
			int timeLost = previousTime - time;
			
			int res = previousNumber;
			if(timeLost > 1000) res -= 2;
			else if(timeLost > 0 && time < 1500) res -= 2;
			else if(timeLost > 600) res--;
			else if(timeLost > 0 && time < 3000) res--;
			else if(timeLost + previousTimeLost < -270 || time >= 10000){
				res++;
			}
			
			previousTimeLost = timeLost;
			previousNumber = res;
			previousTime = time;
			return res;
		}
	}
	
	private static class bot1Depth implements GetSearchNumber {
		private int previousNumber = 4;
		private int previousTime = 10000;
		private int previousTimeLost = -135;
		
		public  int apply(int time, int rounds, int numOfMoveCombinations){
			int timeLost = previousTime - time;
			
			int res = previousNumber;
			if(timeLost > 600) res--;
			else if(timeLost > 0 && time < 3000) res--;
			else if(timeLost + previousTimeLost < -270 || time >= 10000){
				res++;
			}
			
			previousTimeLost = timeLost;
			previousNumber = res;
			previousTime = time;
			return res;
		}
	}
	
	private static class bot2Depth implements GetSearchNumber {
		
		private int previousNumber = 3;
		private int previousTime = 0;
		
		public  int apply(int time, int rounds, int numOfMoveCombinations){
			if(time > 8000 && rounds > 24) return 8;
			if(time > 6000 && rounds > 18) return 7;
			else if(time > 4000 && rounds > 12) return 6;
			else if(time > 2000 && rounds > 6) return 5;
			else return 4;
		}
	}
}
