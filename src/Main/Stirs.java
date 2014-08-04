package Main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.logging.*;
import java.util.StringTokenizer;

import nistEvaluation.PrecisionScore;

import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

import Machine_Learning.TM2;
import TTG.TM3;
import Main.Results;
import API.APISearch;
import API.API_Query;
import API.RankedTweetList;
import API.LuceneQuery;
import API.Tweet;
import Query_ExpansionExternal_Google.Downloader;
import Query_ExpansionExternal_Wordnet.QueryExpansionExternalb;
import Main.Module;
import Main.QueryExpansion;
import Miscellaneous.POSTagger;
import Miscellaneous.TweetScoreComparator;
import LinkCrawling.Links;
import LinkCrawling.RankedJoin;
import Miscellaneous.Normalizer;
import Query_ExpansionInternal.HashTags;
import Query_ExpansionInternal.QueryExpansionInternal;
import Relevance.Google;
import Relevance.rescoreTweets;
import API.QueryProcessor;

/**
 * Stirs is the central class in the STIRS system. It is used to call all other
 * classes in the STIRS system. NOTE: The term Query and Topic are used
 * interchangeably through the code
 * 
 * @author Carl Tompkins v1.0
 * @author Karl Appel and Lauren Mathews v2.0
 * @author Lauren Mathews v3.0
 * @version 6/2011 v1.0
 * @version 6/7/2012 v2.0
 * @version 7/28/2014 v3.0
 */
public class Stirs {
	private final static Logger LOGGER = Logger
			.getLogger(Stirs.class.getName());
	private static FileHandler fileTxt = null;
	private static XMLFormatter formatterXML;
	private static String tag = "default";
	private static boolean scoreComparator = false;
	private static int port = 9090;
	private static int numHits = 10000;
	private static String harddrive = "";

