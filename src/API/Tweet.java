package API;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import LinkCrawling.URLListGen;

/**
 * Originally: The Tweet class holds information that is related to a tweet; for
 * example, the tweetID, the status, the date, and the relevance score given to
 * the Tweet by Lucene.
 * 
 * Version 3.0: Holds tweet information. In this year: tweetId, screen_name,
 * epoch, tweet_text, followers_count, statuses_count, lang, status_id, user_ud,
 * re_status_id, re_user_id, re_count. Described below
 * 
 * @author Matthew Kemmer and Carl Tompkins v1.0
 * @author David Purcell v1.5
 * @author Karl Appel and Lauren Mathews v2.0
 * @author Lauren Mathews v3.0
 * @version 6/2011 v1.0 & v1.5
 * @version 7/25/12 v2.0
 * @version 7/10/14 v3.0
 */
public class Tweet implements Comparable<Tweet> {
	// API specifications
	String username = null, tweetText = null, lang = null, topicNum, tag = "",
			content = null, title = null, url = null;
	long tweetID, epoch, statusID, userID, re_status_id, re_user_id;
	int followersCount = -1, statusesCount = -1, re_count = -1, rankInResult;
	float score = -1, orgScore;

	boolean special = false;

	/**
	 * Creates the tweet "data structure"; all info that API returns.
	 * 
	 * @param tweetID
	 *            (long): The unique identifier assigned to this document by
	 *            Twitter
	 * @param username
	 *            (String): The Twitter screen name of the Status author
	 * @param epoch
	 *            (long): The unix epoch (in seconds) corresponding to the
	 *            created_at JSON element
	 * @param tweet_text
	 *            (String): The text of the status
	 * @param followers_count
	 *            (int): The number of followers that the author of this status
	 *            has
	 * @param statuses_count
	 *            (int): The number of statuses that the author of this status
	 *            had at the time this status was created
	 * @param lang
	 *            (String): The two-character language of the status (not the
	 *            user) as described by the Twitter language id system
	 * @param status_id
	 *            (long): The unique identifier of the status that this document
	 *            replies to
	 * @param user_id
	 *            (long): The unique identifier of the user who posted the
	 *            status that this document replies to
	 * @param re_status_id
	 *            (long): The unique identifier of the tweet that this is a
	 *            retweet of.
	 * @param re_user_id
	 *            (long): The user ID of person who posted the tweet that this
	 *            is a retweet of.
	 * @param re_count
	 *            (int): Number of times this status has been retweeted.
	 *            Non-retweeted documents show 0
	 * @param score
	 *            The overall score given to the tweet.
	 * @param topicNum
	 *            The topic number that the tweet belongs to.
	 * @param rankInResult
	 *            The # given, starting at 1. Goes from most relevant to least
	 *            relevant.
	 */
	public Tweet(long tweetID, String username, long epoch, String tweet_text,
			int followers_count, int statuses_count, String lang,
			long status_id, long user_id, long re_status_id, long re_user_id,
			int re_count, float score, String topicNum, int rankInResult,
			float org_Score) {
		this.tweetID = tweetID;
		this.username = username;
		this.epoch = epoch;
		this.tweetText = tweet_text;
		this.followersCount = followers_count;
		this.statusesCount = statuses_count;
		this.lang = lang;
		this.statusID = status_id;
		this.userID = user_id;
		this.re_status_id = re_status_id;
		this.re_user_id = re_user_id;
		this.re_count = re_count;
		this.score = score;
		this.topicNum = topicNum;
		this.rankInResult = rankInResult;
		this.special = false;
		this.orgScore = org_Score;
	}

	/**
	 * Used for storing the rankedUrlList (Link Crawling)
	 */
	public Tweet(String TOPIC, long tweet_ID, String URL, String TITLE,
			String CONTENT, float SCORE) {
		this.topicNum = TOPIC;
		this.special = true;
		this.tweetID = tweet_ID;
		this.url = URL;
		this.title = TITLE;
		this.content = CONTENT;
		this.score = SCORE;
	}

	/**
	 * Compares two Tweet objects, based on their score field
	 * 
	 * @param o
	 *            A Tweet object
	 * 
	 * @return 1 if o has a greater score, -1 if o has a lower score, 0 if equal
	 */
	public int compareTo(Tweet o) {
		return Float.compare(o.score, this.score);
	}

