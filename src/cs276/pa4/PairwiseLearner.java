package cs276.pa4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import weka.classifiers.Classifier;
import weka.classifiers.functions.LibSVM;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Standardize;

public class PairwiseLearner extends Learner {
  private LibSVM model;
  public PairwiseLearner(boolean isLinearKernel){
    try{
      model = new LibSVM();
    } catch (Exception e){
      e.printStackTrace();
    }
    
    if(isLinearKernel){
      model.setKernelType(new SelectedTag(LibSVM.KERNELTYPE_LINEAR, LibSVM.TAGS_KERNELTYPE));
    }
  }
  
  public PairwiseLearner(double C, double gamma, boolean isLinearKernel){
    try{
      model = new LibSVM();
    } catch (Exception e){
      e.printStackTrace();
    }
    
    model.setCost(C);
    model.setGamma(gamma); // only matter for RBF kernel
    if(isLinearKernel){
      model.setKernelType(new SelectedTag(LibSVM.KERNELTYPE_LINEAR, LibSVM.TAGS_KERNELTYPE));
    }
  }
  
  /* start extract feature helper function */
  
  private static void standardizeFeatures(Map<String, Map<String, Double[]>> features) throws Exception {
	  Instances stardizationDataset = Util.newPairwiseFieldsDataset("standardizedataset");
	  
	  //put all the features into instances, maintain a pointer to the index in the dataset
	  int index = 0;
	  Map<String, Integer> positionInDataset = new HashMap<String, Integer>(); //map of query+url -> dataset position
	  for (String query : features.keySet()) {
		  for (String url : features.get(query).keySet()) {
			  Double[] instance_D = features.get(query).get(url);
			  Instance inst = new DenseInstance(instance_D.length);
			  inst.setDataset(stardizationDataset);
			  for (int i = 0; i < instance_D.length; i++) {
				  inst.setValue(i, instance_D[i]);
			  }
			  //Here I am skipping setting the label. It shouldn't matter when standardizing
			  stardizationDataset.add(inst);
			  positionInDataset.put(query+url,index);
			  index++;
		  }
	  }
	  
	  //standardize
	  Standardize filter = new Standardize();
	  filter.setInputFormat(stardizationDataset);
	  Instances postDataset = Filter.useFilter(stardizationDataset, filter);
	  
	  //put that shit back in the map
	  for (String query : features.keySet()) {
		  for (String url : features.get(query).keySet()) {
			  Instance inst = postDataset.get(positionInDataset.get(query+url));
			  Double[] instance_D = new Double[features.get(query).get(url).length];
			  for (int i = 0; i < instance_D.length;i++) {
				  instance_D[i] = inst.value(i);
			  }
			  features.get(query).put(url, instance_D);
		  }
	  }  
  }
  
  private void createDifferenceSets(Map<String, Map<String, Double[]>> features, 
		  Map<String, Map<String, Double>> relMap, 
		  Map<String, Map<Pair<String,String>, Double[]>> pos,
		  Map<String,Map<Pair<String,String>, Double[]>> neg) {
	  
	  for (String query : features.keySet()) {
		  Set<String> docs = features.get(query).keySet();
		  List<Pair<String, String>> pairs = makeAllPairs(docs, query, relMap);
		  Map<Pair<String, String>, Double[]> posDocPairsInstances = new HashMap<Pair<String, String>, Double[]>();
		  Map<Pair<String, String>, Double[]> negDocPairsInstances = new HashMap<Pair<String, String>, Double[]>();
		  for (Pair<String, String> pair : pairs) {  
			  Double[] difference = diff(features.get(query).get(pair.getFirst()), features.get(query).get(pair.getSecond()));
			  double firstScore = relMap.get(query).get(pair.getFirst());
			  double secondScore = relMap.get(query).get(pair.getSecond());
			  if (firstScore - secondScore > 0) {
				  posDocPairsInstances.put(pair, difference);
			  } else if (firstScore - secondScore < 0){
				  negDocPairsInstances.put(pair, difference);
			  }
		  }
		  if (!posDocPairsInstances.isEmpty()) pos.put(query, posDocPairsInstances);
		  if (!negDocPairsInstances.isEmpty()) neg.put(query, negDocPairsInstances);
	  }
	  
	  balancePosAndNeg(pos, neg, features, relMap);
	  
  }
  
  
  
