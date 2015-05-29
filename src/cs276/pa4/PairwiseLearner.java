package cs276.pa4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	  Instances stardizationDataset = Util.newFieldsDataset();
	  
	  //put all the features into instances, maintain a pointer to the index in the dataset
	  int index = 0;
	  Map<String, Integer> positionInDataset = new HashMap<String, Integer>(); //map of query+url -> dataset position
	  for (String query : features.keySet()) {
		  for (String url : features.get(query).keySet()) {
			  Double[] instance_D = features.get(query).get(url);
			  double[] instance_d = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
			  for (int i = 0; i < instance_d.length; i++) {
				  instance_d[i] = instance_D[i];
			  }
			  //Here I am skipping setting the label. It shouldn't matter when stadardizing
			  Instance inst = new DenseInstance(1.0, instance_d);
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
			  double[] instance_d = inst.toDoubleArray();
			  Double[] instance_D = new Double[instance_d.length];
			  for (int i = 0; i < instance_d.length;i++) {
				  instance_D[i] = instance_d[i];
			  }
			  features.get(query).put(url, instance_D);
		  }
	  }
	  
  }
  
  /* end extract feature helper functions */
  
	@Override
	public Instances extract_train_features(String train_data_file,
			String train_rel_file, Map<String, Double> idfs) throws Exception {
		/*
		 * @TODO: Your code here
		 */
		
		/*Build attributes list*/
		Instances dataset = Util.newFieldsDataset();
		
		/*Load training data */
		Map<Query, List<Document>> queryDocMap = Util.loadQueryDocPairs(train_data_file);
		
		/* Calculate score features from training data for each doc */
		Map<String, Map<String, Double[]>> features = Util.getTFIDFs(dataset, queryDocMap, idfs);
		
		/* Standardize the score features */
		standardizeFeatures(features);
		
		/*Load relevance labels */
		Map<String, Map<String, Double>> relMap = Util.loadRelevanceLabels(train_rel_file);
		
		/* Create difference pairs w/ ranking. Equalize sets */
		
		/* add to dataset */
		
		
		
		return dataset;
	}

	@Override
	public Classifier training(Instances dataset) {
		/*
		 * @TODO: Your code here
		 */
		return null;
	}

	@Override
	public TestFeatures extract_test_features(String test_data_file,
			Map<String, Double> idfs) {
		/*
		 * @TODO: Your code here
		 */
		return null;
	}

	@Override
	public Map<String, List<String>> testing(TestFeatures tf,
			Classifier model) {
		/*
		 * @TODO: Your code here
		 */
		return null;
	}

}