	/**
	 * The main method. Program execution begins here.
	 * 
	 * @param args Program arguments, which include:
	 * 		-harddrive -> required; full file path for all other files to be built
	 * 					  Ex.: C:\\Users\\Lauren\\workspace\\TREC2014
	 * 		-q -> Specifies what query file to use (File type filter textpath) (org_test_topics.txt)
	 * 					Ex.: \\src\\API\\org_test_topics.txt
	 * 		-hits -> number of hits you want to return (10000 max, 1000 min)
	 * 		-tag -> the name of the run , could be anything
	 * 		-scorecomparator -> sets up results to sort by score/rank (or not)
	 * 		-tm -> sets up modules (Query Expansion, Relevance Modules)
	 * 			   1 = Link Crawling, 2 = Machine Learning, 3a = Google, 3b = WordNet, 
	 * 			   3c = CommonWords, 3d = Links, 3e = Hashtags (QE), 
	 * 			   4a = rescoreTweets, 4b = Tweet<->Topic Google (Relevance)
	 * 		-external -> use of external in 1) internal query expansion
	 *										2) cleaning up after all query expansion
	 * 		-api -> Force the API to be updated (usually only updates if files can't be found)
	 * 		-updateContent -> Google Query Expansion, whether to update the Google content (Topics)
	 * 		-querySplitting -> Whether to enable querySplitting through the API
	 * 		-port -> gathers Twitter Collection from API (9090 for 2011, 9091 for 2013)
	 * 		-manual -> whether we want to do a manual run (affects all Query Expansion)
	 * 		-createTrainingSet -> Whether to create the training set (Machine Learning) (Tweets)
	 * 		-hashtag -> turn this on if running Hashtag Query Expansion
	 * 		-TTG -> run the Tweet Timeline Generation Task (after adhoc task run finishes)
	 * 		-useTTG -> whether to use the TTG results in the adhoc run
	 * 		-judges -> what NISTJudgments.txt file to use (depending on track year)
	 */
	public static void main(String[] args) {
		@SuppressWarnings("resource")
		Scanner reader = new Scanner(System.in);

		// Twitter modules we wish to use if any
		ArrayList<String> tms = null;

		// will contain each topic's tweets with that tweet's information
		// (including score)
		ArrayList<ArrayList<RankedTweetList>> allTopicsTweets = new ArrayList<ArrayList<RankedTweetList>>();

		// will contain the query topics from file
		ArrayList<LuceneQuery> queries = null;

		// will contain the query topic file
		File queryFile = null;
		String filepath = "";

		//arguments boolean tags
		boolean external = false;
		boolean forceapi = false;
		boolean updateContent = false;
		boolean querySplitting = false;
		boolean manual = false;
		boolean createTrainingSet = false;
		boolean hashtag = false;
		boolean TTG = false;
		boolean useTTG = false;
		boolean precision = false;

		// will contain the harddrive
		String judges = "/NISTJudgment####.txt";

		// the usage of our system from the command prompt or a terminal window
		final String USAGE = "USAGE: java Stirs [-harddrive] [-q]"
				+ " [-hits] [-external] [-tm 1,2,3a,3b,3c,3d,3e,4a,4b] [-scorecomparator]"
				+ " [-api] [-updateContent] [-querySplitting] [-port] [-manual]"
				+ " [-createTrainingSet] [-raw] [-hashtag] [-TTG] [-useTTG] [-judges] [-precision]";
		final String FILEPATHS = "FILEPATHS: Many java files in STIRS have file paths that need"
				+ " to be changed.\nPlease update, if needed, the following file paths in files: \nStirs, "
				+ "URLContentRetrieval, Downloader, APISearch, TwitterIndexUrls, "
				+ "LucQeWord, QueryExpansionInternal, \nQueryExpansionExternalb, "
				+ "RankedJoin, Google, PrecisionScore, Links, TM2, ProcessAttributes and Weka.";

		// Set up the FileHandler for the log file
		try {
			fileTxt = new FileHandler("Logging.xml");
			formatterXML = new XMLFormatter();
			fileTxt.setFormatter(formatterXML);
			LOGGER.addHandler(fileTxt);
		} catch (IOException e) {
			System.err
					.println("In Stirs.java. Was unable to create logger file.");
			LOGGER.warning("Logging file could not be created.");
		}

		// Initially set so that all output goes to the log file
		LOGGER.setUseParentHandlers(false);

		// Sets the level of all Handlers and the logger to fine
		Handler[] handlers = Logger.getLogger("").getHandlers();
		for (int i = 0; i < handlers.length; i++) {
			handlers[i].setLevel(Level.FINE);
		}
		LOGGER.setLevel(Level.FINE);

		// If there aren't any arguments, end program execution
		if (args.length == 0) {
			LOGGER.severe("FATAL - No parameters specified. Program exit.");
			System.err.println("You must specify a query file path.");
			System.err.println(USAGE);
			System.err.println(FILEPATHS);
			System.exit(-1);
		}

		// Log when STIRS begins
		LOGGER.info("STIRS started.");
		System.out.println("STIRS has started.");
		System.err.println(FILEPATHS);
		System.out.println("STIRS arguments are being initialized...");

		// Set the command line arguments
		for (int i = 0; i < args.length; i++) {
			// output by score or by recent time
			if ("-scorecomparator".equals((args[i]).toLowerCase())) {
				scoreComparator = true;
			}

			// name of run , otherwise if not specified it will be default
			if ("-tag".equals(args[i])) {
				if (i + 1 < args.length) {
					tag = args[i + 1];
					System.err
							.println("Make sure to change the tag for each run! Tag: "
									+ tag);
				} else {
					LOGGER.setUseParentHandlers(true);
					LOGGER.severe("FATAL: '-tag' option not specified. Program exit.");
					System.err.println("You must specify an option.");
					System.err.println(USAGE);
					System.err.println(FILEPATHS);
					System.exit(0);
				}
			}

			// Command line parameter to specify the location of the harddrive
			// This parameter is required to run the program
			if ("-harddrive".equals(args[i])) {
				if (i + 1 < args.length) {
					harddrive = args[i + 1];
				} else {
					LOGGER.setUseParentHandlers(true);
					LOGGER.severe("FATAL: '-harddrive' option not specified. Program exit.");
					System.err.println("You must specify an option.");
					System.err.println(USAGE);
					System.err.println(FILEPATHS);
					System.exit(0);
				}
			}

			//determines how many tweets to make up output.csv from index
			else if ("-hits".equals(args[i])) {
				if (i + 1 < args.length) {
					try {
						int temp = Integer.parseInt(args[i + 1].trim());

						if (temp <= 1000) {
							System.err
									.println("-hits really should be more than 1000 (try 3000 at least)");
							numHits = 3000;
						} else {
							numHits = Integer.parseInt(args[i + 1].trim());
						}
					} catch (NumberFormatException e) {
						LOGGER.severe("FATAL - Number of hits entered is not a number. Java reported: "
								+ e.getMessage() + ". " + "Program exit.");
						System.exit(0);
					}
				} else {
					LOGGER.setUseParentHandlers(true);
					LOGGER.severe("FATAL: '-hits' option not specified. Program exit.");
					System.err.println("You must specify an option.");
					System.err.println(USAGE);
					System.exit(-1);
				}
			}

			// Command line parameter to specify the location of the query file.
			// This parameter is required to run the program.
			else if ("-q".equals(args[i])) {
				if (i + 1 < args.length) {
					filepath = harddrive + args[i + 1];
					try {
						queryFile = new File(filepath);
					} catch (NullPointerException e) {
						LOGGER.severe("FATAL: No file path specified for option '-q'. Program exit.");
						System.err.println("File path is null.");
						System.err.println("You must specify an option.");
						System.err.println(FILEPATHS);
						System.exit(-1);
					}

					if ("".equals(filepath)) {
						LOGGER.warning("File path is empty. Continuing execution however.");
						System.err
								.println("WARNING: File path is empty for option '-q'.");
					}
				} else {
					LOGGER.setUseParentHandlers(true);
					LOGGER.severe("FATAL: '-q' option not specified. Program exit.");
					System.err.println("You must specify an option.");
					System.err.println(USAGE);
					System.exit(-1);
				}
			}

			//the path for the judgments file
			else if ("-judges".equals(args[i])) {
				if (i + 1 < args.length) {
					judges = harddrive + args[i + 1];

					try {
						new File(judges);
						precision = true;
					} catch (NullPointerException e) {
						LOGGER.severe("FATAL: No file path specified for option '-judges'. Program exit.");
						System.err.println("File path is null.");
						System.err.println("You must specify an option.");
						System.err.println(FILEPATHS);
						System.exit(-1);
					}
				} else {
					LOGGER.setUseParentHandlers(true);
					LOGGER.severe("FATAL: '-judges' option not specified. Program exit.");
					System.err.println("You must specify an option.");
					System.err.println(USAGE);
					System.exit(-1);
				}
			}

			// Checks if TMs are enabled. Any number of TMs can be enabled at
			// the same time. Comma-separated list.
			else if ("-tm".equals(args[i])) {
				if (i + 1 < args.length) {
					StringTokenizer strtok = new StringTokenizer(args[i + 1],
							",");
					tms = new ArrayList<String>();
					while (strtok.hasMoreTokens()) {
						String tmString = strtok.nextToken().trim();
						try {
							tms.add(tmString.trim());
						} catch (NumberFormatException e) {
							LOGGER.setUseParentHandlers(true);
							LOGGER.severe("FATAL - TM argument was not a "
									+ "number. Java reported: "
									+ e.getMessage() + " Program exit.");
							System.exit(-1);
						}
					}
				} else {
					LOGGER.setUseParentHandlers(true);
					LOGGER.severe("FATAL: '-tm' option not specified. Program exit.");
					System.err.println("You must specify an option.");
					System.err.println(USAGE);
					System.exit(-1);
				}
			}

			else if ("-external".equalsIgnoreCase(args[i])) {
				external = true;
			}

			else if ("-api".equalsIgnoreCase(args[i])) {
				System.out
						.println("(Y/N) Are you sure you want to update the API? This means that all index (Link Crawling, Links) will have to be re-made.");
				String ans = reader.nextLine();
				if (ans.equalsIgnoreCase("Y")) {
					forceapi = true;
				}
			}

			else if ("-updateContent".equalsIgnoreCase(args[i])) {
				updateContent = true;
			}

			else if ("-querySplitting".equalsIgnoreCase(args[i])) {
				querySplitting = true;
			}

			else if ("-port".equalsIgnoreCase(args[i])) {
				if (i + 1 < args.length) {
					port = Integer.parseInt(args[i + 1]);
				} else {
					LOGGER.setUseParentHandlers(true);
					LOGGER.severe("FATAL: '-port' option not specified. Program exit.");
					System.err.println("You must specify an option.");
					System.err.println(USAGE);
					System.exit(-1);
				}
			}

			else if ("-manual".equalsIgnoreCase(args[i])) {
				manual = true;
			}

			else if ("-createTrainingSet".equalsIgnoreCase(args[i])) {
				createTrainingSet = true;
			}

			else if ("-hashtag".equalsIgnoreCase(args[i])) {
				hashtag = true;
			}

			else if ("-TTG".equalsIgnoreCase(args[i])) {
				TTG = true;
				System.out.println("Created TTG (1).");
			}

			else if ("-useTTG".equalsIgnoreCase(args[i])) {
				useTTG = true;
				TTG = true;
				System.out.println("Created TTG (2).");
			}
		}

		System.out.println("Finished first set of arguments.");

		// If a query file was not specified, output the message and end
		// program execution.
		if (queryFile == null) {
			LOGGER.setUseParentHandlers(true);
			LOGGER.severe("(queryFile) FATAL - No file path given. Can't find file. Program exit.");
			System.err
					.println("You did not specify a query file. This is a required argument.");
			System.err.println(USAGE);
			System.exit(-1);
		}

		// If a file was specified, make sure it is a file.
		else if (!queryFile.isFile()) {
			LOGGER.setUseParentHandlers(true);
			LOGGER.severe("(queryFile) FATAL - Path given does not lead to a file. Program exit.");
			System.err
					.println("The path that you specified does not lead to a file. Please recheck the path and try again.");
			System.err.println(USAGE);
			System.exit(-1);
		}

		// Make sure that file can be read
		else if (!queryFile.canRead()) {
			LOGGER.setUseParentHandlers(true);
			LOGGER.severe("FATAL - File cannot be read. Recheck permissions. Program exit.");
			System.err
					.println("The file that you specified cannot be read. Please recheck file permissions and try again.");
			System.exit(-1);
		}

		LOGGER.info("Query file was checked; verified as a file and determined to be readable.");

		System.out.println("Command line options and file checks completed.");
		LOGGER.info("Started QueryProcessor.");

		System.out.println("QueryProcessor initializing, first time...");

		// if the task is not null we need to process the information out of
		// them properly. This is slightly different for each task
		queries = makeQueriesList(filepath);

		System.out.println("QueryProcessor has successfully finished.");

		// these lines check to see if the API needs to be re-run
		boolean createapi = true;
		File testPath = null;
		String input;

		for (int i = 0; i < queries.size(); i++) {
			input = harddrive + "/src/API/index/MB"
					+ queries.get(i).getQueryNum() + ".txt";

			testPath = new File(input);

			if (testPath != null && i == (queries.size() - 1)) {
				createapi = false;
			}
		}

		//sets up for detecting non-english phrases
		String profileDirectory = harddrive + "/src/API/profiles";

		try {
			DetectorFactory.loadProfile(profileDirectory);
		} catch (LangDetectException e) {
			System.err
					.println("In Stirs.java. Something went wrong making the profile directory. ERROR: "
							+ e.getMessage());
			System.err.println("Profiles' Index: " + profileDirectory);
			System.exit(0);
		}

		//checks whether the API will be run
		if (createapi || forceapi) {
			System.out.println("Creating API, first time...");

			allTopicsTweets = createAPI(queries, allTopicsTweets,
					true, querySplitting, forceapi);

			System.out.println("Finished creating API.");
		} else {
			System.out.println("Creating allTopicsTweets...");

			allTopicsTweets = createTweets(queries, allTopicsTweets,
					true, querySplitting, forceapi);

			System.out.println("Finished creating allTopicsTweets.");
		}

		System.out.println("Creating modules...");

		ArrayList<Module> modules = new ArrayList<Module>();
		ArrayList<QueryExpansion> queExp = new ArrayList<QueryExpansion>();
		// if we have modules to run
		int queryExpModulesRun = 0;
		if (tms != null) {
			for (int i = 0; i < tms.size(); i++) {
				if (tms.get(i).equals("1")) { // Link Crawling
					modules.add(new RankedJoin(harddrive, querySplitting));
					System.out.println("Created Link Crawling (RankedJoin).");
				} else if (tms.get(i).equals("2")) { // Machine Learning
					modules.add(new TM2(harddrive, createTrainingSet, judges));
					System.out.println("Created Machine Learning.");
				} else if (tms.get(i).equals("3a")) { // Google (Query
														// Expansion, External)
					queExp.add(new Downloader(harddrive,
							"libraries/en-pos-maxent.bin", "englishStop.txt",
							updateContent,
							"/src/Query_ExpansionExternal_Google/Content"));
					System.out.println("Created Google (Query Expansion).");
					queryExpModulesRun++;
				} else if (tms.get(i).equals("3b")) { // WordNet (Query
														// Expansion, External)
					queExp.add(new QueryExpansionExternalb(
							LOGGER,
							"/home/lmathews/workspace/TREC2014/src/Query_ExpansionExternal_Wordnet/WordNet/2.1",
							"englishStop.txt", harddrive));
					System.out.println("Created Wordnet.");
					queryExpModulesRun++;
				} else if (tms.get(i).equals("3c")) { // CommonWords (Query
														// Expansion, Internal)
					queExp.add(new QueryExpansionInternal("output.csv",
							"englishStop.txt", external,
							"libraries/en-pos-maxent.bin", harddrive,
							"slangDict.txt", hashtag, allTopicsTweets));
					System.out.println("Created CommonWords.");
					System.out.println("Hashtag (on/off): " + hashtag);
					queryExpModulesRun++;
				} else if (tms.get(i).equals("3d")) { // Links (Query Expansion,
														// External)
					queExp.add(new Links(harddrive, "englishStop.txt",
							querySplitting, allTopicsTweets));
					System.out.println("Created Links.");
					queryExpModulesRun++;
				} else if (tms.get(i).equals("3e")) { // Hashtag (Query
														// Expansion, Internal)
					queExp.add(new HashTags(harddrive, "output.csv"));
					System.out.println("Created Hashtags.");
					hashtag = true;
					queryExpModulesRun++;
				} else if (tms.get(i).equals("4a")) { // rescoreTweets
														// (Relevance)
					modules.add(new rescoreTweets());
					System.out.println("Created rescoreTweets.");
				} else if (tms.get(i).equals("4b")) { // Google (Relevance)
					modules.add(new Google(harddrive, true));
					System.out.println("Created Google (Relevance).");
				}
			}
		} else if (tms == null) {
			System.err
					.println("No modules have been selected to run. Will be showing 'baseline' run.");
		}

		System.out.println("Finished adding modules.");

		ArrayList<String> expandedQueries = null;
		ArrayList<ArrayList<String>> newQueries = new ArrayList<ArrayList<String>>();
		for (QueryExpansion expand : queExp) {
			BufferedWriter moduleWriter = null;
			try {
				moduleWriter = new BufferedWriter(new FileWriter(harddrive
						+ "/queExpFile" + expand.getClass().getSimpleName()
						+ ".txt"));
			} catch (IOException e) {
				System.err
						.println("In Stirs.java. Something has gone wrong creating the moduleFile. ERROR: "
								+ e.getMessage());
			}

			try {
				System.out.println("Starting to run query expansion module: "
						+ expand.getClass().getSimpleName());

				moduleWriter.write(expand.getClass().getSimpleName());
				moduleWriter.newLine();
				moduleWriter.flush();

				expandedQueries = expand.getNewQueries(queries);
				queries = makeQueriesList(filepath);

				if (expandedQueries.size() != queries.size()) {
					System.err
							.println("(In Stirs.java) During a real run, the number of topics shouldn't be less than "
									+ queries.size() + "!");
					System.err.println("expandedQueries.size(): "
							+ expandedQueries.size());
					
					System.exit(0);
				}

				for (int i = 0; i < expandedQueries.size(); i++) {
					if (expand.getClass().getSimpleName()
							.equalsIgnoreCase("QueryExpansionExternalb")) {
						expandedQueries.set(i,
								expandedQueries.get(i).replace("\"", ""));
					}

					System.out.println("newQuery #" + i + ": "
							+ expandedQueries.get(i));
					moduleWriter.write(String.valueOf(i) + ": "
							+ expandedQueries.get(i));
					moduleWriter.newLine();
					moduleWriter.flush();
				}

				newQueries.add(expandedQueries);

				System.out.println("Finished running query expansion module: "
						+ expand.getClass().getSimpleName());

				moduleWriter.close();

			} catch (Exception e) {
				System.err
						.println("In Stirs.java. Something occurred while running the query expansion modules. ERROR: "
								+ e.getMessage());
				System.exit(0);
			}
		}

		ArrayList<LuceneQuery> queriesAfterExpansion = null;

		if (queryExpModulesRun >= 1) {
			queries = makeQueriesList(filepath);

			if (manual) {
				queriesAfterExpansion = queries;
				queries = makeQueriesList(filepath);
				
				System.out
						.println("Starting manual run on all Query Expansion modules...");
				
				HashSet<String> queExpStopWords = queryExpStopWords();
				
				for (int k = 0; k < queries.size(); k++) {
					String orgQuery = queries.get(k).getQuery();

					System.out.println("orgQuery: " + orgQuery);

					System.out.println("Which queries do you want to keep?");

					String newQueryTerms = "";

					for (String str : orgQuery.split(" ")) {
						System.out.println("(Y/N) Keep: " + str + "?");
						String ans = reader.nextLine();
						if (ans.equalsIgnoreCase("Y")) {
							newQueryTerms += str + " ";
						}
					}
					
					String terms = "";
					
					for(int m = 0; m < newQueries.size(); m++){
						HashSet<String> allWords = new HashSet<String>();
						
						StringTokenizer sT = new StringTokenizer(newQueries.get(m).get(k).trim(), " ");
						
						while(sT.hasMoreTokens()){
							String curTok = sT.nextToken();
							
							if(!(allWords.contains(curTok) || queExpStopWords.contains(curTok))){
								terms += curTok.trim() + " ";
								
								allWords.add(curTok);
							}
						}
						
						terms = terms.trim();
					}
					
					if(terms.length() > 0){
						System.out.println("Topic Number is " + (k + 1)
								+ " and newQueries are: " + terms);

						for (String str : terms.split(" ")) {
							System.out.println("(Y/N) Keep: " + str + "?");
							String ans = reader.nextLine();
							if (ans.equalsIgnoreCase("Y")) {
								newQueryTerms += str + " ";
							}
						}
					}else{
						System.out.println("No new queries found!");
					}
					
					newQueryTerms = newQueryTerms.trim();
					newQueryTerms = newQueryTerms.replace("  ", " ");

					queriesAfterExpansion.get(k).setQuery(newQueryTerms);

					System.out.println("queriesAfterExpansion.get(" + k + "): " + queriesAfterExpansion.get(k).getQuery());
				}
				
				System.out.println("Finished manual run.");
			}else if (queryExpModulesRun > 1) {
				System.out.println("Creating new query list...");

				queriesAfterExpansion = evaluateQueryExp(queries, external,
							queryExpModulesRun, newQueries);

				System.out.println("Finished making list.");
			} else {
				queriesAfterExpansion = queries;
				for (int m = 0; m < newQueries.get(0).size(); m++) {
					queriesAfterExpansion.get(m).setQuery(
								queries.get(m).getQuery() + " "
										+ newQueries.get(0).get(m));
				}
			}

			System.out.println("Creating API, second time...");

			allTopicsTweets = createAPI(queriesAfterExpansion, allTopicsTweets, false, querySplitting, false);

			System.out.println("Finished creating API.");
		}
		
		// for each module we are going to get the results
		int number = 0;

		for (Module module : modules) {
			BufferedWriter moduleWriter = null;
			try {
				moduleWriter = new BufferedWriter(new FileWriter(harddrive
						+ "/moduleFile" + number + " - " + tag + ".txt"));
			} catch (IOException e) {
				System.err
						.println("In Stirs.java. Something has gone wrong creating the moduleFile. ERROR: "
								+ e.getMessage());
			}

			try {
				System.out.println("Starting to run module: "
						+ module.getClass().getSimpleName());

				moduleWriter.write("Before Module: "
						+ module.getClass().getSimpleName());
				moduleWriter.newLine();
				moduleWriter.flush();
				for (int i = 0; i < allTopicsTweets.size(); i++) {
					for (int k = 0; k < allTopicsTweets.get(i).size(); k++) {
						ArrayList<Tweet> rankedTweetList = allTopicsTweets
								.get(i).get(k).getRankedList();
						for (int m = 0; m < rankedTweetList.size(); m++) {
							moduleWriter.write("The tweetID for list " + m
									+ " is: ");
							moduleWriter.write(Long.toString(rankedTweetList
									.get(m).getTweetID()));
							moduleWriter.newLine();
							moduleWriter.flush();
							moduleWriter.write("The rank for list " + m
									+ " is: ");
							moduleWriter.write(String.valueOf(rankedTweetList
									.get(m).getRankInResult()));
							moduleWriter.newLine();
							moduleWriter.flush();
							moduleWriter.write("And the score is: ");
							moduleWriter.write(Float.toString(rankedTweetList
									.get(m).getScore()));
							moduleWriter.newLine();
							moduleWriter.flush();
						}
					}
				}

				if (queryExpModulesRun >= 1 && queriesAfterExpansion != null) {
					allTopicsTweets = module.getResults(queriesAfterExpansion,
							allTopicsTweets);
				} else {
					allTopicsTweets = module.getResults(queries,
							allTopicsTweets);
				}

				for (int i = 0; i < allTopicsTweets.size(); i++) {
					if (allTopicsTweets.get(i) != null) {
						ArrayList<RankedTweetList> tweets = allTopicsTweets
								.get(i);
						allTopicsTweets.set(i, Normalizer.rerank(tweets));
					} else {
						System.err
								.println("(In Stirs.java, fixing ranks.) During a real run, shouldn't be null!");
						System.err.println("i: " + (i + 1));
						System.exit(0);
					}
				}

				moduleWriter.write("After Module: "
						+ module.getClass().getSimpleName());
				moduleWriter.newLine();
				moduleWriter.flush();
				for (int i = 0; i < allTopicsTweets.size(); i++) {
					for (int k = 0; k < allTopicsTweets.get(i).size(); k++) {
						RankedTweetList rtl = allTopicsTweets.get(i).get(k);

						List<Tweet> sublist = rtl.getRankedList().subList(0,
								rtl.size());
						Collections.sort(sublist, Collections
								.reverseOrder(new TweetScoreComparator()));

						if (allTopicsTweets.get(i).get(k) != null) {
							ArrayList<Tweet> rankedTweetList = rtl
									.getRankedList();
							for (int m = 0; m < rankedTweetList.size(); m++) {
								if (rankedTweetList.get(m).getStatus() != null) {
									moduleWriter.write("The tweetID for list "
											+ m + " is: ");
									moduleWriter.write(Long
											.toString(rankedTweetList.get(m)
													.getTweetID()));
									moduleWriter.newLine();
									moduleWriter.flush();
									moduleWriter.write("The rank for list " + m
											+ " is: ");
									moduleWriter.write(String
											.valueOf(rankedTweetList.get(m)
													.getRankInResult()));
									moduleWriter.newLine();
									moduleWriter.flush();
									moduleWriter.write("And the score is: ");
									moduleWriter.write(Float
											.toString(rankedTweetList.get(m)
													.getScore()));
									moduleWriter.newLine();
									moduleWriter.flush();
								} else {
									System.err
											.println("In Stirs.java (making modules). Something was null! (1)");
									System.err.println("i: " + i);
									System.err.println("k: " + k);
									System.err.println("m: " + m);
									System.exit(0);
								}
							}
						} else {
							System.err
									.println("In Stirs.java (making modules). Something was null! (2)");
							System.err.println("i: " + i);
							System.err.println("k: " + k);
							System.exit(0);
						}
					}
				}

				moduleWriter.close();

				System.out.println("Finished running module: "
						+ module.getClass().getSimpleName());

				number++;
			} catch (IOException e) {
				System.err
						.println("In Stirs.java. Something occurred while running the modules. ERROR: "
								+ e.getMessage());
				System.exit(0);
			}
		}

		if (TTG) {
			System.out
					.println("Finished Adhoc Task. Starting Tweet Timeline Generation Task...");

			TM3 tm3 = new TM3(20);

			ArrayList<ArrayList<RankedTweetList>> TTGList = tm3.getResults(
					queries, allTopicsTweets);

			System.out.println("Finished TTG - starting Results...");

			Results.output(TTGList, tag + "TTG", false, queries, false, false,
					1, "TTG");

			System.out
					.println("Finished Results. Finished Tweet Timeline Generation Task.");

			if (useTTG) {
				allTopicsTweets = useTTGResults(allTopicsTweets, TTGList);

				for (int i = 0; i < allTopicsTweets.size(); i++) {
					if (allTopicsTweets.get(i) != null) {
						ArrayList<RankedTweetList> tweets = allTopicsTweets
								.get(i);
						allTopicsTweets.set(i, Normalizer.rerank(tweets));
					} else {
						System.err
								.println("(In Stirs.java, fixing ranks. (2)) During a real run, shouldn't be null!");
						System.err.println("i: " + (i + 1));
						System.exit(0);
					}
				}
			}
		}

		// normalizes the tweets properly from [0 to 1]
		System.out
				.println("Running Normalizer (normalizes tweets properly between 0 and 1)...");

		for (int i = 0; i < allTopicsTweets.size(); i++) {
			if (allTopicsTweets.get(i) != null) {
				ArrayList<RankedTweetList> tweets = allTopicsTweets.get(i);
				allTopicsTweets.set(i, Normalizer.normalize(tweets));
			} else {
				System.err
						.println("(In Stirs.java, Normalizer running.) During a real run, shouldn't be null!");
				System.err.println("i: " + (i + 1));
				System.exit(0);
			}
		}

		System.out.println("Finished Normalizer. Starting Results...");

		// prints tweets appropriately to file
		Results.output(allTopicsTweets, tag, scoreComparator, queries, true,
				false, 0, "adhoc");

		if (precision) {
			System.out.println("Finished Results. Starting Precision Score...");
			new PrecisionScore(harddrive, tag, queries.size(), judges);
			System.out.println("Finished Precision Score. Finished STIRS.");
		} else {
			System.out.println("Finished Results. Finished STIRS.");
		}
	}

