package edu.stanford.cs276;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import edu.stanford.cs276.util.Candidate;
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
		' ',','};

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


	// Recursively generates the cartesian product for all sets
	private Set<Candidate> cartesianProduct(ArrayList<Set<Candidate>> remainder){
		Set<Candidate> result = new HashSet<Candidate>();
		if(remainder.size() == 1) return remainder.get(0);
		Set<Candidate> current = remainder.get(0);
		ArrayList<Set<Candidate>> remainder_cpy = new ArrayList<Set<Candidate>>();
		for(Set<Candidate> set : remainder){
			remainder_cpy.add(set);
		}
		remainder_cpy.remove(0);
		for(Candidate candidate_word : current){
			for(Candidate candidate_str : cartesianProduct(remainder_cpy)){
				String str = candidate_word.getCandidate() + " " + candidate_str.getCandidate();
				int distance = candidate_word.getDistance() + candidate_str.getDistance();
				ArrayList<Character> edits = new ArrayList<Character>();
				for(char edit : candidate_word.getEdits()) {
					edits.add(edit);
				}
				for(char edit : candidate_str.getEdits()) {
					edits.add(edit);
				}
				if(distance <= 2) {
					result.add(new Candidate(str, distance, edits));
				}
			}
		}
		return result;
	}

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

	private Set<String> generateInsertions(String word, int pos, Dictionary words) {
		Set<String> inserts = new HashSet<String>();
		for(char ch : alphabet) {
			String candidate = word.substring(0, pos) + ch + word.substring(pos);
			if(words.count(candidate) != 0) {
				inserts.add(candidate);
			}
		}
		return inserts;
	}

	private Set<String> generateSubstitutions(String word, int pos, Dictionary words) {
		Set<String> subs = new HashSet<String>();
		for(char ch : alphabet) {
			String candidate = word.substring(0, pos) + ch + word.substring(pos+1);
			if(words.count(candidate) != 0) {
				subs.add(candidate);
			}
		}
		return subs;
	}

	private Set<String> generateDeletions(String word, int pos, Dictionary words) {
		Set<String> deletes = new HashSet<String>();
		String candidate = word.substring(0, pos) + word.substring(pos+1);
		if(words.count(candidate) != 0) {
			deletes.add(candidate);
		}
		return deletes;
	}

	private Set<String> generateTranspositions(String word, int curr, int next, Dictionary words) {
		Set<String> trans = new HashSet<String>();

		// Char Array idea from Stack Overflow
		char[] c = word.toCharArray();
		char temp = c[curr];
		c[curr] = c[next];
		c[curr] = temp;
		String candidate = new String(c);
		if(words.count(candidate) != 0) {
			trans.add(candidate);
		}
		return trans;
	}


	private Set<Candidate> editOneCandidates(String queryWord, Dictionary words) {
		Set<Candidate> editCandidates = new HashSet<Candidate>();
		for(int i = 0; i < queryWord.length(); i++) {
			Set<String> inserts = generateInsertions(queryWord, i, words);
			Set<String> subs = generateSubstitutions(queryWord, i, words);
			Set<String> deletes = generateDeletions(queryWord, i, words);
			Set<String> trans = new HashSet<String>();
			if(i > 0) {
				trans = generateTranspositions(queryWord, i-1, i, words);
			}

			for(String candidate : inserts) {
				ArrayList<Character> in = new ArrayList<Character>();
				in.add('i');
				editCandidates.add(new Candidate(candidate, 1, in));
			}
			for(String candidate : subs) {
				ArrayList<Character> in = new ArrayList<Character>();
				in.add('s');
				editCandidates.add(new Candidate(candidate, 2, in));
			}
			for(String candidate : deletes) {
				ArrayList<Character> in = new ArrayList<Character>();
				in.add('d');
				editCandidates.add(new Candidate(candidate, 1, in));
			}
			for(String candidate : trans) {
				ArrayList<Character> in = new ArrayList<Character>();
				in.add('t');
				editCandidates.add(new Candidate(candidate, 1, in));
			}
		}
		return editCandidates;
	}

	public Set<Candidate> getCandidates(String query, Dictionary unigram) {
		StringTokenizer st = new StringTokenizer(query);
		ArrayList<Set<Candidate>> candidateSets = new ArrayList<Set<Candidate>>();
		while(st.hasMoreTokens()){
			String queryWord = st.nextToken();
			candidateSets.add(editOneCandidates(queryWord, unigram));
		}

		// Generate Cartesian product for candidate sets
		return cartesianProduct(candidateSets);
	}

}
