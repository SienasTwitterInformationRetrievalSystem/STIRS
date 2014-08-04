package Query_ExpansionInternal;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import API.LuceneQuery;
import LinkCrawling.Links;
import LinkCrawling.URLListGen;

/**
 * Query Expansion Internal (And External) Gets the X most common words from our
 * twitter corpus and writes it to a file
 * 
 * Version 2.0: uses stop words and gets rid of punctuation Gathers both for top
 * words overall across topics and top 10 words per topic
 * 
 * @author Karl Appel v1.0
 * @author Lauren Mathews v2.0
 * @version 6/29/2012 v1.0
 * @version 6/17/2014 v2.0
 */
public class CommonWords {

	// The top x most common words you want to retrieve
	private static final int THRESHOLD = 50;

	// the top words across topic (original will contain all words)
	public static HashMap<String, String> topXWords = new HashMap<String, String>();

	// the top words per topic (original contains all words per topic)
	public static ArrayList<String> topWordsPerTopics = new ArrayList<String>();

	/**
	 * Goes through output.csv
	 * 
	 * For topXWords: adds all words to this list, only 1 copy of each word;
	 * keeps track of how many times it occurs For
	 * 
	 * top10Words: adds all words occuring within topic; does sort them; only 1
	 * copy of each; keeps track of how many times it occurs
	 * 
	 * @param inputName
	 *            output.csv created by Results.java; all tweets by topics
	 *            retrived by API
	 * @param thresholdTopic
	 */
	public static void scrapOutput(String inputName,
			HashMap<String, Float> thresholdTopic, String lastPossibleTopicNum,
			boolean hashtag) throws IOException {
		// checks to make sure this isn't empty
		if (inputName.isEmpty()) {
			System.err
					.println("In CommonWords.java (in scrapOutput). inputName doesn't have a file path (output.csv) in it.");
			System.exit(0);
		}

		// Sets the arrayList for the top ten words data
		HashMap<String, String> topTen = new HashMap<String, String>();

		// read in our corpus file
		BufferedReader bPos = null;
		try {
			bPos = new BufferedReader(new FileReader(inputName));
		} catch (FileNotFoundException e) {
			System.err
					.println("In CommonWords.java (in scrapOutput). output.csv not found (STIRS.java creates this file). Java error: "
							+ e.getMessage());
			System.exit(0);
		}

		// current line read in from our file
		// muliple readLine's here because first few lines aren't relevant
		String currentLine = bPos.readLine();
		currentLine = bPos.readLine();
		currentLine = bPos.readLine();

		// starts with first line with tweets
		currentLine = bPos.readLine();

		// tokenizes the whole string
		StringTokenizer sT = new StringTokenizer(currentLine);

		// grabs the topicNum
		String curTok = sT.nextToken();
		String lastTopicNum = curTok.substring(0, 5);

		// System.out.println("Starting topic " + lastTopicNum + "...");

		// adds topicNum to arraylist
		topWordsPerTopics.add(lastTopicNum.toLowerCase().trim());

		// goes through every line of output.csv
		while (currentLine != null) {
			// converts everything to lowercase for comparison
			currentLine = currentLine.toLowerCase().trim();

			// tokenizes the new line
			sT = new StringTokenizer(currentLine, ",\"");

			// for checking to see if new topic
			curTok = sT.nextToken();
			String topicNum = curTok.substring(0, 5);

			// if a new topic, deals with top10Words stuff
			if (!topicNum.equalsIgnoreCase(lastTopicNum)) {
				// keeps track of checking topic numbers
				lastTopicNum = topicNum;

				// updates top words across topics (sorts then adds them)
				updateTopWords(topTen);

				// Gets stuff ready for next topic

				// First clears the arrayLists for new use
				topTen.clear();

				// Reads in the inputLine to find topic num
				sT = new StringTokenizer(currentLine, ",\"");
				curTok = sT.nextToken();
				topicNum = curTok.substring(0, 5);

				// adds new topic num
				topWordsPerTopics.add(topicNum);

				// System.out.println("Starting topic " + topicNum + "...");
			}

			// includes the topic num and tweet id
			curTok = sT.nextToken();

			// if a tweet is empty (rare occurance)
			if (sT.hasMoreTokens()) {
				curTok = sT.nextToken();
			} else {
				currentLine = bPos.readLine();
				continue;
			}

			String tweetCheck = curTok;

			curTok = sT.nextToken();

			while (!isNumeric(curTok)) {
				curTok = sT.nextToken();
			}

			curTok = sT.nextToken();
			curTok = sT.nextToken();
			curTok = sT.nextToken();
			curTok = sT.nextToken();
			curTok = sT.nextToken();
			curTok = sT.nextToken();
			curTok = sT.nextToken();

			float score = Float.valueOf(curTok);

			float threshold = thresholdTopic.get(topicNum);

			if (score < threshold) {
				currentLine = bPos.readLine();

				// goes through here for last topic
				if (topicNum.equalsIgnoreCase(lastPossibleTopicNum)
						&& currentLine == null) {
					updateTopWords(topTen);
					break;
				}

				continue;
			}

			// Changes the tweet all to lowercase for comparison purposes
			tweetCheck = tweetCheck.toLowerCase().trim();
			tweetCheck = URLListGen.removeURLs(tweetCheck);

			StringTokenizer tweetToken = new StringTokenizer(tweetCheck, " ");
			String curWord;

			// Goes through each word in the tweet, gathers data
			while (tweetToken.hasMoreTokens()) {
				curWord = tweetToken.nextToken();
				curWord = curWord.toLowerCase().trim();

				// for empty spaces
				while (curWord.isEmpty()) {
					curWord = tweetToken.nextToken();
					curWord = curWord.toLowerCase().trim();
				}

				// hashtags and retweets are kept for special purposes
				if (!(curWord.startsWith("#") && curWord.startsWith("@"))) {
					curWord = updatesWord(curWord);
				}

				// for top ten words
				// If the word is already in arrayList, updates counter;
				// otherwise adds to end of arrayList
				if (topTen.containsKey(curWord)) {
					int wrdCnt = Integer.parseInt(topTen.get(curWord)
							.substring(1));
					wrdCnt++;

					topTen.remove(curWord);

					topTen.put(curWord, "&" + Integer.toString(wrdCnt));
				} else {
					topTen.put(curWord, "&1");
				}

				if (hashtag) {
					if (curWord.startsWith("#")) {
						continue;
					}
				}

				// for top overall words
				if (curWord.isEmpty() || curWord.equals("")
						|| curWord.equals(" ") || curWord.equals("'")) {
				} else if (topXWords.containsKey(curWord)) {
					int number = Integer.parseInt(topXWords.get(curWord)
							.substring(1));
					number++;

					String wrdCnt = "&" + Integer.toString(number);

					topXWords.remove(curWord);

					topXWords.put(curWord, wrdCnt);
				} else {
					topXWords.put(curWord, "&1");
				}
			}

			currentLine = bPos.readLine();

			// goes through here for last topic
			if (topicNum.equalsIgnoreCase(lastPossibleTopicNum)
					&& currentLine == null) {
				updateTopWords(topTen);
				break;
			}
		}

		bPos.close();
	}

