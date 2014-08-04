package Machine_Learning;

/**
 * @author Tim LaRock v1.0
 * @version 7/15/2014 v1.0
 */
public class QTweet {
	int qNum;
	long tweetID;
	int relevanceNum;

	public QTweet(int qNum, long tweetID, int relevanceNum) {
		this.qNum = qNum;
		this.tweetID = tweetID;
		this.relevanceNum = relevanceNum;
	}
	
	public int getqNum() {
		return qNum;
	}

	public void setqNum(int qNum) {
		this.qNum = qNum;
	}

	public long getTweetID() {
		return tweetID;
	}

	public void setTweetID(long tweetID) {
		this.tweetID = tweetID;
	}

	public int getRelevanceNum() {
		return relevanceNum;
	}

	public void setRelevanceNum(int relevanceNum) {
		this.relevanceNum = relevanceNum;
	}
}