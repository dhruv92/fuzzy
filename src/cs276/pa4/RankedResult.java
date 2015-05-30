package cs276.pa4;

import weka.core.Instance;

public class RankedResult implements Comparable {

	public String url = null;
	public Instance inst = null;
	
	public RankedResult(String url, Instance inst) {
		this.url = url;
		this.inst = inst;
	}
	
	@Override
	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