	private static HashSet<String> queryExpStopWords() {
		HashSet<String> queExp = new HashSet<String>();
		
		queExp.add("2011");
		queExp.add("2012");
		queExp.add("2013");
		queExp.add("2014");
		queExp.add("january");
		queExp.add("february");
		queExp.add("march");
		queExp.add("april");
		queExp.add("may");
		queExp.add("june");
		queExp.add("july");
		queExp.add("august");
		queExp.add("september");
		queExp.add("october");
		queExp.add("november");
		queExp.add("december");
		queExp.add("content");
		queExp.add("media");
		queExp.add("website");
		queExp.add("email");
		queExp.add("online");
		queExp.add(" ");
		queExp.add("hours");
		queExp.add("video");
		queExp.add("2010");
		queExp.add("read");
		queExp.add("comments");
		queExp.add("comment");
		queExp.add("years");
		queExp.add("reply");
		queExp.add("share");
		queExp.add("email");
		queExp.add("2000");
		queExp.add("sign");
		queExp.add("add");
		queExp.add("prompts");
		queExp.add("pm");
		queExp.add("press");
		queExp.add("jan");
		queExp.add("feb");
		queExp.add("mar");
		queExp.add("apr");
		queExp.add("jun");
		queExp.add("jul");
		queExp.add("aug");
		queExp.add("oct");
		queExp.add("sep");
		queExp.add("nov");
		queExp.add("dec");
		queExp.add("press");
		queExp.add("replydelete");
		queExp.add("show");
		queExp.add("rate");
		queExp.add("div");
		queExp.add("review");
		queExp.add("reviews");
		queExp.add("login");
		queExp.add("post");
		queExp.add("var");
		queExp.add("pst");
		queExp.add("gmt");
		queExp.add("sat");
		queExp.add("videos");
		
		return queExp;
	}

