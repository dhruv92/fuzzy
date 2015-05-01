package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.stanford.cs276.util.Candidate;
import edu.stanford.cs276.util.Pair;

public class RunCorrector {

	public static LanguageModel languageModel;
	public static NoisyChannelModel nsm;


	public static void main(String[] args) throws Exception {

		long startTime = System.currentTimeMillis();

		// Parse input arguments
		String uniformOrEmpirical = null;
		String queryFilePath = null;
		String goldFilePath = null;
		String extra = null;
		BufferedReader goldFileReader = null;
		if (args.length == 2) {
			// Run without extra and comparing to gold
			uniformOrEmpirical = args[0];
			queryFilePath = args[1];
		}
		else if (args.length == 3) {
			uniformOrEmpirical = args[0];
			queryFilePath = args[1];
			if (args[2].equals("extra")) {
				extra = args[2];
			} else {
				goldFilePath = args[2];
			}
		} 
		else if (args.length == 4) {
			uniformOrEmpirical = args[0];
			queryFilePath = args[1];
			extra = args[2];
			goldFilePath = args[3];
		}
		else {
			System.err.println(
					"Invalid arguments.  Argument count must be 2, 3 or 4" +
							"./runcorrector <uniform | empirical> <query file> \n" + 
							"./runcorrector <uniform | empirical> <query file> <gold file> \n" +
							"./runcorrector <uniform | empirical> <query file> <extra> \n" +
							"./runcorrector <uniform | empirical> <query file> <extra> <gold file> \n" +
							"SAMPLE: ./runcorrector empirical data/queries.txt \n" +
							"SAMPLE: ./runcorrector empirical data/queries.txt data/gold.txt \n" +
							"SAMPLE: ./runcorrector empirical data/queries.txt extra \n" +
					"SAMPLE: ./runcorrector empirical data/queries.txt extra data/gold.txt \n");
			return;
		}

		if (goldFilePath != null ){
			goldFileReader = new BufferedReader(new FileReader(new File(goldFilePath)));
		}

		// Load models from disk
		languageModel = LanguageModel.load(); 
		nsm = NoisyChannelModel.load();
		BufferedReader queriesFileReader = new BufferedReader(new FileReader(new File(queryFilePath)));
		nsm.setProbabilityType(uniformOrEmpirical);

		System.out.println("Unigram Size: " + languageModel.unigram.getTermCount());
		System.out.println("Brigram Size: " + languageModel.bigramDict.size());
		//		System.out.println("Trigram Size: " + languageModel.trigramDict.size());

		int totalCount = 0;
		int yourCorrectCount = 0;
		String query = null;

		/*
		 * Each line in the file represents one query.  We loop over each query and find
		 * the most likely correction
		 */
		while ((query = queriesFileReader.readLine()) != null) {

			String correctedQuery = query;
			CandidateGenerator cg = CandidateGenerator.get();
			// Generate candidates
			//			Set<String> candidates = cg.getCandidates(query, 	languageModel.getTrigramDict(),
			//																languageModel.getTermLookup(),
			//																languageModel.getTrigramIdDict());

			Set<Candidate> candidateSet = cg.getCandidates(query, languageModel.unigram);
			double maxProbability = 0;
			// Find that candidate that produces the max languageModel * noisyChannelModel probability
			System.out.println("NumCandidates: " + candidateSet.size());
			System.out.println("Query: " + query);
			for(Candidate candidate : candidateSet) {
				System.out.println("     Candidate: " + candidate.toString());
				int distance = candidate.getDistance();
				if (distance <= 2) {
					double probability = NoisyChannelModel.calculateCandidateProbability(candidate, query);
					probability += languageModel.calculateQueryProbability(candidate.getCandidate());
					if (probability > maxProbability) {
						maxProbability = probability;
						correctedQuery = candidate.getCandidate();
					}
				}

			}


			if ("extra".equals(extra)) {
				/*
				 * If you are going to implement something regarding to running the corrector, 
				 * you can add code here. Feel free to move this code block to wherever 
				 * you think is appropriate. But make sure if you add "extra" parameter, 
				 * it will run code for your extra credit and it will run you basic 
				 * implementations without the "extra" parameter.
				 */	
			}


			// If a gold file was provided, compare our correction to the gold correction
			// and output the running accuracy
			if (goldFileReader != null) {
				String goldQuery = goldFileReader.readLine();
				if (goldQuery.equals(correctedQuery)) {
					yourCorrectCount++;
				}
				totalCount++;
			}
			System.out.println(correctedQuery);
		}
		System.out.println(yourCorrectCount + " / " + totalCount);

		queriesFileReader.close();
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		// System.out.println("RUNNING TIME: "+totalTime/1000+" seconds ");
	}

	// Calculate Levenshtein Distance between two strings
	private static int calculateEditDistance(String start, String end) {
		return editDistanceDynamic(start, start.length(), end, end.length(), new HashMap<Pair<String, String>, Integer>());

	}

	// Derived from pseudo-code from lecture and Wikipedia
	private static int editDistanceDynamic(String start, int sLength, String end, int eLength, Map<Pair<String, String>, Integer> savedDistances) {
		if (sLength == 0) return eLength;
		if (eLength == 0) return sLength;

		Pair<String, String> s_e = new Pair<String, String>(start, end);
		Pair<String, String> e_s = new Pair<String, String>(end, start);
		if (savedDistances.containsKey(s_e)) return savedDistances.get(s_e);
		if (savedDistances.containsKey(e_s)) return savedDistances.get(e_s);

		int cost = 1;
		if (start.charAt(sLength - 1) == end.charAt(eLength - 1)) cost = 0;

		int editDistance =  Math.min(Math.min(editDistanceDynamic(start, sLength-1, end, eLength, savedDistances) + 1, 
				editDistanceDynamic(start, sLength, end, eLength-1, savedDistances) + 1),
				editDistanceDynamic(start, sLength-1, end, eLength-1, savedDistances) + cost);
		savedDistances.put(s_e, editDistance);
		savedDistances.put(e_s, editDistance);

		return editDistance;

	}
}
