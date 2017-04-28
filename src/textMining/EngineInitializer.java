package textMining;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.parser.nndep.DependencyParser;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import gov.nih.nlm.nls.metamap.MetaMapApi;
import gov.nih.nlm.nls.metamap.MetaMapApiImpl;

public class EngineInitializer {
	String modelPath = DependencyParser.DEFAULT_MODEL;
	String taggerPath = "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger";
	DependencyParser parser = DependencyParser.loadFromModelFile(modelPath);
	String grammar = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
	String[] options = { "-maxLength", "80", "-retainTmpSubcategories" };

	LexicalizedParser lp = null;
	TreebankLanguagePack tlp = null;
	GrammaticalStructureFactory gsf = null;
	
	public LexicalizedParser initializeLp() {
		lp = LexicalizedParser.loadModel(grammar, options);
		
		return lp;
	}
	
	public GrammaticalStructureFactory initializeGsf() {
		tlp = lp.getOp().langpack();
		gsf = tlp.grammaticalStructureFactory();
		
		return gsf;
	}
	
	public MetaMapApi initializeMetamapApi() {
		MetaMapApi api = new MetaMapApiImpl();
		api.setOptions("-y -k <aapp,acty,aggp,amas,amph,anim,anst,antb,arch,bacs,bact,bdsu,bdsy,bhvr,bird,blor,bmod,bodm,bsoj,carb,celc,celf,cell,chem,chvf,chvs,clas,clnd,cnce,crbs,diap,dora,drdd,edac,eehu,eico,elii,emst,enty,enzy,euka,evnt,famg,ffas,fish,fngs,food,ftcn,genf,geoa,gngm,gora,grpa,grup,hcpp,hcro,hlca,horm,humn,idcn,imft,inbe,inch,inpr,irda,lang,lbpr,lipd,mamm,mbrt,mcha,medd,mnob,moft,mosq,nnon,npop,nsba,nusq,ocac,ocdi,opco,orch,orga,orgf,orgm,orgt,ortf,phob,phpr,phsu,plnt,podg,popg,prog,pros,qlco,qnco,rcpt,rept,resa,resd,rnlw,sbst,shro,socb,spco,strd,tmco,topp,virs,vita,vtbt>");
		
		return api;
	}
	
	public Banner initializeBanner(String BannerPath) {		
		String configurationFilename = BannerPath;
		Banner banr = new Banner(configurationFilename);
		
		return banr;
	}
}
