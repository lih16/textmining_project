package com.sema4var.test;

import seth.ner.wrapper.Type;
import de.hu.berlin.wbi.objects.MutationMention;

public class IndexedMutation extends MutationMention{

	private int id;

//	public IndexedMutation(int id, MutationMention mm) {
//		super(mm.getStart(), mm.getEnd(), mm.getText(), mm.getRef(), mm.getPosition(), mm.getWtResidue(), mm.getMutResidue(), mm.getType(), mm.getTool());
//		this.id = id;
//	}
	

	public IndexedMutation(int id, int start, int end, String text, String ref, String location, String wild, String mutated, Type type, Tool tool) {
		super(start, end, text, ref, location, wild, mutated, type, tool);
		this.id = id;
	}



	public int getId() {
		return id;
	}
}
