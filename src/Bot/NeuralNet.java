package Bot;

import java.util.Random;

public class NeuralNet {
	public static double sigmoid(double x){
		return 1.0 / (1 + Math.exp(x));
	}
	
	public static double sigmoid_derivative(double x){
		return (1.0 / (1 + Math.exp(x))) * (1 - 1.0 / (1 + Math.exp(x)));
	}
	
	private double[][][] weights;
	private double[][] biases;
	private Random random = new Random();
	
	public NeuralNet(int numInputs, int numHiddenNeurons, int numOutputs){
		weights = new double[2][][];
		biases = new double[2][];
		
		double[][] firstLayerWeights = new double[numHiddenNeurons][numInputs];
		for(int i = 0; i < numHiddenNeurons; i++) for(int j = 0; j < numInputs; j++) firstLayerWeights[i][j] = random.nextDouble();
		weights[0] = firstLayerWeights;
		double[] firstLayerBiases = new double[numHiddenNeurons];
		for(int i = 0; i < numHiddenNeurons; i++) firstLayerBiases[i] = random.nextDouble();
	}
}
