package API;

import java.util.Arrays;

/**
 * Originally: A class that contains a converted query in the Lucene Format,
 * with extra information from the original query.
 * 
 * Version 3.0: A "data structure" class used to hold topic queries (from
 * topics_????.txt)
 * 
 * @author Matthew Kemmer v1.0
 * @author Karl Appel v2.0
 * @edited Lauren Mathews v3.0
 * @version 6/6/11 v1.0
 * @version 6/2012 v2.0
 * @version 6/12/2014 v3.0
 */
public class LuceneQuery implements Comparable<LuceneQuery> {
	private String queryNum;
	private String query;
	private String[] queryTerms;
	private String queryTime;
	private long tweetTime;
	private long boundaryTime;

	/**
	 * Creates a LuceneQuery object
	 * 
	 * @param n
	 *            The query number, within the <num> tag
	 * @param q
	 *            The query, within the <query> tag
	 * @param qt
	 *            The time the query was issued, within the <querytime> tag
	 * @param tt
	 *            The timestamp of the closeset tweet to this query, within the
	 *            <querytweettime> tag
	 * @param bt
	 *            The oldest or newest tweet time that our results can contain,
	 *            given the task.
	 * @param task
	 *            Given TREC 2012 task; adhoc only (set up for now)
	 */
	public LuceneQuery(String n, String q, String qt, Long tt) {
		if (n.startsWith("Number")) {
			n = n.substring(8);
		}

		queryNum = n.trim();
		query = q.trim();
		query = query.trim();
		queryTime = qt.trim();
		tweetTime = tt;
		
		fillQueryTerms();
	}

	public LuceneQuery(String n, String q, String qt, Long tt, Long bt,
			String task) {
		if (n.startsWith("Number")) {
			n = n.substring(8);
		}

		queryNum = n.trim();
		query = q.trim();
		query = query.trim();
		queryTime = qt.trim();
		tweetTime = tt;

		fillQueryTerms();
	}

	public void setQuery(String q) {
		query = q;
		fillQueryTerms();
	}

	/**
	 * Fills an array with each query term
	 */
	private void fillQueryTerms() {
		query = query.trim();
		queryTerms = query.split(" ");

		for (int i = 0; i < queryTerms.length; i++) {
			queryTerms[i] = queryTerms[i].trim();
			queryTerms[i] = queryTerms[i].replaceAll(",", "");
		}
	}

	public String[] getQueryTerms() {
		return queryTerms;
	}

	public String getQuery() {
		return query;
	}

	public Long getTweetTime() {
		return tweetTime;
	}

	public String getQueryNum() {
		return queryNum.substring(2);
	}

	public String getQueryTime() {
		return queryTime;
	}

	public String toString() {
		return "LuceneQuery [queryNum=" + queryNum + ", query=" + query
				+ ", queryTerms=" + Arrays.toString(queryTerms)
				+ ", queryTime=" + queryTime + ", tweetTime=" + tweetTime
				+ ", boundaryTime=" + boundaryTime + "]";
	}

	public int compareTo(LuceneQuery o) {
		return getTweetTime().compareTo(o.getTweetTime());
	}
}