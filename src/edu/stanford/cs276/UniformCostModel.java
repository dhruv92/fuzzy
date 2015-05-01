package edu.stanford.cs276;

import edu.stanford.cs276.util.Candidate;

public class UniformCostModel implements EditCostModel {
	
	@Override
	public double editProbability(Candidate candidate, String R) {
		/*
		 * Your code here - NOTE FROM DHRUV, added code
		 */
		if (candidate.getCandidate().equals(R)) return Math.log(EQUAL_PROBABILITY);
		
		return Math.log(Math.pow(EDIT_PROBABILITY, candidate.getDistance()));
	}
	
	//we should play with these numbers
	private static final double EQUAL_PROBABILITY = 0.95;
	private static final double EDIT_PROBABILITY = 0.1;
}