	private static ArrayList<ArrayList<RankedTweetList>> useTTGResults(
			ArrayList<ArrayList<RankedTweetList>> allTopicTweets,
			ArrayList<ArrayList<RankedTweetList>> TTG_List) {
		float DEFAULT_TTG_BONUS = 0.2f;

		for (int i = 0; i < allTopicTweets.size(); i++) {
			for (int j = 0; j < allTopicTweets.get(i).size(); j++) {
				RankedTweetList rtl = allTopicTweets.get(i).get(j);

				for (int k = 0; k < rtl.size(); k++) {
					Tweet tweet = rtl.getTweet(k);
					long tweetID = tweet.getTweetID();
					String topicNum = tweet.getTopicNum();

					for (int h = i; h < (i + 1); h++) {
						for (int g = 0; g < TTG_List.get(h).size(); g++) {
							RankedTweetList rtlTTG = TTG_List.get(h).get(g);

							for (int m = 0; m < rtlTTG.size(); m++) {
								Tweet currTweet = rtlTTG.getTweet(m);
								long tweet_ID = currTweet.getTweetID();
								String topic_Num = currTweet.getTopicNum();

								if (topicNum.equalsIgnoreCase(topic_Num)
										&& tweetID == tweet_ID) {
									tweet.setScore(tweet.getScore()
											+ DEFAULT_TTG_BONUS);
								}
							}
						}
					}

				}
			}
		}

		return allTopicTweets;
	}

