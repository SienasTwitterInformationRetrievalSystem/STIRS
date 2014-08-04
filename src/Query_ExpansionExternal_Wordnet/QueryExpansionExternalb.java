package Query_ExpansionExternal_Wordnet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;

import API.LuceneQuery;
import Main.QueryExpansion;
import Query_ExpansionInternal.CommonWords;

public class QueryExpansionExternalb implements QueryExpansion{

	Logger log = null;
	String index, stopWordsColl, harddrive;

	public QueryExpansionExternalb(Logger logger, String ind, String stopWords, String hD) {
		this.log = logger;
		this.index = ind;
		this.stopWordsColl = stopWords;
		this.harddrive = hD;
	}

	public ArrayList<LuceneQuery> WordNet(ArrayList<LuceneQuery> queries) {
		// checks all the arguments (global variables) for correct name)
		if (!stopWordsColl.equals("englishStop.txt")) {
			System.err
					.println("In QueryExpansionExternalb.java (in WordNet). stopWordsColl doesn't equal englishStop.txt.");
			System.err.println("stopWordsColl: " + stopWordsColl);
			System.exit(0);
		}

		if (queries.isEmpty() || queries == null) {
			System.err
					.println("In QueryExpansionExternalb.java (in WordNet). queList (queries - topics) is empty or null.");
			System.exit(0);
		}

		if (harddrive.isEmpty() || harddrive == null) {
			System.err
					.println("In QueryExpansionExternalb.java (in WordNet). harddrive is empty or null.");
			System.exit(0);
		}
		
		ArrayList<LuceneQuery> queList = queries;

		System.out.println("Cleaning up the query topic list...");
		
		for (int i = 0; i < queList.size(); i++) {
			String cleanQuery = queList.get(i).getQuery().trim().toLowerCase();
			cleanQuery = cleanQuery.replaceAll(",", "");
			queList.get(i).setQuery(cleanQuery);
		}
		
		System.out
				.println("Finished cleaning list. Starting createStopWords...");

		stopWordsColl = harddrive + "/src/Query_ExpansionInternal/"
				+ stopWordsColl;
		
		HashSet<String> stopWords = CommonWords.createStopWords(stopWordsColl);
		
		System.out.println("Finished createStopWords. Starting LucQeWord...");

		if (stopWords == null || stopWords.isEmpty()) {
			System.err
					.println("In QueryExpansionExternalb.java (in WordNet). Something is wrong with stop words collection.");
			System.exit(0);
		}

		LucQeWord lW = new LucQeWord(queList, log);
		ArrayList<LuceneQuery> wordNetQueries = lW.runWordNet(stopWords);

		System.out.println("Finished LucQeWord.");

		return wordNetQueries;
	}

	public ArrayList<String> getNewQueries(ArrayList<LuceneQuery> queList) {
		ArrayList<LuceneQuery> wordNetQueries = null;
		ArrayList<LuceneQuery> queries = queList;
		try{
			wordNetQueries = WordNet(queries);
		}catch(Exception e){
			System.err.println("In QueryExpansionExternalb.java (in getNewQueries). Something happened when making WordNet. ERROR: " + e.getMessage());
			System.exit(0);
		}
		
		ArrayList<String> wordnet = new ArrayList<String>();
		
		for(int i = 0; i < wordNetQueries.size(); i++){
			wordnet.add(wordNetQueries.get(i).getQuery());
		}
		
		return wordnet;
	}
}