package edu.stanford.cs276;

import java.io.Serializable;
import edu.stanford.cs276.util.Candidate;

public interface EditCostModel extends Serializable {

	public double editProbability(Candidate candidate, String R);
}
