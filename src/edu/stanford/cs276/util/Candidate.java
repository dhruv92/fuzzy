package edu.stanford.cs276.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Candidate implements Serializable {

	private String candidateWord;
	private int editDistance;
	private ArrayList<Character> edits;

	public Candidate(String candidateWord, int editDistance, ArrayList<Character> edits) {
		this.candidateWord = candidateWord;
		this.editDistance = editDistance;
		this.edits = edits;
	}
	
	public String getCandidate() {
		return candidateWord;
	}
	
	public int getDistance() {
		return editDistance;
	}
	
	public ArrayList<Character> getEdits() {
		return edits;
	}
	
	//public toString
}
