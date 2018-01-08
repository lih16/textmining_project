package com.sema4var.test;

import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import com.mysql.jdbc.PreparedStatement;

import de.hu.berlin.wbi.objects.Gene;
import de.hu.berlin.wbi.objects.MatchOptions;
import de.hu.berlin.wbi.objects.Transcript;
import de.hu.berlin.wbi.objects.TranscriptNormalized;
import de.hu.berlin.wbi.objects.UniprotFeature;
import de.hu.berlin.wbi.objects.dbSNP;
import de.hu.berlin.wbi.objects.dbSNPNormalized;

public class SethNorm {
	PreparedStatement ps;
	PreparedStatement ps2;
	PreparedStatement ps3;

	public void init(String table) throws Exception{
		final Properties property = new Properties();
		property.loadFromXML(new FileInputStream(new File("resources/myProperty.xml"))); //The connection to the database is stored in a property file
		final MySQL database = new MySQL(property);	//Only used for normalization
		database.connect();

		//Initialize database for the most relevant tables
		dbSNP.init(database, property.getProperty("database.PSM"), property.getProperty("database.hgvs_view"));
		Gene.init(database, property.getProperty("database.geneTable"), property.getProperty("database.gene2pubmed"));
		UniprotFeature.init(database, property.getProperty("database.uniprot"));
		Transcript.init(database, property.getProperty("database.transcript"));		

		ps = (PreparedStatement) database.getConn().prepareStatement("UPDATE " +table +" SET rs = ?, considered = ?, matches = ?, matchesScore = ? WHERE mID = ?");
		ps2 = (PreparedStatement) database.getConn().prepareStatement("UPDATE " +table +" SET matchSequence = ?, matchSequenceGene = ?, matchSequenceGeneScore = ? WHERE mID = ?");
		ps3 = (PreparedStatement) database.getConn().prepareStatement("UPDATE " +table +" SET analyzed = true WHERE mID = ?");
	}


