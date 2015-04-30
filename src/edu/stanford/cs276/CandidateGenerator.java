package edu.stanford.cs276;

import java.io.Serializable;
import java.util.*;

import edu.stanford.cs276.util.Pair;

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
	private Set<String> trigramLists(String queryWord, 	Map<Integer, Set<Integer>> trigramDict,
														Map<Integer, String> termLookup,
														Map<String, Integer> trigramIdDict){
		Set<String> candidateSet = new HashSet<String>();
		if(queryWord.length() >= 3){
			for(int i = 2; i < queryWord.length(); i++){
				String trigram = "" + queryWord.charAt(i-2) + queryWord.charAt(i-1) + queryWord.charAt(i);
				if(trigramIdDict.containsKey(trigram)){
					int trigramId = trigramIdDict.get(trigram);
					if (trigramDict.containsKey(trigramId)) {
						Set<Integer> termIdSet = trigramDict.get(trigramId);
						for(int candidateId : termIdSet){
							if(termLookup.containsKey(candidateId)){
								candidateSet.add(termLookup.get(candidateId));
							}
						}
					}
				}
			}
		}

		return candidateSet;
	}


	// Recursively generates the cartesian product for all sets
	private Set<String> cartesianProduct(ArrayList<Set<String>> remainder){
		Set<String> result = new HashSet<String>();
		if(remainder.size() == 1) return remainder.get(0);
		Set<String> current = remainder.get(0);
		ArrayList<Set<String>> remainder_cpy = new ArrayList<Set<String>>();
		for(Set<String> set : remainder){
			remainder_cpy.add(set);
		}
		remainder_cpy.remove(0);
		for(String word : current){
			for(String str : cartesianProduct(remainder_cpy)){
				result.add(word + " " + str);
			}
		}
		return result;
	}

	// Generate all candidates for the target query
	public Set<String> getCandidates(String query, 	Map<Integer, Set<Integer>> trigramDict,
													Map<Integer, String> termLookup,
													Map<String, Integer> trigramIdDict) throws Exception {
		StringTokenizer st = new StringTokenizer(query);
		ArrayList<Set<String>> candidateSets = new ArrayList<Set<String>>();
		while(st.hasMoreTokens()){
			String queryWord = st.nextToken();
			candidateSets.add(trigramLists(queryWord, trigramDict, termLookup, trigramIdDict));
		}

		// Generate Cartesian product for candidate sets
		return cartesianProduct(candidateSets);
	}

}