	/**
	 * Formats this Tweet as to TREC's liking
	 * 
	 * @return A single line in the TREC format.
	 */
	public String format(String task, boolean raw) {
		String format = "";

		if (!raw && task.equals("adhoc")) {
			format = topicNum + "," + tweetID + ",\"" + tweetText + "\","
					+ username + "," + followersCount + "," + statusesCount
					+ "," + statusID + "," + userID + "," + re_status_id + ","
					+ re_user_id + "," + re_count + "," + score + ","
					+ rankInResult + ",";
			
			Pattern pattern = Pattern.compile(URLListGen.URL_REGEX);
			Matcher matcher = pattern.matcher(tweetText);

			while (matcher.find()) {
				format += "\"=HYPERLINK(\"\"" + matcher.group()
						+ "\"\",\"\"LINK\"\")\"";
			}
		} else if (task.equals("TTG")) {

			format = topicNum + ",Q0," + tweetID + "," + rankInResult + ","
					+ score + "," + tag;

		} else if (task.equals("adhoc")) {
			format = topicNum + "," + getOrgScore() + "," + tweetID + ","
					+ rankInResult + "," + score + "," + tag;
		}

		return format;
	}

	public long getTweetID() {
		return tweetID;
	}

	public void setTweetID(long tweetID) {
		this.tweetID = tweetID;
	}

	public String getStatus() {
		return tweetText;
	}

	public void setStatus(String status) {
		this.tweetText = status;
	}

	public String getUsername() {
		return username;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getTag() {
		return tag;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setRank(int r) {
		rankInResult = r;
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public String toString() {
		String tweetString = "[";
		if (this.special) {
			tweetString += "Url's Tweet ID: " + tweetID + ", ";
			tweetString += "URL: " + url + ", ";
			tweetString += "Title: " + title + ", ";
			tweetString += "Content: " + content + ", ";
		} else if (!this.special) {
			tweetString += "Tweet ID: " + tweetID + ", ";
			tweetString += "Tweet Status: " + tweetText + ", ";
			tweetString += "Tweet Username: " + username + ", ";
		}
		return tweetString;
	}

	public int getTopic() {
		return Integer.parseInt(topicNum.substring(2));
	}

	public void setTopic(int newNumber) {
		if (newNumber < 10) {
			topicNum = "MB00" + newNumber;
		} else if (newNumber < 100) {
			this.topicNum = "MB0" + newNumber;
		} else if (newNumber < 1000) {
			this.topicNum = "MB" + newNumber;
		}
	}

	public long getStatusID() {
		return statusID;
	}

	public void setStatusID(long statusID) {
		this.statusID = statusID;
	}

	public long getUserID() {
		return userID;
	}

	public void setUserID(long userID) {
		this.userID = userID;
	}

	public long getRe_status_id() {
		return re_status_id;
	}

	public void setRe_status_id(long re_status_id) {
		this.re_status_id = re_status_id;
	}

	public long getRe_user_id() {
		return re_user_id;
	}

	public void setRe_user_id(long re_user_id) {
		this.re_user_id = re_user_id;
	}

	public int getFollowersCount() {
		return followersCount;
	}

	public void setFollowersCount(int followersCount) {
		this.followersCount = followersCount;
	}

	public int getStatusesCount() {
		return statusesCount;
	}

	public void setStatusesCount(int statusesCount) {
		this.statusesCount = statusesCount;
	}

	public int getRe_count() {
		return re_count;
	}

	public void setRe_count(int re_count) {
		this.re_count = re_count;
	}

	public String getTopicNum() {
		return topicNum;
	}

	public void setTopicNum(String topicNum) {
		this.topicNum = topicNum;
	}

	public int getRankInResult() {
		return rankInResult;
	}

	public void setRankInResult(int rankInResult) {
		this.rankInResult = rankInResult;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public long getEpoch() {
		return epoch;
	}

	public void setEpoch(long epoch) {
		this.epoch = epoch;
	}

	public float getOrgScore() {
		return orgScore;
	}

	public void setOrgScore(float orgScore) {
		this.orgScore = orgScore;
	}
}