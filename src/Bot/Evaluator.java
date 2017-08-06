package Bot;

public interface Evaluator {
	public int getMaxValue(); // this should be greater than the largest value that the evaluator can output
	public int getPartitionedOffset(); // this value should be added to the evaluation of any non-partitioned board
	public int evaluate(Board board);
}
