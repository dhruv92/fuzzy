package edu.stanford.cs276;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import edu.stanford.cs276.util.Candidate;
import edu.stanford.cs276.util.Edit;
import edu.stanford.cs276.util.Pair;
import edu.stanford.cs276.util.Dictionary;

public class CandidateGenerator implements Serializable {


	private static CandidateGenerator cg_;

	// Don't use the constructor since this is a Singleton instance
	private CandidateGenerator() {}

	public static CandidateGenerator get() throws Exception{
		if (cg_ == null ){
			cg_ = new CandidateGenerator();
		}
		return cg_;
	}


	public static final Character[] alphabet = {
		'a','b','c','d','e','f','g','h','i','j','k','l','m','n',
		'o','p','q','r','s','t','u','v','w','x','y','z',
		'0','1','2','3','4','5','6','7','8','9',
		' ',',','\''};


	private ArrayList<ArrayList<Candidate>> cartesianProduct(ArrayList<ArrayList<Candidate>> sets) {
		if (sets.size() < 2)
			throw new IllegalArgumentException(
					"Can't have a product of fewer than two sets (got " +
							sets.size() + ")");

		return _cartesianProduct(0, sets);
	}

	private static ArrayList<ArrayList<Candidate>> _cartesianProduct(int index, ArrayList<ArrayList<Candidate>> sets) {
		ArrayList<ArrayList<Candidate>> ret = new ArrayList<ArrayList<Candidate>>();
		if (index == sets.size()) {
			ret.add(new ArrayList<Candidate>());
		} else {
			for (Candidate obj : sets.get(index)) {
				for (ArrayList<Candidate> set : _cartesianProduct(index+1, sets)) {
					set.add(obj);
					ret.add(set);
				}
			}
		}
		return ret;
	}

	private boolean allValidWords(String query, Dictionary words) {
		StringTokenizer st = new StringTokenizer(query);
		while(st.hasMoreTokens()) {
			if(words.count(st.nextToken()) == 0) return false;
		}
		return true;
	}

	// Given a word and pos, insert every letter in the alphabet at that position
	// You're guessing that the original query was this new query with an insertion & that
	// the user actually deleted this character by accident
	private Set<Candidate> generateInsertions(String query, int pos, Dictionary words) {
		Set<Candidate> inserts = new HashSet<Candidate>();
		for(char ch : alphabet) {
			String candidateString = query.substring(0, pos) + ch + query.substring(pos);
			if(allValidWords(candidateString, words)) {
				ArrayList<Edit> edits = new ArrayList<Edit>();
				// Original char is char before insertion, replacement char is alphabet letter
				String original = "";
				if(pos > 0) original += query.charAt(pos-1);
				edits.add(new Edit(Edit.EditType.DELETION, "" + query.charAt(pos), "" + ch));
				inserts.add(new Candidate(candidateString, 1, edits));
			}
		}
		return inserts;
	}

	// Given a start word and position, swap the char at position with every letter in the alphabet
	// You're guessing that the original query was the new substituted query & that
	// the user actually substituted it with whatever was there before by accident
	private Set<Candidate> generateSubstitutions(String query, int pos, Dictionary words) {
		Set<Candidate> subs = new HashSet<Candidate>();
		for(char ch : alphabet) {
			char[] c = query.toCharArray();
			char original = c[pos];
			if(ch != original){
				c[pos] = ch;
				String candidateString = new String(c);
				if(allValidWords(candidateString, words)) {
					ArrayList<Edit> edits = new ArrayList<Edit>();
					// Original char is char at pos for user query, replacement char is alphabet letter
					edits.add(new Edit(Edit.EditType.SUBSTITUTION, "" + ch, "" + original));
					subs.add(new Candidate(candidateString, 2, edits));
				}
			}
		}
		return subs;
	}

	// Given a position and word, remove char at position in word
	// You're guessing that the original query was this new query without the char & that
	// the user actually inserted this character by accident
	private Set<Candidate> generateDeletions(String query, int pos, Dictionary words) {
		Set<Candidate> deletes = new HashSet<Candidate>();
		String candidateString = query.substring(0, pos) + query.substring(pos+1);
		if(allValidWords(candidateString, words)) {
			// Original char is char at pos, replacement char is no_char
			String original = "";
			if(pos > 0) original += query.charAt(pos-1);
			ArrayList<Edit> edits = new ArrayList<Edit>();
			edits.add(new Edit(Edit.EditType.INSERTION, original, "" + query.charAt(pos)));
			deletes.add(new Candidate(candidateString, 1, edits));
		}
		return deletes;
	}

	// Given a current position and next position,
	// swap the chars at those positions
	// return resulting string in a set for consistency
	// You're guessing that the original query was this new query with the transpose & that
	// the user actually swapped the two by accident
	private Set<Candidate> generateTranspositions(String query, int curr, int next, Dictionary words) {
		Set<Candidate> trans = new HashSet<Candidate>();

		// Char Array idea from Stack Overflow
		char[] c = query.toCharArray();
		if(c[curr] != c[next]) {
			char temp = c[curr];
			c[curr] = c[next];
			c[next] = temp;
			String candidateString = new String(c);
			if(allValidWords(candidateString, words)) {
				ArrayList<Edit> edits = new ArrayList<Edit>();
				// Original char is char at curr, replacement char is char at next
				edits.add(new Edit(Edit.EditType.TRANSPOSITION, ""+query.charAt(next), ""+query.charAt(curr)));
				trans.add(new Candidate(candidateString, 1, edits));
			}
		}
		return trans;
	}


	// Given a word, generates all candidates that are 1
	// insertion, deletion, substitution, and transposition
	// away from given word
	private Set<Candidate> editOneCandidates(String query, Dictionary words) {
		Set<Candidate> editCandidates = new HashSet<Candidate>();
		// for each position in the query word
		for(int i = 0; i < query.length(); i++) {
			Set<Candidate> inserts = generateInsertions(query, i, words);
			Set<Candidate> subs = generateSubstitutions(query, i, words);
			Set<Candidate> deletes = generateDeletions(query, i, words);
			Set<Candidate> trans = new HashSet<Candidate>();
			if(i > 0) {
				trans = generateTranspositions(query, i-1, i, words);
			}
			// Add all different edits to the candidate sets for the query word
			for(Candidate candidate : inserts) {
				editCandidates.add(candidate);
			}
			for(Candidate candidate : subs) {
				editCandidates.add(candidate);
			}
			for(Candidate candidate : deletes) {
				editCandidates.add(candidate);
			}
			for(Candidate candidate : trans) {
				editCandidates.add(candidate);
			}
		}
		return editCandidates;
	}

	public Set<Candidate> getCandidates(String query, Dictionary unigram) {
		Set<Candidate> candidates = new HashSet<Candidate>();
		candidates.add(new Candidate(query, 0, new ArrayList<Edit>()));
		Set<Candidate> editCandidates = editOneCandidates(query, unigram);
		for(Candidate candidate : editCandidates) {
			candidates.add(candidate);
		}
		return candidates;
	}

}
