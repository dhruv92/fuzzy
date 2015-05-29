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

	// Various types of term frequencies that you will need
	String[] TFTYPES = {"url","title","body","header","anchor"};
	double smoothingBodyLength = 500;

//	private Map<String,Double> getQueryFreqs(Query q) {
//		Map<String,Double> tfQuery = new HashMap<String, Double>(); // queryWord -> term frequency
//
//		/*
//		 * @//TODO : Your code here
//		 */
//		for (String w : q.words) {
//			if (tfQuery.containsKey(w)) {
//				tfQuery.put(w, tfQuery.get(w) + (1.0 / q.words.size()));
//			} else {
//				tfQuery.put(w, (double) 1 / q.words.size());
//			}
//		}
//
//		return tfQuery;
//	}


	private void normalizeTFs(Map<String,Map<String, Double>> tfs,Document d) {
		/*
		 * @//TODO : Your code here
		 */

		for (Map<String, Double> termFreq: tfs.values()) {
			for (String k : termFreq.keySet()) {
				termFreq.put(k, termFreq.get(k) / ((double) d.body_length + smoothingBodyLength));
			}
		}
	}

	private Map<String,Map<String, Double>> getDocTermFreqs(Document d, Query q) {
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

	private ArrayList<Double> score(Map<String,Map<String, Double>> tfs, Query q, Map<String, Double> idfs) {

		ArrayList<Double> tfidfs = new ArrayList<Double>();
		for (String field : tfs.keySet()) {
			double dot_product = 0.0;
			for (String word : q.words) {
				if(tfs.get(field).containsKey(word) && idfs.containsKey(word)) {
					dot_product += tfs.get(field).get(word) * idfs.get(word);
				}
			}
			tfidfs.add(dot_product);
		}
		return tfidfs;
	}

	private Instances newFieldsDataset() {
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(new Attribute("url_w"));
		attributes.add(new Attribute("title_w"));
		attributes.add(new Attribute("body_w"));
		attributes.add(new Attribute("header_w"));
		attributes.add(new Attribute("anchor_w"));
		attributes.add(new Attribute("relevance_score"));
		
		Instances dataset = new Instances("train_dataset", attributes, 0);
		
		/* Set last attribute as target */
		dataset.setClassIndex(dataset.numAttributes() - 1);
		
		return dataset;
	}

	private Map<Query,List<Document>> loadQueryDocPairs(String train_data_file) {
		Map<Query,List<Document>> queryDocMap = null;
		try {
			queryDocMap = Util.loadTrainData(train_data_file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return queryDocMap;
	}

	private Map<String, Map<String, Double[]>> getTFIDFs(	Instances dataset, 
			Map<Query,List<Document>> queryDocMap,
			Map<String, Double> idfs) {

		Map<String, Map<String, Double[]>> all_tfidfs = new HashMap<String, Map<String, Double[]>>();
		for (Query query : queryDocMap.keySet()) {
			Map<String, Double[]> doc_tfidfs = new HashMap<String, Double[]>();
			for (Document doc : queryDocMap.get(query)) {
				Map<String,Map<String, Double>> termFreqs = getDocTermFreqs(doc, query);
				Double[] instance = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
				ArrayList<Double> query_tfidfs = score(termFreqs, query, idfs);
				for (int i = 0; i < query_tfidfs.size(); i++) {
					instance[i] = query_tfidfs.get(i);
				}
				doc_tfidfs.put(doc.url, instance);
			}
			all_tfidfs.put(query.query, doc_tfidfs);
		}
		return all_tfidfs;
	}

	private Map<String, Map<String, Double>> loadRelevanceLabels(String train_rel_file) {
		Map<String, Map<String, Double>> relMap = null;
		try {
			relMap = Util.loadRelData(train_rel_file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return relMap;
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
		Instances dataset = newFieldsDataset();

		/* Load training data */
		Map<Query,List<Document>> queryDocMap = loadQueryDocPairs(train_data_file);

		/* Calculate score features from training data */
		Map<String, Map<String, Double[]>> features = getTFIDFs(dataset, queryDocMap, idfs);

		/* Load relevance labels */
		Map<String, Map<String, Double>> relMap = loadRelevanceLabels(train_rel_file);

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
		
		Instances dataset = newFieldsDataset();
		
		Map<Query,List<Document>> queryDocMap = loadQueryDocPairs(test_data_file);
		
		Map<String, Map<String, Double[]>> features = getTFIDFs(dataset, queryDocMap, idfs);
		
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
