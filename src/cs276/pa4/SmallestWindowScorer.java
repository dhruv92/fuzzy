package cs276.pa4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * A skeleton for implementing the Smallest Window scorer in Task 3.
 * Note: The class provided in the skeleton code extends BM25Scorer in Task 2. However, you don't necessarily
 * have to use Task 2. (You could also use Task 1, in which case, you'd probably like to extend CosineSimilarityScorer instead.)
 */
public class SmallestWindowScorer extends CosineSimilarityScorer {

	/////// Smallest window specific hyper-parameters ////////
	double B = 155; //some arbitrary start     	    
	double boostmod = -1;

	//////////////////////////////

	public SmallestWindowScorer(Map<String, Double> idfs) {
		super(idfs);
	}

	//find the smallest window and set the params
	public void handleSmallestWindow(Document d, Query q) {
		/*
		 * @//TODO : Your code here
		 */
		double curSmallestWindow = Double.POSITIVE_INFINITY;

		//TODO FIll this out for each type 
		if (d.title != null) curSmallestWindow = checkWindow(q, d, d.title, curSmallestWindow, false);
		if (d.anchors != null) {
			for (String anchor : d.anchors.keySet()) {
				curSmallestWindow = checkWindow(q, d, anchor, curSmallestWindow, false);
			}
		}
		if (d.headers != null) {
			for (String header : d.headers) {
				curSmallestWindow = checkWindow(q, d, header, curSmallestWindow, false);
			}
		}
		if (d.body_hits != null) curSmallestWindow = checkWindow(q, d, "", curSmallestWindow, true); //body hits

		if (curSmallestWindow == Double.POSITIVE_INFINITY) {
			boostmod = 1 / B;
		} else {
			boostmod = q.words.size() / curSmallestWindow; //need to make sure dups elimated in query
		}
	}

	//look for the smallest window of the query in the string. If it's lower than the passed in smallest window
	//then return the new window size. If the passed in current smallest window is smaller, send it back
	public double checkWindow(Query q,Document d,String docstr,double curSmallestWindow,boolean isBodyField) {
		/*
		 * @//TODO : Your code here
		 */
		double window;

		//turn a string into a hit list of query terms and positions, or just use body_hits
		Map<String, List<Integer>> hits;
		if (isBodyField) {
			hits = d.body_hits;
		} else {
			hits = turnIntoHits(docstr, q);
		}

		//make sure the query terms are all in the hit list. If not, smallest window stays the same
		if (queryNotFullyContained(q, hits)) {
			return curSmallestWindow;
		}

		window = getSmallestWindowFromHits(hits);

		return (window < curSmallestWindow) ? window : curSmallestWindow;
	}

	//use smallest window algorithm to get the size of the smallest window that contains all terms in hits
	private double getSmallestWindowFromHits(Map<String, List<Integer>> hits) {
		double smallestWindow = Double.POSITIVE_INFINITY;

		SortedSet<String> queryTerms = new TreeSet<String>(hits.keySet());

		List<List<Integer>> termPositions = new ArrayList<List<Integer>>();
		for (String term : queryTerms) {
			List<Integer> positions = new ArrayList<Integer>(hits.get(term));
			termPositions.add(positions);
		}

		List<Integer> curPositions = new ArrayList<Integer>();
		for (List<Integer> positions : termPositions) {
			curPositions.add(positions.remove(0));
		}

		while (true) {
			double window = findWindow(curPositions);
			if (window < smallestWindow) smallestWindow = window;
			if (positionsEmpty(termPositions)) break;
			int minIndex = curPositions.indexOf(Collections.min(curPositions));
			curPositions.set(minIndex, termPositions.get(minIndex).remove(0));
		}

		return smallestWindow;
	}

	//look through the various term position lists. If any are empty, return false. Otherwise, return true
	private boolean positionsEmpty(List<List<Integer>> termPositions) {
		for (List<Integer> positions : termPositions) {
			if (positions.isEmpty()) return true;
		}
		return false;
	}

	//return the window size of this list of positions (max - min)
	private double findWindow(List<Integer> curPositions) {
		if (curPositions.size() <= 1) return 1;

		double min = curPositions.get(0);
		double max = curPositions.get(0);
		for (Integer pos : curPositions) {
			if (pos < min) min = pos;
			if (pos > max) max = pos;
		}

		return max - min;
	}

	//checks to make sure every word in the query is contained in the doc string
	private boolean queryNotFullyContained(Query q, Map<String, List<Integer>> hits) {
		for (String term : q.words) {
			if (!hits.containsKey(term)) return true;
		}
		return false;
	}

	//turns any string into a Map of position hits like body_hits. Only contains the query terms
	private Map<String, List<Integer>> turnIntoHits(String docString, Query q) {
		Map<String, List<Integer>> hits = new HashMap<String, List<Integer>>();
		StringTokenizer tokenizer = new StringTokenizer(docString);
		int pos = 1;
		while (tokenizer.hasMoreTokens()) {
			String nextWord = tokenizer.nextToken();
			if (q.words.contains(nextWord)) {
				List<Integer> positions;
				if (hits.containsKey(nextWord)) {
					positions = hits.get(nextWord);
				} else {
					positions = new ArrayList<Integer>();
				}
				positions.add(pos);
				hits.put(nextWord, positions);
			}
			pos++;
		}
		return hits;
	}


	private double findWindowInString(Query q, String docstr) {
		int window = 0;

		return window;
	}

	//for each term in the document vector, boost it by the appropriate boost amount based on the found
	//parameters of B and boostmod
	private void boostDocumentScore(Map<String, Double> documentVector) {
		for (String term : documentVector.keySet()) {
			documentVector.put(term, documentVector.get(term) * B * boostmod);
		}
	}

	@Override
	public double getSimScore(Document d, Query q) {
		Map<String,Map<String, Double>> tfs = this.getDocTermFreqs(d,q);

		this.normalizeTFs(tfs, d, q);

		Map<String,Double> tfQuery = getQueryFreqs(q);

		Map<String, Double> documentVector = getDocumentVector(tfQuery, tfs);

		normalizeQFs(tfQuery);

		handleSmallestWindow(d, q);

		boostDocumentScore(documentVector);

		return getNetScore(documentVector, tfQuery);
	}

}
