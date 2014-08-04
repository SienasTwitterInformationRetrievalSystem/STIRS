package Miscellaneous;

import java.util.Comparator;

import API.Tweet;

public class TweetRankComparator implements Comparator<Tweet> {

    public int compare(Tweet tweetA, Tweet tweetB){
         float score1 = tweetA.getRankInResult();
         float score2 = tweetB.getRankInResult();
         
         if(score1 > score2){
        	 return 1;
        	 
         }else if(score1 < score2){ 
        	 return -1; 
         }else{
        	 return 0;
         }
    }
}