	public static ArrayList<LuceneQuery> makeQueriesList(String queryFile) {
		// the queries we have from our file
		ArrayList<LuceneQuery> queries = new ArrayList<LuceneQuery>();

		try {
			// the query processor is going to change the format of the query
			// from the the trec format to one we can use for our system
			QueryProcessor queryProcessor = null;
			queryProcessor = new QueryProcessor(queryFile, LOGGER);

			// just gets the queries in the proper format we want
			// nothing special out it being sanitized
			queries = queryProcessor.getSanitizedQueries();

			// Prints out the query information
			String queryStrings = "";
			for (LuceneQuery query : queries) {
				queryStrings += query.toString();
			}

			LOGGER.fine("Queries extracted from file:\n " + queryStrings);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}

		return queries;
	}

	public static ArrayList<ArrayList<RankedTweetList>> createAPI(
			ArrayList<LuceneQuery> queries, ArrayList<ArrayList<RankedTweetList>> allTopicTweets,
			boolean original, boolean split, boolean api) {

		if (original) {
			System.out
					.println("Creating the API index, splitting turned on...");

			apiQuery(queries, original, true);

			System.out
					.println("Creating the API index, splitting turned off...");

			apiQuery(queries, original, false);

			System.out.println("Finishing the API index.");
		} else {
			System.out.println("Creating the API index...");

			apiQuery(queries, original, split);

			System.out.println("Finishing the API index.");
		}

		allTopicTweets = createTweets(queries, allTopicTweets,
				original, split, api);

		return allTopicTweets;
	}

