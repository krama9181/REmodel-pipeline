package textMining;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;

public class EdgeDetector {
	
	public LinkedHashSet<String> extractRelation(String annFilePath, LexicalizedParser lp, GrammaticalStructureFactory gsf) throws IOException {
		LinkedHashSet<String> Result = new LinkedHashSet<String>();
		
		File file = new File(annFilePath);
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = null;

		LinkedHashSet<String> annotatedPhenotype = new LinkedHashSet<String>();
		LinkedHashSet<String> annotatedTrigger = new LinkedHashSet<String>();
		
		String sentence = null;
		
		while ((line = br.readLine()) != null) {

			if (line.startsWith("sentence:")) {
				sentence = line.substring(0 + "sentence:".length(), line.length());
				continue;
			}
			
			if (line.startsWith("phenotype:")) {
				String phenotype = line.substring(0 + "phenotype:".length(), line.length());
				annotatedPhenotype.add(phenotype);
				continue;
			}
			
			if (line.startsWith("trigger:")) {
				String trigger = line.substring(0 + "trigger:".length(), line.length());
				annotatedTrigger.add(trigger);
				continue;
			}
			
			if (line.equals("@@@")) {				
				if (annotatedTrigger.size() >= 0 && annotatedPhenotype.size() > 0) {
					
					if (annotatedTrigger.size() == 0) {
						for (String phenInfo : annotatedPhenotype) {
							String[] phen = phenInfo.split("\t");
							
							if(phen[4].equals("bpoc")) phen[0] = "activity of " + phen[0];																						
							
							Result.add(phen[3] + "\t" + phen[4] + "\t" + phen[0] + "\t" + "NA" + "\t" + sentence);
						}
					}
					
					else if (annotatedTrigger.size() == 1) {
						for (String triggerInfo : annotatedTrigger) {
							String[] trigger = triggerInfo.split("\t");
							int t_begin = Integer.valueOf(trigger[1]);

							for (String phenInfo : annotatedPhenotype) {
								String[] phen = phenInfo.split("\t");
								
								if(phen[4].equals("bpoc")) phen[0] = "activity of " + phen[0];
								
								int p_begin = Integer.valueOf(phen[1]);
								int tp_dif = p_begin - t_begin;

								if (tp_dif > 0) {
									boolean marking = checkMark(sentence, trigger[1], phen[1]);
									if(marking){										
										int[] dep_distance = cal_distance(sentence, phen[0], trigger[0], trigger[1], lp, gsf);
										int result = Math.abs(dep_distance[0] - dep_distance[1]);
										
										if (result < 5)  Result.add(phen[3] + "\t" + phen[4] + "\t" + phen[0] + "\t" + trigger[0] + "\t" + sentence);
									}
								}
							}
						}
					}

					else {
						for (String phenInfo : annotatedPhenotype) {
							String[] phen = phenInfo.split("\t");
							
							if(phen[4].equals("bpoc")) phen[0] = "activity of " + phen[0];
							
							int p_begin = Integer.valueOf(phen[1]);
							ArrayList<Integer> difList = new ArrayList<Integer>();
							LinkedHashMap<Integer, String> filteredTriggerMap = new LinkedHashMap<Integer, String>();

							for (String triggerInfo : annotatedTrigger) {
								String[] trigger = triggerInfo.split("\t");
								int t_begin = Integer.valueOf(trigger[1]);
								int tp_dif = p_begin - t_begin;
								if (tp_dif > 0) {
									difList.add(tp_dif);
									filteredTriggerMap.put(tp_dif, triggerInfo);
								}
							}
							
							int first = getMin(difList);

							for(int key : filteredTriggerMap.keySet()) {
								String value = filteredTriggerMap.get(key);
								String[] trigger = value.split("\t");

								if (first == key) {									
									boolean marking = checkMark(sentence, trigger[1], phen[1]);
									if(marking){							
										int[] dep_distance = cal_distance(sentence, phen[0], trigger[0], trigger[1], lp, gsf);
										int result = Math.abs(dep_distance[0] - dep_distance[1]);

										if (result < 5)  Result.add(phen[3] + "\t" + phen[4] + "\t" + phen[0] + "\t" + trigger[0] + "\t" + sentence);
									}
								}
							}
						}
					}
				}
				
				annotatedPhenotype.clear();
				annotatedTrigger.clear();					
			}					
		}
		br.close();
		
		return Result;
	}
	
	static int getMin(ArrayList<Integer> intList) {
		int min = Integer.MAX_VALUE;
		for(int i : intList) {
			if(i < min) min = i;
		}
		
		return min;
	}

