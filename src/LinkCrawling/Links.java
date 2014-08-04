package LinkCrawling;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import API.LuceneQuery;
import API.RankedTweetList;
import Main.QueryExpansion;
import Query_ExpansionInternal.CommonWords;

/**
 * Uses the Link Crawling Lucene URL Index to get top 30 links for each topic.
 * Grabs all the content from these links for each topic and finds top 10 words.
 * 
 * @author Lauren Mathews v1.0
 * @version 7/10/2014 v1.0
 */
public class Links implements QueryExpansion {
	private String stopWordsColl, harddrive;
	private CharsetEncoder asciiEncoder = Charset.forName("US-ASCII")
			.newEncoder();
	private static int hitNum = 30;
	private static ArrayList<ArrayList<RankedTweetList>> rankedTweetLists = null;

	/**
	 * @param hD
	 *            The harddrive location (changes depending on the machine)
	 * @param stopWords
	 *            englishStop.txt collection of stopwords created by Dr. Lim
	 */
	public Links(String hD, String stopWords, boolean split,
			ArrayList<ArrayList<RankedTweetList>> rankedList) {
		this.harddrive = hD;
		this.stopWordsColl = hD + "/src/Query_ExpansionInternal/" + stopWords;
		Links.rankedTweetLists = rankedList;
	}

	/**
	 * Creates the list of new queries based off top 30 links of each topic
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList<String> getNewQueries(ArrayList<LuceneQuery> queries) {
		ArrayList<String> newTopics = new ArrayList<String>();

		for (int i = 0; i < queries.size(); i++) {
			newTopics.add(null);
		}

		System.out.println("Creating ranked url list...");
		ArrayList<RankedTweetList> rankedUrlLists = TwitterIndexerUrls.createRankedUrls(queries, rankedTweetLists, hitNum, harddrive);
		System.out.println("Finished ranked url list. Created topic words...");

		// string is topic, hashmap is each word and the amount of times it
		// occurred
		HashMap<String, HashMap<String, Integer>> topicWords = null;
		try {
			topicWords = createTopicWords(rankedUrlLists, queries);
		} catch (IOException e) {
			System.err
					.println("In Links. java (in getNewQueries). Error when creating topic words. ERROR: "
							+ e.getMessage());
			System.exit(0);
		}

		System.out.println("Finished topic words. Starting createStopWords...");

		HashSet<String> stopWords = CommonWords.createStopWords(stopWordsColl);

		System.out.println("Finished createStopWords. Making top 10 list...");

		Iterator it = topicWords.entrySet().iterator();

		// goes through each hashmap object
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();

			// as long as the hashmap isn't empty
			if (pairs.getValue() != null) {
				// grabs the top words
				ArrayList<String> topWords = updateTopWords(
						(HashMap<String, Integer>) pairs.getValue(), stopWords);

				// grabs the topic numbers (1, 51, 111, etc)
				String testTopic = ((String) pairs.getKey()).substring(3);
				if (testTopic.startsWith("0")) {
					testTopic = testTopic.substring(1);
				}

				int topic = Integer.parseInt(testTopic);

				String newQuery = "";
				// goes through and collects top words
				for (int i = 0; i < topWords.size(); i++) {
					newQuery += topWords.get(i).substring(0,
							topWords.get(i).indexOf(" "))
							+ " ";
				}

				// sets returning value to right set
				// done this way because hashmaps add topics out of order
				newTopics.set(topic - 1, newQuery.trim());
			}
		}

		System.out.println("Finished making top 10 list.");

		return newTopics;
	}

	/**
	 * Goes through and gather the words, and the amount of times they occur,
	 * across all urls in the topic.
	 * 
	 * @param rankedUrlLists
	 *            The list of urls; 30 for each topic
	 * @param queries
	 *            The topics for the twitter collecton
	 * @return Each topic and a HashMap of words and the amount of times they
	 *         occurred
	 */
	private HashMap<String, HashMap<String, Integer>> createTopicWords(
			ArrayList<RankedTweetList> rankedUrlLists,
			ArrayList<LuceneQuery> queries) throws IOException {
		HashMap<String, HashMap<String, Integer>> topicWords = new HashMap<String, HashMap<String, Integer>>();

		for (int i = 0; i < queries.size(); i++) {
			String qID = "MB" + queries.get(i).getQueryNum();

			topicWords.put(qID, null);
		}

		// goes through each topic's urls
		for (int i = 0; i < rankedUrlLists.size(); i++) {
			// goes through top urls (set to 30) for each topic
			for (int m = 0; m < rankedUrlLists.get(i).size(); m++) {
				String contentString = rankedUrlLists.get(i).getRankedList()
						.get(m).getContent();
				String topicNumString = rankedUrlLists.get(i).getRankedList()
						.get(m).getTopicNum();
				String titleString = rankedUrlLists.get(i).getRankedList()
						.get(m).getTitle();

				HashMap<String, Integer> words = topicWords.get(topicNumString);

				if (words == null) {
					words = new HashMap<String, Integer>();
				}

				// Goes through the title and content for top words

				StringTokenizer sT = null;
				if (!titleString.equals("TITLEBLANK")) {
					sT = new StringTokenizer(titleString);
					String curTok;

					while (sT.hasMoreTokens()) {
						curTok = sT.nextToken();

						// keeps weird words from messing up our system
						if (!asciiEncoder.canEncode(curTok)) {
							continue;
						}

						curTok = CommonWords.updatesWord(curTok);
						curTok = curTok.toLowerCase().trim();

						if (queries.get(i).getQuery().toLowerCase()
								.contains(curTok)) {
							continue;
						}

						if (!curTok.isEmpty()) {
							if (words.containsKey(curTok)) {

								int amount = words.get(curTok);

								words.remove(curTok);

								amount++;

								words.put(curTok, amount);
							} else {
								words.put(curTok, 1);
							}
						}
					}
				}

				sT = new StringTokenizer(contentString);
				String curTok;

				while (sT.hasMoreTokens()) {
					curTok = sT.nextToken();
					if (!asciiEncoder.canEncode(curTok)) {
						continue;
					}

					curTok = CommonWords.updatesWord(curTok);
					curTok = curTok.toLowerCase().trim();

					if (queries.get(i).getQuery().toLowerCase()
							.contains(curTok)) {
						continue;
					}

					if (!curTok.isEmpty()) {
						if (words.containsKey(curTok)) {
							int amount = words.get(curTok);

							words.remove(curTok);

							amount++;

							words.put(curTok, amount);
						} else {
							words.put(curTok, 1);
						}
					}
				}

				topicWords.remove(topicNumString);
				topicWords.put(topicNumString, words);
			}
		}

		return topicWords;
	}

