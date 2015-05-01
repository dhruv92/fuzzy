package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import edu.stanford.cs276.util.Candidate;
import edu.stanford.cs276.util.Dictionary;
import edu.stanford.cs276.util.Edit;
import edu.stanford.cs276.util.Edit.EditType;
import edu.stanford.cs276.util.Pair;

public class EmpiricalCostModel implements EditCostModel{
	
	// All data structures needed to hold counts needed to generate probabilities
	private Map<Pair<String, String>, Integer> insertionMatrix = new HashMap<Pair<String, String>, Integer>(); 
	private Map<Pair<String, String>, Integer> deletionMatrix = new HashMap<Pair<String, String>, Integer>(); 
	private Map<Pair<String, String>, Integer> substitutionMatrix = new HashMap<Pair<String, String>, Integer>(); 
	private Map<Pair<String, String>, Integer> transpositionMatrix = new HashMap<Pair<String, String>, Integer>(); 
	private Dictionary charCounts = new Dictionary();
	private Dictionary bigramCounts = new Dictionary();
	
	public EmpiricalCostModel(String editsFile) throws IOException {
		BufferedReader input = new BufferedReader(new FileReader(editsFile));
		System.out.println("Constructing edit distance map...");
		String line = null;
		while ((line = input.readLine()) != null) {
			Scanner lineSc = new Scanner(line);
			lineSc.useDelimiter("\t");
			String noisy = lineSc.next();
			String clean = lineSc.next();
			
			// Determine type of error and record probability -- records counts in matrices
			updateDictionaries(clean);
			if (noisy.length() > clean.length()) { //is ins
				insertionCounts(noisy, clean);
			} else if (noisy.length() < clean.length()){ // is del
				deletionCounts(noisy, clean);
			} else { // is subs / transp
				transposeSubstituteCounts(noisy, clean);
			}
			
		}

		input.close();
		System.out.println("Done.");
	}
	
	private void transposeSubstituteCounts(String noisy, String clean) {
		for (int i = 0; i < clean.length(); i++) {
			String cleanChar = "" + clean.charAt(i);
			String noisyChar = "" + noisy.charAt(i);
			if (!cleanChar.equals(noisyChar)) {
				if (i + 1 < clean.length()) {
					String nextCleanChar = "" + clean.charAt(i+1);
					String nextNoiseChar = "" + noisy.charAt(i+1);
					if (nextCleanChar.equals(noisyChar) && cleanChar.equals(nextNoiseChar)) { // it's transpose
						Pair<String, String> transpose = new Pair<String, String>(cleanChar, nextCleanChar);
						incrementMatrix(transpose, transpositionMatrix);
						break;
					}
				}
				Pair<String, String> substitute = new Pair<String, String>(cleanChar, noisyChar);
				incrementMatrix(substitute, substitutionMatrix);
				break;
			}
		}
	}
	
	//Go through and add the first deletion you find to the deletion table
	private void deletionCounts(String noisy, String clean) {
		String prevChar = "";
		for (int i = 0; i < clean.length(); i++) {
			if (i == noisy.length()) {
				Pair<String, String> deletion = new Pair<String, String>(prevChar, "" + clean.charAt(i));
				incrementMatrix(deletion, deletionMatrix);
				break;
			}
			String cleanChar = "" + clean.charAt(i);
			String noisyChar = "" + noisy.charAt(i);
			if (!noisyChar.equals(cleanChar)) { // found a deletion
				Pair<String, String> deletion = new Pair<String, String>(prevChar, cleanChar);
				incrementMatrix(deletion, deletionMatrix);
				break;
			}
			prevChar = cleanChar;
		}
	}

	// Go through and add the first insertion you find to the insertion table
	private void insertionCounts(String noisy, String clean) {
		String prevChar = "";
		//find the first difference, that is the insert
		for (int i = 0; i < noisy.length(); i++) {
			if (i == clean.length()) {
				Pair<String, String> insertion = new Pair<String, String>(prevChar, "" + noisy.charAt(i));
				incrementMatrix(insertion, insertionMatrix);
				break;
			}
			String cleanChar = "" + clean.charAt(i);
			String noisyChar = "" + noisy.charAt(i);
			if (!noisyChar.equals(cleanChar)) { // not the same, 
				Pair<String, String> insertion = new Pair<String, String>(prevChar, noisyChar);
				incrementMatrix(insertion, insertionMatrix);
				break;
			}
			prevChar = cleanChar;
		}
	}
	
	private void updateDictionaries(String clean) {
		String prevChar = "";
		charCounts.add(prevChar);
		for (int i = 0; i < clean.length(); i++) {
			String curChar = "" + clean.charAt(i);
			String bigram = prevChar + curChar;
			charCounts.add(curChar);
			bigramCounts.add(bigram);
			prevChar = curChar;
		}
	}

	private void incrementMatrix(Pair<String, String> pair, Map<Pair<String, String>, Integer> map) {
		if (map.containsKey(pair)) {
			map.put(pair, map.get(pair) + 1);
		} else {
			map.put(pair, 1);
		}
	}

	
	// You need to update this to calculate the proper empirical cost
	// This method takes into account the edits, calculates the probabilities, does LaPlace smoothing
	// and then out puts them as a log probability
	@Override
	public double editProbability(Candidate candidate, String R) {
		if (candidate.getCandidate().equals(R)) return Math.log(EQUAL_PROBABILITY);
		
		ArrayList<Edit> edits = candidate.getEdits();
		double probability = 0.0;
		for (Edit edit : edits) {
			EditType et = edit.getEditType();
			Pair<String, String> change = edit.getChange();
			switch (et) {
				case INSERTION:
					int insCount = (insertionMatrix.containsKey(change)) ? insertionMatrix.get(change) : 1; //1 for smoothing
					int beforeCount = charCounts.count(change.getFirst()) + charCounts.getUniqueKeyCount(); // smoothing, add alphabet
					probability += Math.log(insCount / beforeCount);
					break;
				case DELETION:
					int delCount = (deletionMatrix.containsKey(change)) ? deletionMatrix.get(change) : 1;
					String bigram = change.getFirst() + change.getSecond();
					int seqCount = bigramCounts.count(bigram) + bigramCounts.getUniqueKeyCount();
					probability += Math.log(delCount / seqCount);
					break;
				case TRANSPOSITION:
					int traCount = (transpositionMatrix.containsKey(change)) ? transpositionMatrix.get(change) : 1;
					String big = change.getFirst() + change.getSecond();
					int seqC = bigramCounts.count(big) + bigramCounts.getUniqueKeyCount();
					probability += Math.log(traCount / seqC);
					break;
				case SUBSTITUTION:
					int subCount = (substitutionMatrix.containsKey(change)) ? substitutionMatrix.get(change) : 1;
					int replaceCount = charCounts.count(change.getSecond()) + charCounts.getUniqueKeyCount();
					probability += Math.log(subCount / replaceCount);
					break;
				default:
					break;
			}
		}
		return probability;
	}
	
	private static final double EQUAL_PROBABILITY = 0.95;
}
