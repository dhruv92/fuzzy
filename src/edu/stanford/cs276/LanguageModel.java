package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import edu.stanford.cs276.util.Dictionary;
import edu.stanford.cs276.util.Pair;


public class LanguageModel implements Serializable {

	private static LanguageModel lm_;
	
	/* Feel free to add more members here.
	 * You need to implement more methods here as needed.
	 * 
	 * Your code here ...
	 */
	
	// Outputs the probability of this being a query from the training corpus
	// language model
	public double calculateQueryProbability(String query) {
		if (query.isEmpty()) return 0; // handle empty strings not being in the corpus
		
		StringTokenizer queryTokenizer = new StringTokenizer(query);
		String firstWord = queryTokenizer.nextToken();
		double probability = unigramProb(firstWord);
		String lastWord = firstWord;
		while (queryTokenizer.hasMoreTokens()) {
			String nextWord = queryTokenizer.nextToken();
			probability += bigramProb(lastWord, nextWord);
			lastWord = nextWord;
		}
		return probability;
	}
	
	// Output uni gram probability of a word based on the training corpus in log space. 
	// Note this method converts to log space before outputting the probability
	private double unigramProb(String word) {
		System.out.println("          Unigram: " + word);
		System.out.println("          UnigramFrac: " + unigram.count(word) + " / " + unigram.termCount());
		
//		double result = Math.log(unigram.count(word)) - Math.log(unigram.termCount());
		double result = (double)unigram.count(word) / unigram.termCount();
		System.out.println("          UnigramProb: " + result);
		return result;
	}
	
	private static final double INTERPOLATION_LAMDA = 0.1;
	
	// Output the interpolated bigram probability of a pair of words
	// Note this method converts to log space before outputting the probability
	private double bigramProb(String first, String second) {
		System.out.println("          Bigram: " + first + "," + second);
		System.out.println("          BigramFrac: " + countBigramDict(first, second) + " / " + unigram.count(first));
//		double bigramProb = Math.log(countBigramDict(first, second)) - Math.log(unigram.count(first));
		double bigramProb = (double)countBigramDict(first, second) / unigram.count(first);
		System.out.println("          BigramProb: " + bigramProb);
		double interpProb = Math.log(INTERPOLATION_LAMDA * unigramProb(second) + (1 - INTERPOLATION_LAMDA) * (bigramProb));
		System.out.println("          InterpolationProb: " + interpProb);
		
		return interpProb;
	}
	
	Dictionary unigram = new Dictionary();
	// vocab is only distinct terms, helpful for smoothing
//	Set<String> vocab = new HashSet<String>();
	
	// Added a bigram dictionary to make bigram calculations easier
	Map<Pair<String, String>, Integer> bigramDict = new HashMap<Pair<String, String>, Integer>();
	
	// Added a trigram (char, not word) dictionary to help with candidate generation
	// Maps trigram to all words containing that trigram in document set
	// e.g. "app" -> ["apple","apples","applicant","application",...]
//	Map<String, Set<String>> trigramDict = new HashMap<String, Set<String>>();
	
	// trigram -> (set of words) is too expensive in space, will use
	// trigramIds and termIds to convert strings to ints
	// this must be written to disk
//	Map<Integer, Set<Integer>> trigramDict = new HashMap<Integer, Set<Integer>>();
	
	// trigram -> trigramId
	// this must be written to disk
//	Map<String, Integer> trigramIdDict = new HashMap<String, Integer>();
	
	// word -> termId
	// static so it is not written to disk
//	static Map<String, Integer> termIdDict = new HashMap<String, Integer>();
	
	// trigramId -> trigram
	// static so it is not written to disk
//	static Map<Integer, String> trigramLookup = new HashMap<Integer, String>();
	
	// termId -> word
	// this must be written to disk
//	Map<Integer, String> termLookup = new HashMap<Integer, String>();
	
	// Do not call constructor directly since this is a Singleton
	private LanguageModel(String corpusFilePath) throws Exception {
		constructDictionaries(corpusFilePath);
	}


