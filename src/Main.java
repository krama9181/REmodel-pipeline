import java.io.IOException;
import java.util.LinkedHashSet;

import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import gov.nih.nlm.nls.metamap.MetaMapApi;
import textMining.Banner;
import textMining.EngineInitializer;
import textMining.TextMiner;

public class Main {	 
	
	public static void main(String[] args) throws IOException {
		
		String bannerPath = "lib/textMining/Banner/config/banner_NCBIDisease_UMLS2013AA_TEST.xml";
		String annFilePath = "lib/textMining/annotation/temp.txt";		
		String dicPath = "lib/textMining/Dictionary/relationType.txt";
		String stopWordPath = "lib/textMining/Dictionary/stopWord.txt";
		
		EngineInitializer initializer = new EngineInitializer();
		Banner banr = initializer.initializeBanner(bannerPath);
		MetaMapApi mmApi = initializer.initializeMetamapApi();
		LexicalizedParser lp = initializer.initializeLp();
		GrammaticalStructureFactory gsf = initializer.initializeGsf();
		
		TextMiner textMiner = new TextMiner();
		
		String inputText = "To remove heat, expell superfical evils. ; "
				+ "Hysterotonic. Used for the treatment of abnormal menstruation, post partum metrorrhagia, incomplete reduction of uterus. ; "
				+ "To activate blood circulation, to relieve pain. ; "
				+ "Stomachic and carminative ; "
				+ "To promote spitting of phlegm";
		
		LinkedHashSet<String> sentenceSet = textMiner.preprocessData(inputText, ";");
		
		System.out.println("\n\n<sentenceSet>");
		for(String s : sentenceSet) System.out.println(s);
		
		LinkedHashSet<String> tempDataSet = textMiner.extractRelation(sentenceSet, annFilePath, dicPath, mmApi, banr, lp, gsf);
		
		System.out.println("\n\n<tempDataSet>");
		for(String s: tempDataSet) System.out.println(s);
		
		LinkedHashSet<String[]> tmResultSet = textMiner.classifyType(tempDataSet, stopWordPath);
		
		System.out.println("\n\n<tmResultSet>");
		for(String[] tmResult: tmResultSet){
			for(String s: tmResult) System.out.print(s + "\t");
			System.out.print("\n");
		}
		
	}
	
	
}