	/**
	 * Creates the topWords across all topics list Only has top THRESHOLD words
	 * Uses stopWords (if external is on) to remove words Uses own stop words
	 * list to remove words Uses queries list to remove words
	 * 
	 * @param stopWords
	 *            a list of stop words, external
	 * @param ownStopWords
	 *            a list of stop words, using the corpus
	 * @param queries
	 *            a list of query topics
	 * @return a HashSet of top words across all topics
	 */
	@SuppressWarnings("rawtypes")
	public static HashSet<String> topXWords(HashSet<String> stopWords,
			HashSet<String> ownStopWords, ArrayList<LuceneQuery> queries) {

		// checks arguments to make sure they're working
		if (topXWords == null || topXWords.isEmpty()) {
			System.err
					.println("In CommonWords.java (in topXWords). Something is wrong with topXWords.");
			System.exit(0);
		}

		if (queries == null || queries.isEmpty()) {
			System.err
					.println("In CommonWords.java (in topXWords). The queries, from output.csv, is null.");
			System.exit(0);
		}

		// used for topXWords
		ArrayList<String> top50 = new ArrayList<String>();

		Iterator it = topXWords.entrySet().iterator();
		// First adds all numbers to the new arrayList topNumbers
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();

			top50.add((String) pairs.getKey());
			top50.add((String) pairs.getValue());
		}

		// used to organize top 50 words
		ArrayList<Integer> topNumbers = new ArrayList<Integer>();

