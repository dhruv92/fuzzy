package cs276.pa4;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import weka.core.Attribute;
import weka.core.Instances;

public class Util {
	public static Map<Query,List<Document>> loadTrainData (String feature_file_name) throws Exception {
		Map<Query, List<Document>> result = new HashMap<Query, List<Document>>();

		File feature_file = new File(feature_file_name);
		if (!feature_file.exists() ) {
			System.err.println("Invalid feature file name: " + feature_file_name);
			return null;
		}

		BufferedReader reader = new BufferedReader(new FileReader(feature_file));
		String line = null, anchor_text = null;
		Query query = null;
		Document doc = null;
		int numQuery=0; int numDoc=0;
		while ((line = reader.readLine()) != null) {
			String[] tokens = line.split(":", 2);
			String key = tokens[0].trim();
			String value = tokens[1].trim();

			if (key.equals("query")){
				query = new Query(value);
				numQuery++;
				result.put(query, new ArrayList<Document>());
			} else if (key.equals("url")) {
				doc = new Document();
				doc.url = new String(value);
				result.get(query).add(doc);
				numDoc++;
			} else if (key.equals("title")) {
				doc.title = new String(value);
			} else if (key.equals("header"))
			{
				if (doc.headers == null)
					doc.headers =  new ArrayList<String>();
				doc.headers.add(value);
			} else if (key.equals("body_hits")) {
				if (doc.body_hits == null)
					doc.body_hits = new HashMap<String, List<Integer>>();
				String[] temp = value.split(" ", 2);
				String term = temp[0].trim();
				List<Integer> positions_int;

				if (!doc.body_hits.containsKey(term))
				{
					positions_int = new ArrayList<Integer>();
					doc.body_hits.put(term, positions_int);
				} else
					positions_int = doc.body_hits.get(term);

				String[] positions = temp[1].trim().split(" ");
				for (String position : positions)
					positions_int.add(Integer.parseInt(position));

			} else if (key.equals("body_length"))
				doc.body_length = Integer.parseInt(value);
			else if (key.equals("pagerank"))
				doc.page_rank = Integer.parseInt(value);
			else if (key.equals("anchor_text")) {
				anchor_text = value;
				if (doc.anchors == null)
					doc.anchors = new HashMap<String, Integer>();
			}
			else if (key.equals("stanford_anchor_count"))
				doc.anchors.put(anchor_text, Integer.parseInt(value));      
		}

		reader.close();
		System.err.println("# Signal file " + feature_file_name + ": number of queries=" + numQuery + ", number of documents=" + numDoc);

		return result;
	}

	public static Map<String,Double> loadDFs(String dfFile) throws IOException {
		Map<String,Double> dfs = new HashMap<String, Double>();

		BufferedReader br = new BufferedReader(new FileReader(dfFile));
		String line;
		while((line=br.readLine())!=null){
			line = line.trim();
			if(line.equals("")) continue;
			String[] tokens = line.split("\\s+");
			dfs.put(tokens[0], Double.parseDouble(tokens[1]));
		}
		br.close();
		return dfs;
	}

	/* query -> (url -> score) */
	public static Map<String, Map<String, Double>> loadRelData(String rel_file_name) throws IOException{
		Map<String, Map<String, Double>> result = new HashMap<String, Map<String, Double>>();

		File rel_file = new File(rel_file_name);
		if (!rel_file.exists() ) {
			System.err.println("Invalid feature file name: " + rel_file_name);
			return null;
		}

		BufferedReader reader = new BufferedReader(new FileReader(rel_file));
		String line = null, query = null, url = null;
		int numQuery=0; 
		int numDoc=0;
		while ((line = reader.readLine()) != null) {
			String[] tokens = line.split(":", 2);
			String key = tokens[0].trim();
			String value = tokens[1].trim();

			if (key.equals("query")){
				query = value;
				result.put(query, new HashMap<String, Double>());
				numQuery++;
			} else if (key.equals("url")){
				String[] tmps = value.split(" ", 2);
				url = tmps[0].trim();
				double score = Double.parseDouble(tmps[1].trim());
				result.get(query).put(url, score);
				numDoc++;
			}
		}	
		reader.close();
		System.err.println("# Rel file " + rel_file_name + ": number of queries=" + numQuery + ", number of documents=" + numDoc);

		return result;
	}