  private void balancePosAndNeg(Map<String, Map<Pair<String,String>, Double[]>> pos,
		  Map<String,Map<Pair<String,String>, Double[]>> neg, 
		  Map<String, Map<String, Double[]>> features, 
		  Map<String, Map<String, Double>> relMap) {
	  int posSize = sizeExampleSet(pos);
	  int negSize = sizeExampleSet(neg);
	  
	  if (posSize > negSize + 1) {
		  move(pos, neg, features, relMap, (posSize - negSize) / 2);
	  } else if (negSize > posSize + 1) {
		  move(neg, pos, features, relMap, (negSize - posSize) / 2);
	  }
  }
  
  //flip examples and move them from the pos set to the neg set or vice versa
  private void move(Map<String, Map<Pair<String,String>, Double[]>> from,
		  Map<String,Map<Pair<String,String>, Double[]>> to, 
		  Map<String, Map<String, Double[]>> features, 
		  Map<String, Map<String, Double>> relMap, int number) {
	  Random r = new Random();
	  List<String> queries = new ArrayList<String>(from.keySet());
	  for (int i = 0; i < number; i++) {
		  String randQuery = queries.get(r.nextInt(queries.size()));
		  Map<Pair<String, String>, Double[]> fromList = from.get(randQuery);
		  if (fromList.keySet().size() > 0) {
			  ArrayList<Pair<String,String>> keys = new ArrayList<Pair<String, String>>(fromList.keySet());
			  Pair<String, String> oPair = keys.get(0);
			  fromList.remove(oPair); // pull it out of original
			  if (fromList.isEmpty()) { // pull the query out, since the list is empty
				queries.remove(randQuery);  
			  	from.remove(randQuery);
			  }
			  Pair<String, String> newPair = new Pair<String, String>(oPair.getSecond(), oPair.getFirst());
			  Double[] newFirstFeatures = features.get(randQuery).get(newPair.getFirst());
			  Double[] newSecondFeatures = features.get(randQuery).get(newPair.getSecond());
			  Double newFirstScore = relMap.get(randQuery).get(newPair.getFirst());
			  Double newSecondScore = relMap.get(randQuery).get(newPair.getSecond());
			  Double[] difference = diff(newFirstFeatures, newSecondFeatures);
			  if (to.containsKey(randQuery)) {
				  to.get(randQuery).put(newPair, difference); // put it in the new map
			  } else {
				  Map<Pair<String, String>, Double[]> queryDocPairs = new HashMap<Pair<String, String>, Double[]>();
				  queryDocPairs.put(newPair, difference);
				  to.put(randQuery, queryDocPairs);
			  }
		  }
	  }
  }
  
  private int sizeExampleSet(Map<String, Map<Pair<String,String>, Double[]>> pos) {
	  int count = 0;
	  for (String query : pos.keySet()) {
		  count += pos.get(query).keySet().size();
	  }
	  return count;
  }
  
  private Double[] diff(Double[] first, Double[] second) {
	  Double[] result = new Double[first.length];
	  for (int i = 0; i < first.length; i++) {
		  result[i] = first[i] - second[i];
	  }
	  return result;
  }
  
  /* efficiently generate all pairs, ignore pairs where the relevance score of the documents are the same */
  private List<Pair<String, String>> makeAllPairs(Set<String> docs, String query,  Map<String, Map<String, Double>> relMap) {
	  List<String> docList = new ArrayList<String>(docs);
	  List<Pair<String, String>> allPairs = new ArrayList<Pair<String,String>>();
	  
	  for (int i = 0; i < docList.size(); i++) {
		  for (int j = i + 1; j < docList.size(); j++) {
			  String firstDoc = docList.get(i);
			  String secondDoc = docList.get(j);
			  if (relMap.get(query).get(firstDoc) != relMap.get(query).get(secondDoc)) {
				  Pair<String, String> docPair = new Pair<String, String>(firstDoc, secondDoc);
				  allPairs.add(docPair);
			  }
		  }
	  }
	  
	  return allPairs;
  }
  
  
  /* end extract feature helper functions */
  
