package Query_ExpansionInternal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import API.LuceneQuery;
import API.RankedTweetList;
import API.Tweet;
import Main.QueryExpansion;
import Query_ExpansionInternal.CommonWords;
import Query_ExpansionInternal.expandedQuery;

/**
 * Main class run for Internal Query Expansion. All internal query expansion run
 * from this file. 
 * 
 * rescoreTweets.java Rescores the tweets based on percentage
 * CommonWords.java Finds top X words overall in corpus 
 * expandedQuery.java Creates arraylist for top10Words
 * 
 * @author Lauren Mathews v1.0
 * @version 6/16/14 v1.0
 */
public class QueryExpansionInternal implements QueryExpansion{

	boolean externalData, hashtags;
	String inputName, stopWordsColl, model, harddrive, slangDict;
	ArrayList<ArrayList<RankedTweetList>> rankList;

	/**
	 * Sets each of the parameters for Internal Query Expansion
	 * 
	 * @param inputFile
	 *            inputName output.csv from Results.java (file name)
	 * @param stopWords
	 *            stopWordsColl englishStop.txt, self-created by Dr. Lim (file
	 *            name)
	 * @param externalData
	 *            externalDataUsed Whether the query expansion wants to use
	 *            external data
	 * @param modelFile
	 *            model en-pos-maxent.bin for expandedQuery (file name)
	 * @param hD
	 *            harddrive (file path)
	 * @param slang
	 * 			  slangDict slangDict.txt, created in 2011 by Matt Kemmer
	 * @param rtl 
	 */
	public QueryExpansionInternal(String inputFile, String stopWords,
			boolean externalData, String modelFile, String hD, String slang, boolean hash, ArrayList<ArrayList<RankedTweetList>> rtl) {
		this.inputName = inputFile;
		this.stopWordsColl = stopWords;
		this.externalData = externalData;
		this.model = modelFile;
		this.harddrive = hD;
		this.slangDict = slang;
		this.hashtags = hash;
		this.rankList = rtl;
		
		System.err
		.println("(In/For QueryExpansionInternal.java, for CommonWords Internal Query Expansion) Did you want to use external data? Because externalData is: "
				+ externalData);
	}

