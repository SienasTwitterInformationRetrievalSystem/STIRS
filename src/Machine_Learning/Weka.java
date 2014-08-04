package Machine_Learning;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import weka.classifiers.Classifier;
import weka.core.converters.ArffLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.Instances;

import API.Tweet;

/**
 * Uses a Weka generated decision tree to predict the relevance of a given set
 * of tweets
 * 
 * @author Matthew Kemmer v1.0
 * @author Lauren Mathews v2.0
 * @version 7/14/2011 v1.0
 * @version 7/17/2014 v2.0
 */
public class Weka {
	private Classifier cls;
	private boolean loadedCorrectly;
	private String arffHeading;
	private String modelName;
	private String arffName;

	/**
	 * Creates the constructor for the Weka class. By default when this
	 * constructor is created it will set the arff heading which it will take
	 * from the file. In addition the model will be created
	 */
	public Weka(String harddrive, String model, String csv) {
		this.modelName = harddrive + "/src/Machine_Learning/" + model + ".model";
		this.arffName = harddrive + "/src/Machine_Learning/" + csv + ".arff";
		
		if (!setArffHeading(harddrive, csv)) {
			System.err.println("In Weka.java (in Weka()). Arff file not loaded correctly!");
		}

		if (!loadModel()) {
			System.err.println("In Weka.java (in Weka()). Model file not loaded correctly!");
		}

		if (arffHeading != null && cls != null) {
			loadedCorrectly = true;
		} else {
			loadedCorrectly = false;
		}
	}

	/**
	 * Determines whether the model and arff file loaded correctly
	 * 
	 * @return boolean whether model and arff loaded correctly
	 */
	public boolean loadedCorrectly() {
		return loadedCorrectly;
	}

	/**
	 * Loads the model
	 * 
	 * @return whether the model loaded correctly
	 */
	private boolean loadModel() {
		try {
			cls = (Classifier) weka.core.SerializationHelper.read(modelName);
			
			return true;
		} catch (Exception e) {
			cls = null;
			
			System.err.println("ERROR: " + e.getMessage());
			
			return false;
		}
	}
	
	/**
	 * Runs the test using the pre-generated model on the .arff file generated
	 * from the Tweets
	 * 
	 * Runs the model on the given set of Tweets. If the predicted relevance of
	 * any of the Tweets is YES, then the relevance on the corresponding objects
	 * will be set to true.
	 * 
	 * @return true if the method runs correctly, false otherwise
	 */
	public ArrayList<Tweet> run(ArrayList<Tweet> tweets) {
		try {
			// Gets the test set Instances(Each tweet) from the arff file
			Instances testSet = new DataSource(arffName).getDataSet();

			// gets the attributes for each instance(except the last, relevance,
			// which is what we are trying to find)
			testSet.setClassIndex(testSet.numAttributes() - 1);
			
			ArrayList<Double> allScores = new ArrayList<Double>();
			
			for (int i = 0; i < testSet.numInstances(); i++) {
				double[] scores = cls.distributionForInstance(testSet
						.instance(i));
				
				for(int k = 0; k < scores.length; k++){
					if(!allScores.contains(scores[k])){
						allScores.add(scores[k]);
					}
				}
			}
			
			double amount = 0;
			
			for(int i = 0; i < allScores.size(); i++){
				amount += allScores.get(i);
			}
			
			double THRESHOLD = amount/allScores.size();
			
			// for each of the instances determine the probability of it being
			// relevant
			for (int i = 0; i < testSet.numInstances(); i++) {
				// gets the probability of a tweet being relevant based on the
				// model
				double[] scores = cls.distributionForInstance(testSet
						.instance(i));
				
				String score = null;
				if(scores.length == 1){
					score = String.valueOf(scores[0]);
				}else{
					score = String.valueOf(scores[scores.length-1]);
				}
				
				float newScore = Float.valueOf(score);
				
				if(newScore == 1.0){
					tweets.get(i).setScore(tweets.get(i).getScore() + newScore);
				}else if(newScore == 0.0){
					if((tweets.get(i).getScore() - (float) 1.0) > 0){
						tweets.get(i).setScore(tweets.get(i).getScore() - (float) 1.0);
					}else{
						tweets.get(i).setScore((float) 0.0);
					}
				}else if(newScore > THRESHOLD){
					tweets.get(i).setScore(tweets.get(i).getScore() * newScore);
					//tweets.get(i).setScore(tweets.get(i).getScore() + newScore);
				}else if(newScore < THRESHOLD){
					if((tweets.get(i).getScore() - newScore) > 0){
						tweets.get(i).setScore(tweets.get(i).getScore() - newScore);
					}else{
						tweets.get(i).setScore((float) 0.0);
					}
				}
				
				//tweets.get(i).setScore(tweets.get(i).getScore() * newScore);
			}
			
			return tweets;
		} catch (Exception e) {
			System.out.println("In Weka.java (in run). Something occurred when running WEKA! ERROR: " + e.getMessage());

			System.exit(0);
			return null;
		}
	}

	/**
	 * Sets the arff heading and returns true if done successfully
	 * 
	 * @return boolean the success of the arff heading
	 */
	private boolean setArffHeading(String harddrive, String csv) {
		try {
			ArffLoader a = new ArffLoader();
			
			// gets the arff loader initialized
			a.setFile(new File(arffName));
			
			Instances i = a.getDataSet();
			String s = i.toString();

			// gets the attribute headings
			int index = s.indexOf("@data");
			arffHeading = s.substring(0, index + 6);

			return true;
		} catch (IOException e) {
			System.out.println("In Weka.java (in setArffHeading). Exception in loading ArffLoader. ERROR: " + e.getMessage());
			arffHeading = null;

			return false;
		}
	}
}