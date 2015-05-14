package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Skeleton code for the implementation of a Cosine Similarity Scorer in Task 1.
 */
public class CosineSimilarityScorer extends AScorer {

	public CosineSimilarityScorer(Map<String,Double> idfs) {
		super(idfs);
	}

	/////////////// Weights //////////////////
	double urlweight = -1;
	double titleweight = -1;
	double bodyweight = -1;
	double headerweight = -1;
	double anchorweight = -1;

	double smoothingBodyLength = 500; // Smoothing factor when the body length is 0.
	//////////////////////////////////////////

	public double getNetScore(Map<String, Map<String, Double>> tfs, Query q, Map<String,Double> tfQuery, Document d) {
		double score = 0.0;
		
		/*
		 * @//TODO : Your code here
		 */
		//combine the various term frequencies into one document vector
		Map<String, Double> documentVector = new HashMap<String, Double>();
		for (String term : tfQuery.keySet()) {
			double termScore = 0.0;
			documentVector.put(term, termScore);
			for (String type : tfs.keySet()) {
				Map<String, Double> termFreq = tfs.get(type);
				double weight = 1.0;
				switch(type) {
					case "url":
						weight = urlweight;
						break;
					case "title":
						weight = titleweight;
						break;
					case "body":
						weight = bodyweight;
						break;
					case "anchor":
						weight = anchorweight;
						break;
					case "header":
						weight = headerweight;
						break;
				}
				//TODO decide if we should do sublinear scaling on document term frequencies
				termScore += weight * termFreq.get(term); //weight * subLinearScale(termFreq.get(term))
			}
		}
		
		//normalize the query vector by the idfs. For each term, come up with IDF & multiply against TF
		for (String term : tfQuery.keySet()) {
			double idf;
			if (!idfs.containsKey(term)) {
				//TODO get the total document count from data
				idf = Math.log(totalDocCount + 1); 
			} else {
				idf = idfs.get(term);
			}
		}
		
		//do dot product of query vector & document vector to get score
		for (String term : tfQuery.keySet()) {
			//TODO decide if we should do sublinear scaling on query term frequencies
			double queryFreq = tfQuery.get(term); //subLinearScale(tfQuery.get(term)
			double docFreq = documentVector.get(term);
			score += queryFreq * docFreq;
		}
		
		return score;
	}
	
	private double subLinearScale(double rawScore) {
		if (rawScore <= 0) return 0;
		
		return 1 + Math.log(rawScore);
	}
	
	// Normalize the term frequencies. Note that we should give uniform normalization to all fields as discussed
	// in the assignment handout.
	public void normalizeTFs(Map<String,Map<String, Double>> tfs,Document d, Query q) {
		/*
		 * @//TODO : Your code here
		 */
		
		for (Map<String, Double> termFreq: tfs.values()) {
			for (String k : termFreq.keySet()) {
				termFreq.put(k, termFreq.get(k) / ((double) d.body_length + smoothingBodyLength));
			}
		}
	}


	@Override
	public double getSimScore(Document d, Query q) {
		
		Map<String,Map<String, Double>> tfs = this.getDocTermFreqs(d,q);
		
		this.normalizeTFs(tfs, d, q);
		
		Map<String,Double> tfQuery = getQueryFreqs(q);

	    return getNetScore(tfs,q,tfQuery,d);
	}

}