	private int[] cal_distance(String main_sentence, String entity2, String trigger , String triggerOffset, LexicalizedParser lp, GrammaticalStructureFactory gsf) {
		String whWord = "";
		int whOffset = 0;
		int triOffset = Integer.parseInt(triggerOffset);
		
		int[] distance = new int[2]; // 0 : from entity2, 1 : from trigger
		
		String originalTrigger = trigger;
		String text = main_sentence;
		text = text.toLowerCase();
		text = text.replace(".", "");

		DocumentPreprocessor tokenizer = new DocumentPreprocessor(new StringReader(text));
		Iterator<List<HasWord>> it = tokenizer.iterator();
		List<HasWord> sentence = null;
		
		while(it.hasNext()){
			sentence = it.next();
		}
		
		String[] splitEntity2 = entity2.split(" ");		
		entity2 = splitEntity2[splitEntity2.length-1];
		
		Tree parse = lp.apply(sentence);
		GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		Collection<TypedDependency> tdl = gs.typedDependencies();

		List<Tree> leaves = parse.getLeaves();
		String termSplitter = ",WDT, ,WP, ,WP$, ,WRB,";
				
		for (Tree leaf : leaves) {
            Tree parent = leaf.parent(parse);
            
            if(termSplitter.contains(","+parent.label().value()+",")){
            	whWord = leaf.label().value();
            	whOffset = main_sentence.lastIndexOf(whWord);
            }            
        }
		
		String[] depArrayTrigger = new String[tdl.size()];
		String[] depArrayEntity2 = new String[tdl.size()];
		String[] depArrayWh = new String[tdl.size()];
		String[] token_array = new String[tdl.size()];
		int depSize = 0;

		for (Iterator<TypedDependency> iter = tdl.iterator(); iter.hasNext();) {

			TypedDependency var = iter.next();

			String token = var.dep().toString();
			String Parent = var.gov().toString();

			String token_ch = token;
			String parent_ch = Parent;

			String regBigAlpha = "ABCDEFGHIJKMNLOPQRSTUVWXYZ";

			StringBuffer sb = new StringBuffer();

			// token
			for (int j = 0; j < token_ch.length(); j++) {
				char c = token_ch.charAt(j);
				String c_string = "" + c;

				if (regBigAlpha.contains(c_string)) {
				} else {
					sb.append(c_string);
				}
			}
			String token_result = sb.toString();

			if (token_result.length() > 1) {
				token_result = token_result.substring(0, token_result.length() - 1);
			}
			
			// parent
			StringBuffer sb_2 = new StringBuffer();
			for (int j = 0; j < parent_ch.length(); j++) {
				char c = parent_ch.charAt(j);
				String c_string = "" + c;
				if (regBigAlpha.contains(c_string)) {
				} else {
					sb_2.append(c_string);
				}
			}
			String parent_result = sb_2.toString();
			if (parent_result.length() > 1) {
				parent_result = parent_result.substring(0, parent_result.length() - 1);
			} else {
				parent_result = "root";
			}

			depArrayTrigger[depSize] = token_result + "\t" + parent_result;
			depArrayEntity2[depSize] = token_result + "\t" + parent_result;
			depArrayWh[depSize] = token_result + "\t" + parent_result;

			token_array[depSize] = token_result;
			depSize++;
		}

		// Make Dependency parser
		String upper = "";
		int limit = 0;
		
		StringBuffer triggerDep = new StringBuffer();
		triggerDep.append(trigger);
		triggerDep.append("\t");
		
		while(!upper.equals("root")){
			limit++;
			for(int i=0; i<depSize ; i++){
				String[] depSplit = depArrayTrigger[i].split("\t");
				
				if(trigger.equals(depSplit[0])){
					upper = depSplit[1];
					trigger = depSplit[1];
					
					depArrayTrigger[i] = "z9x9c9" + "\t" +"z9x9c9";
					
					triggerDep.append(upper);
					triggerDep.append("\t");
					break;
				}
			}
			if(limit > 200){
				break;
			}
		}
		String[] tempTrigger = triggerDep.toString().split("\t");
		int distanceTrigger = tempTrigger.length;
		
		if(!(limit == 0))
			distanceTrigger = limit;
				
		upper = "";
		limit = 0;
		
		StringBuffer entity2Dep = new StringBuffer();
		entity2Dep.append(entity2);
		entity2Dep.append("\t");
		
		while(!upper.equals("root")){
			limit++;
			for(int i=0; i<depSize ; i++){
				String[] depSplit = depArrayEntity2[i].split("\t");
				
				if(entity2.equals(depSplit[0])){
					upper = depSplit[1];
					entity2 = depSplit[1];
					
					depArrayEntity2[i] = "z9x9c9" + "\t" +"z9x9c9";
					
					entity2Dep.append(upper);
					entity2Dep.append("\t");
					break;
				}
			}
			
			if(limit > 200){
				break;
			}
		}
		String[] tempEntity2 = entity2Dep.toString().split("\t");
		int distanceEntity2 = tempEntity2.length;
		
		if(!(limit == 0))
			distanceEntity2 = limit;
				
		upper = "";
		limit = 0;
		
		StringBuffer whDep = new StringBuffer();
		whDep.append(whWord);
		whDep.append("\t");
		
		while(!upper.equals("root")){
			limit++;
			for(int i=0; i<depSize ; i++){
				String[] depSplit = depArrayWh[i].split("\t");
				
				if(whWord.equals(depSplit[0])){
					upper = depSplit[1];
					whWord = depSplit[1];
					
					depArrayWh[i] = "z9x9c9" + "\t" +"z9x9c9";
					
					whDep.append(upper);
					whDep.append("\t");
					break;
				}
			}
			
			if(limit > 200){
				break;
			}
		}
		
		String whDepTree = whDep.toString();		
		
		distance[0] = distanceTrigger;
		distance[1] = distanceEntity2;
		
		String subSentence = main_sentence.substring(0,whOffset);
		
		if(whDepTree.contains(originalTrigger) && whOffset < triOffset){
			if(!(subSentence.contains(" is ") || subSentence.contains(" binds "))){
				distance[0] = 1000;
				distance[1] = 0;
			}
		}
		
		return distance;
	}
	
	boolean checkMark(String sentence, String triggerOffset_str, String phenoOffset_str){
		boolean check = true;
		///// exclude relation when find ; . ( /////
		
		int triggerOffset = Integer.parseInt(triggerOffset_str);
		int phenoOffset = Integer.parseInt(phenoOffset_str);
		
		if(triggerOffset < phenoOffset){
			String subString = sentence.substring(triggerOffset, phenoOffset);
			
			if(subString.contains(";") || subString.contains("(") ||subString.contains(".")){
				check =  false;
			}
		}
		
		else{
			check = false;
		}
		
		return check;
	}
	
}
