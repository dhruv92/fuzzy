package cs276.pa4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

public class PointwiseLearner extends Learner {

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
				"relevance_score"}, 
				"train_dataset");

		/* Load training data */
		Map<Query,List<Document>> queryDocMap = Util.loadQueryDocPairs(train_data_file);

		/* Calculate score features from training data */
		Map<String, Map<String, Double[]>> features = Util.getTFIDFs(queryDocMap, idfs);

		/* Load relevance labels */
		Map<String, Map<String, Double>> relMap = Util.loadRelevanceLabels(train_rel_file);

		/* Pair score features with relevance labels and add to dataset */
		for (String query : features.keySet()) {
			for (String url : features.get(query).keySet()) {
				Double[] instance_D = features.get(query).get(url);
				double[] instance_d = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0}; //needs to be primative for DenseInstance
				for (int i = 0; i < instance_d.length; i++) {
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
				"relevance_score"}, 
				"test_dataset");
		
		Map<Query,List<Document>> queryDocMap = Util.loadQueryDocPairs(test_data_file);
		
		Map<String, Map<String, Double[]>> features = Util.getTFIDFs(queryDocMap, idfs);
		
		Map<String, Map<String, Integer>> index_map = new HashMap<String, Map<String, Integer>>();
		Integer counter = 0;
		
		for (String query : features.keySet()) {
			Map<String, Integer> query_indexes = new HashMap<String, Integer>();
			for (String url : features.get(query).keySet()) {
				Double[] instance_D = features.get(query).get(url);
				double[] instance_d = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0}; //needs to be primative for DenseInstance
				for (int i = 0; i < instance_d.length; i++) {
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
