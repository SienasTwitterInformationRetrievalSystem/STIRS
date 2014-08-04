package Relevance;

import java.util.ArrayList;

import Query_ExpansionExternal_Google.Link;

/**
 * Used to hold link information for each of the topics.
 * 
 * @author Lauren Mathews
 * @versio 7/2/2014
 */
public class GoogleInfo {
	
	String topicNum = null, queryTime = null;
	ArrayList<Link> links = null;

	public GoogleInfo(String topic_Num, String query_Time, ArrayList<Link> topic_Links){
		this.topicNum = topic_Num;
		this.queryTime = query_Time;
		this.links = topic_Links;
	}

	public String getTopicNum() {
		return topicNum;
	}

	public void setTopicNum(String topicNum) {
		this.topicNum = topicNum;
	}

	public String getQueryTime() {
		return queryTime;
	}

	public void setQueryTime(String queryTime) {
		this.queryTime = queryTime;
	}

	public ArrayList<Link> getLinks() {
		return links;
	}

	public void setLinks(ArrayList<Link> links) {
		this.links = links;
	}
}