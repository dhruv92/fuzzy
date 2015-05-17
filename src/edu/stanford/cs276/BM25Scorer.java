package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Skeleton code for the implementation of a BM25 Scorer in Task 2.
 */
public class BM25Scorer extends AScorer {
	Map<Query,Map<String, Document>> queryDict; // query -> url -> document

	public BM25Scorer(Map<String,Double> idfs, Map<Query,Map<String, Document>> queryDict) {
		super(idfs);
		this.queryDict = queryDict;
		this.calcAverageLengths();
	}

	private final static int TOTAL_CORPUS_DOCS = 98999;


	/////////////// Weights /////////////////
	double urlweight = -1.5;
	double titleweight = -4.4;
	double bodyweight = -1;
	double headerweight = -1.1;
	double anchorweight = -10.5;

	/////// BM25 specific weights ///////////
	double burl=-.2;
	double btitle=-.05;
	double bheader=-1.1;
	double bbody=-1;
	double banchor=-20;

	double k1=-1.01;
	double pageRankLambda=-1;
	double pageRankLambdaPrime=300;
	double pageRankLambdaDoublePrime=1;
	//////////////////////////////////////////

	/////// BM25 data structures - feel free to modify ///////

	Map<Document,Map<String,Double>> lengths; // Document -> field -> length
	Map<String,Double> avgLengths;  // field name -> average length
	Map<Document,Double> pagerankScores; // Document -> pagerank score

	//////////////////////////////////////////

	// Set up average lengths for bm25, also handles pagerank
	public void calcAverageLengths() {
		lengths = new HashMap<Document,Map<String,Double>>();
		avgLengths = new HashMap<String,Double>();
		pagerankScores = new HashMap<Document,Double>();

		/*
		 * @//TODO : Your code here
		 */

		
		for (Query query : queryDict.keySet()) {
			Map<String, Document> retrievedDocs = queryDict.get(query);
			for (String url : retrievedDocs.keySet()) {
				Document d = retrievedDocs.get(url);
				Map<String,Double> dLengths = new HashMap<String,Double>();
				double dl = 0.0;
				for (String type : this.TFTYPES) {
					double weight = 1.0;
					double length = 0.0;
					switch(type) {
					case "url":
						weight = urlweight;
						length = 1.0;
						break;
					case "title":
						weight = titleweight;
						length = d.title.split(" ").length;
						break;
					case "body":
						weight = bodyweight;
						length = d.body_length;
						break;
					case "anchor":
						weight = anchorweight;
						double anchor_counts = 0.0;
						if(d.anchors != null) {
							for (String anchor : d.anchors.keySet()) {
								anchor_counts += d.anchors.get(anchor);
							}
						}
						length = anchor_counts;
						break;
					case "header":
						weight = headerweight;
						if (d.headers != null) {
							length = d.headers.size();
						} else {
							length = 0.0;
						}
						break;
					}
					length *= weight;
					if (avgLengths.containsKey(type)) {
						avgLengths.put(type, avgLengths.get(type) + length/TOTAL_CORPUS_DOCS);
					} else {
						avgLengths.put(type, length/TOTAL_CORPUS_DOCS);
					}
					dLengths.put(type, length);
					dl += length;
				} // type loop
				lengths.put(d, dLengths);
				pagerankScores.put(d, (double)d.page_rank);
			} // url (doc) loop
		} // query loop

		//normalize avgLengths
		//TODO : figure out if we want this kind of normalization
		for (String tfType : this.TFTYPES) {
			/*
			 * @//TODO : Your code here
			 */
			double vecLength = 	Math.sqrt(Math.pow(avgLengths.get("url"), 2) +
					Math.pow(avgLengths.get("title"), 2) +
					Math.pow(avgLengths.get("body"), 2) +
					Math.pow(avgLengths.get("anchor"), 2) +
					Math.pow(avgLengths.get("header"), 2));
			avgLengths.put(tfType, avgLengths.get(tfType) / vecLength);

		}

	}

	////////////////////////////////////


	public double getNetScore(Map<String,Map<String, Double>> tfs, Query q, Map<String,Double> tfQuery,Document d) {
		double score = 0.0;

		/*
		 * @//TODO : Your code here
		 */
		Map<String, Double> documentVector = new HashMap<String, Double>();
		for (String term : tfQuery.keySet()) {
			double termScore = 0.0;
			documentVector.put(term, termScore);
			for (String type : this.TFTYPES) {
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
				if (termFreq.containsKey(term)) {
					termScore += weight * termFreq.get(term);
				}
			} //type loop
			documentVector.put(term, termScore);
		} //term loop

		for (String term : tfQuery.keySet()) {
			double idf;
			if (!idfs.containsKey(term)) {
				// Get the total document count from data
				idf = Math.log(TOTAL_CORPUS_DOCS + 1); 
			} else {
				idf = idfs.get(term);
			}
			//TODO figure out what vj function is
			double vj = V_func(d.page_rank);
			score += (documentVector.get(term) * idf) / (k1 + documentVector.get(term)) + pageRankLambda*vj;
		}
		return score;
	}
	
	private double V_func(double pageRank) {
		// three options provided by lecture
//		return Math.log(pageRankLambdaPrime * pageRank);
		return pageRank / (pageRankLambdaPrime + pageRank);
//		return 1.0 / (pageRankLambdaPrime + Math.exp(-pageRank * pageRankLambdaDoublePrime));
	}


	//do bm25 normalization
	public void normalizeTFs(Map<String,Map<String, Double>> tfs,Document d, Query q) {
		/*
		 * @//TODO : Your code here
		 */
		for (String type: tfs.keySet()) {
			Map<String, Double> termFreq = tfs.get(type);
			double bweight = 1.0;
			switch(type) {
			case "url":
				bweight = burl;
				break;
			case "title":
				bweight = btitle;
				break;
			case "body":
				bweight = bbody;
				break;
			case "anchor":
				bweight = banchor;
				break;
			case "header":
				bweight = bheader;
				break;
			}
			double avlen = avgLengths.get(type);
			for (String k : termFreq.keySet()) {
				double tf = termFreq.get(k);
				double len = lengths.get(d).get(type);
				double normalizedTF = tf / (1 + bweight*(len/avlen - 1));
				termFreq.put(k, normalizedTF);
			} // term loop
			tfs.put(type, termFreq);
		} // type loop

	}


	@Override
	public double getSimScore(Document d, Query q) {

		Map<String,Map<String, Double>> tfs = this.getDocTermFreqs(d,q);

		this.normalizeTFs(tfs, d, q);

		Map<String,Double> tfQuery = getQueryFreqs(q);

		return getNetScore(tfs,q,tfQuery,d);
	}

}
