package edu.stanford.cs276;

public class UniformCostModel implements EditCostModel {
	
	@Override
	public double editProbability(String original, String R, int distance) {
		/*
		 * Your code here - NOTE FROM DHRUV, added code
		 */
		if (original.equals(R)) return Math.log(EQUAL_PROBABILITY);
		
		return Math.log(Math.pow(EDIT_PROBABILITY, distance));
	}
	
	//we should play with these numbers
	private static final double EQUAL_PROBABILITY = 0.95;
	private static final double EDIT_PROBABILITY = 0.1;
}
