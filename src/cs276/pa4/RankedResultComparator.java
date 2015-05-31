package cs276.pa4;

import java.util.Comparator;

import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class RankedResultComparator implements Comparator<RankedResult>{
	private Classifier model;
	private Instances dataset;
	
	public RankedResultComparator(Classifier model, Instances dataset) {
		this.model = model;
		this.dataset = dataset;
	}
	
	@Override
	public int compare(RankedResult o1, RankedResult o2) {
		double[] instance_D_1 = o1.inst.toDoubleArray();
		double[] instance_D_2 = o2.inst.toDoubleArray();
		double[] combined_instance = new double[instance_D_1.length];
		
		for (int i = 0; i < instance_D_1.length; i++) {
			combined_instance[i] = instance_D_1[i] - instance_D_2[i];
		}
		
		Instance inst = new DenseInstance(1.0, combined_instance);
		inst.setDataset(dataset);
		
		double prediction = 0.0;
		try {
			prediction = model.classifyInstance(inst);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (prediction == 0) return -1;
		return 1;
	}

}