	public void constructDictionaries(String corpusFilePath)
			throws Exception {

		System.out.println("Constructing dictionaries...");
		File dir = new File(corpusFilePath);
		for (File file : dir.listFiles()) {
			if (".".equals(file.getName()) || "..".equals(file.getName())) {
				continue; // Ignore the self and parent aliases.
			}
			System.out.printf("Reading data file %s ...\n", file.getName());
			BufferedReader input = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = input.readLine()) != null) {
				/* This section of the code parses the line into trigram, bigram, and unigram dictionaries */
				StringTokenizer tokenizer = new StringTokenizer(line);
				String lastWord = tokenizer.nextToken();
				unigram.add(lastWord);
//				vocab.add(lastWord);
//				addToTrigramDict(lastWord);
				while (tokenizer.hasMoreTokens()) {
					String nextWord = tokenizer.nextToken();
					unigram.add(nextWord);
//					vocab.add(nextWord);
//					addToTrigramDict(nextWord);
					addToBigramDict(lastWord, nextWord);
					lastWord = nextWord;
				}
			}
			input.close();
		}
		System.out.println("Done.");
	}
	
	// This helper method adds a bigram to the dictionary 
	private void addToBigramDict(String last, String next) {
		Pair<String, String> bigram = new Pair<String, String>(last, next);
		if (bigramDict.containsKey(bigram)) {
			bigramDict.put(bigram, bigramDict.get(bigram) + 1);
		} else {
			bigramDict.put(bigram, 1);
		}
	}
	
	// This helper method splits a words into its trigrams and adds them to the dictionary
//	private void addToTrigramDict(String word) {
//		// Assume word is new to termIdDict,
//		int termId = termIdDict.keySet().size();
//		// If word is not new, assign actual termId
//		if(termIdDict.containsKey(word)) {
//			termId = termIdDict.get(word);
//		} else {
//			termIdDict.put(word, termId);
//			termLookup.put(termId, word);
//		}
//		if(word.length() >= 3) {
//			for (int i = 2; i < word.length(); i++) {
//				String trigram = "" + word.charAt(i-2) + word.charAt(i-1) + word.charAt(i);
//				int trigramId = trigramIdDict.keySet().size();
//				if(trigramIdDict.containsKey(trigram)) {
//					trigramId = trigramIdDict.get(trigram);
//				} else {
//					trigramIdDict.put(trigram, trigramId);
//					trigramLookup.put(trigramId, trigram);
//				}
//				if (trigramDict.containsKey(trigramId)) {
//					if(!trigramDict.get(trigramId).contains(termId)) {
//						trigramDict.get(trigramId).add(termId);
//					}
//				} else {
//					Set<Integer> newSet = new HashSet<Integer>();
//					newSet.add(termId);
//					trigramDict.put(trigramId, newSet);
//				}
//			}
//		}
//	}
	
//	public Map<Integer, Set<Integer>> getTrigramDict() {
//		return trigramDict;
//	}
//	
//	public Map<Integer, String> getTermLookup() {
//		return termLookup;
//	}
//	
//	public Map<String, Integer> getTermIdDict() {
//		return termIdDict;
//	}
//	
//	public Map<Integer, String> getTrigramLookup() {
//		return trigramLookup;
//	}
//	
//	public Map<String, Integer> getTrigramIdDict() {
//		return trigramIdDict;
//	}

	
	// This helper method gets the count of a given bigram in the corpus
	private double countBigramDict(String first, String second) {
		Pair<String, String> bigram = new Pair<String, String>(first, second);
		if (bigramDict.containsKey(bigram)) {
			return bigramDict.get(bigram);
		}
		return 0.0;
	}
	
	// Loads the object (and all associated data) from disk
	public static LanguageModel load() throws Exception {
		try {
			if (lm_==null){
				FileInputStream fiA = new FileInputStream(Config.languageModelFile);
				ObjectInputStream oisA = new ObjectInputStream(fiA);
				lm_ = (LanguageModel) oisA.readObject();
			}
		} catch (Exception e){
			throw new Exception("Unable to load language model.  You may have not run build corrector");
		}
		return lm_;
	}
	
	// Saves the object (and all associated data) to disk
	public void save() throws Exception{
		FileOutputStream saveFile = new FileOutputStream(Config.languageModelFile);
		ObjectOutputStream save = new ObjectOutputStream(saveFile);
		save.writeObject(this);
		save.close();
	}
	
	// Creates a new lm object from a corpus
	public static LanguageModel create(String corpusFilePath) throws Exception {
		if(lm_ == null ){
			lm_ = new LanguageModel(corpusFilePath);
		}
		return lm_;
	}
}
