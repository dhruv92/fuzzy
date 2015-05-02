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
	//	private static trigramDict;

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

	// Given a word, goes through every trigram in the word
	// For each trigram, looks up the word set for that trigram dictionary
	// Returns a set of all elements from each word set
	//	private Set<String> trigramLists(String queryWord, 	Map<Integer, Set<Integer>> trigramDict,
	//														Map<Integer, String> termLookup,
	//														Map<String, Integer> trigramIdDict){
	//		Set<String> candidateSet = new HashSet<String>();
	//		if(queryWord.length() >= 3){
	//			for(int i = 2; i < queryWord.length(); i++){
	//				String trigram = "" + queryWord.charAt(i-2) + queryWord.charAt(i-1) + queryWord.charAt(i);
	//				if(trigramIdDict.containsKey(trigram)){
	//					int trigramId = trigramIdDict.get(trigram);
	//					if (trigramDict.containsKey(trigramId)) {
	//						Set<Integer> termIdSet = trigramDict.get(trigramId);
	//						for(int candidateId : termIdSet){
	//							if(termLookup.containsKey(candidateId)){
	//								candidateSet.add(termLookup.get(candidateId));
	//							}
	//						}
	//					}
	//				}
	//			}
	//		}
	//
	//		return candidateSet;
	//	}


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

	// Recursively generates the cartesian product for all sets
	//	private Set<Set<Candidate>> cartesianProduct(ArrayList<Set<Candidate>> remainder){
	//		Set<Candidate> result = new HashSet<Candidate>();
	//		if(remainder.size() == 1) return remainder.get(0);
	//		Set<Candidate> current = remainder.get(0);
	//		ArrayList<Set<Candidate>> remainder_cpy = new ArrayList<Set<Candidate>>();
	//		for(Set<Candidate> set : remainder){
	//			remainder_cpy.add(set);
	//		}
	//		remainder_cpy.remove(0);
	//		for(Candidate candidate_word : current){
	//			for(Candidate candidate_str : cartesianProduct(remainder_cpy)){
	//				String str = candidate_word.getCandidate() + " " + candidate_str.getCandidate();
	//				int distance = candidate_word.getDistance() + candidate_str.getDistance();
	//				ArrayList<Character> edits = new ArrayList<Character>();
	//				for(char edit : candidate_word.getEdits()) {
	//					edits.add(edit);
	//				}
	//				for(char edit : candidate_str.getEdits()) {
	//					edits.add(edit);
	//				}
	//				if(distance <= 2) {
	//					result.add(new Candidate(str, distance, edits));
	//				}
	//			}
	//		}
	//		return result;
	//	}

	// Generate all candidates for the target query TRIGRAMS
	//	public Set<String> getCandidates(String query, 	Map<Integer, Set<Integer>> trigramDict,
	//													Map<Integer, String> termLookup,
	//													Map<String, Integer> trigramIdDict) throws Exception {
	//		StringTokenizer st = new StringTokenizer(query);
	//		ArrayList<Set<String>> candidateSets = new ArrayList<Set<String>>();
	//		while(st.hasMoreTokens()){
	//			String queryWord = st.nextToken();
	//			candidateSets.add(trigramLists(queryWord, trigramDict, termLookup, trigramIdDict));
	//		}
	//
	//		// Generate Cartesian product for candidate sets
	//		return cartesianProduct(candidateSets);
	//	}

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
		//		StringTokenizer st = new StringTokenizer(query);
		//		Set<Set<Candidate>> candidateSets = new HashSet<Set<Candidate>>();
		candidates.add(new Candidate(query, 0, new ArrayList<Edit>()));
		Set<Candidate> editCandidates = editOneCandidates(query, unigram);
		for(Candidate candidate : editCandidates) {
			candidates.add(candidate);
		}
		return candidates;

		//		ArrayList<ArrayList<Candidate>> candidateLists = new ArrayList<ArrayList<Candidate>>();
		//		for(Set<Candidate> set : candidateSets) {
		//			ArrayList<Candidate> list = new ArrayList<Candidate>();
		//			for(Candidate candidate : set) {
		//				list.add(candidate);
		//			}
		//			candidateLists.add(list);
		//		}

		// Generate Cartesian product for candidate sets
		//		return cartesianProduct(candidateLists);
	}

}
