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
	
	public simulationResult playMatch(Bot bot0, Bot bot1){
		ArrayList<Board> boards = new ArrayList<Board>();
		ArrayList<Move> bot0Moves = new ArrayList<Move>();
		ArrayList<Move> bot1Moves = new ArrayList<Move>();
		
		Random random = new Random();
		long timePerMovePlayer = 200;
		long timePerMoveOpponent = 200;
		int timeFudge = 2; //used for matching the speed of the competition servers
//		int playerRow = random.nextInt(Board.rows - 2) + 1;
//		int playerCol = random.nextInt((Board.cols / 2) - 2) + 1;
		int playerRow = 7;
		int playerCol = 4;
		long playerTime = 10000;
		int opponentRow = playerRow;
		int opponentCol = Board.cols - playerCol - 1;
		long opponentTime = 10000;
		int round = 0;
		Board board = new PieceBoard2(16, 16, playerRow, playerCol, opponentRow, opponentCol);
		boards.add(board.deepcopy());
		
		long time = System.currentTimeMillis();
		Move bot0Move = bot0.getMove(board, (int) playerTime, round, Move.UP);
		Move lastBot0Move = bot0Move;
		long elapsed = timeFudge * (System.currentTimeMillis() - time);
		playerTime -= elapsed;
		System.out.println(round + " bot0 time: " + playerTime);
		
		time = System.currentTimeMillis();
		Move bot1Move = bot1.getMove(board.toOpponentBoard(), (int) opponentTime, round, Move.UP);
		Move lastBot1Move = bot1Move;
		elapsed = timeFudge * (System.currentTimeMillis() - time);
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
			elapsed = timeFudge * (System.currentTimeMillis() - time);
			playerTime -= elapsed;
			System.out.println(round + " bot0 time: " + playerTime);
			
			time = System.currentTimeMillis();
			bot1Move = bot1.getMove(board.toOpponentBoard(), (int) opponentTime, round, lastBot0Move);
			elapsed = timeFudge * (System.currentTimeMillis() - time);
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
	
	public static void main(String[] args){
		Bot bot0 = new Bot((GetMoves) new GetMovesABCacheTreeKillerFirst((Evaluator) new ChamberEvaluator(), new constantBotDepth(6)));
		Bot bot1 = new Bot((GetMoves) new GetMovesABCacheTreeKillerFirst((Evaluator) new ChamberEvaluator(), new constantBotDepth(6)));
		
		simulationResult res = (new simulateGame()).playMatch(bot0, bot1);
		ArrayList<Board> boards = res.boards;
		
		printBoards(boards);
		
//		ArrayList<tournamentResult> results = new ArrayList<tournamentResult>();
//		for(int i = 0; i * i <= 250; i++){
//			int threshold = i * i;
//			int gamesToPlay = 100;
//			int gamesWonByBot1 = 0;
//			int gamesWonByBot0 = 0;
//			int gamesTied = 0;
//			int gamesTimedOutByBot0 = 0;
//			int gamesTimedOutByBot1 = 0;
//			for(int round = 0; round < gamesToPlay; round++){
//				Bot bot0 = new Bot((GetMoves) new GetMovesABCacheTreeKillerFirst((Evaluator) new ChamberEvaluator(), new bot0Depth()));
//				Bot bot1 = new Bot((GetMoves) new GetMovesABCTKillerPrune((Evaluator) new ChamberEvaluator(), new bot0Depth(), threshold));
//				
//				if(round % 2 == 0){
//					simulationResult res = (new simulateGame()).playMatch(bot0, bot1);
//					
//					if(res.winner == -1) gamesTied++;
//					else if(res.winner == 1) gamesWonByBot1++;
//					else gamesWonByBot0++;
//					if(res.bot0TimedOut) gamesTimedOutByBot0++;
//					if(res.bot1TimedOut) gamesTimedOutByBot1++;
//				} else {
//					simulationResult res = (new simulateGame()).playMatch(bot1, bot0);
//					
//					if(res.winner == -1) gamesTied++;
//					else if(res.winner == 0) gamesWonByBot1++;
//					else gamesWonByBot0++;
//					if(res.bot0TimedOut) gamesTimedOutByBot1++;
//					if(res.bot1TimedOut) gamesTimedOutByBot0++;
//				}
//			}
//			tournamentResult tres = new tournamentResult(gamesToPlay, gamesWonByBot0, gamesWonByBot1, gamesTimedOutByBot0, gamesTimedOutByBot1, gamesTied, threshold);
//			results.add(tres);
//		}
//		for(tournamentResult tres : results){
//			System.out.println("Games played: " + tres.gamesPlayed + " bot 0: " + tres.gamesWonByPlayer0 + " bot 0 timeouts: " + tres.gamesTimedOutByPlayer0 + " bot 1: " + tres.gamesWonByPlayer1 + " bot 1 timeouts: " + tres.gamesTimedOutByPlayer1 + " tied: " + tres.gamesTied + " threshold: " + tres.parameter);;
//		}
		
//		int gamesToPlay = 300;
//		int gamesWonByBot0 = 0;
//		int gamesWonByBot1 = 0;
//		int gamesTimedOutByBot0 = 0;
//		int gamesTimedOutByBot1 = 0;
//		int gamesTied = 0;
//		for(int i = 0; i < gamesToPlay; i++){
//			Bot bot0 = new Bot((GetMoves) new GetMovesABCacheTreeKillerFirst((Evaluator) new ChamberEvaluator(), new bot0Depth()));
//			Bot bot1 = new Bot((GetMoves) new GetMovesABCTKillerPartition((Evaluator) new ChamberEvaluator(), new bot0Depth()));
//			
//			if(i % 2 == 0){
//				simulationResult res = (new simulateGame()).playMatch(bot0, bot1);
//				
//				if(res.winner == -1) gamesTied++;
//				else if(res.winner == 1) gamesWonByBot1++;
//				else gamesWonByBot0++;
//				if(res.bot0TimedOut) gamesTimedOutByBot0++;
//				if(res.bot1TimedOut) gamesTimedOutByBot1++;
//			} else {
//				simulationResult res = (new simulateGame()).playMatch(bot1, bot0);
//				
//				if(res.winner == -1) gamesTied++;
//				else if(res.winner == 0) gamesWonByBot1++;
//				else gamesWonByBot0++;
//				if(res.bot0TimedOut) gamesTimedOutByBot1++;
//				if(res.bot1TimedOut) gamesTimedOutByBot0++;
//			}
//		}
//		System.out.println("Games played: " + gamesToPlay);
//		System.out.println("Games won by bot 0: " + gamesWonByBot0);
//		System.out.println("Games timed out by bot 0: " + gamesTimedOutByBot0);
//		System.out.println("Games won by bot 1: " + gamesWonByBot1);
//		System.out.println("Games timed out by bot 1: " + gamesTimedOutByBot1);
//		System.out.println("Games tied: " + gamesTied);
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
	
	//equivalent to adaptiveBotDepth(270)
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
		
		private int previousNumber = 3;
		private int previousTime = 0;
		
		public  int apply(int time, int rounds, int numOfMoveCombinations){
			if(time > 8000 && rounds > 24) return 9;
			if(time > 6000 && rounds > 18) return 8;
			else if(time > 4000 && rounds > 12) return 7;
			else if(time > 2000 && rounds > 6) return 6;
			else return 5;
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