	public void normalize(int pmid, List<IndexedMutation> mentions) throws SQLException {

        System.out.println(pmid);
       // System.exit(0);
		final Set<Gene> genes = Gene.queryGenesForArticle(pmid);
		final Map<Gene, List<dbSNP>> geneToDbSNP = new HashMap<Gene, List<dbSNP>>(); 		
		final Map<Gene, List<UniprotFeature>> geneToUniProtFeature = new HashMap<Gene, List<UniprotFeature>>();
		final Map<Gene, Set<Transcript>> geneToTranscript = new HashMap<Gene, Set<Transcript>>();
			
		
		//Retrieve all possible SNPs for this gene
		for (Gene gene : genes) {
		
			
			final List<dbSNP> potentialSNPs = dbSNP.getSNP(gene.getGeneID()); 		
			final List<UniprotFeature> features = UniprotFeature.getFeatures(gene.getGeneID()); 		
			final Set<Transcript> transcripts = Transcript.getTranscripts(gene.getGeneID());

			geneToDbSNP.put(gene, potentialSNPs); 		
			geneToUniProtFeature.put(gene, features); 		
			geneToTranscript.put(gene, transcripts);
		}


		//Normalization based on co-occurrence
		for(IndexedMutation mm : mentions){
			for (Gene gene : genes) {
				final List<dbSNP> potentialSNPs = geneToDbSNP.get(gene);
				final List<UniprotFeature> features = geneToUniProtFeature.get(gene);
				final Set<Transcript> transcripts = geneToTranscript.get(gene);

				mm.normalizeSNP(potentialSNPs, features, true);
				mm.normalizeSequences(transcripts, features, true);
			}

			if(mm.getNormalized() != null && mm.getNormalized().size() > 0){

				final Set<Integer> rsIDs = new TreeSet<Integer>();	//List of rsIds
				final Set<Integer> geneIDs = new TreeSet<Integer>();	//List of all considered genes
				final Set<Integer> normalizedGenes = new TreeSet<Integer>(); //Contains the list of genes a mutation is normalized to 

				StringBuilder geneString = new StringBuilder();
				for (Gene gene : genes) {
					if(gene.getSpecies() != 9606)
						continue;
					
					if(!geneIDs.contains(gene.getGeneID())){
						geneIDs.add(gene.getGeneID());
						geneString.append(gene.getGeneID() +"|");
					}
				}
					
				List<dbSNPNormalized> sorted = mm.getNormalized();
				Collections.sort(sorted); //Sort by confidence
								
				StringBuilder snpString = new StringBuilder();				
				StringBuilder geneNormString = new StringBuilder();
				StringBuilder geneNormScore = new StringBuilder();
				for (dbSNPNormalized norm : sorted) {
					if(!rsIDs.contains(norm.getRsID())){
						rsIDs.add(norm.getRsID());		
						snpString.append(norm.getRsID() +"|");
					}
					
					if(!normalizedGenes.contains(norm.getGeneID())){
						normalizedGenes.add(norm.getGeneID());
						geneNormString.append(norm.getGeneID() +"|");
						
						StringBuilder sb = new StringBuilder();
						EnumSet<MatchOptions> matchOptions = norm.getMatchType();
						for(MatchOptions mo : matchOptions){
							if(mo.equals(MatchOptions.LOC))
								sb.append("exact-");
							else
								sb.append(mo.toString() +"-");
						}
						if(norm.isFeatureMatch())
							sb.append("uniprot-");
						sb.deleteCharAt(sb.length()-1);
						
						geneNormScore.append(norm.getGeneID() +"-" +sb.toString() +"|");
					}												
				}
				geneString = geneString.deleteCharAt(geneString.length()-1);
				snpString = snpString.deleteCharAt(snpString.length()-1);
				geneNormString = geneNormString.deleteCharAt(geneNormString.length()-1);
				geneNormScore = geneNormScore.deleteCharAt(geneNormScore.length()-1);
				
				//Update the database.
				ps.setString(1, snpString.toString());
				ps.setString(2, geneString.toString());
				ps.setString(3, geneNormString.toString());
				ps.setString(4, geneNormScore.toString());

				ps.setInt(5, mm.getId());

				try{
					ps.executeUpdate();	
				}
				catch(Exception ex){
					ex.printStackTrace();
					System.err.println(ps.toString());
				}	
			}
			//Okay, in case we have genes but were not able to normalize update the considered column only
			else if(genes.size() > 0){
				final Set<Integer> geneIDs = new TreeSet<Integer>();	//List of all considered genes
				loop:for (Gene gene : genes) {
					if(gene.getSpecies() != 9606)
						continue loop;
					
					geneIDs.add(gene.getGeneID());
				}
									
				StringBuilder geneString = new StringBuilder();
				for(Integer i : geneIDs)
					geneString.append(i +"|");
				geneString = geneString.deleteCharAt(geneString.length()-1);
				
				ps.setNull(1, Types.VARCHAR);
				ps.setString(2, geneString.toString());
				ps.setNull(3, Types.VARCHAR);
				ps.setNull(4, Types.VARCHAR);

				ps.setInt(5, mm.getId());

				try{
					ps.executeUpdate();
				}
				catch(Exception ex){
					ex.printStackTrace();
					System.err.println(ps.toString());
				}
		
			}



			//Here we check if we were able to normalize to a protein seqeunce or CDS 
			if(mm.getTranscripts() != null && mm.getTranscripts().size() > 0){
				final Set<String> sequenceIDs = new TreeSet<String>();	
				final Set<Integer> sequenceGene = new TreeSet<Integer>();
				
				
				StringBuilder sequenceString = new StringBuilder();
				StringBuilder sequenceGeneString = new StringBuilder();
				StringBuilder sequenceGeneScore = new StringBuilder();
				
				TreeSet<TranscriptNormalized> transcripts = new TreeSet<TranscriptNormalized>(mm.getTranscripts()); 				
				for(TranscriptNormalized transcript : transcripts){
					if(!sequenceIDs.contains(transcript.getEnsp())){
						sequenceIDs.add(transcript.getEnsp());	
						sequenceString.append(transcript.getEnsp() +"|");
					}
										
					if(!sequenceGene.contains(transcript.getEntrez())){
						sequenceGene.add(transcript.getEntrez());	
						sequenceGeneString.append(transcript.getEntrez() +"|");
						
						StringBuilder sb = new StringBuilder();
						EnumSet<MatchOptions> matchOptions = transcript.getMatchType();
						for(MatchOptions mo : matchOptions){
							if(mo.equals(MatchOptions.LOC))
								sb.append("exact-");
							else
								sb.append(mo.toString() +"-");
						}
						if(transcript.isFeatureMatch())
							sb.append("uniprot-");
						sb.deleteCharAt(sb.length()-1);
						sequenceGeneScore.append(transcript.getEntrez() +"-" +sb.toString()  +"|");
					}								
				}
							
				
				sequenceString = sequenceString.deleteCharAt(sequenceString.length()-1);
				sequenceGeneString = sequenceGeneString.deleteCharAt(sequenceGeneString.length()-1);
				sequenceGeneScore = sequenceGeneScore.deleteCharAt(sequenceGeneScore.length()-1);

				ps2.setString(1, sequenceString.toString());				
				ps2.setString(2, sequenceGeneString.toString());
				ps2.setString(3, sequenceGeneScore.toString());
					
				ps2.setInt(4, mm.getId());
				
				try{
					ps2.executeUpdate();					
				}
				catch(Exception ex){
					ex.printStackTrace();
					System.err.println(ps.toString());
				}					
			}
			
			//Okay we analyzed this entry!
//			ps3.setBoolean(1, true);
			ps3.setInt(1, mm.getId());
			try{
				ps3.executeUpdate(); 		
			}
			catch(Exception ex){
				ex.printStackTrace();
				System.err.println(ps3.toString());
			}					
			
			
		}

	}

}
