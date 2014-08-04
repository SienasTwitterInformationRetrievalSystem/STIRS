package API;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

import API.RankedTweetList;
import API.Tweet;

/**
 * Originally: The TwitterSearch class queries a user-specified index and
 * returns ranked results. Called "LuceneSearch.java"
 * 
 * Version 3.0: Goes into the "index" (provided by API class), which is 50
 * files, titled with their topic number, and each holds all the tweets
 * retrieved by the API for that topics. Returns a ranked tweet list of those
 * tweets (simply takes in each line and puts them in).
 * 
 * @author Carl Tompkins v1.0
 * @edited Matt Kemmer v2.0
 * @edited Karl Appel and Lauren Mathews v3.0
 * @version Lauren Mathews v4.0
 * @version 6/2011 v1.0
 * @version 6/6/12 v3.0
 * @version 7/10/14 v4.0
 */
public class APISearch {

	// The index where all the topic .txt files are
	// "TREC2014\\src\\API\\index";
	private String index = "";

	// initializes logger
	private Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	// number of hits that are returned from our search
	private int hitsReturned = 1000;

	// which field (tweetID, tweet, etc) to search
	private String field;

	/**
	 * Used by Links, RankedJoin and STIRS
	 */
	public void setHitsReturned(int hitsReturned) {
		this.hitsReturned = hitsReturned;
	}

	// Used by method createTweets in STIRS
	public APISearch(Logger logger, String index) {
		this.index += index;
		this.logger = logger;
	}

	/**
	 * Used by Links and RankedJoin
	 */
	public APISearch(Logger logger, String index, String field) {
		this.index += index;
		this.logger = logger;
		this.field = field;
	}

	/**
	 * Goes through the file specified (should be for the query being searched)
	 * and returns an arraylist of tweets. Makes a RankedTweetList of ALL tweets
	 * returned for A topic. Determined by index (file). index should be:
	 * TREC2014\\src\\API\\index\\MB0#.txt
	 */
	public ArrayList<RankedTweetList> search() throws Exception {
		// arraylist of each tweet for the topic
		ArrayList<Tweet> tweets = new ArrayList<Tweet>();

		// API specifications
		String username = null, tweetText = null, lang = null, topicNum = null;
		long tweetID, epoch, statusID, userID, re_status_id, re_user_id;
		int followersCount = -1, statusesCount = -1, re_count = -1, rankInResult = -1;
		float score = -1;

		// used to keep track of how many have been printed vs. how many we want
		// (hitsReturned)
		int count = 0;

		// used for string tokenizing in loop
		String curTok = "";

		// reads in topic's .txt file
		BufferedReader bfr = null;
		try {
			bfr = new BufferedReader(new FileReader(index));
		} catch (Exception e) {
			logger.setUseParentHandlers(true);
			logger.severe(index);
			logger.severe("Something is wrong with reading in the query's file of tweets. "
					+ "Java reported: " + e.getMessage());

			System.err
					.println("In APISearch.java (in search). Something went wrong reading the index folder.");
			System.err.println("index: " + index);
			System.exit(0);
		}

		String inputLine = bfr.readLine();

		// goes through tweet in the topic and gets the information
		while (inputLine != null && count < hitsReturned) {
			// weird error not returning full tweet info
			// couldn't find cause of it, so we remove those
			// tweets (sadly)
			if (!inputLine.trim().endsWith(")")) {
				System.err.println("inputLine: " + inputLine);
				inputLine = bfr.readLine();
				continue;
			}

			// first part is the topicNum, rank result and score
			StringTokenizer sT = new StringTokenizer(inputLine);

			curTok = sT.nextToken();
			topicNum = curTok;

			// topicNum should always be first
			if (!topicNum.startsWith("MB") || topicNum.length() != 5) {
				System.err
						.println("In APISearch.java. (in search (1)). Something is wrong with topicNum: "
								+ topicNum);
			}

			curTok = sT.nextToken();
			curTok = sT.nextToken();
			curTok = sT.nextToken();
			rankInResult = Integer.parseInt(curTok);

			curTok = sT.nextToken();
			score = Float.valueOf(curTok);

			inputLine = inputLine.substring(inputLine.indexOf("#") + 2);

			// second line contains all the tweet information
			sT = new StringTokenizer(inputLine, ",");

			curTok = sT.nextToken();
			tweetID = Long.parseLong(curTok.substring(curTok.indexOf(":") + 1));

			curTok = sT.nextToken();
			curTok = sT.nextToken();
			username = curTok.substring(curTok.indexOf(":") + 1);

			curTok = sT.nextToken();

			while (!curTok.contains(" epoch:")) {
				curTok = sT.nextToken();
			}

			epoch = Long.parseLong(curTok.substring(curTok.indexOf(":") + 1));

			curTok = sT.nextToken();
			tweetText = curTok.substring(curTok.indexOf(":") + 1);

			curTok = sT.nextToken();

			while (!curTok.contains(" followers_count:")) {
				curTok = sT.nextToken();
			}

			followersCount = Integer.parseInt(curTok.substring(curTok
					.indexOf(":") + 1));

			curTok = sT.nextToken();
			statusesCount = Integer.parseInt(curTok.substring(curTok
					.indexOf(":") + 1));

			curTok = sT.nextToken();
			lang = curTok.substring(curTok.indexOf(":") + 1);

			// extra language buffer
			if (lang.trim().equalsIgnoreCase("null")) {
			} else if (!lang.trim().equalsIgnoreCase("en")) {
				inputLine = bfr.readLine();
				continue;
			}

			curTok = sT.nextToken();
			statusID = Long
					.parseLong(curTok.substring(curTok.indexOf(":") + 1));

			curTok = sT.nextToken();
			userID = Long.parseLong(curTok.substring(curTok.indexOf(":") + 1));

			curTok = sT.nextToken();
			re_status_id = Long
					.parseLong(curTok.substring(curTok.indexOf(":") + 1));

			curTok = sT.nextToken();
			re_user_id = Long
					.parseLong(curTok.substring(curTok.indexOf(":") + 1));

			curTok = sT.nextToken();
			re_count = Integer.parseInt(curTok.substring(
					curTok.indexOf(":") + 1, curTok.indexOf(")")));

			// adds it to tweets via a new tweet
			tweets.add(new Tweet(tweetID, username, epoch, tweetText,
					followersCount, statusesCount, lang, statusID, userID,
					re_status_id, re_user_id, re_count, score, topicNum,
					rankInResult, score));

			inputLine = bfr.readLine();

			count++;
		}

		bfr.close();

		// creates the results arraylist - one RankedTweetList of all tweets for
		// A topic
		ArrayList<RankedTweetList> results = new ArrayList<RankedTweetList>();
		results.add(new RankedTweetList(tweets));

		return results;
	}

