package com.sema4var.test;

import java.io.File;
import java.io.FileInputStream;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import seth.ner.wrapper.Type;
import de.hu.berlin.wbi.objects.MutationMention.Tool;



public class SethNormRunner {
	private static final int PARTS = 5;
	private static String table = "kb_CLG_cutation.mutations_PDF";
	private static int part = -1;
	
	public static void main(String[] args) throws Exception {
		System.out.println("Running SETH Norm7777");
		
		System.out.println(de.hu.berlin.wbi.objects.MutationMention.Tool.MUTATIONFINDER);
		Tool.valueOf("MUTATIONFINDER");
		//System.exit(0);
		
		if(args.length == 1)
			table = args[0];
		else if(args.length == 2){
			table = args[0];
			part =Integer.parseInt(args[1]); 
		}
		
		System.out.println("Table = '" +table +"'");
		System.out.println("Part = '" +part +"'");
 
		final Properties property = new Properties();
		property.loadFromXML(new FileInputStream(new File("resources/myProperty.xml"))); //The connection to the database is stored in a property file
		final MySQL database = new MySQL(property);	//Only used for normalization
		database.connect();
		HashMap<Integer, List<IndexedMutation>> allMutations = new HashMap<Integer, List<IndexedMutation>>();		
		System.out.println("Fetching mutations from database "+table);
		//if(part == -1)
		database.query("SELECT * FROM " +table +" WHERE tool IS NOT NULL AND tool != 'DBSNP' AND type != 'STRUCTURAL ABNORMALITY'");
		//else
		//	database.query("SELECT * FROM " +table +" WHERE tool IS NOT NULL AND tool != 'DBSNP' AND type != 'STRUCTURAL ABNORMALITY' AND analyzed IS false AND pmid %"+PARTS  +"=" +part);
		ResultSet rs = database.getRs();
		while(rs.next()){
			int pmid = rs.getInt("pmid");
			System.out.println(pmid);
			System.out.println(rs.getString("tool"));
			System.out.println(Tool.valueOf("MUTATIONFINDER"));
			//System.exit(0);
			
			IndexedMutation mm  = new IndexedMutation(rs.getInt("mID"),rs.getInt("begin"), rs.getInt("end"),rs.getString("entity"), rs.getString("refSeq") , rs.getString("location"), rs.getString("wildtype"), rs.getString("mutated"), Type.valueOf(rs.getString("type")), Tool.valueOf(rs.getString("tool")));//rs.getString("tool")) );

			if(!allMutations.containsKey(pmid))
				allMutations.put(pmid, new ArrayList<IndexedMutation>());

			allMutations.get(pmid).add(mm);			
		}
		rs.close();
		System.out.println("Mutations for " +allMutations.size() +" articles loaded!\n normalizing...");
		
		//System.exit(0);
		Set<Integer> pmids = new HashSet<Integer>(allMutations.keySet());
		SethNorm norm = new SethNorm();
		norm.init(table);
		TimeWatch watch =  TimeWatch.start();
		int loop=0;
		for(int pmid : pmids){
			if(++loop%500 == 0){
				double progress = (double) 100.0*loop/pmids.size();
				System.out.format("Progress: %.3f%% %d seconds elapsed and %d documents done (equaling %.1f seconds per document)  %n",progress, watch.time(TimeUnit.SECONDS), loop,  watch.time(TimeUnit.SECONDS)/((float)loop)); //		
				System.out.format("%d Minutes remaining  %n",  (int)( watch.time(TimeUnit.MINUTES)*100/progress-watch.time(TimeUnit.MINUTES)));
			}
			norm.normalize(pmid, allMutations.get(pmid));
				

		}
		
		
		
	}

}
