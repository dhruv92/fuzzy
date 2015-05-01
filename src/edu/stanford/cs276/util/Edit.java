package edu.stanford.cs276.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Edit implements Serializable {

	
	public static enum EditType {INSERTION, DELETION, SUBSTITUTION, TRANSPOSITION};
	
	private EditType et;
	// Pair(original,replacement)
	private Pair<String, String> stringChange;

	public Edit(EditType et, String original, String replacement) {
		this.et = et;
		this.stringChange = new Pair(original, replacement);
	}
	
	public EditType getEditType() {
		return et;
	}
	
	public Pair<String, String> getChange() {
		return stringChange;
	}

}
