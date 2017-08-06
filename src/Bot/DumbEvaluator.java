package Bot;

public class DumbEvaluator implements Evaluator{
	public int getMaxValue(){
		return 1;
	}
	
	public int getPartitionedOffset(){
		return 2;
	}
	
	
	public int evaluate(Board board){
		return 0;
	}
}