	@Override
	public Instances extract_train_features(String train_data_file,
			String train_rel_file, Map<String, Double> idfs) throws Exception {
		
		/*Build attributes list*/
		Instances dataset = Util.newPairwiseFieldsDataset("traindataset");
		
		/*Load training data */
		Map<Query, List<Document>> queryDocMap = Util.loadQueryDocPairs(train_data_file);
		
		/* Calculate score features from training data for each doc */
		Map<String, Map<String, Double[]>> features = Util.getTFIDFs(queryDocMap, idfs, false);
		
		/* Standardize the score features */
		standardizeFeatures(features);
		
		/*Load relevance labels */
		Map<String, Map<String, Double>> relMap = Util.loadRelevanceLabels(train_rel_file);
		
		/* Create difference pairs w/ ranking. Equalize sets */
		Map<String, Map<Pair<String,String>, Double[]>> pos = new HashMap<String, Map<Pair<String,String>, Double[]>>();
		Map<String, Map<Pair<String,String>, Double[]>> neg = new HashMap<String, Map<Pair<String,String>, Double[]>>();;
		createDifferenceSets(features, relMap, pos, neg);
		
		/* add to dataset both the positive & negative features */
		for (String query : pos.keySet()) {
			for (Pair<String, String> docPair : pos.get(query).keySet()) {
				Double[] difference = pos.get(query).get(docPair);
				Instance inst = new DenseInstance(difference.length);
				inst.setDataset(dataset);
				for (int i = 0; i < difference.length; i++) {
					inst.setValue(i, difference[i]);
				}
				inst.setValue(difference.length-1, 0); //setting final class value to "first"
				dataset.add(inst);
			}
		}
		
		for (String query : neg.keySet()) {
			for (Pair<String, String> docPair : neg.get(query).keySet()) {
				Double[] difference = neg.get(query).get(docPair);
				Instance inst = new DenseInstance(difference.length);
				inst.setDataset(dataset);
				for (int i = 0; i < difference.length; i++) {
					inst.setValue(i, difference[i]);
				}
				inst.setValue(difference.length-1, 1); //setting final class value to "second"
				dataset.add(inst);
			}
		}
		
		return dataset;
	}

	@Override
	public Classifier training(Instances dataset) {
		/*
		 * @TODO: Your code here
		 */
		
		//TODO Try adjusting C and gamma
		
		try {
			model.buildClassifier(dataset);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return model;
	}

	@Override
	public TestFeatures extract_test_features(String test_data_file,
			Map<String, Double> idfs) throws Exception {
		TestFeatures tester = new TestFeatures();
		Instances dataset = Util.newPairwiseFieldsDataset("testdataset");
		
		Map<Query, List<Document>> queryDocMap = Util.loadQueryDocPairs(test_data_file);
		Map<String, Map<String, Double[]>> features = Util.getTFIDFs(queryDocMap, idfs, false);
		standardizeFeatures(features);
		Map<String, Map<String, Integer>> index_map = new HashMap<String, Map<String, Integer>>();
		Integer counter = 0;
		
		for (String query : features.keySet()) {
			Map<String, Integer> query_indexes = new HashMap<String, Integer>();
			for (String url : features.get(query).keySet()) {
				Double[] difference = features.get(query).get(url);
				Instance inst = new DenseInstance(difference.length);
				inst.setDataset(dataset);
				for (int i = 0; i < difference.length; i++) {
					inst.setValue(i, difference[i]);
				}
				//inst.setValue(difference.length-1, "first"); shouldn't matter
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
	
	/* helpers */
	
	private List<RankedResult> formResultObjectList(List<String> docList, TestFeatures tf, String query) {
		List<RankedResult> list = new ArrayList<RankedResult>();
		for (String doc : docList) {
			Integer index = tf.index_map.get(query).get(doc);
			Instance inst = tf.features.instance(index);
			RankedResult rankRes = new RankedResult(doc, inst);
			list.add(rankRes);
		}
		return list;
	}
	
	private List<String> getResultList(List<RankedResult> rankResults) {
		List<String> list = new ArrayList<String>();
		for (RankedResult rank : rankResults) {
			list.add(rank.url);
		}
		return list;
	}
	
	/* end helpers */
	

	@Override
	public Map<String, List<String>> testing(TestFeatures tf,
			Classifier model) {
		Map<String, List<String>> relevance_predictions = new HashMap<String, List<String>>();
		RankedResultComparator comparator = new RankedResultComparator(model, Util.newPairwiseFieldsDataset("differencedataset"));
		for (String query : tf.index_map.keySet()) {
			Map<String, Integer> queryMap = tf.index_map.get(query);
			List<String> docList = new ArrayList<String>(queryMap.keySet());
			List<RankedResult> results = formResultObjectList(docList, tf, query);
			Collections.sort(results, comparator);
			relevance_predictions.put(query, getResultList(results));
		}
		return relevance_predictions;
	}

}
