package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
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
	private static double calculateQueryProbability(String query) {
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
	private static double unigramProb(String word) {
		double result = Math.log(unigram.count(word)) - Math.log(unigram.termCount());
		return result;
	}
	
	private static final double INTERPOLATION_LAMDA = 0.1;
	
	// Output the interpolated bigram probability of a pair of words
	// Note this method converts to log space before outputting the probability
	private static double bigramProb(String first, String second) {
		double bigramProb = Math.log(countBigramDict(first, second)) - Math.log(unigram.count(first));
		return INTERPOLATION_LAMDA * unigramProb(second) + (1 - INTERPOLATION_LAMDA) * (bigramProb);
	}
	
	static Dictionary unigram = new Dictionary();
	
	// Added a bigram dictionary to make bigram calculations easier
	static Map<Pair<String, String>, Integer> bigramDict = new HashMap<Pair<String, String>, Integer>();
	
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
				/* This section of the code parses the line into bigram + unigram dictionaries */
				StringTokenizer tokenizer = new StringTokenizer(line);
				String lastWord = tokenizer.nextToken();
				unigram.add(lastWord);
				while (tokenizer.hasMoreTokens()) {
					String nextWord = tokenizer.nextToken();
					unigram.add(nextWord);
					addToBigramDict(lastWord, nextWord);
					lastWord = nextWord;
				}
			}
			input.close();
		}
		System.out.println("Done.");
	}
	
	// This helper method adds a bigram to the dictionary 
	private static void addToBigramDict(String last, String next) {
		Pair<String, String> bigram = new Pair<String, String>(last, next);
		if (bigramDict.containsKey(bigram)) {
			bigramDict.put(bigram, bigramDict.get(bigram) + 1);
		} else {
			bigramDict.put(bigram, 1);
		}
	}
	
	// This helper method gets the count of a given bigram in the corpus
	private static double countBigramDict(String first, String second) {
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
