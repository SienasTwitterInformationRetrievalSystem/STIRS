package Machine_Learning;

/**
 * Stores important information about individual tweets for WEKA
 * 
 * @author Matthew Kemmer v.1.0
 * @author Lauren Mathews v.2.0
 * @author Timothy LaRock v3.0
 * @version 6/2011 v1.0
 * @version 6/25/12 v2.0
 * @version 7/15/2014 v3.0
 */
public class WekaTweet {
	private boolean url;
	private boolean relevant;
	@SuppressWarnings("unused")
	private long tweetID;

	private String hashtag;
	private String mention;
	private int followers;
	private boolean maxRetweeted;
	private double percentMatch;
	private double tweetsToFollowers;
	private int retweets;
	//private int nouns;
	//private int verbs;
	private boolean relevantURL;

	/**
	 * Constructor for TREC2014
	 * 
	 * @param retweeted
	 *            True if the tweet has been retweeted
	 * @param url
	 *            true if there is a url present
	 * @param hashtag
	 *            true if there is a hashtag
	 * @param mention
	 *            true if there is a mention
	 * @param numbers
	 * @param tweetID
	 */
	public WekaTweet(boolean retweeted, int retweets, boolean url,
			String hashtag, String mention, long tweetID,
			double percentMatch, double tweetsToFollowers, int followers,
			/*int nouns, int verbs, */boolean relevantURL) {
		this.tweetID = tweetID;
		this.url = url;
		this.hashtag = hashtag;
		this.mention = mention;
		this.maxRetweeted = retweeted;
		this.percentMatch = percentMatch;
		this.tweetsToFollowers = tweetsToFollowers;
		this.followers = followers;
		this.retweets = retweets;
		//this.nouns = nouns;
		//this.verbs = verbs;
		this.relevantURL = relevantURL;
	}

	/**
	 * Sets the relevance of this Tweet
	 * 
	 * @param r
	 *            True if this Tweet is relevant, false otherwise
	 */
	public void setRelevance(boolean rel) {
		this.relevant = rel;
	}

	/**
	 * Creates the heading for the .csv file
	 * 
	 * @return string containing the heading for the .csv file
	 */
	public String newAttributes() {
		return "url" + "," + "URL Relevance" + "," + "NumberOfRetweets" + ","
				+ "Hashtag" + "," + "Mention" + "," + "Max Retweets" + ","
				+ "PercentMatch" + "," + "Tweets:Followers" + "," + "Followers"
				+ "," + /*"Nouns" + "," + "Verbs" + "," + */"Relevant";
	}

	/**
	 * Returns the attributes and their value
	 * 
	 * @return attributes - a string containing all of the values for a
	 *         WekaTweet
	 */
	public String toString() {
		return this.url + "," + this.relevantURL + "," + this.retweets + ","
				+ this.hashtag + "," + this.mention + "," + this.maxRetweeted
				+ "," + this.percentMatch + "," + this.tweetsToFollowers + ","
				+ this.followers + "," + /*this.nouns + "," + this.verbs + ","
				+ */this.relevant;
	}
}