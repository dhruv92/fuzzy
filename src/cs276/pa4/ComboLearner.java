package cs276.pa4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;








//import edu.stanford.cs276.Document;
//import edu.stanford.cs276.Query;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class ComboLearner extends Learner {
	
	Map<String, Map<String, Double[]>> combineFeatures(Map<Query,List<Document>> queryDocMap, Map<String, Double> idfs) {
		
		Map<String, Map<String, Double[]>> comboFeatures = new HashMap<String, Map<String, Double[]>>(); 
		
		Map<String, Map<String, Double[]>> tfidfs = Util.getTFIDFs(queryDocMap, idfs);
		Map<String, Map<String, Double>> bm25_scores = null;//Util.getBM25s();
		Map<String, Map<String, Double>> smallest_windows = null;//Util.getSmallestWindows();
		Map<String, Map<String, Double>> pageranks = null;//Util.getPageranks();
		
		for (String query : tfidfs.keySet()) {
			Map<String, Double[]> query_doc_set = new HashMap<String, Double[]>();
			for (String url : tfidfs.get(query).keySet()) {
				ArrayList<Double> doc_features = new ArrayList<Double>();
				for (Double field_tfidf : tfidfs.get(query).get(url)) {
					doc_features.add(field_tfidf);
				}
				if(bm25_scores != null) doc_features.add(bm25_scores.get(query).get(url));
				if(smallest_windows != null) doc_features.add(smallest_windows.get(query).get(url));
				if(pageranks != null) doc_features.add(pageranks.get(query).get(url));
				Double[] doc_arr = new Double[doc_features.size()];
				query_doc_set.put(url, doc_features.toArray(doc_arr));
			}
			comboFeatures.put(query, query_doc_set);
		}
		return comboFeatures;
	}

	@Override
	public Instances extract_train_features(String train_data_file,
			String train_rel_file, Map<String, Double> idfs) {

		/*
		 * @TODO: Below is a piece of sample code to show 
		 * you the basic approach to construct a Instances 
		 * object, replace with your implementation. 
		 */

		/* Build attributes list */
		Instances dataset = Util.newFieldsDataset(new String[]{
				"url_w",
				"title_w",
				"body_w",
				"header_w",
				"anchor_w",
//				"bm25_score_w",
//				"smallest_window_w",
//				"pagerank_w",
				"relevance_score"}, 
				"train_dataset");

		/* Load training data */
		Map<Query,List<Document>> queryDocMap = Util.loadQueryDocPairs(train_data_file);

		/* Calculate score features from training data */
		Map<String, Map<String, Double[]>> features = combineFeatures(queryDocMap, idfs);

		/* Load relevance labels */
		Map<String, Map<String, Double>> relMap = Util.loadRelevanceLabels(train_rel_file);

		/* Pair score features with relevance labels and add to dataset */
		for (String query : features.keySet()) {
			for (String url : features.get(query).keySet()) {
				Double[] instance_D = features.get(query).get(url);
				double[] instance_d = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0}; //needs to be primative for DenseInstance
				for (int i = 0; i < instance_D.length; i++) {
					instance_d[i] = instance_D[i];
				}
				instance_d[instance_d.length-1] = relMap.get(query).get(url);
				Instance inst = new DenseInstance(1.0, instance_d);
				dataset.add(inst);
			}
		}

		return dataset;
	}

	@Override
	public Classifier training(Instances dataset) {
		LinearRegression model = new LinearRegression();
		try {
			model.buildClassifier(dataset);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return model;
	}

	@Override
	public TestFeatures extract_test_features(String test_data_file,
			Map<String, Double> idfs) {
		TestFeatures tester = new TestFeatures();
		
		Instances dataset = Util.newFieldsDataset(new String[]{
				"url_w",
				"title_w",
				"body_w",
				"header_w",
				"anchor_w",
//				"bm25_score_w",
//				"smallest_window_w",
//				"pagerank_w",
				"relevance_score"}, 
				"test_dataset");
		
		Map<Query,List<Document>> queryDocMap = Util.loadQueryDocPairs(test_data_file);
		
		Map<String, Map<String, Double[]>> features = combineFeatures(queryDocMap, idfs);
		
		Map<String, Map<String, Integer>> index_map = new HashMap<String, Map<String, Integer>>();
		Integer counter = 0;
		
		for (String query : features.keySet()) {
			Map<String, Integer> query_indexes = new HashMap<String, Integer>();
			for (String url : features.get(query).keySet()) {
				Double[] instance_D = features.get(query).get(url);
				double[] instance_d = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0}; //needs to be primative for DenseInstance
				for (int i = 0; i < instance_D.length; i++) {
					instance_d[i] = instance_D[i];
				}
				Instance inst = new DenseInstance(1.0, instance_d);
				
				dataset.add(inst);
				query_indexes.put(url, counter);
				counter++;
			}
			index_map.put(query, query_indexes);
		}
		
		tester.features = dataset;
		tester.index_map = index_map;

		return tester;
	}
	
	private List<String> sort(Map<String, Double> doc_rel_map) {
		List<String> sorted_urls = new LinkedList<String>();
		for (String url : doc_rel_map.keySet()) {
			double relevance = doc_rel_map.get(url);
			if(sorted_urls.isEmpty()) {
				sorted_urls.add(url);
				continue;
			}
			boolean inserted = false;
			for (int i = 0; i < sorted_urls.size(); i++) {
				if(doc_rel_map.get(sorted_urls.get(i)) < relevance) {
					sorted_urls.add(i, url);
					inserted = true;
					break;
				}
			}
			if(!inserted) sorted_urls.add(url);
		}
		return sorted_urls;
	}

	@Override
	public Map<String, List<String>> testing(TestFeatures tf,
			Classifier model) {
		Map<String, List<String>> relevance_predictions = new HashMap<String, List<String>>();
		for (String query : tf.index_map.keySet()) {
			Map<String, Double> doc_rel_map = new HashMap<String, Double>();
			for (String url : tf.index_map.get(query).keySet()) {
				int index = tf.index_map.get(query).get(url);
				Instance inst = tf.features.instance(index);
				double prediction = 0.0;
				try {
					prediction = model.classifyInstance(inst);
				} catch (Exception e) {
					e.printStackTrace();
				}
				doc_rel_map.put(url, prediction);
			}
			
//			System.out.println(query);
//			for (String url : doc_rel_map.keySet()) {
//				System.out.println("     " + url + " " + doc_rel_map.get(url));
//			}
//			
//			System.out.println(query);
//			List<String> sorted = sort(doc_rel_map);
//			for (String url : sorted) {
//				System.out.println(url);
//			}
			
			relevance_predictions.put(query, sort(doc_rel_map));
		}
		return relevance_predictions;
	}

}
