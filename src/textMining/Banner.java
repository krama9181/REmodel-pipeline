package textMining;

import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import banner.eval.BANNER;
import banner.postprocessing.PostProcessor;
import banner.tagging.CRFTagger;
import banner.tagging.dictionary.DictionaryTagger;
import banner.tokenization.Tokenizer;
import banner.types.Sentence;
import banner.types.SentenceWithOffset;
import dragon.nlp.tool.Tagger;
import dragon.nlp.tool.lemmatiser.EngLemmatiser;

public class Banner {

	private static Tokenizer tokenizer;
	private static PostProcessor postProcessor;
	private static CRFTagger tagger;
	private static HierarchicalConfiguration config;

	public Banner (String configurationFilename) {			
		try {
			prepareBANNER(configurationFilename);
		} catch (Exception e) {
			System.err.println("Text Mining: Banner error");
			System.exit(1);
		}
	}
	
	private void prepareBANNER(String configurationFile) throws ConfigurationException, IOException {
		config = new XMLConfiguration(configurationFile);
		
		DictionaryTagger dictionary = BANNER.getDictionary(config);
		EngLemmatiser lemmatiser = BANNER.getLemmatiser(config);
		Tagger posTagger = BANNER.getPosTagger(config);
		HierarchicalConfiguration localConfig = config.configurationAt(BANNER.class.getPackage().getName());	
		String modelFilename = localConfig.getString("modelFilename");		
		tagger = CRFTagger.load(new File(modelFilename), lemmatiser, posTagger, dictionary);		
	}

	public Sentence processSentences_BANNER(String input) throws IOException {		
		tokenizer = BANNER.getTokenizer(config);
		postProcessor = BANNER.getPostProcessor(config);
		Sentence inputSentence = new SentenceWithOffset("0", "0", input, 0);
		Sentence outputSentence = BANNER.process(tagger, tokenizer, postProcessor, inputSentence);
		
		return outputSentence;
	}
}