		// goes through all the words from the corpus
		for (int i = 0; i < top50.size(); i += 2) {

			// removes stop words
			if (stopWords != null) {
				if (stopWords.contains(top50.get(i))) {
					top50.remove(i);
					top50.remove(i);

					// after removal, must to keep from missing words
					i -= 2;

					continue;
				}
			}

			// always goes through and eliminates own stop words
			if (ownStopWords != null) {
				if (ownStopWords.contains(top50.get(i))) {
					top50.remove(i);
					top50.remove(i);

					// after removeal, must keep it from missing words
					i -= 2;

					continue;
				}
			}

			// removes query topic words
			boolean temp = false;
			query: for (int j = 0; j < queries.size(); j++) {
				StringTokenizer sT = new StringTokenizer(queries.get(j)
						.getQuery());
				String curTok;

				// goes through each word of query
				while (sT.hasMoreTokens()) {
					curTok = sT.nextToken();
					curTok = curTok.toLowerCase().trim();

					curTok = updatesWord(curTok);

					// if found, doesn't add it
					if (curTok.equalsIgnoreCase(top50.get(i))) {
						temp = true;
						break query;
					}
				}
			}

			// if found, removes it
			if (temp) {
				temp = false;

				top50.remove(i);
				top50.remove(i);

				i -= 2;

				continue;
			}

			// only adds it if not removed
			topNumbers.add(Integer.parseInt(top50.get(i + 1).substring(1)));
		}

		// goes through and adds in order
		HashSet<String> topFifyWords = sortsNumbers(top50, topNumbers,
				THRESHOLD);

