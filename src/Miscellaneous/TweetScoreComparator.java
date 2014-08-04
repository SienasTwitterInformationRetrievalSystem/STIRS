package Miscellaneous;

import java.util.Comparator;

import API.Tweet;

public class TweetScoreComparator implements Comparator<Tweet> {

    public int compare(Tweet tweetA, Tweet tweetB){
         float score1 = tweetA.getScore();
         float score2 = tweetB.getScore();
         
         if(score1 > score2){
        	 return 1;
        	 
         }else if(score1 < score2){ 
        	 return -1; 
         }else{
        	 return 0;
         }
    }
}