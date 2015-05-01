package edu.stanford.cs276.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Candidate implements Serializable {

	private String candidateQuery;
	private int editDistance;
	private ArrayList<Edit> edits;

	public Candidate(String candidateQuery, int editDistance, ArrayList<Edit> edits) {
		this.candidateQuery = candidateQuery;
		this.editDistance = editDistance;
		this.edits = edits;
	}
	
	public String getCandidate() {
		return candidateQuery;
	}
	
	public int getDistance() {
		return editDistance;
	}
	
	public ArrayList<Edit> getEdits() {
		return edits;
	}
	
	// public toString
}
