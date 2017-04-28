package textMining;

import gov.nih.nlm.nls.metamap.Ev;
import gov.nih.nlm.nls.metamap.Mapping;
import gov.nih.nlm.nls.metamap.MetaMapApi;
import gov.nih.nlm.nls.metamap.PCM;
import gov.nih.nlm.nls.metamap.Position;
import gov.nih.nlm.nls.metamap.Result;
import gov.nih.nlm.nls.metamap.Utterance;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

public class Metamap {
   String preprocessing(String input) {         
      String result = input.replaceAll("[^a-zA-Z0-9 ]{2,}", "__");
      return result;
   }
   
   String getPhrase(String text, int start, int end) throws IOException {
      if(start < 0 || start > text.length() || text.length() < end){
         return "NULL";
      }
      String result = text.substring(start, end);
      return result;
   }

   LinkedHashMap<String, String> metamap(MetaMapApi api, String input) {      
      LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();   
      List<Result> resultList = api.processCitationsFromString(input);
      
      Result nerResult = resultList.get(0);
      
      try{
    	  for (Utterance utterance : nerResult.getUtteranceList()) {
	         for (PCM pcm : utterance.getPCMList()) {
	            for (Mapping map : pcm.getMappingList()) {               
	               for (Ev mapEv : map.getEvList()) {
	                  if(!mapEv.isHead()){
	                     continue;
	                  }
	                  
	                  Position start_end = mapEv.getPositionalInfo().get(0);
	                  int start = start_end.getX();
	                  int end = start + start_end.getY();
	                  
	                  String phenotype_name = input.substring(start, end);
	                  String UMLS_ID = mapEv.getConceptId();
	                  String UMLS_Semantic_type = mapEv.getSemanticTypes().get(0);
	                  String tmp1 = phenotype_name + "\t" + start + "\t" + end;
	                  String tmp2 = UMLS_ID + "\t" + UMLS_Semantic_type;
	                  result.put(tmp1, tmp2);
	               }
	            }
	         }
	      } 
      }
      catch(Exception e){
    	  System.err.println("Text Mining: MetaMap API error");
    	  System.exit(1);
      }
      
      return result;
   }
   
}
