package cs276.pa4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * An abstract class for a scorer. Need to be extended by each specific implementation of scorers.
 */
public abstract class AScorer {
	
	Map<String,Double> idfs; // Map: term -> idf
    // Various types of term frequencies that you will need
	String[] TFTYPES = {"url","title","body","header","anchor"};
	
	public AScorer(Map<String,Double> idfs) {
		this.idfs = idfs;
	}
	
	// Score each document for each query.
	public abstract double getSimScore(Document d, Query q);
	
	// Handle the query vector
	public Map<String,Double> getQueryFreqs(Query q) {
		Map<String,Double> tfQuery = new HashMap<String, Double>(); // queryWord -> term frequency
		
		/*
		 * @//TODO : Your code here
		 */
		for (String w : q.words) {
			if (tfQuery.containsKey(w)) {
				tfQuery.put(w, tfQuery.get(w) + 1);
			} else {
				tfQuery.put(w, (double) 1);
			}
		}
		
		
		
		return tfQuery;
	}
	
	
	////////////// Initialization/Parsing Methods ///////////////
	
	/*
	 * @//TODO : Your code here
	 */

    /////////////////////////////////////////////////////////////
	
	
	/*/
	 * Creates the various kinds of term frequencies (url, title, body, header, and anchor)
	 * You can override this if you'd like, but it's likely that your concrete classes will share this implementation.
	 */
	public Map<String,Map<String, Double>> getDocTermFreqs(Document d, Query q) {
		// Map from tf type -> queryWord -> score
		Map<String,Map<String, Double>> tfs = new HashMap<String,Map<String, Double>>();
		
		////////////////////Initialization/////////////////////
		
		/*
		 * @//TODO : Your code here
		 */
		for (String type : TFTYPES) {
			Map<String, Double> tf = new HashMap<String, Double>();
			tfs.put(type,tf);
		}
		
	    ////////////////////////////////////////////////////////
		
		// Loop through query terms and increase relevant tfs. Note: you should do this to each type of term frequencies.
		for (String queryWord : q.words) {
			/*
			 * @//TODO : Your code here
			 */
			
			//thing to watch out for: lower casing between doc & query, duplicate words in query
			increaseBodyTF(queryWord, tfs.get("body"), d);
			increaseURLTF(queryWord, tfs.get("url"), d);
			increaseHeaderTF(queryWord, tfs.get("header"), d);
			increaseTitleTF(queryWord, tfs.get("title"), d);
			increaseAnchorTF(queryWord, tfs.get("anchor"), d);
		}
		
		return tfs;
	}
	
	private void increaseHeaderTF(String queryWord, Map<String, Double> headerTF, Document d) {
		headerTF.put(queryWord, 0.0);
		if(d.headers != null) {
			for (String header : d.headers) {
				if (header.toLowerCase().contains(queryWord)) {
					headerTF.put(queryWord, headerTF.get(queryWord) + 1);
				}
			}
		}
	}
	
	private void increaseTitleTF(String queryWord, Map<String, Double> titleTF, Document d) {
		titleTF.put(queryWord, 0.0);
		StringTokenizer tokenizer = new StringTokenizer(d.title);
		while (tokenizer.hasMoreTokens()) {
			String word = tokenizer.nextToken().toLowerCase();
			if (queryWord.equals(word)) {
				titleTF.put(queryWord, titleTF.get(queryWord) + 1);
			}
		}
	}
	
	private void increaseAnchorTF(String queryWord, Map<String, Double> anchorTF, Document d) {
		anchorTF.put(queryWord, 0.0);
		if(d.anchors != null) {
			if (d.anchors.containsKey(queryWord)) {
				anchorTF.put(queryWord, (double) d.anchors.get(queryWord));
			}
		}
	}
	
	private void increaseURLTF(String queryWord, Map<String, Double> urlTF, Document d) {
		String[] tokens = d.url.toLowerCase().split("\\P{Alpha}+");
		urlTF.put(queryWord, 0.0);
		for (String t : tokens) {
			if (queryWord.equals(t)) {
				urlTF.put(queryWord, urlTF.get(queryWord) + 1);
			}
		}
	}
	
	private void increaseBodyTF(String queryWord, Map<String, Double> bodyTF, Document d)  {
		if (d.body_hits != null) {
			Map<String, List<Integer>> termPos = d.body_hits;
			List<Integer> positions = termPos.get(queryWord);
			if (positions == null) {
				bodyTF.put(queryWord, 0.0);
			} else {
				bodyTF.put(queryWord, (double) positions.size());
			}
		}
	}

}