	public static ArrayList<ArrayList<RankedTweetList>> createTweets(
			ArrayList<LuceneQuery> queries, ArrayList<ArrayList<RankedTweetList>> allTopicTweets,
			boolean original, boolean split, boolean api) {
		// list of ranked tweet list
		ArrayList<RankedTweetList> rtl = new ArrayList<RankedTweetList>();

		if (api) {
			for (int m = 0; m < 2; m++) {
				allTopicTweets.clear();
				System.out.println("Starting up APISearch (" + m + ")...");

				LOGGER.info("Starting up APISearch.");

				String index = null;
				if (m == 0) {
					System.out.println("Using index-split...");
					index = harddrive + "/src/API/index-split";
				} else {
					System.out.println("Using index...");
					index = harddrive + "/src/API/index";
				}

				for (int i = 0; i < queries.size(); i++) {
					String ind = index + "/MB" + queries.get(i).getQueryNum()
							+ ".txt";

					// System.out.println("Index: " + ind);

					APISearch apisearch = new APISearch(LOGGER, ind);
					apisearch.setHitsReturned(10000);

					try {
						rtl = apisearch.search();
					} catch (Exception e) {
						LOGGER.setUseParentHandlers(true);
						LOGGER.severe("FATAL - We have encountered an error when parsing the query. Please check the query/queries. "
								+ "Java reported: " + e.getMessage());
					}

					allTopicTweets.add(rtl);
				}

				LOGGER.info("APISearch finished searching.");
				System.out.println("Finished APISearch.");

				for (int i = 0; i < allTopicTweets.size(); i++) {
					for (int j = 0; j < allTopicTweets.get(i).size(); j++) {
						ArrayList<Tweet> tweets = allTopicTweets.get(i).get(j)
								.getRankedList();

						if (tweets.size() < 1000) {
							System.err
									.println("In Stirs.java (in createTweets). output.csv will have less than 1000! (1)");
							System.err.println("tweets.size(): "
									+ tweets.size());
							if (tweets.size() > 1) {
								System.err.println("Topic: "
										+ tweets.get(0).getTopicNum());
							}
						}
					}
				}

				// prints tweets appropriately to file
				System.out.println("Starting Results...");
				Results.output(allTopicTweets, tag, scoreComparator, queries,
						false, api, m, "adhoc");
				System.out.println("Finished Results.");
			}
		} else {
			allTopicTweets.clear();

			System.out.println("Starting up APISearch...");

			LOGGER.info("Starting up APISearch.");

			String index = null;
			if (original) {
				if (split) {
					System.out.println("Using the split index!");
					index = harddrive + "/src/API/index-split";
				} else {
					System.out.println("Using the regular index!");
					index = harddrive + "/src/API/index";
				}
			} else {
				System.out.println("Using the run index!");
				index = harddrive + "/src/API/index-run";
			}

			for (int i = 0; i < queries.size(); i++) {
				String ind = index + "/MB" + queries.get(i).getQueryNum()
						+ ".txt";

				// System.out.println("Index: " + ind);

				APISearch apisearch = new APISearch(LOGGER, ind);
				apisearch.setHitsReturned(numHits);

				try {
					rtl = apisearch.search();
				} catch (Exception e) {
					LOGGER.setUseParentHandlers(true);
					LOGGER.severe("FATAL - We have encountered an error when parsing the query. Please check the query/queries. "
							+ "Java reported: " + e.getMessage());
				}

				allTopicTweets.add(rtl);
			}

			LOGGER.info("APISearch finished searching.");
			System.out.println("Finished APISearch.");

			for (int i = 0; i < allTopicTweets.size(); i++) {
				for (int j = 0; j < allTopicTweets.get(i).size(); j++) {
					ArrayList<Tweet> tweets = allTopicTweets.get(i).get(j)
							.getRankedList();

					if (tweets.size() < 1000) {
						System.err
								.println("In Stirs.java (in createTweets). output.csv will have less than 1000! (2)");
						System.err.println("tweets.size(): " + tweets.size());
						if (tweets.size() > 0) {
							System.err.println("Topic: "
									+ tweets.get(0).getTopicNum());
						} else {
							System.err.println("Topic: " + (i + 1));
						}
					}
				}
			}

			// prints tweets appropriately to file
			System.out.println("Starting Results...");
			Results.output(allTopicTweets, tag, scoreComparator, queries,
					false, false, 0, "adhoc");
			System.out.println("Finished Results.");
		}

		return allTopicTweets;
	}