	/**
	 * search() uses Lucene to find relevant tweets Used only for Link Crawling
	 * (which uses Lucene)
	 * 
	 * @param queryList
	 *            the topic queries (of which there are 50)
	 */
	public ArrayList<RankedTweetList> search(ArrayList<LuceneQuery> queryList)
			throws CorruptIndexException, IOException, ParseException {
		String curInd = this.index;

		// will contain the results (the ranking of each url)
		ArrayList<RankedTweetList> results = new ArrayList<RankedTweetList>();

		// Lucene stuff for searching the index
		IndexSearcher searcher = null;
		QueryParser parser = new QueryParser(Version.LUCENE_31, field,
				new SimpleAnalyzer(Version.LUCENE_31));

		// what this does is goes through each query and finds the top 30
		// (hitsReturned)
		// urls for that query and ranks them
		for (int i = 0; i < queryList.size(); i++) {
			// finds the correct query folder
			curInd = this.index + "/MB" + queryList.get(i).getQueryNum();

			try {
				searcher = new IndexSearcher(FSDirectory.open(new File(curInd)));
			} catch (Exception e) {
				System.err
						.println("In APISearch (in search (queList)). Something happened to opening the searcher. ERROR: "
								+ e.getMessage());
				System.err
						.println("Usually because too many open files. Try closing them!");
				System.err.println("i: " + i + " curInd: " + curInd);
				RankedTweetList rtl = new RankedTweetList(null);
				results.add(rtl);
				continue;
			}

			String query = queryList.get(i).getQuery();
			query = query.trim();

			if (query.length() == 0) {
				break;
			}

			Query luceneQuery = parser.parse(query);

			// finds the top 30 urls of that query
			ArrayList<Tweet> tweets = doPagingSearch(i + 1, searcher,
					luceneQuery, hitsReturned);

			RankedTweetList rtl = new RankedTweetList(tweets);
			results.add(rtl);
		}

		searcher.close();

		return results;
	}

	/**
	 * This searches through your lucene index and finds what you're looking
	 * for. In this case, we only now used Lucene for Link Crawling. So the link
	 * crawler originally split each of the links content into different topics
	 * files, this way the Lucene search isn't finding relevant links for tweets
	 * that were found for a different topic. The search is based on the query,
	 * the file and what field you're looking at (usually content).
	 * 
	 * @param topicNum
	 *            topic number
	 * @param searcher
	 *            Lucene searcher object
	 * @param query
	 *            current query
	 * @param hitsReturned
	 *            number of hits we want to return
	 * @return arraylist of tweets which are goind to be ranked
	 */
	public ArrayList<Tweet> doPagingSearch(int topicNum,
			IndexSearcher searcher, Query query, int hitsReturned)
			throws IOException {
		// will hold the new tweets found in index
		ArrayList<Tweet> tempList = new ArrayList<Tweet>();

		// Gets the results for our topic
		TopDocs results = searcher.search(query, hitsReturned);

		// for the hits we get we make a new tweet object
		// out of them and get their score
		ScoreDoc[] hits = results.scoreDocs;
		hits = results.scoreDocs;

		// gathers top 30 urls for this topic
		for (int i = 0; i < hits.length; i++) {
			Document doc = searcher.doc(hits[i].doc);

			tempList.add(new Tweet(doc.get("topicNum"), Long.parseLong(doc
					.get("tweetID")), doc.get("url"), doc.get("title"), doc
					.get("content"), hits[i].score));
		}

		return tempList;
	}

	/**
	 * https://code.google.com/p/language-detection/ If you need help editing
	 * this code, go there for help
	 * 
	 * Basically takes in a line, and detects what language it is
	 * 
	 * Used by API_Query and UrlContentRetrieval.
	 */
	public static String EnglishFilter(String tweet, String lang) {
		com.cybozu.labs.langdetect.Detector detector = null;

		try {
			detector = DetectorFactory.create();
		} catch (LangDetectException e1) {
			System.err
					.println("In APISearch (in EnglishFilter). Something went wrong making the detector. ERROR: "
							+ e1.getMessage());
			System.exit(0);
		}

		detector.append(tweet);

		try {
			lang = detector.detect();
		} catch (Exception e) {
			lang = "nonEnglish";
		}

		return lang;
	}
}