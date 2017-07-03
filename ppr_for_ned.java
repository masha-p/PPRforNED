package after_paper;
// This is the file that produced an output for Ander, working with Eneko Aggire, etc.
// So it is the "official code" for NAACL paper.
/*
========================================
Results from running this code on the data in 
https://github.com/masha-p/PPRforNED
========================================
freebasePopularity= true, similarityIsOne=false, tieBreaking = true
========================================
+c2, +c1
micro = 0.9182484900776532	macro = 0.9016161875469987	totalCand = 378513
correct = 25542		nBestCorrect = 27593		total = 27816		nil_count = 2612	urlTotalCount.size() = 5593
========================================
+c2
micro = 0.917169974115617	macro = 0.9003227713730069	totalCand = 378513
========================================
+c1
micro = 0.9063848144952545	macro = 0.8930315639874264	totalCand = 378513
========================================
NO constraints:
micro = 0.9051984469370147	macro = 0.8919898531901623	totalCand = 378513
========================================
NO constraints, YES/NO self-score(does not matter here), similarityISone = true (set freebasePopularity to false)
micro = 0.8549396031061259	macro = 0.8569268892499189	totalCand = 378513
*/
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class ppr_for_ned {
	public static HashMap<String, HashSet<Integer> >  entityCandidates = new HashMap<String, HashSet<Integer>>();
	public static HashMap<Integer, String> numberEntity = new HashMap<Integer, String>();     		
	public static HashMap<String, String> entityAnswer = new HashMap<String, String>();		
	public static HashMap<String, Integer> entityCount = new HashMap<String, Integer>();		
	public static HashMap<String, String> entityText = new HashMap<String, String>();		
	public static HashMap<String, Integer> urlTotalCount = new HashMap<String, Integer>();		
	public static HashMap<String, Integer> urlTrueCount = new HashMap<String, Integer>();		
	public static HashMap<String, Integer> entityCorrectCandidate = new HashMap<String, Integer>();
	public static HashMap<Integer, String> candidateName = new HashMap<Integer, String>();		
	public static HashMap<Integer, String> candidateAnswer = new HashMap<Integer, String>();		
	public static HashMap<Integer, Integer> candidateINcount = new HashMap<Integer, Integer>();		
	public static HashMap<Integer, Integer> candidateDegree = new HashMap<Integer, Integer>();		
	public static HashMap<String, Double> normalization = new HashMap<String, Double>();
	// Map of freebase popularity for different urls.
    static HashMap<String, Double> freebase = new HashMap<String,Double>();
	// Final graph for personalized page rank and map for unfinished trips.
	public static HashMap<Integer, ArrayList<Integer>> adjacency = new HashMap<Integer, ArrayList<Integer>>();
	public static HashMap<Integer, HashMap<Integer, Integer>> unfinished_trips = new HashMap<Integer, HashMap<Integer,Integer>>();	
	public static int walkers = 2000;
	public static double teleport = 0.2;
	// We drop trips finished after first iteration.
	// Number of finished trips after second iteration = (number of UNfinished trips after first iter) * epsilon
	//public static double second = 2000*0.8*0.2;
	//public static double normalizationFactorForPPRtripsIfYouWantIt = second + second*0.8 + second*0.64 + second*0.64*0.8 = 320 + 256 + 204.8 + 163.84 = 944.64
	public static boolean plusFirstIteration = false;// default = false;
	public static boolean firstConstraint = true;    // default = true;
	public static boolean secondConstraint = true;   // default = true;
	public static boolean tieBreaking = true;        // default = true;
	public static boolean plusSelfScore = true;      // default = true;
	public static boolean similarityIsOne = false;   // default = false; 
	public static boolean freebasePopularity = true; // default = true;

	public static int theBestCandidate = 0;
	public static double theBestScore = 0.0;
	public static double gap = 0.0;
	public static double gapLowerBound = 0.1;
	public static HashSet<Integer> nBestSet = new HashSet<Integer>();
	public static int nBest = 3;
	public static int correct = 0;
	public static int total = 0;
	public static int nil_count = 0;	
	public static StringBuilder wrong = new StringBuilder();
	public static StringBuilder right = new StringBuilder();
	public static StringBuilder all = new StringBuilder();
	public static int totalCandidates = 0;
	
	public static void main(String[] args) throws IOException {	
		String run_name = "run_0830";	
		// Path to the file with freebase popularity scores.
		String dir_freebase = "/Users/masha/_entity_linking/POPULARITY/freebase_scores";
		// Path to the directory with files with candidates.
		String dir_in = "/Users/masha/_entity_linking/output_CLEANED_0311/out_clean_4_CLEANED/";
		// Path to already created directory where output statistics will be written.
        String dir_out = "/Users/masha/_entity_linking_2015/output_0828/";        
		
        BufferedReader fr = new BufferedReader(new FileReader( dir_freebase) ); 
    	// freebase = <String url, double score>
        while (fr.ready()) {
        	String[] parts = fr.readLine().trim().split("\\s+");
        	freebase.put( parts[0],  Double.parseDouble(parts[1]) );
        }
        fr.close();
        
		// Output files with disambiguation mistakes, with correctly disambiguated entities, with both (in concise format).
		BufferedWriter bw_wrong = new BufferedWriter(new FileWriter( dir_out + "wrong_" + run_name ) );																	
		BufferedWriter bw_correct = new BufferedWriter(new FileWriter( dir_out + "correct_" + run_name ) );																	
		BufferedWriter bw_all = new BufferedWriter(new FileWriter( dir_out + "all_" + run_name ) );																	

		File folder = new File(dir_in);
		File[] listOfFiles = folder.listFiles();
		int t = 1;
		for (File file : listOfFiles) {
			System.out.print("processedFiles=" + (t++) );			
			entityCandidates.clear();
			entityAnswer.clear();
			entityCount.clear();
			entityText.clear();
			entityCorrectCandidate.clear();
			numberEntity.clear();
			candidateName.clear();
			candidateAnswer.clear();
			candidateINcount.clear();
			candidateDegree.clear();
			adjacency.clear();
			unfinished_trips.clear();
			normalization.clear();
			wrong = new StringBuilder();
			right = new StringBuilder();	
			all = new StringBuilder();	
			
		    if (file.isFile()) {
		    	String fileName = file.getName();
		    	System.out.println("\t\t\tfileName=" + fileName);
		        BufferedReader br = new BufferedReader(new FileReader( dir_in + file.getName()) ); 		        
				computePersonalizedPageRank(br);
				bw_wrong.write("======= " + fileName + " ========= \n" + wrong.toString() + "\n");
				bw_correct.write("======= " + fileName + " ========= \n" + right.toString() + "\n");
				bw_all.write("======= " + fileName + " ========= \n" + all.toString() + "\n");
		        br.close();
		     }
			System.out.println("correct = " + correct + "\t\ttotal = " + total + "\t\tnil_count = " + nil_count + "\n======================");
		}		
		System.out.println("micro = " + ( (double) correct / total) + "\tmacro = " + computeMacroAccuracy() + "\ttotalCand = " + totalCandidates);
		bw_all.flush();
		bw_all.close();
		bw_wrong.flush();
		bw_wrong.close();
		bw_correct.flush();
		bw_correct.close();		
	}

	private static void computePersonalizedPageRank(BufferedReader br) throws IOException {					
		constructGraph( readGraph(br) );
		for (String entity : entityCandidates.keySet() ) {
			double sum = 0.0;
			for (int candidate : entityCandidates.get(entity) )
				if (freebasePopularity) 
					sum += freebase.get(candidateAnswer.get(candidate));
			normalization.put(entity, sum);
		}		
		HashMap<Integer, Double> scores = combinePPR( personalizedPageRank() );		
	    displayPersonalizedPageRank(scores);
	}

	private static double computeMacroAccuracy() {
		double accuracy = 0.0;
		for (String url : urlTotalCount.keySet() ) 
			if ( urlTrueCount.containsKey(url) && !url.equals("NIL") )
				accuracy += (double) urlTrueCount.get(url) / urlTotalCount.get(url) ;
		
		System.out.println("urlTotalCount.size() = " + urlTotalCount.size()	);
		return (double) accuracy / urlTotalCount.size();
	}

	private static HashMap<Integer, Double> combinePPR(
			HashMap<Integer, HashMap<Integer, Integer>> finished) throws IOException {
		HashMap<Integer, Double> coherenceScores = new HashMap<Integer, Double>();
		HashMap<Integer, HashMap<String, Integer>> endpointContributors = new HashMap<Integer, HashMap<String, Integer>>();
		HashMap<Integer, HashMap<String, Double>> endpointContributorsInitials = new HashMap<Integer, HashMap<String, Double>>();	
		double pprAveraged = 0.0;
		// Loop through all finished trips.
		// Then #walks for every start_point should be multiplied by its similarity score and contributed to the endpoint (subject to constraints).
		for (int start: finished.keySet() ) {
			double startInitialScore = 1.0;				
			if (freebasePopularity) { 
		    	if (numberEntity.containsKey(start))
		    		startInitialScore = freebase.get(candidateAnswer.get(start)) / normalization.get( numberEntity.get(start));
			} else if (similarityIsOne) 
				startInitialScore = 1.0;
			
			for (int endpoint : finished.get(start).keySet() ) {								
				// Ignore selfloops.
				if (endpoint == start)
					continue;

				// Do NOT count contribution from COMPETING candidates.
				if ( firstConstraint &&  numberEntity.get(endpoint).equals(numberEntity.get(start)))
					continue;
				 
			    // If (NOT secondConstraint) then every start contributes to every endpoint its number of walks.
				if ( !secondConstraint ) {
					double score = startInitialScore * finished.get(start).get(endpoint);				
					// Accumulate scores from every start point to a fixed endpoint in the map coherenceScores.
					if (coherenceScores.containsKey(endpoint))
						score += coherenceScores.get(endpoint);				
					coherenceScores.put(endpoint, score);	
					pprAveraged += finished.get(start).get(endpoint);
					continue;
				}
				// If (secondConstraint) then pick the highest contribution from candidates competing for the same entity.
				if ( secondConstraint ) {
					double numberWalks = finished.get(start).get(endpoint);				
					HashMap<String, Integer> entitiesForContributorsWalks = new HashMap<String, Integer>();
					if ( endpointContributors.containsKey(endpoint))
						entitiesForContributorsWalks = endpointContributors.get(endpoint);
	
					int oldNumberWalks = 0;
					// Contribution from another node competing for the same entity.
					// Key = entity. Value = number of walks from previous most significant candidate FROM this entity.
					if (entitiesForContributorsWalks.containsKey( numberEntity.get(start)))
						oldNumberWalks = entitiesForContributorsWalks.get(numberEntity.get(start) ); 
					
					// Key = entity. Value = initialScore from previous most significant candidate FROM this entity.
					HashMap<String, Double> entInitialScores = new HashMap<String, Double>();
					if (endpointContributorsInitials.containsKey(endpoint) )
						entInitialScores = endpointContributorsInitials.get(endpoint);
	
					double oldScore = 0.0;
					if ( entInitialScores.containsKey(numberEntity.get(start) )  )
							oldScore = entInitialScores.get(numberEntity.get(start));
					
					if ( numberWalks * startInitialScore > oldNumberWalks * oldScore  ) {
						// Update numberWalks in entitiesForContributors map.
						// Then update it in endpointContributors.
						entitiesForContributorsWalks.put( numberEntity.get(start), (int) numberWalks );
						endpointContributors.put(endpoint, entitiesForContributorsWalks);
						// Update initialScore in entInitialScores map.
						// Then update it in endpointContributorsInitials.
						entInitialScores.put(numberEntity.get(start), startInitialScore);
						endpointContributorsInitials.put(endpoint, entInitialScores);
						
					} 
				} // if second constraint
			}	
		}
		// If (NOT secondConstraint) then we have already accumulated all contributions from all candidates towards all other candidates.
		// If (secondConstraint) then accumulate PPR weights from all contributors we selected.
		if (secondConstraint ) {
			for (int endpoint : endpointContributors.keySet() ) {
				double score = 0;
				// Accumulate contribution scores from optimal candidate for each entity. 
				for (String ent : endpointContributors.get(endpoint).keySet() ) {
					score += endpointContributors.get(endpoint).get(ent) 
							* endpointContributorsInitials.get(endpoint).get(ent);
					pprAveraged += endpointContributors.get(endpoint).get(ent);
				}
				coherenceScores.put(endpoint, score);
			}
		}			
		// Add self-loops to all nodes. Process isolated nodes that did not get any finished trips at all.	
		// If graph is disconnected, then pprAveraged==0 (no edges => no walks). 
		// In this case set pprAveraged=1.0. 
		// This part is implicitly assumed. It is missed in the paper though...
		// Otherwise divide the total number of "used trips" (pprAveraged) by the total number of nodes in the graph.
		pprAveraged = pprAveraged < 1.0 ? 1.0 : (double) pprAveraged / numberEntity.size();
		for (int cand : numberEntity.keySet() ) {
			double coherenceSc = 0.0;
			if ( coherenceScores.containsKey(cand) )
				coherenceSc = coherenceScores.get(cand);			
			double initialSimilarity = 1.0;
			if (freebasePopularity) 
				initialSimilarity = freebase.get(candidateAnswer.get(cand)) / normalization.get( numberEntity.get(cand));
			else if (similarityIsOne) 
				initialSimilarity = 1.0;
			
			if ( !plusSelfScore )
				initialSimilarity = 0.0;				

			// Formula (5) from the paper: score(node) = coherence(node) + PPR_ave * iSim(node) 
			//      with correction that for disconnected graph pprAveraged == 1.0.
			coherenceScores.put(cand, coherenceSc + pprAveraged * initialSimilarity);			
		}
		return coherenceScores;
	}
		
	private static HashMap<Integer, HashMap<Integer, Integer>> personalizedPageRank() {
		// Initialize all unfinished trips.
		for (int start : adjacency.keySet() ) {
			unfinished_trips.put(start, new HashMap<Integer, Integer>());
			unfinished_trips.get(start).put(start,  walkers);
		}
		// Run iterations of PPR.
		Random randomGenerator = new Random(5);
		HashMap<Integer, HashMap<Integer, Integer>> finished_1 = one_iteration_ppr(randomGenerator);		
		HashMap<Integer, HashMap<Integer, Integer>> finished_2 = one_iteration_ppr(randomGenerator);	
		HashMap<Integer, HashMap<Integer, Integer>> finished_3 = one_iteration_ppr(randomGenerator);	
		HashMap<Integer, HashMap<Integer, Integer>> finished_4 = one_iteration_ppr(randomGenerator);	
		HashMap<Integer, HashMap<Integer, Integer>> finished_5 = one_iteration_ppr(randomGenerator);	
		
		//Combine finished trips from first, second, etc iterations. Merge two hashmaps into first (hashmap) argument.
		HashMap<Integer, HashMap<Integer, Integer>> finished_00 = combineIterations(finished_2, finished_3);
		HashMap<Integer, HashMap<Integer, Integer>> finished_000 = combineIterations(finished_00, finished_4);
		HashMap<Integer, HashMap<Integer, Integer>> finished_0000 = combineIterations(finished_5, finished_000);

		HashMap<Integer, HashMap<Integer, Integer>> finished_0 = combineIterations(finished_1, finished_0000);

		// Return all "finished" results combined.
		if (plusFirstIteration)
			return finished_0;	
		
		return finished_0000;
	}

	private static HashMap<Integer, HashMap<Integer, Integer>> combineIterations(
			HashMap<Integer, HashMap<Integer, Integer>> finished_1,
			HashMap<Integer, HashMap<Integer, Integer>> finished_2) {
       for (int start : finished_1.keySet() ) {
    	   if ( finished_2.containsKey(start)) { // Have to merge them together.
        	   HashMap<Integer, Integer> end_trips_1 = finished_1.get(start);
        	   HashMap<Integer, Integer> end_trips_2 = finished_2.get(start);
        	   for (int endpoint : end_trips_1.keySet()) {
        		   int count_1 = end_trips_1.get(endpoint);
        		   int count_2 = 0;
        		   if ( end_trips_2.containsKey(endpoint))
        			   count_2 = end_trips_2.get(endpoint);
        		   
        		   end_trips_1.put(endpoint,  count_1 + count_2);
        	   }
        	   for (int endpoint : end_trips_2.keySet()) {		
        		   int count_2 = end_trips_2.get(endpoint);
        		   // Nothing to merge. Just add to trip_1 whatever we have in trips_2.
        		   if ( !end_trips_1.containsKey(endpoint))
        			   end_trips_1.put(endpoint,  count_2);
        	   }
    	   } // else: do NOT need to do anything. Nothing to merge.
       }
       for (int start : finished_2.keySet() ) 
    	   if ( ! finished_1.containsKey(start))  
    		   finished_1.put(start,  finished_2.get(start));     
		
		return finished_1;
	}

	private static HashMap<Integer, HashMap<Integer, Integer>> one_iteration_ppr(Random randomGenerator) {
		HashMap<Integer, HashMap<Integer, Integer>> unfinished = new HashMap<Integer, HashMap<Integer,Integer>>();	
		HashMap<Integer, HashMap<Integer, Integer>> finished = new HashMap<Integer, HashMap<Integer,Integer>>();
		// For every unfinished trip - pick a random neighbor - flip a coin, whether to teleport - update corresponding map.
		for (int start: unfinished_trips.keySet() ) {
			for (int endpoint : unfinished_trips.get(start).keySet() ) {
				for (int trips = 0; trips < unfinished_trips.get(start).get(endpoint) ; trips++ ) {
					// Random neighbor.
					int random_neighbor = -1;
					if ( adjacency.containsKey(endpoint) && adjacency.get(endpoint).size() > 0)
						random_neighbor = adjacency.get(endpoint).get( randomGenerator.nextInt(	adjacency.get(endpoint).size() ) );
					else   // Isolated node. Does not have neighbors.
						random_neighbor = endpoint;
					
					// Teleport probability. If less than teleport => walk is finished.
					if ( randomGenerator.nextDouble() < teleport) {
						HashMap<Integer, Integer> walks = new HashMap<Integer, Integer>();
						if (finished.containsKey(start))
							walks = finished.get(start);
					    int count = 0;
					    if (walks.containsKey(random_neighbor))
					    	count = walks.get(random_neighbor);
					    
					    walks.put(random_neighbor,  ++count);
					    finished.put(start, walks);
					} else { // Do NOT teleport. Add another unfinished trip.
						HashMap<Integer, Integer> walks = new HashMap<Integer, Integer>();
						if (unfinished.containsKey(start))
							walks = unfinished.get(start);
					    int count = 1;
					    if (walks.containsKey(random_neighbor))
					    	count += walks.get(random_neighbor);
					    
					    walks.put(random_neighbor,  count);
					    unfinished.put(start, walks);
					}
				}
			}
		}
		// Clear old unfinished_trips. Update it with new unfinished.
		unfinished_trips.clear();
		unfinished_trips = unfinished;
		return finished;
	}

	private static void displayPersonalizedPageRank(HashMap<Integer, Double> final_scores) throws IOException { 		
		for (String entity : entityCandidates.keySet() ) {
			String statistics_string = findBestCandidate(entity, final_scores);
			populateUrlTrueCount(entity, theBestCandidate);	
			String result =	statistics_string + "\n\tbest = " + theBestCandidate 
					+ " (" + candidateAnswer.get(theBestCandidate) + ") => " 
					+ theBestScore + " \t(in=" + candidateINcount.get(theBestCandidate) + "  deg=" + candidateDegree.get(theBestCandidate) + ") " 
					+ "(fb=" + ( freebase.get(candidateAnswer.get(theBestCandidate)) / normalization.get(numberEntity.get(theBestCandidate))  ) + ")"
					+ "\n\t(" + entityAnswer.get(entity) + ") " + entityAnswer.get(entity).equals(candidateAnswer.get(theBestCandidate)) 
					+ " => " + final_scores.get( entityCorrectCandidate.get(entity) )
					+ "\t (in=" + candidateINcount.get( entityCorrectCandidate.get(entity) ) 
					+ "  deg=" + candidateDegree.get( entityCorrectCandidate.get(entity) ) + ")" ;
			updateAllCounts(entity, theBestCandidate, result + "\n\n", final_scores);
		}
	}

	private static String findBestCandidate(String entity,
			HashMap<Integer, Double> pprScores) throws IOException {
		// For given entity select candidates and their scores into map candidateScores = <Candidate, Score>. 
		HashMap<Integer, Double> candidateScores = new HashMap<Integer, Double>();
		// Place all scores into array to be sorted afterwards. 
		// It will be used to find top nBest scores and select corresponding candidates into nBestSet.
		ArrayList<Double> scoresToBeSorted = new ArrayList<Double>();
		StringBuilder bs = new StringBuilder();
		// A candidate with maximum score. 
		int bestCandidate = 0;
		double bestScore = 0.0;
		// A candidate with maximum incount (maximum incoming links).
		int maxIncountCandidate = 0;
		int maxIncountScore = 0;
		for (int cand : entityCandidates.get(entity)  )  {
			candidateScores.put(cand, pprScores.get(cand) );
			scoresToBeSorted.add(pprScores.get(cand));
			if ( pprScores.get(cand) >= bestScore ) {
				bestScore = pprScores.get(cand);
				bestCandidate = cand;
			}
			if ( candidateINcount.get(cand) >= maxIncountScore) {
				maxIncountScore = candidateINcount.get(cand) ;
				maxIncountCandidate = cand;				
			}			
			bs.append("\t" + cand + " (" + candidateAnswer.get(cand) + ")=>" + pprScores.get(cand) + 
					" (in=" + candidateINcount.get(cand) + "  deg=" + candidateDegree.get(cand) 
					+ ")  (fb=" + ( freebase.get(candidateAnswer.get(cand)) / normalization.get(numberEntity.get(cand))  ) + ")\n");
		}	

		theBestCandidate = bestCandidate;
		theBestScore = bestScore;
		// For simple PPR baseline we do not use any sophisticated logic to break ties. 
		// So we output the best candidate/score we have found so far. 
		// Same is in case if tieBreaking = false.
		// Same if we have only one candidate for this entity.
		if (similarityIsOne || !tieBreaking || entityCandidates.get(entity).size() == 1)	{
			return ("==============================================\n" + "=" + entityCount.get(entity) + "= " + entity + "\n" + bs.toString() ) ;
		}
		
		// Sort all candidate scores (ascending order), pick top nBest of them, put into nBestSet.
		Collections.sort(scoresToBeSorted);
		nBestSet.clear();
		int last = Math.max(scoresToBeSorted.size() - nBest, 0 );
		// n-th largest score (minimal score to get to nBestSet).
		double nBestScore = scoresToBeSorted.get( last );
		int totalMaxScoredCandidates = 0;	
		HashSet<Integer> maxScored = new HashSet<Integer>();
		int maxScoredWithHighestIncountCandidate = 0;
		double maxScoredWithHighestIncountScore = 0.0;		
		for (int cand : candidateScores.keySet() ) {
			// Build nBestSet.
			if (candidateScores.get(cand) >= nBestScore )
				nBestSet.add(cand);
			
			// From all candidates with bestScore find the one with highest inCount.
			if (candidateScores.get(cand) >= bestScore) {
				totalMaxScoredCandidates++;		
				maxScored.add(cand);
				if (candidateINcount.get(cand) >= maxScoredWithHighestIncountScore) {
					maxScoredWithHighestIncountScore = candidateINcount.get(cand);
					maxScoredWithHighestIncountCandidate = cand;
				}
			}
		}	
		// If total number of candidates that got bestScore is bigger than 1 => gap is zero.
		// Otherwise gap = the difference between bestScore and its runner-up.
		if ( totalMaxScoredCandidates > 1)
			gap = 0.0;
		else 
			gap = bestScore - scoresToBeSorted.get(scoresToBeSorted.size() -2);		
		
		// If gap is toooooo small then output the candidate with highest inCount.
		if (gap < gapLowerBound) {
			if ( totalMaxScoredCandidates > 1) {
				// Choice is based on maximum inCount between totalMaxScoredCandidates.
				theBestCandidate = maxScoredWithHighestIncountCandidate ;
				theBestScore = maxScoredWithHighestIncountScore;
			} else {
				// Choice is based on maximum inCount among all candidates.
				theBestCandidate = maxIncountCandidate ;
				theBestScore = maxIncountScore;
			}
		} 
		return ("==============================================\n" + "=" + entityCount.get(entity) + "= " + entity + "\n" + bs.toString() ) ;
	}
	
	private static void updateAllCounts(String entity, int bestCandidate,
			String result, HashMap<Integer, Double> scores) throws IOException {
		if (! entityAnswer.get(entity).equals("NIL") ) 
			total += entityCount.get(entity);		
		if (entityAnswer.get(entity).equals(candidateAnswer.get(bestCandidate))  ) { 
			correct += entityCount.get(entity);
			right.append(result);
			all.append(  entityCount.get(entity) + "\tENT=" + entity + "\tANS=" + candidateAnswer.get(bestCandidate) + "\n" );
		} else if ( !entityAnswer.get(entity).equals("NIL")  ) { 
			wrong.append(result);
			all.append(  entityCount.get(entity) + "\tENT=" + entity + "\tANS=" + candidateAnswer.get(bestCandidate) + "\n");
		}
		//"url:http://en.wikipedia.org/wiki/NIL"					
		if ( entityAnswer.get(entity).equals("NIL"))
			nil_count += entityCount.get(entity);			
	}

	private static void populateUrlTrueCount(String entity, int bestCandidate) {//"url:http://en.wikipedia.org/wiki/secondary_entity"
		int before = entityCount.get(entity);
		if ( entityAnswer.get(entity).equals(candidateAnswer.get(bestCandidate) ) ) { //&& ! entityText.get(entity).equals("uuuniverse")  ) {
			if ( urlTrueCount.containsKey( entityAnswer.get(entity) ) ) 
				before += urlTrueCount.get( entityAnswer.get(entity) );
			
			urlTrueCount.put( entityAnswer.get(entity), before );
		}		
	}

	private static void constructGraph(HashMap<Integer, HashSet<Integer>> graph ) {
		String separator = "_@_";
		HashMap<String, Integer> nodeCandidate = new HashMap<String, Integer>();		//T1
		//T1 Construct nodes = entity + candidate. Build a map (node, original_candidate).
		for (String entity : entityCandidates.keySet() ) 
			for (int cand : entityCandidates.get(entity) ) 
				nodeCandidate.put(entity + separator + cand, cand);	
		
		// Synonym is the same candidate used for different entities. It has number, name, wiki-name, incount, type.
		// Update synonymName with synonym enumeration and corresponding candidate names.
		HashMap<String, Integer> nodeNumber = new HashMap<String, Integer>();
		HashMap<Integer, String> tempCandidateName = new HashMap<Integer, String>();
		//T2 Enumerate nodes (synonyms): (number, name).
		int num = 2;
		for (String node: nodeCandidate.keySet()) {
			nodeNumber.put(node, num++);	
			tempCandidateName.put(  num - 1, candidateName.get(nodeCandidate.get(node) ) );
		}
		candidateName.clear();
		candidateName = tempCandidateName;

		//Update the best candidate number for every entity.
		HashMap<String, Integer> temp = new HashMap<String, Integer>();
		for (String entity : entityCorrectCandidate.keySet() )
			temp.put(entity, nodeNumber.get(entity + separator + entityCorrectCandidate.get(entity)));
		entityCorrectCandidate.clear();
		entityCorrectCandidate = temp;	

		//T22 Inverse map to nodeNumber: (number, node).
		HashMap<Integer, String> numberNode = new HashMap<Integer, String>();
		for (String node : nodeNumber.keySet() )
			numberNode.put( nodeNumber.get(node), node);		

		//T3 Inverse map for nodeCandidate. Map of (original_candidate, Set<candidate_synonyms> )
		HashMap<Integer, HashSet<Integer>> candidateSynonyms = new HashMap<Integer, HashSet<Integer>>();		//T3
		for (String node : nodeCandidate.keySet() ) {
			int cand = nodeCandidate.get(node);
			HashSet<Integer> synonyms = new HashSet<Integer>();
			if (candidateSynonyms.containsKey(cand))
				synonyms = candidateSynonyms.get(cand);			
			
			synonyms.add( nodeNumber.get(node));
			candidateSynonyms.put(cand,  synonyms);
		}
		//T4 Inverse map for entityCandidates. Map of (entity, Set<candidate_numbers> ).
		HashMap<String, HashSet<Integer>> tempEntityCandidates = new HashMap<String, HashSet<Integer>>();
		for (String entity : entityCandidates.keySet() ) {
			HashSet<Integer> numCandidates = new HashSet<Integer>();
			for (int cand : entityCandidates.get(entity) ) 
				numCandidates.add( nodeNumber.get(entity + separator + cand) );

			tempEntityCandidates.put(entity,  numCandidates);			
		}		
		//T5 Map ( node_number, corresponding_entity).
		for (String entity : entityCandidates.keySet() ) 
			for (int cand : entityCandidates.get(entity) ) 
				numberEntity.put( nodeNumber.get(entity + separator + cand), entity);		

		// Now we can update entityCandidates with map T4.
		entityCandidates.clear();
		entityCandidates = tempEntityCandidates;

		//T6 Map ( node_number, cand_url). 
		//   Map ( node_number, cand_incount)
		HashMap<Integer, Integer> tempIncount = new HashMap<Integer, Integer>();
		HashMap<Integer, String> tempAnswer = new HashMap<Integer, String>();
		for (int cand : candidateSynonyms.keySet()) {
			for (int synonym : candidateSynonyms.get(cand)) {
				tempAnswer.put(synonym,  candidateAnswer.get(cand));		
				tempIncount.put(synonym, candidateINcount.get(cand));
			}
		}
		candidateINcount.clear();
		candidateINcount = tempIncount;
		candidateAnswer.clear();
		candidateAnswer = tempAnswer;

		constructAdjacencyLists(numberNode, graph, candidateSynonyms);
	}

	private static void constructAdjacencyLists(
				HashMap<Integer, String> numberNode,
				HashMap<Integer, HashSet<Integer>> graph,
				HashMap<Integer, HashSet<Integer>> candidateSynonyms) {
		HashMap<Integer, HashSet<Integer>> adjacency_set = new HashMap<Integer, HashSet<Integer>>();
		//Adding vertices.
		for (int vertex : numberNode.keySet())
			adjacency_set.put(vertex,  new HashSet<Integer>());
		//Adding edges that are derived from original graph adjacency.
		for (int cand : graph.keySet() ) {
			for (int synonym : candidateSynonyms.get(cand) ) {
				//Look at the adjacent nodes of original cand.
				for (int neighbor : graph.get(cand) ) {
					//Look at the synonyms of each neighbor
					for (int neighbor_synonym : candidateSynonyms.get(neighbor) ) {
						//Check that synonym and neighbor_synonym do NOT compete for the same entity
						if (  ! numberEntity.get(synonym).equals( numberEntity.get(neighbor_synonym) ) ) {
								adjacency_set.get(synonym).add(neighbor_synonym);
								adjacency_set.get(neighbor_synonym).add(synonym);								
						} 
					}
				}
			}
		}		
		//Adding edges between "clones".
		for (int cand : candidateSynonyms.keySet() ) {
			for (int cand_syn_1 : candidateSynonyms.get(cand) ) {
				for (int cand_syn_2 : candidateSynonyms.get(cand) ) {
					if ( cand_syn_1 != cand_syn_2 ) {
						adjacency_set.get(cand_syn_1).add(cand_syn_2);
						adjacency_set.get(cand_syn_2).add(cand_syn_1);
					}
				}
			}
		}
		// Export adjacency_set into adjacency (global one).
		for (int vertex: adjacency_set.keySet() ) {
			ArrayList<Integer> neighbors = new ArrayList<Integer>();
			for (int neigh : adjacency_set.get(vertex))
				neighbors.add(neigh);
			
			adjacency.put(vertex, neighbors);
			candidateDegree.put(vertex, neighbors.size() );
		}		
	}

//	CANDIDATE	id:34683840	inCount:0	outCount:4	links:4011179;19520719;29581942;12048763	url:http://en.wikipedia.org/wiki/Henryk	name:Henryk	normalName:henryk
//	ENTITY	text:LONDON	url:http://en.wikipedia.org/wiki/London	normalName:london
	// Parse the input file.
	private static HashMap<Integer, HashSet<Integer>> readGraph(BufferedReader br) throws IOException {
		HashMap<Integer, HashSet<Integer>> undirectedLinks = new HashMap<Integer, HashSet<Integer>>();
		String line_entity = "";
		String line_first_candidate = "";
		boolean first_candidate = false;
		HashSet<Integer> Ecandidates = new HashSet<Integer>();
		HashSet<Integer> allCandidates = new HashSet<Integer>();
		while (br.ready()) {
			String line = br.readLine().trim();
			String[] parts = line.split("\\s+");
			if (parts[0].equals("ENTITY") ) {
				if (! line_entity.equals("") )
					entityUpload(line_entity, line_first_candidate, Ecandidates);
				
				Ecandidates = new HashSet<Integer>();				
				first_candidate = true;
				line_entity = line;
			}	
			else if (parts[0].equals("CANDIDATE") ) {
        		Map<String, String> values = processParts(parts);
                int cand = Integer.parseInt( values.get("id"));
                // First candidate is the correct one so we will put it in the map entityCorrectCandidate<Entity-String, Candidate_Integer>.
				if (first_candidate) {
					line_first_candidate = line;
					first_candidate = false;
				}
                Ecandidates.add(cand);
                allCandidates.add(cand);
                
                candidateAnswer.put(cand, values.get("url"));        
                candidateName.put(cand, values.get("normalName") ); 
                candidateINcount.put(cand, Integer.parseInt( values.get("inCount") ) );

                String[] links = values.get("links").trim().split(";");
                //For the case when edges are directed, we keep every candidate. 
                //There can exist another node, connected to this one.
                if (links[0].equals("") ) {
                	undirectedLinks.put(cand, new HashSet<Integer>() );
                	continue;
                }         
                // (1) Add current link to the set of candEdges .
                // (2) Add inverse edges from links -> cand .
                HashSet<Integer> candEdges = new HashSet<Integer>();
                if (undirectedLinks.containsKey(cand))
                	candEdges = undirectedLinks.get(cand);
                
                for (int i = 0; i < links.length; i++) {
                	int current = Integer.parseInt( links[i]);
                	candEdges.add( current );                     	  
                	HashSet<Integer> inverse = new HashSet<Integer>();
                	if (undirectedLinks.containsKey( current  ) ) 
                		inverse = undirectedLinks.get( current ) ;                	
                	inverse.add(cand);
                	undirectedLinks.put( current, inverse);
                }
                
                undirectedLinks.put(cand,  candEdges);      
			} // else if ("CANDIDATE")
		}// while br.ready()
		if (! line_entity.equals("") )
			entityUpload(line_entity, line_first_candidate, Ecandidates);
		
		return undirectedLinks;
	}	
	
	private static void entityUpload(String line_entity,
			String line_first_candidate, HashSet<Integer> Ecandidates) {
		Map<String, String> values_entity = processParts(line_entity.split("\\s+"));
		if (values_entity.get("normalName").equals("nil")) {
			String name = line_entity.substring( line_entity.indexOf("text") + 5, line_entity.indexOf("url") -1);
			String new_name = name.toLowerCase().replaceAll(",","").replaceAll("'","").replaceAll(" ", "-");
			values_entity.put("normalName",  new_name);
		}	
		String current_entity = values_entity.get("normalName") + "\t" + values_entity.get("url");
		if ( Ecandidates.size() > 0)
			entityCandidates.put(current_entity, Ecandidates);	
		
		totalCandidates += Ecandidates.size();
		entityAnswer.put(current_entity,  values_entity.get("url") );
		entityText.put(current_entity, values_entity.get("normalName").replaceAll("-",  " ") );

		// Count repeating entities.		
		int countE = 1;
		if (entityCount.containsKey(current_entity) ) 
			countE += entityCount.get(current_entity);
		entityCount.put(current_entity, countE);

		Map<String, String> values_first = processParts(line_first_candidate.split("\\s+"));
		int cand = Integer.parseInt( values_first.get("id"));
		if ( cand > 0 ) {
			// Populate entityBestCandidate.
			entityCorrectCandidate.put(current_entity, cand);	
			// Populate urlTotalCount to compute macro-accuracy.
			int count = 1;
			if (urlTotalCount.containsKey( values_entity.get("url") ) ) 
				count += urlTotalCount.get( values_entity.get("url") ); 
			urlTotalCount.put( values_entity.get("url") , count);			
		} 
	}

	private static Map<String, String> processParts(String[] parts) {
		// Examples: url:http://en.wikipedia.org/wiki/Titanic:_Music_from_the_Motion_Picture
		// 			 url:http://en.wikipedia.org/wiki/Titanic_(magazine)
		Map<String, String> values = new TreeMap<String, String>();
		for (String part : parts) {
			if ( part.indexOf(":") < 0) continue;
			if ( part.indexOf("::") > -1) continue;			
			if ( part.split(":").length == 0) continue;
			if ( part.split(":").length == 1)
				values.put(part.split(":")[0], "");
			else if ( part.split(":").length == 3)
				values.put(part.split(":")[0], "url:http:" + part.split(":")[2]);				
			else if ( part.split(":").length == 4)
				values.put(part.split(":")[0], "url:http:" + part.split(":")[2] + part.split(":")[3] );				
			else 
				values.put(part.split(":")[0], part.split(":")[1]);
		}
		return values;
	}
}