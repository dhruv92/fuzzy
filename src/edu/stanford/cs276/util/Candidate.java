package edu.stanford.cs276.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Candidate implements Serializable {

	private String candidateWord;
	private int editDistance;
	private Edit edit;

	public Candidate(String candidateWord, int editDistance, Edit edit) {
		this.candidateWord = candidateWord;
		this.editDistance = editDistance;
		this.edit = edit;
	}
	
	public String getCandidate() {
		return candidateWord;
	}
	
	public int getDistance() {
		return editDistance;
	}
	
	public Edit getEdit() {
		return edit;
	}
	
	// public toString
}