	/**
	 * Used to run all three of the "topTenWords" Internal Query Expansion
	 * collection. CommonWords.java is used to find the top 50 words in the file
	 * for all tweets. topTenWords.java then uses top50.txt to grab the top ten
	 * words used in all tweets per topics. expandedQuery.java then takes
	 * topTenWords.txt and expands the original test_topics.txt file
	 * 
	 * @return a new arraylist containing the new words found for each topic
	 */
	public ArrayList<String> getNewQueries(ArrayList<LuceneQuery> queries){
		// checks all the arguments (global variables) for correct name)
		if (!inputName.equals("output.csv")) {
			System.err
					.println("In QueryExpansionInternal.java (in commonwords). inputName doesn't equal output.csv, which is what Results.java makes.");
			System.err.println("inputName: " + inputName);
			System.exit(0);
		}

		if (!stopWordsColl.equals("englishStop.txt")) {
			System.err
					.println("In QueryExpansionInternal.java (in commonwords). stopWordsColl doesn't equal englishStop.txt.");
			System.err.println("stopWordsColl: " + stopWordsColl);
			System.exit(0);
		}

		if (!model.contains("en-pos-maxent.bin") && externalData) {
			System.err
					.println("In QueryExpansionInternal.java (in commonwords). model doesn't equal en-pos-maxent.bin.");
			System.err.println("model: " + model);
			System.err.println("externalData: " + externalData);
			System.exit(0);
		}

		if (queries.isEmpty() || queries == null) {
			System.err
					.println("In QueryExpansionInternal.java (in commonwords). queList (queries - topics) is empty or null.");
			System.exit(0);
		}

		if (harddrive.isEmpty() || harddrive == null) {
			System.err
					.println("In QueryExpansionInternal.java (in commonwords). harddrive is empty or null.");
			System.exit(0);
		}
		
		ArrayList<LuceneQuery> queList = queries;
		
		String lastTopicNum = "mb" + queList.get(queList.size()-1).getQueryNum();

		// creates the full file path of inputName
		inputName = harddrive + "/" + inputName;
		stopWordsColl = harddrive + "/src/Query_ExpansionInternal/" + stopWordsColl;
		slangDict = harddrive + "/src/Query_ExpansionInternal/" + slangDict;

		// creates topX words if external is on
		HashSet<String> stopWords = null;
		if (externalData) {
			System.out.println("Starting createStopWords...");
			stopWords = CommonWords.createStopWords(stopWordsColl);
			System.out.println("Finished createStopWords. Updating slang in output.csv...");
			try {
				changeSlang.checkWords(inputName, slangDict, harddrive + "/" + "slangOutput.csv");
			} catch (IOException e) {
				System.err.println("In QueryExpansionInternal.java. Something happened when changing slang. ERROR: " + e.getMessage());
				System.exit(0);
			}
			
			inputName = harddrive + "/" + "slangOutput.csv";
			
			System.out.println("Finished updating slang.");
		}
		
		HashMap<String, Float> thresholdTopic = findThreshold();

		// helps keep track of what's happening
		System.out.println("Starting scrapOutput... (this will take about 2-8 minutes)");
		
		//scraps output.csv and creates original topXWords and topWordsPerTopics
		try {
			CommonWords.scrapOutput(inputName, thresholdTopic, lastTopicNum, hashtags);
		} catch (IOException e) {
			System.err.println("In QueryExpansionInternal.java (in getNewQueries). Something went wrong when using the output.csv.");
			System.exit(0);
		}
		
		// helps keep track of what's happening
		System.out.println("Finished scrapOutput.");
		
		HashSet<String> ownStopWords = null;
		if(!externalData){
			System.out.println("Creating ownStopWords... (this will take about 2 minutes)");
			//creates own stop words list
			try {
				ownStopWords = CommonWords
						.createOwnStopWords(stopWords, queList);
			} catch (IOException e) {
				System.err.println("In QueryExpansionInternal.java (in getNewQueries). Something went wrong when creating own stop words.");
				System.exit(0);
			}
			System.out.println("Finished ownStopWords.");
		}

		// helps keep track of what's happening
		System.out.println("Starting topXWords...");
		
		//creates topXWords overall in file (across topics)
		HashSet<String> topXWords = CommonWords.topXWords(stopWords,
				ownStopWords, queList);

		// helps keep track of what's happening
		System.out.println("Finished topXWords. Starting top10Words...");

		//creates top 10 words per topic
		ArrayList<String> top10Words = CommonWords.top10Words(topXWords,
				stopWords, ownStopWords, queList, hashtags);

		// helps keep track of what's happening
		System.out.println("Finished top10Words. Starting expandedQuery...");

		// expands query, depending on external (Parts of Speech)
		ArrayList<String> newTopics = null;
		if (externalData) {
			String library = harddrive.substring(0, harddrive.lastIndexOf("/")+1);
			model = library + model;

			//expands query (returns a list of 50 new queries)
			newTopics = expandedQuery
					.expandTheQuery(queList, top10Words, model);
		} else {
			newTopics = expandedQuery.expandTheQuery(queList, top10Words, "");
		}

		// helps keep track of what's happening
		System.out.println("Finished expandedQuery.");

		// basically, if it's not the right size, something went wrong
		if (newTopics.size() > 100) {
			System.err
					.println("Something went wrong - there isn't a correct number of new topics listed.");
		}

		return newTopics;
	}

	/**
	 * Finds the score threshold for each topic
	 */
	private HashMap<String, Float> findThreshold() {
		HashMap<String, Float> thresholds = new HashMap<String, Float>();
		
		for(int i = 0; i < rankList.size(); i++){
			
			float amount = 0;
			float totalScores = 0;
			
			for(int j = 0; j < rankList.get(i).size(); j++){
				ArrayList<Tweet> tweets = rankList.get(i).get(j).getRankedList();
				
				for(int m = 0; m < Math.min(tweets.size(), 500); m++){
					Tweet curTweet = tweets.get(m);
					
					Float score = curTweet.getScore();
					
					amount++;
					totalScores += score;
				}
				
				thresholds.put(tweets.get(0).getTopicNum().toLowerCase(), totalScores/amount);
			}
		}
		
		return thresholds;
	}
}