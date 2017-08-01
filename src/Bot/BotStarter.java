package Bot;
import java.util.Scanner;

public class BotStarter {
	
	public static String MoveToStr(Move move){
		if(move == Move.UP) return "up";
		else if(move == Move.DOWN) return "down";
		else if(move == Move.LEFT) return "left";
		else if(move == Move.RIGHT) return "right";
		else return "invalid move";
	}
	
	public static void makeMove(Bot bot, Board board, int time, int round, Move lastPlayerMove){
		long t0 = System.currentTimeMillis();
		Move move = bot.getMove(board, time, round, lastPlayerMove);
		double elapsed = (System.currentTimeMillis() - t0) / (double) 1000;
		System.err.println(elapsed);
		System.out.println(MoveToStr(move));
	}
	
	public static Move getLastOpponentMove(Board board, Board newBoard){
		Move res;
		if(board == null) return Move.UP;
		Location opponentLocation = board.getOpponentLocation();
		Location newOpponentLocation = newBoard.getOpponentLocation();
		int drow = newOpponentLocation.row - opponentLocation.col;
		int dcol = newOpponentLocation.col - opponentLocation.col;
		if(drow == 0){
			if(dcol == 1) res = Move.RIGHT;
			else res = Move.LEFT;
		} else {
			if(drow == 1) res = Move.DOWN;
			else res = Move.UP;
		}
		System.err.println("last opponent move: " + MoveToStr(res));
		return res;
	}
	
	public void run(){
		Bot bot = new Bot((GetMoves) new GetMovesABSort((Evaluator) new ChamberEvaluator(), new botDepth()));
		
		int timebank = 0;
		int time_per_move = 0;
		String[] player_names = {};
		String my_bot = "";
		String my_botid = "";
		String opponent_botid = "";
		int rows = -1;
		int cols = -1;
		int round = -1;
		Board board = null;
		Move lastOpponentMove = null;
		
		Scanner scan = new Scanner(System.in);
		
		while(scan.hasNextLine()){
			String line = scan.nextLine();
			if (line.length() == 0) continue;
			
			String[] tokens = line.split(" ");
			String key0 = tokens[0];
			if(key0.equals( "settings")){
				String key1 = tokens[1];
				if(key1.equals("timebank")) timebank = Integer.valueOf(tokens[2]);
				else if(key1.equals("time_per_move")) time_per_move = Integer.valueOf(tokens[2]);
				else if(key1.equals("player_names")) player_names = tokens[2].split(",");
				else if(key1.equals("your_bot")) my_bot = tokens[2];
				else if(key1.equals("your_botid")){
					my_botid = tokens[2];
					int opponent_botidint = 1 - Integer.valueOf(my_botid);
					opponent_botid = String.valueOf(opponent_botidint);
				}
				else if(key1.equals("field_width")) cols = Integer.valueOf(tokens[2]);
				else if(key1.equals("field_height")) rows = Integer.valueOf(tokens[2]);
			}
			else if(key0.equals("update")){
				String key1 = tokens[1];
				if(key1.equals("game")){
					String key2 = tokens[2];
					if(key2.equals("round")) round = Integer.valueOf(tokens[3]);
					else if(key2.equals("field")){
						Board newBoard = new PieceBoard(tokens[3], rows, cols, my_botid, opponent_botid);
						lastOpponentMove = getLastOpponentMove(board, newBoard);
						board = newBoard;
					}
				}
			}
			else if(key0.equals("action") && tokens[1].equals("move")){
				timebank = Integer.valueOf(tokens[2]);
				makeMove(bot, board, timebank, round, lastOpponentMove);
			}
		}
	}
	
	private static class botDepth implements GetSearchNumber {
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
	
	public static void main(String[] args){
		(new BotStarter()).run();
	}
}
