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

	// For smoothing
	private final static int TOTAL_CORPUS_DOCS = 98999;

	public CosineSimilarityScorer(Map<String,Double> idfs) {
		super(idfs);
	}

	/////////////// Weights //////////////////
	double urlweight = -1.5;
	double titleweight = -4.4;
	double bodyweight = -1;
	double headerweight = -1.1;
	double anchorweight = -10.5;

	double smoothingBodyLength = 575; // Smoothing factor when the body length is 0.
	//////////////////////////////////////////

	public double getNetScore(Map<String, Double> documentVector, Map<String,Double> tfQuery) {
		double score = 0.0;

		/*
		 * @//TODO : Your code here
		 */
		//do dot product of query vector & document vector to get score
		for (String term : tfQuery.keySet()) {
			double queryFreq = tfQuery.get(term);
			double docFreq = documentVector.get(term);
			score += queryFreq * docFreq;
		}

		return score;
	}



	public Map<String, Double> getDocumentVector(Map<String, Double> tfQuery, Map<String, Map<String, Double>> tfs) {
		//combine the various term frequencies into one document vector
		Map<String, Double> documentVector = new HashMap<String, Double>();
		for (String term : tfQuery.keySet()) {
			double termScore = 0.0;
			documentVector.put(term, termScore);
			for (String type : tfs.keySet()) {
				Map<String, Double> termFreq = tfs.get(type);
				double weight = 1.0;
				double tf = 1.0;
				if (termFreq.containsKey(term)) {
					switch(type) {
					case "url":
						weight = urlweight;
						tf = termFreq.get(term);
						break;
					case "title":
						weight = titleweight;
						tf = termFreq.get(term);
						break;
					case "body":
						weight = bodyweight;
						tf = termFreq.get(term);
						break;
					case "anchor":
						weight = anchorweight;
						tf = termFreq.get(term);
						break;
					case "header":
						weight = headerweight;
						tf = termFreq.get(term);
						break;
					}
					//TODO decide if we should do sublinear scaling on document term frequencies
					// Sublinear scoring reduced accuracy when applied to any zone
					termScore += weight * tf;
				}
			}
			documentVector.put(term, termScore);
		}
		return documentVector;
	}

	private double subLinearScale(double rawScore) {
		if (rawScore <= 0) return 0;

		return 1 + Math.log(rawScore);
	}

	//Normalize the query frequencies using IDF. Also decide if you want to do sublinear scaling
	public void normalizeQFs(Map<String, Double> tfQuery) {
		//For each term, come up with IDF & multiply against TF
		for (String term : tfQuery.keySet()) {
			double idf;
			if (!idfs.containsKey(term)) {
				// Get the total document count from data
				idf = Math.log(TOTAL_CORPUS_DOCS + 1); 
			} else {
				idf = idfs.get(term);
			}
			double idfWeightedFreq = tfQuery.get(term) * idf; // multiply each TF by IDF
			tfQuery.put(term, idfWeightedFreq); //TODO decide if you want to do sublinear scaling: subLinearScale(idfWeightedFrew)
		}
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

		Map<String, Double> documentVector = getDocumentVector(tfQuery, tfs);

		normalizeQFs(tfQuery);

		return getNetScore(documentVector, tfQuery);
	}

}