	/**
	 * Takes each list, for each topic, and sorts it (highest amount to lowest).
	 * 
	 * @param topTen
	 *            The words found, with amounts, for each topic
	 * @param stopWords
	 *            The stopWords collection (doesn't allow stop words for top 10)
	 * @return A new arraylist of the words, in order, with amounts
	 */
	@SuppressWarnings("rawtypes")
	private ArrayList<String> updateTopWords(HashMap<String, Integer> topTen,
			HashSet<String> stopWords) {
		int topAmount = 10;
		int amount = 0;

		ArrayList<String> wordsInOrder = new ArrayList<String>();

		// checks arguments
		if (topTen == null || topTen.isEmpty()) {
			System.err
					.println("In Links.java (in updateTopWords). Something is wrong with topTen!");
			System.exit(0);
		}

		// creates the top numbers for all words
		ArrayList<Integer> topNumbers = new ArrayList<Integer>();

		// First adds all numbers to the new arrayList topNumbers
		Iterator it = topTen.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();

			// grabs each number for each word
			topNumbers.add((Integer) pairs.getValue());
		}

		// Sorts the collection and prints out, from max to min
		for (int i = 0; i < topNumbers.size(); i++) {
			// Gets max number within the current collection of numbers
			int maxNum = Collections.max(topNumbers);

			// once it gets down to ones, we don't want it anymore
			if (maxNum == 1) {
				break;
			}

			// uses topTen to find number
			String key = "";
			it = topTen.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pairs = (Map.Entry) it.next();

				// grabs each number and checks it
				key = (String) pairs.getKey();
				int temp = topTen.get(key);
				if (temp == maxNum) {
					break;
				}
			}

			// gets the word that matches the top number
			String wordMax = key;

			// find urls in tweet
			if (!(URLListGen.containsUrl(wordMax) && wordMax.isEmpty() && wordMax.equals("") && wordMax
					.equals(" "))) {
				// adds to arraylist if okay word
				if (!stopWords.contains(wordMax) && amount < topAmount && isAlphaNumeric(wordMax)) {
					wordsInOrder.add(wordMax + " &" + maxNum);
					amount++;
				}

				if (amount >= topAmount) {
					break;
				}
			}

			// Removes the maxNum and maxWord from the needed arraylist
			// So that we can find the next max
			topTen.remove(wordMax);
			topNumbers.remove(topNumbers.indexOf(maxNum));
		}

		topNumbers.clear();

		return wordsInOrder;
	}
	
	public static boolean isAlphaNumeric(String s){
		String pattern = "^[a-zA-Z0-9]*$";
		if(s.matches(pattern)){
			return true;
		}
		
		return false;
	}
}