	public static void apiQuery(ArrayList<LuceneQuery> queries,
			boolean original, boolean split) {
		if (queries == null) {
			System.err.println("In Stirs.java (in apiQuery). queries is null!");
			System.exit(0);
		}

		System.out.println("Starting API_Query...");

		for (int i = 0; i < queries.size(); i++) {
			String qID;

			qID = "MB" + queries.get(i).getQueryNum();

			System.out.println("Starting " + qID + " ("
					+ queries.get(i).getQuery() + ")...");

			// Get query text
			String query = queries.get(i).getQuery();

			// Create an API query instance
			API_Query api = new API_Query(port, qID, query, queries.get(i)
					.getTweetTime(), 10000, tag, split, harddrive);

			// Write the path to the query's .txt file location
			String fileName = qID + ".txt";
			String path = "";

			if (original) {
				if (split) {
					System.out.println("Using index-split...");
					path = (harddrive + "/src/API/index-split/" + fileName);
				} else {
					System.out.println("Using index...");
					path = (harddrive + "/src/API/index/" + fileName);
				}
			} else {
				System.out.println("Using index-run...");
				path = (harddrive + "/src/API/index-run/" + fileName);
			}

			// Run the query
			try {
				api.run_query(path);
			} catch (IOException e) {
				System.err
						.println("In Stirs.java (in apiQuery). Something went wrong running the query.");
				System.exit(0);
			}

			try {
				Thread.sleep(5000);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}

		System.out.println("Finished API_Query.");
	}

	public static ArrayList<LuceneQuery> evaluateQueryExp(
			ArrayList<LuceneQuery> queriesAfterExpansion, boolean external,
			int queryExpModulesRun, ArrayList<ArrayList<String>> newQueries) {
		HashSet<String> queExpStopWords = queryExpStopWords();
		
		for (int i = 0; i < queriesAfterExpansion.size(); i++) {
			String query = queriesAfterExpansion.get(i).getQuery().trim();
			String newQuery = query + " ";
			
			for (int j = 0; j < queryExpModulesRun; j++) {
				String words = newQueries.get(j).get(i);

				if (words.contains("\"")) {
					words = words.replaceAll("\"", "");
				}

				String word = queriesAfterExpansion.get(i).getQuery();
				if (word.contains("  ")) {
					queriesAfterExpansion.get(i).setQuery(
							word.replace("  ", " "));
				}

				newQuery += words.trim() + " ";
			}
			
			queriesAfterExpansion.get(i).setQuery(newQuery.trim());
		}

		POSTagger tagger = null;
		String speechCodes = "NN NNS NNP NNPS";
		StringTokenizer sTnoun = null;
		int count = 0;
		if (external) {
			// Creates an instance of the POSTagger class, for finding nouns
			try {
				tagger = new POSTagger();
				tagger.initializeTagger("/home/lmathews/workspace/libraries/en-pos-maxent.bin");
			} catch (Exception e) {
				System.err
						.println("In Stirs.java. Something happened when creating the POSTagger. ERROR: "
								+ e.getMessage());
				System.exit(0);
			}
		}

		StringTokenizer sT = null;
		HashSet<String> allWords = new HashSet<String>();

		int numToAllow = 6;

		for (int i = 0; i < queriesAfterExpansion.size(); i++) {
			sT = new StringTokenizer(queriesAfterExpansion.get(i).getQuery());
			allWords.clear();
			String curTok = "";
			String newQuery = "";
			count = 0;
			while (sT.hasMoreTokens()) {
				curTok = sT.nextToken();

				if (!allWords.contains(curTok.toLowerCase())) {
					if (external) {
						String[] tags = tagger.findTags(curTok.toLowerCase());

						sTnoun = new StringTokenizer(speechCodes, " ");
						String nounInput = sTnoun.nextToken();

						// goes through each of the speech codes
						while (sTnoun.hasMoreTokens()) {
							// if the word is a noun
							if (tags[0].equals(nounInput)) {
								// Updates the newQuery line for each loop
								if (count < numToAllow && !queExpStopWords.contains(curTok.toLowerCase())) {
									newQuery += curTok.toLowerCase() + " ";
									count++;
								}
							}

							nounInput = sTnoun.nextToken();
						}
					} else {
						if (count < numToAllow && !queExpStopWords.contains(curTok.toLowerCase())) {
							newQuery += curTok.toLowerCase() + " ";
							count++;
						}
					}

					allWords.add(curTok.toLowerCase());

					if (curTok.toLowerCase().endsWith("s")) {
						allWords.add(curTok.toLowerCase().substring(0,
								curTok.length() - 1));
					} else {
						allWords.add(curTok.toLowerCase() + "s");
					}

					if (curTok.toLowerCase().contains("'")) {
						allWords.add(curTok.substring(0, curTok.indexOf("'")));
					} else {
						allWords.add(curTok + "'s");
					}

					if (curTok.toLowerCase().contains("#")) {
						allWords.add(curTok.substring(1));
					} else {
						allWords.add("#" + curTok);
					}
				}
			}

			queriesAfterExpansion.get(i).setQuery(newQuery);

			String word = queriesAfterExpansion.get(i).getQuery();
			if (word.contains("  ")) {
				queriesAfterExpansion.get(i).setQuery(word.replace("  ", " "));
			}

			System.out.println("queriesAfterExpansion.get(" + i + "): "
					+ queriesAfterExpansion.get(i).getQuery());
		}

		return queriesAfterExpansion;
	}
}