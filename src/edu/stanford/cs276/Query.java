package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is used to store a query sequence.
 */
public class Query {

	List<String> queryWords;
	
	public Query(String query) {
		queryWords = new ArrayList<String>();
		ArrayList<String> queryWordsInput = new ArrayList<String>(Arrays.asList(query.split(" ")));
		
		// force lowercase
		for (String word : queryWordsInput) {
			queryWords.add(word.toLowerCase());
		}
	}
	
	
	
}