		return topFifyWords;
	}

	/**
	 * Gathers the top 10 words for each topics Uses topOverallWords, stopWords,
	 * ownStopWords and queries to remove
	 * 
	 * @param topOverallWords
	 *            top words found across all topics
	 * @param stopWords
	 *            stop words list
	 * @param ownStopWords
	 *            stop words from own corpus
	 * @param queries
	 *            list of query topics
	 * @return a new arraylist containing all the new words
	 */
	public static ArrayList<String> top10Words(HashSet<String> topOverallWords,
			HashSet<String> stopWords, HashSet<String> ownStopWords,
			ArrayList<LuceneQuery> queries, boolean hashtag) {
		// checks arguments for errors
		if (topOverallWords == null || topOverallWords.isEmpty()) {
			System.err
					.println("In CommonWords.java (in top10Words). Something is wrong with topOverallWords.");
			System.exit(0);
		}

		if (topWordsPerTopics == null || topWordsPerTopics.isEmpty()) {
			System.err
					.println("In CommonWords.java (in topWordsPerTopics). Something is wrong with topWordsPerTopics.");
			System.exit(0);
		}

		if (queries == null || queries.isEmpty()) {
			System.err
					.println("In CommonWords.java (in top10Words). The queries, from output.csv, is null.");
			System.exit(0);
		}

		// will contain the top 10 words per topic
		ArrayList<String> top10Words = new ArrayList<String>();

		// keeps it to 10
		int amt = 0;

		// used for checking queries
		int queNum = -1;

		// used for keeping track of topics and the current line
		String topicNum = "";
		String currentLine = "";

		// goes through all topics
		for (int i = 0; i < topWordsPerTopics.size(); i++) {
			// gets the current line (either the topic number or the word +
			// amount)
			currentLine = topWordsPerTopics.get(i);

			// doesn't need to do checking if we've already reach max amount
			if (amt >= 10) {
				String breakNum = "mb"
						+ queries.get(queries.size() - 1).getQueryNum();

				if (topicNum.equalsIgnoreCase(breakNum)) {
					break;
				}

				// once it hits a new topic, want to start checking again
				while (!(currentLine.startsWith("MB") || currentLine
						.startsWith("mb")) || currentLine.contains("&")) {
					i++;
					currentLine = topWordsPerTopics.get(i);
				}
			}

			// basically resets if new topic
			if (currentLine.startsWith("MB") || currentLine.startsWith("mb")
					|| !currentLine.contains("&")) {
				topicNum = currentLine;

				top10Words.add(topicNum);
				amt = 0;
				queNum++;
				continue;
			}

			// gets just the word
			String word = currentLine
					.substring(0, currentLine.indexOf("&") - 1);

			// checks stop words
			if (stopWords != null) {
				if (stopWords.contains(word)) {
					continue;
				}
			}
			
			if(!Links.isAlphaNumeric(word)){
				continue;
			}

			// checks own stop words
			if (ownStopWords != null) {
				if (ownStopWords.contains(word)) {
					continue;
				}
			}

			if (hashtag) {
				if (word.startsWith("#")) {
					continue;
				}
			}

			// checks query (for only this topic)
			boolean temp = false;
			StringTokenizer sT = new StringTokenizer(queries.get(queNum)
					.getQuery());
			String curTok;

			// goes through the query topic
			while (sT.hasMoreTokens()) {
				curTok = sT.nextToken();
				curTok = curTok.toLowerCase().trim();

				curTok = updatesWord(curTok);

				if (curTok.equalsIgnoreCase(word)) {
					temp = true;
					break;
				}

				// does checks for words like
				// "service" to "services", etc
				String checkTok = curTok;

				if (checkTok.endsWith("s")) {
					checkTok = checkTok.substring(0, checkTok.length() - 1);
				} else {
					checkTok = checkTok + "s";
				}

				if (checkTok.equalsIgnoreCase(word)) {
					temp = true;
					break;
				}

				checkTok = curTok;

				if (checkTok.contains("'")) {
					checkTok = checkTok.substring(0, checkTok.indexOf("'"));
				} else {
					checkTok = checkTok + "'s";
				}

				if (checkTok.equalsIgnoreCase(word)) {
					temp = true;
					break;
				}
			}

			if (temp) {
				temp = false;
				continue;
			}

			// checks to see if in top words across topics
			if (topOverallWords.contains(word)) {
				continue;
			} else {
				// only want to add 10 per topic
				if (amt >= 10) {
					continue;
				} else {
					amt++;

					top10Words.add(currentLine.toLowerCase().trim());
				}
			}
		}
		
		return top10Words;
	}

	/**
	 * Creates our own stopWords list (for internal, and external, query
	 * expansion). This method is called twice; the first time it makes it's
	 * original list of stop words from the corpus, while the second time it
	 * cleans up the list of words that cropped up after the original run, which
	 * usually still contains stop words.
	 * 
	 * @param top10Words
	 *            The top ten words found for each topic using all tweets
	 *            originally returned
	 * @param listOfWords
	 *            If run the second time, contains the original list of stop
	 *            words created from the corpus
	 * @return an arraylist of all the stop words found in the file
	 */
	public static HashSet<String> createOwnStopWords(HashSet<String> stopList,
			ArrayList<LuceneQuery> queries) throws IOException {
		// checks arguments for errors
		if (queries == null || queries.isEmpty()) {
			System.err
					.println("In CommonWords.java (in createOwnStopWords). The queries, from output.csv, is null.");
			System.exit(0);
		}

		// contains the list of words found
		ArrayList<String> listOfWords = new ArrayList<String>();

		// goes through all words found for each topic
		for (int i = 0; i < topWordsPerTopics.size(); i++) {
			// gets each line of the file
			String inputLine = topWordsPerTopics.get(i);

			// if it contains a topic num, ignore it
			if (inputLine.startsWith("MB") || inputLine.startsWith("mb")) {
				continue;
			}

			// finds the word, separate from the num, in the file
			String word = inputLine.substring(0, inputLine.indexOf(" "))
					.toLowerCase().trim();

			// checks stop list
			if (stopList != null) {
				if (stopList.contains(word)) {
					continue;
				}
			}
			
			if(!Links.isAlphaNumeric(word)){
				continue;
			}

			// if found, and isn't already in (own) stop words collection, adds
			// it, otherwise, finds it and updates the count
			if (!listOfWords.contains(word)) {
				listOfWords.add(word);
				listOfWords.add("&1");
			} else {
				int indexOfWord = listOfWords.indexOf(word);
				int newAmount = Integer.parseInt(listOfWords.get(
						indexOfWord + 1).substring(1)) + 1;
				String strAmount = String.valueOf(newAmount);
				listOfWords.set(indexOfWord + 1, "&" + strAmount);
			}
		}

		// used to figure out which word has the most found
		ArrayList<Integer> topNumbers = new ArrayList<Integer>();

		// simply goes through and adds all the numbers only to arraylist
		for (int i = 1; i < listOfWords.size(); i += 2) {
			topNumbers.add(Integer.parseInt(listOfWords.get(i).substring(1)));
		}

		// goes through list, adds to ownstopwords in order of highest to lowest
		HashSet<String> ownStopWords = sortsNumbers(listOfWords, topNumbers,
				1000);

		return ownStopWords;
	}

	/**
	 * Creates an arraylist based off of the words in englishStop.txt
	 * 
	 * @param stopWordsColl
	 *            The full file path of the file (englishStop.txt)
	 * @return an arraylist containing the words in the englishStop.txt file
	 */
	public static HashSet<String> createStopWords(String stopWordsColl) {
		// makes sure argument is working
		if (stopWordsColl.isEmpty() || stopWordsColl == null) {
			System.err
					.println("In CommonWords.java (in createStopWords). stopWorsColl doesn't contain file path.");
			System.exit(0);
		}

		// will contain the list of stop words
		HashSet<String> stopWords = new HashSet<String>();

		// reads in list of all stop words (englishStop.txt)
		try {
			BufferedReader bfr = new BufferedReader(new FileReader(
					stopWordsColl));

			String inputLine = bfr.readLine();
			// adds in all words to arraylist
			while (inputLine != null) {
				stopWords.add(inputLine.toLowerCase().trim());
				inputLine = bfr.readLine();
			}

			stopWords.add("null");
			stopWords.add("@");
			stopWords.add("#");
			stopWords.add("click");
			stopWords.add("link");
			stopWords.add("news");
			stopWords.add("views");

			stopWords.add("1");
			stopWords.add("2");
			stopWords.add("3");
			stopWords.add("4");
			stopWords.add("5");
			stopWords.add("6");
			stopWords.add("7");
			stopWords.add("8");
			stopWords.add("9");
			stopWords.add("0");

			bfr.close();
		} catch (FileNotFoundException e) {
			System.err
					.println("In CommonWords.java (in createStopWords). englishStop.txt not found. Java error: "
							+ e.getMessage());
			System.exit(0);
		} catch (IOException e) {
			System.err
					.println("In CommonWords.java (in createStopWords). An error occurred created stopWords. Error: "
							+ e.getMessage());
			System.exit(0);
		}

		return stopWords;
	}

	/**
	 * Removes specific punctuation from the word.
	 * 
	 * @param currentWord
	 *            the word to be updated
	 * @return an updated word with only numbers/letters
	 */
	public static String updatesWord(String currentWord) {
		if (currentWord.isEmpty() || currentWord == null) {
			System.err
					.println("In CommonWords.java (in updatesWord). currentWord is empty or null!");
			System.exit(0);
		}

		// goes through each letter of the word
		for (int i = 0; i < currentWord.length(); i++) {
			// gets the character at the current spot
			char curLetter = currentWord.toLowerCase().charAt(i);
			// checks if it's not a letter
			if ((curLetter < 97 || curLetter > 122) && curLetter != ' ') {
				// makes sure not to get rid of "'" in the middle of words
				// this way words like "I'm" and "let's" stay intact
				if (curLetter == 39
						&& (currentWord.lastIndexOf(curLetter) != currentWord
								.length() - 1)
						&& (currentWord.indexOf(curLetter) != 0)) {
				} else if (curLetter != 35 && curLetter != 64) {
					// checks if it's not a number
					if (curLetter < 48 || curLetter > 57) {
						// if not a letter and number, removes it
						currentWord = currentWord.substring(0, i)
								+ currentWord.substring(i + 1);

						// checks to make sure last character still isn't "'"
						if ((currentWord.lastIndexOf("'") == currentWord
								.length() - 1) && currentWord.length() > 2) {
							currentWord = currentWord.substring(0,
									currentWord.length() - 1);
						}
					}
				}
			}
		}

		return currentWord;
	}

	/**
	 * Removes all punctuation from the word.
	 * 
	 * @param text
	 *            the word to be updated
	 * @return an updated word with only numbers/letters
	 */
	public static String removesAllPunct(String text) {
		if (text.isEmpty() || text == null) {
			return text;
		}

		for (int i = 0; i < text.length() - 1; i++) {
			char curChar = text.charAt(i);
			String curWord = text.substring(i, i + 1);
			// 48-57 (0-9); 97-122 (a-z)
			if (curChar < 97 || curChar > 122) {
				if (curChar < 48 || curChar > 57) {
					if (curChar != 32) {
						text = text.replace(curWord, "");
					}
				}
			}
		}

		return text;
	}

	/**
	 * Used for "scrapOutput". Goes through and sorts out words, in order of
	 * amount.
	 * 
	 * @param topTen
	 *            The list of words for current topic
	 */
	@SuppressWarnings("rawtypes")
	private static void updateTopWords(HashMap<String, String> topTen) {
		// checks arguments
		if (topTen == null || topTen.isEmpty()) {
			System.err
					.println("In CommonWords.java (in updateTopWords). Something is wrong with topTen!");
			System.exit(0);
		}

		// creates the top numbers for all words
		ArrayList<Integer> topNumbers = new ArrayList<Integer>();

		// First adds all numbers to the new arrayList topNumbers
		Iterator it = topTen.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();

			// grabs each number for each word
			String number = (String) pairs.getValue();
			number = number.substring(1);

			topNumbers.add(Integer.parseInt(number));
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
				int temp = Integer.parseInt(topTen.get(key).substring(1));
				if (temp == maxNum) {
					break;
				}
			}

			// gets the word that matches the top number
			String wordMax = key;

			// adds word and number to arraylist
			if (wordMax.isEmpty() || wordMax.equals("") || wordMax.equals(" ")
					|| wordMax.contains("http")) {
			} else {
				topWordsPerTopics.add(wordMax + " &" + maxNum);
			}

			// Removes the maxNum and maxWord from the needed arraylist
			// So that we can find the next max
			topTen.remove(wordMax);
			topNumbers.remove(topNumbers.indexOf(maxNum));
		}

		topNumbers.clear();
	}

	/**
	 * Sorts the numbers (for topXWords & createOwnStopWords) for words.
	 * 
	 * @param listOfWords
	 *            The list being used to gather each word
	 * @param topNumbers
	 *            THe numbers to be sorted
	 * @param THRESHOLD
	 *            The amount of times to run loop
	 */
	private static HashSet<String> sortsNumbers(ArrayList<String> listOfWords,
			ArrayList<Integer> topNumbers, int THRESHOLD) {
		// checks arguments
		if (listOfWords.isEmpty() || listOfWords == null) {
			System.err
					.println("In Commonwords.java (in sortsNumbers). Something is wrong with listOfWords!");
			System.exit(0);
		}

		if (topNumbers.isEmpty() || topNumbers == null) {
			System.err
					.println("In Commonwords.java (in sortsNumbers). Something is wrong with topNumbers!");
			System.exit(0);
		}

		if (THRESHOLD < 0) {
			System.err
					.println("In Commonwords.java (in sortsNumbers). Something is wrong with THRESHOLD!");
			System.err.println("THRESHOLD: " + THRESHOLD);
			System.exit(0);
		}

		// the list of words to be returned
		HashSet<String> wordsReturned = new HashSet<String>();

		// goes through the list
		for (int i = 0; i < THRESHOLD; i++) {
			// finds the top number each time
			int maxNum = Collections.max(topNumbers);

			// once it gets down to ones, we don't want it anymore
			if (maxNum == 1) {
				break;
			}

			// all values had been inputted with "&", to distinguish
			// them from words which were numbers
			// this re-adds them so it can be matched up with arraylist
			String findMaxNum = "&" + String.valueOf(maxNum);

			// gets the index of the max number
			int maxNumIndex = listOfWords.indexOf(findMaxNum);

			// gets the word that matches the top number
			String wordMax = listOfWords.get(maxNumIndex - 1);

			// adds the word and number, in order, to arraylist
			wordsReturned.add(wordMax.toLowerCase().trim());

			// removes it from these two arraylists
			// keeps the word and number from being found again
			listOfWords.remove(maxNumIndex);
			topNumbers.remove(topNumbers.indexOf(maxNum));
		}

		return wordsReturned;
	}

	private static boolean isNumeric(String str) {
		return str.matches("-?\\d+(\\.\\d+)?");
	}

	/**
	 * Removes all query words from the text
	 */
	public String removeQueryWords(String query, String text) {
		// Removes any words that are contained in the query itself
		StringTokenizer sT = new StringTokenizer(
				query.replace(",", " ").trim(), " ");

		outer: while (sT.hasMoreTokens()) {
			String indivQuery = sT.nextToken().trim();

			StringTokenizer sT2 = new StringTokenizer(text);

			while (sT2.hasMoreTokens()) {
				String indivWord = sT2.nextToken().trim();

				if (indivWord.equalsIgnoreCase(indivQuery)) {
					text = text.replace(indivWord, "");
					continue outer;
				}
			}
		}

		return text;
	}
}