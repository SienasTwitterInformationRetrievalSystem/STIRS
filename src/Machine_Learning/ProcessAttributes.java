package Machine_Learning;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import API.LuceneQuery;

import LinkCrawling.URLListGen;
import Miscellaneous.POSTagger;
import Query_ExpansionInternal.CommonWords;

/**
 * Used to find attributes within tweet objects and create WekaTweets.
 * 
 * @author Lauren Mathews v1.0
 * @author Timothy LaRock v2.0
 * @version 6/27/12 v1.0
 * @version 7/15/2014 v2.0
 */
public class ProcessAttributes {
	private String tweet, hashtag, mention, judges;
	private long tweetID;
	private int followers, retweets;
	private boolean relevantURL;
	private double tweetsToFollowers;
	private ArrayList<LuceneQuery> queries;

	/**
	 * @param t
	 *            status
	 * @param tweetID
	 * @param retweets
	 *            number of retweets
	 * @param tweetsToFollowers
	 *            ratio of tweets to followers
	 * @param followers
	 *            number of followers
	 * @param queries
	 *            List of query topics
	 */
	public ProcessAttributes(String t, long tweetID, int retweets,
			double tweetsToFollowers, int followers, boolean relevantURL,
			ArrayList<LuceneQuery> queries, String judgment) {
		this.tweet = t;
		this.tweetID = tweetID;
		this.retweets = retweets;
		this.queries = queries;
		this.tweetsToFollowers = tweetsToFollowers;
		this.followers = followers;
		this.relevantURL = relevantURL;
		this.judges = judgment;
	}

	/**
	 * This method takes a regular tweet and its data and creates a wekaTweet
	 * object.
	 * 
	 * THIS IS WHERE RELEVANCE DECISIONS FOR TRAINING SETS ARE MADE
	 * 
	 * @param queryIndex
	 *            used to get the query for the current tweet
	 */
	public WekaTweet processWekaTweet(int queryIndex, boolean trainingSet) {
		WekaTweet wt;

		// Find out whether there is a hashtag or a mention
		// in the current tweet
		hashtag = (tweet.startsWith("#") || tweet.contains(" #")) ? "Yes"
				: "No";
		mention = (tweet.startsWith("@") || tweet.contains(" @")) ? "Yes"
				: "No";

		double percentMatch = queryMatchPercentage(tweet,
				queries.get(queryIndex).getQuery());

		boolean max_rts;

		// 100 is the maximum num_retweet value returned by
		// the API. Using that as the threshold.
		if (this.retweets > 99) {
			max_rts = true;
		} else {
			max_rts = false;
		}

		// create an integer array holding
		// [0] nouns and [1] verbs
		// int[] posResults = new int[2];
		// posResults = posResults();

		// create wekatweet
		wt = new WekaTweet(max_rts, retweets, URLListGen.containsUrl(tweet),
				hashtag, mention, tweetID, percentMatch, tweetsToFollowers,
				followers,
				/* posResults[0], posResults[1], */relevantURL);

		if (trainingSet) {
			// check and set relevance
			if (checkRelevance(tweetID)) {
				wt.setRelevance(true);
			} else {
				wt.setRelevance(false);
			}
		}

		return wt;
	}

