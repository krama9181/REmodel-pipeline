package textMining;

import gov.nih.nlm.nls.metamap.MetaMapApi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import banner.types.Mention;
import banner.types.Sentence;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;
import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.ExactDictionaryChunker;
import com.aliasi.dict.MapDictionary;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;

public class NodeDetector {	
	double CHUNK_SCORE = 1.0;

	public int detectNode(LinkedHashSet<String> sentenceSet, String annFilePath, String dictPath, MetaMapApi mmApi, Banner banr) throws IOException {		
		File file = new File(annFilePath);
		BufferedWriter out = new BufferedWriter(new FileWriter(file));

		MapDictionary<String> triggerDic = new MapDictionary<String>();
		File dicfile = new File(dictPath);
		BufferedReader dicBr = new BufferedReader(new FileReader(dicfile));
		String line = null;

		while ((line = dicBr.readLine()) != null) {
			String[] content = line.split("\t");
			String word = content[0];
			String id = content[1];

			triggerDic.addEntry(new DictionaryEntry<String>(word, id, CHUNK_SCORE));
		}
		dicBr.close();
		ExactDictionaryChunker triggerDicChunker = new ExactDictionaryChunker(triggerDic, IndoEuropeanTokenizerFactory.INSTANCE, true, false);
		
		Metamap mm = new Metamap();
		
		for (String sentence: sentenceSet) {
			if(sentence != null){
				out.write("sentence:" + sentence);		
				out.newLine();
				
				String preprocessedString = mm.preprocessing(sentence);
				LinkedHashMap<String, String> metamapChunk = mm.metamap(mmApi, preprocessedString);
				Sentence bannerResult = banr.processSentences_BANNER(preprocessedString);
				LinkedHashSet<String> combinedResult = combineResults(metamapChunk, bannerResult);
				
				LinkedHashSet<String> PhenSet = filtering(combinedResult);			
				for(String phenotype : PhenSet) {
					out.write("phenotype:" + phenotype);
					out.newLine();
				}
				
				LinkedHashSet<String> triggerSet = Tchunk(triggerDicChunker, sentence.toLowerCase().trim());
				for (String trigger : triggerSet) {				
					out.write("trigger:" + trigger);
					out.newLine();
				}
				
				out.write("@@@");
				out.newLine();
			}			
		}
		
		out.close();
		
		return 0;
	}

	private LinkedHashSet<String> filtering(LinkedHashSet<String> hashSet) {
		LinkedHashSet<String> new_chunk_result = new LinkedHashSet<String>();
		LinkedHashSet<String> new_new_chunk_result = new LinkedHashSet<String>();

		for (String h1 : hashSet) {
			String[] contents = h1.split("\t");
			String start = contents[1];
			String end = contents[2];
			String saveEnd = "";
			int counter = 0;
			for (String h2 : hashSet) {
				String[] contents_new = h2.split("\t");
				if (start.toLowerCase().trim().equals(contents_new[1].toLowerCase().trim())) {
					saveEnd = saveEnd + contents_new[2] + "\t";
					++counter;
				}
			}
			if (counter == 0) {
				int counter_new = 0;
				for (String h3 : hashSet) {
					String[] contents_new = h3.split("\t");
					if (end.toLowerCase().trim().equals(contents_new[2].toLowerCase().trim())) {
						if (Integer.valueOf(start.toLowerCase().trim()) > Integer
								.valueOf(contents_new[1].toLowerCase().trim())) {
							++counter_new;
						}
					}
				}
				if (counter_new == 0) {
					new_chunk_result.add(h1);
				} else {
					continue;
				}
			} else {
				String[] End_contents = saveEnd.split("\t");
				String newEnd = "";
				for (String n : End_contents) {
					newEnd = n;
				}
				int counter_new_new = 0;
				for (String h3 : hashSet) {
					String[] contents_new = h3.split("\t");
					if (newEnd.toLowerCase().trim().equals(contents_new[2].toLowerCase().trim())) {
						if (Integer.valueOf(start.toLowerCase().trim()) > Integer
								.valueOf(contents_new[1].toLowerCase().trim())) {
							++counter_new_new;
						}
					}
				}
				if (counter_new_new == 0) {
					for (String h3 : hashSet) {
						String[] contents_new = h3.split("\t");
						if (newEnd.toLowerCase().trim().equals(contents_new[2].toLowerCase().trim())) {
							if (start.toLowerCase().trim().equals(contents_new[1].toLowerCase().trim())) {
								new_chunk_result.add(h3);
							}
						}
					}
				} else {
					continue;
				}
			}
		}

		for (String str : new_chunk_result) {
			String[] contents = str.split("\t");
			int counter = 0;

			for (String str_new : new_chunk_result) {
				String[] contents2 = str_new.split("\t");
				if (Integer.valueOf(contents[1]) > Integer.valueOf(contents2[1])) {
					if (Integer.valueOf(contents[2]) < Integer.valueOf(contents2[2])) {
						++counter;
					}
				}
			}
			if (counter == 0) {
				new_new_chunk_result.add(str);
			} else {
				continue;
			}
		}
		
		return new_new_chunk_result;
	}

	private LinkedHashSet<String> Tchunk(ExactDictionaryChunker chunker, String text) throws IOException {
		LinkedHashMap<String, String> resultMap = new LinkedHashMap<String, String>();
		LinkedHashSet<String> resultSet = new LinkedHashSet<String>();

		Chunking chunking = chunker.chunk(text);
		for (Chunk chunk : chunking.chunkSet()) {
			int start = chunk.start();
			int end = chunk.end();
			String type = chunk.type();
			String phrase = text.substring(start, end);

			// stop words
			if (end + 3 <= text.length() && text.toLowerCase().trim().substring(start, end + 3).equals(phrase.toLowerCase().trim() + " by")){
				continue;
			}
			
			String key = phrase + "\t" + start + "\t" + end;
			if (resultMap.containsKey(key)){
				resultMap.put(key, resultMap.get(key) + "|" + type);
			}
			else resultMap.put(key, type);
		}

		for (String key : resultMap.keySet()){
			String value = resultMap.get(key);
			
			resultSet.add(key + "\t" + value);
		}
		
		return resultSet;
	}

	private LinkedHashSet<String> combineResults(LinkedHashMap<String, String> metamap, Sentence banner) {
		LinkedHashSet<String> results = new LinkedHashSet<String>();
		
		for (Mention mention : banner.getMentions()) {
			StringBuilder outputLine = new StringBuilder();
			outputLine.append(mention.getText());
			outputLine.append("\t");
			outputLine.append(mention.getStartChar());
			outputLine.append("\t");
			outputLine.append(mention.getEndChar());
			
			if(metamap.containsKey(outputLine.toString())) {
				String id = metamap.get(outputLine.toString());
				String tmp[] = id.split("\t");
				metamap.remove(outputLine.toString());
				outputLine.append("\t");
				outputLine.append(tmp[0]);
			} else {
				outputLine.append("\t");
				outputLine.append("null");
			}
			
			outputLine.append("\t");
			outputLine.append("banr");			
			results.add(outputLine.toString());
		}
		
		Iterator<String> it = metamap.keySet().iterator();
		while(it.hasNext()) {
			String key = it.next();
			String value = metamap.get(key);
			results.add(key + "\t" + value);
		}
		
		return results;
	}
	
}