	/* start extracting tf idfs, setting up models, etc. helper functions */

	public static Instances newFieldsDataset(String[] attribute_strs, String dataset_name) {
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		for (String att_str : attribute_strs) {
			attributes.add(new Attribute(att_str));
		}
		

		Instances dataset = new Instances(dataset_name, attributes, 0);

		/* Set last attribute as target */
		dataset.setClassIndex(dataset.numAttributes() - 1);

		return dataset;
	}

	public static Map<Query,List<Document>> loadQueryDocPairs(String train_data_file) {
		Map<Query,List<Document>> queryDocMap = null;
		try {
			queryDocMap = Util.loadTrainData(train_data_file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return queryDocMap;
	}

	public static Map<String, Map<String, Double[]>> getTFIDFs(Instances dataset, Map<Query,List<Document>> queryDocMap, Map<String, Double> idfs) {

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

	private static void normalizeTFs(Map<String,Map<String, Double>> tfs,Document d) {
		/*
		 * @//TODO : Your code here
		 */

		for (Map<String, Double> termFreq: tfs.values()) {
			for (String k : termFreq.keySet()) {
				termFreq.put(k, termFreq.get(k) / ((double) d.body_length + smoothingBodyLength));
			}
		}
	}

	static String[] TFTYPES = {"url","title","body","header","anchor"};
	static double smoothingBodyLength = 500;

	private static Map<String,Map<String, Double>> getDocTermFreqs(Document d, Query q) {
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

	private static void increaseHeaderTF(String queryWord, Map<String, Double> headerTF, Document d) {
		headerTF.put(queryWord, 0.0);
		if(d.headers != null) {
			for (String header : d.headers) {
				if (header.toLowerCase().contains(queryWord)) {
					headerTF.put(queryWord, headerTF.get(queryWord) + 1);
				}
			}
		}
	}

	private static void increaseTitleTF(String queryWord, Map<String, Double> titleTF, Document d) {
		titleTF.put(queryWord, 0.0);
		StringTokenizer tokenizer = new StringTokenizer(d.title);
		while (tokenizer.hasMoreTokens()) {
			String word = tokenizer.nextToken().toLowerCase();
			if (queryWord.equals(word)) {
				titleTF.put(queryWord, titleTF.get(queryWord) + 1);
			}
		}
	}

	private static void increaseAnchorTF(String queryWord, Map<String, Double> anchorTF, Document d) {
		anchorTF.put(queryWord, 0.0);
		if(d.anchors != null) {
			if (d.anchors.containsKey(queryWord)) {
				anchorTF.put(queryWord, (double) d.anchors.get(queryWord));
			}
		}
	}

	private static void increaseURLTF(String queryWord, Map<String, Double> urlTF, Document d) {
		String[] tokens = d.url.toLowerCase().split("\\P{Alpha}+");
		urlTF.put(queryWord, 0.0);
		for (String t : tokens) {
			if (queryWord.equals(t)) {
				urlTF.put(queryWord, urlTF.get(queryWord) + 1);
			}
		}
	}

	private static void increaseBodyTF(String queryWord, Map<String, Double> bodyTF, Document d)  {
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

	private static ArrayList<Double> score(Map<String,Map<String, Double>> tfs, Query q, Map<String, Double> idfs) {

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

	public static Map<String, Map<String, Double>> loadRelevanceLabels(String train_rel_file) {
		Map<String, Map<String, Double>> relMap = null;
		try {
			relMap = Util.loadRelData(train_rel_file);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return relMap;
	}

	/* end added / shared helper functions */

	public static void main(String[] args) {
		try {
			System.out.print(loadRelData(args[0]));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