	/**
	 * This method checks the relevance of a given tweetID compared to a qrels
	 * file. In the future, it should accept both a tweetID value AND a qrels
	 * filepath in order successfully evaluate any tweetset. This method should
	 * only be used to create training sets, it cannot deal with incoming data
	 * because there will be no qrels file to evaluate against, it is up to the
	 * system to decide relevance.
	 * 
	 * @param tweetID
	 * @return true if relevant, false if not
	 */
	private boolean checkRelevance(long thisTweetID) {
		String line = "";
		ArrayList<QTweet> qRelsList = new ArrayList<QTweet>();
		int qNum, relevanceNum;
		qNum = relevanceNum = 0;
		boolean rel = false;
		// create the qRels list
		try {
			BufferedReader qrelsBuf = new BufferedReader(
					new FileReader(judges));

			// Read all qrels tweets into ArrayList
			// while qrelsBuf still has information
			while ((line = qrelsBuf.readLine()) != null) {
				// Tokenize the line
				StringTokenizer st = new StringTokenizer(line);

				// Grab the qNum, skip the unused column, grab tweetID and
				// relevanceNum
				qNum = Integer.parseInt(st.nextToken());
				st.nextToken();
				tweetID = Long.parseLong(st.nextToken());
				relevanceNum = Integer.parseInt(st.nextToken());

				// add the new QTweet to the ArrayList
				qRelsList.add(new QTweet(qNum, tweetID, relevanceNum));
			}

			qrelsBuf.close();
		} catch (IOException io) {
			System.err.println("Caught IOException: " + io.getMessage());
		}

		// traverse to the tweetID
		for (int j = 0; j < qRelsList.size(); j++) {
			// when the ID is found
			if (qRelsList.get(j).getTweetID() == thisTweetID) {

				// System.out.println("Found TweetID in qRelsList: " +
				// thisTweetID);
				// Check the judged relevance of the tweet
				// if it is a 1 or a 2 it is relevant, otherwise
				// it is not
				if ((qRelsList.get(j).getRelevanceNum() == 2)
						|| (qRelsList.get(j).getRelevanceNum() == 1)) {
					rel = true;
				}
			}
		}

		return rel;
	}

	/**
	 * This class returns an integer array holding nouns and verbs found using a
	 * Part of speech tagger
	 * 
	 * @return resultArray int[] - int[0] nouns int[1] verbs
	 */
	@SuppressWarnings("unused")
	private int[] posResults() {
		// declare variables
		int verbs = 0;
		int nouns = 0;
		String[] taggerArray;
		POSTagger tagger = new POSTagger();

		String modelFile = "/home/lmathews/workspace/libraries/en-pos-maxent.bin";

		// initialize tagger
		tagger.initializeTagger(modelFile);

		// run tagger on status
		taggerArray = tagger.findTags(tweet);

		// for each part of speech returned
		for (int i = 0; i < taggerArray.length; i++) {
			if ((taggerArray[i].compareTo("NN") == 0)
					|| (taggerArray[i].compareTo("NNS") == 0)
					|| (taggerArray[i].compareTo("NNP") == 0)) {
				nouns++;
			} else if (((taggerArray[i].compareTo("VBD") == 0) || (taggerArray[i]
					.compareTo("VBZ") == 0))) {
				verbs++;

			}
		}
		int[] resultArray = new int[2];
		resultArray[0] = nouns;
		resultArray[1] = verbs;
		return resultArray;
	}

	/**
	 * Uses code taken from rescoreTweets.java to resolve the percentage of
	 * words found in both the query and the tweet.
	 * 
	 * @param tweet
	 *            the tweet being re-calculated
	 * @param curScore
	 *            the original score of the tweet
	 * @param query
	 *            the topic the tweet belongs to
	 * @return a new score to be added to that tweet
	 */
	private static double queryMatchPercentage(String tweet, String query) {
		// Fixes the tweet, for later use (to find it the whole tweet is just a
		// url or not)
		tweet = tweet.trim();

		// Creates a temp tweet, tweetCheck, that will be the tweet we use to
		// compare with
		String tweetCheck = tweet;

		// Sets both tweet(Check) and query to lowercase and trims for
		// comparison purposes below
		tweetCheck = tweetCheck.toLowerCase().trim();
		query = query.toLowerCase().trim();

		// Goes through the tweet and removes all non alphabet/number/space
		// values
		tweetCheck = CommonWords.removesAllPunct(tweetCheck);

		// A counter the counts how many of the query's words are in the tweet
		double count = 0;

		// While loop: Goes through every tweet word and checks to see if it
		// matches a query word
		StringTokenizer sTT = new StringTokenizer(tweetCheck, " ");
		while (sTT.hasMoreTokens()) {
			String inputLine = sTT.nextToken();

			StringTokenizer sTQ = new StringTokenizer(query, " ");

			while (sTQ.hasMoreTokens()) {

				String lineInput = sTQ.nextToken();

				if (lineInput.equals(inputLine)) {
					count++;
					break;
				}
			}
		}

		// Creates a percent of how much of the tweet is the query
		double percent = count / tweetCheck.length() * 1000;
		return percent;
	}
}