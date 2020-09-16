package objectTracking;

import static segImAnalysis.Util.weightedDistanceMatrix;
import static segImAnalysis.Util.printArray;
import static segImAnalysis.Util.println;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;


public class TrackAndNodeOperations {
	
	
	public static void addNodeAdjacency(Node nd1, Node nd2, int adjacency) {
		int prevAdj =  nd1.adjacencies.containsKey(nd2) ? nd1.adjacencies.get(nd2) : 0;
		nd1.adjacencies.put(nd2,prevAdj+adjacency);
		prevAdj =  nd2.adjacencies.containsKey(nd1) ? nd2.adjacencies.get(nd1) : 0;
		nd2.adjacencies.put(nd1,prevAdj+adjacency);	
	}
	
	public static double distanceEuclidean(Node n1, Node n2) {
		return Math.sqrt( 
				(n1.position[0]-n2.position[0])*(n1.position[0]-n2.position[0]) +
				(n1.position[1]-n2.position[1])*(n1.position[1]-n2.position[1]) +
				(n1.position[2]-n2.position[2])*(n1.position[2]-n2.position[2]));
	}
	
	// methods for matching nodes at adjacent times
	
	
	public static void matchNodes(
			ArrayList<Node> nds1,
			ArrayList<Node> nds2,
			double logSizeWeight,
			double matchThreshold,
			boolean verbose) {
		if (nds1.size()==0 || nds2.size()==0) {
			return;
		}
    	double[][] dist = nodeDistMatrix(nds1,nds2,logSizeWeight);
    	if (verbose) {System.out.println("Distance matrix: "); printArray(dist);}
    	ArrayList<int[]> matches = partialHungarianMatch(dist,matchThreshold,verbose);
    	if (verbose) {System.out.println("Matches: "); for (int[] mtch : matches) { printArray(mtch);}}
    	
    	for (int[] match : matches) {
    		if (match[0]<0 || match[1] <0 || match[0]>=nds1.size() || match[1]>=nds2.size() ) {
    			int x = match[0];int y = match[1];
    			try {
					throw new IllegalAccessException("partialHungarianMatch gave invalid match pair: sizes "+nds1.size()+","+nds2.size()+", matched pair " + x + ","+y+"!");
                } catch (IllegalAccessException ex) {
                    System.err.println(ex);
                    System.exit(1);
                }
    		}
    		linkMatchedNodes(nds1.get(match[0]),nds2.get(match[1]),dist[match[0]][match[1]]);	
    	}
	}

	public static void linkMatchedNodes(Node predNode, Node succNode, double dist) {
		predNode.succs.add(succNode);
		predNode.nextNodeDist.put(succNode,dist);
		succNode.preds.add(predNode);
		succNode.nextNodeDist.put(predNode,dist);
	}
	
	public static double[][] nodeDistMatrix(ArrayList<Node> nds1,
			ArrayList<Node> nds2,
			double logSizeWeight){
		double[][][] nodePosMatrices = new double[2][][];
		for (int ii=0;ii<2;ii++) {
			ArrayList<Node> nds = ii==0 ? nds1 : nds2;
			nodePosMatrices[ii] = new double[nds.size()][];
			for (int jj=0;jj<nds.size();jj++) {
				double[] pos = nds.get(jj).position;
				nodePosMatrices[ii][jj] = new double[] {pos[0],pos[1],pos[2],Math.log(nds.get(jj).voxels)};
			}
		}
		double[] wts = {1,1,1,logSizeWeight};
		return(weightedDistanceMatrix(nodePosMatrices[0],nodePosMatrices[1],wts));
	}
	
	// version for single pair of nodes, used in iterative merge/match process 
	public static double matchDistance(Node nd1, Node nd2, double logSizeWeight) {
		return Math.sqrt(
				Math.pow(nd1.position[0]-nd2.position[0],2) + 
				Math.pow(nd1.position[1]-nd2.position[1],2) + 
				Math.pow(nd1.position[2]-nd2.position[2],2) + 
				Math.pow(logSizeWeight*(Math.log(nd1.voxels)-Math.log(nd2.voxels)),2)
				);
	}
	
	public static ArrayList<int[]> partialHungarianMatch(double[][] distMatrix, double maxMatchDist,boolean verbose){
		// gets optimal matching between 2 groups of unequal size, in which each object is matched with 0 or 1 objects in other group (symmetric matching, so formed into pairs)
		// all match distances are <= maxMatchDist, and we minimise the sum of the match distances plus a penalty of maxMatchDist for each unmatched object
		// double epsilon = 0.00001;
		int nrows = distMatrix.length;
		int ncols = distMatrix[0].length;
		int n = Math.max(nrows,ncols);
		if (verbose) {System.out.println("Matching set sizes " + nrows+", "+ncols+", "+n+" with maxMatchDist "+maxMatchDist);}
		double[][] paddedMatrix = new double[n][n];
		for (int r=0; r<nrows; r++) {
			for (int c=0; c<ncols; c++) {
				paddedMatrix[r][c] = Math.min(distMatrix[r][c], maxMatchDist);
			}
			for (int c=ncols; c<n; c++) {
				paddedMatrix[r][c] = maxMatchDist;
			}
		}
		for (int r=nrows; r<n; r++) {
			for (int c=0; c<n; c++) {
				paddedMatrix[r][c] = maxMatchDist;
			}
		}
		if (verbose) {System.out.println("Padded dist matrix: "); printArray(paddedMatrix);}
		HungarianAlgorithm ha = new HungarianAlgorithm(paddedMatrix); // destructive
	    int[][] assignment = ha.findOptimalAssignment();
	    ArrayList<int[]> rowCol = new ArrayList<int[]>();
	    for (int[] pr : assignment) {
	    	// pr is [col,row] - opposite to my convention
	    	if (verbose) {printArray(pr);}
	    	if (pr[1]<nrows && pr[0]<ncols && distMatrix[pr[1]][pr[0]]< maxMatchDist) {
	    		rowCol.add(new int[] {pr[1],pr[0]});
	    		if (verbose) {System.out.println("KEEP");}
	    	}
	    }
	    //for (int[] pr : assignment) {
	    //	double dist = paddedMatrix[pr[0]][pr[1]];
	    //	System.out.println(pr[0]+", "+pr[1]+": "+dist+ (dist<maxMatchDist ? " include" : " drop"));
	    //}
	    //printArray(paddedMatrix);
	    
	    return(rowCol); 
	}
	
	// methods for assessing potential merges of nodes / tracks
	public static double relativeNodeContact(Node n1, Node n2) {
		// try to approx contact area as proportion of minimum surface area of smaller node (i.e. assuming sphere) 
		// Adjacencies are based on a 18-neighbour model; the adjacency count is the number of distinct pairs of neighbouring voxels under this def, that includes one from each object in question.
		// divide by 5 to get approximate contact area in voxel units
		// The surface area of a sphere of volume V is V^{2/3} * (36pi)^{1/3} ~ 4.835976 V^{2/3}
		// Thus the proportional contact estimate is (adjacency/5) / (V^{2/3} * (36pi)^{1/3}) = 1/(5*(36*pi)^{1/3}) * (adjacency/V^{2/3}) ~ 0.0413567 (adjacency/V^{2/3})
		// hard code constant for efficiency
		if (!n1.adjacencies.containsKey(n2)) {return 0.0;}
		int adj = n1.adjacencies.get(n2);
		int vol = Math.min(n1.voxels, n2.voxels);
		return 0.0413567*adj/Math.pow(vol, 2.0/3.0);
	}
	
	public static double relativeNodeCloseness(Node n1, Node n2) {
		// calculate how far the centroids would be apart if the nodes were touching spheres, then divide by actual separation 
		// to get compactness measure of combined object
		// (4*pi/3)^(1/3) ~ 1.611992 is a correction factor for converting volume to radius
		return (Math.pow(n1.volume(), 1.0/3.0)+Math.pow(n2.volume(), 1.0/3.0))/(1.611992*distanceEuclidean(n1,n2));	
	}
	
	// to get overall score for whether 2 nodes should be merged, get ratios of relativeNodeContact and relativeNodeDistance to specified reference values,
	// then take weighted mean of these ratios with specified weights
	// Thus 1=neutral, higher values indicate evidence for merging
	public static double nodeMergeScore(Node n1, Node n2, double relativeNodeContact_referenceValue, double relativeNodeDistance_referenceValue, double relativeNodeContact_weight, double relativeNodeDistance_weight) {
		return (relativeNodeContact_weight*relativeNodeContact(n1,n2)/relativeNodeContact_referenceValue + relativeNodeDistance_weight*relativeNodeCloseness(n1,n2)/relativeNodeDistance_referenceValue)/(relativeNodeContact_weight+relativeNodeDistance_weight);
	}
	
	// get an overall score for merging 2 tracks (with possible extensions), by combining nodeMergeScore results on common times with change to match distance penalties
		// 0 = neutral, higher is evidence for merging
		// if initial score is<0 but >0 before matching adjustment, look for additional tracks that could be added (specifically looking for case where 2 tracks should be merged, but one is broken by a tracking failure)
		// additional tracks are added to tracks ArrayList
		// for extension, go with the first track which improves match score and merge score sum
		// important that no tracks are changed in this method - informative purposes only
		public static TrackSetMergeScore trackMergeScore(ArrayList<Track> tracks, 
				double relativeNodeContact_referenceValue, double relativeNodeDistance_referenceValue, double relativeNodeContact_weight, double relativeNodeDistance_weight,
				double logSizeWeight, double matchThreshold, double matchScoreWeighting, boolean verbose, boolean haltOnInfiniteScore) {
			double mergeScoreTotalAllCommonTime=0;
			HashMap<Integer,Double> mergeScoresByTime = new HashMap<Integer,Double>();
			int firstCommonTime = Integer.MAX_VALUE;
			int lastCommonTime = Integer.MIN_VALUE;
			for (int ts : tracks.get(0).times()) {
				if (!tracks.get(1).nodesByTime.containsKey(ts)) {continue;}
				double mergeScore = nodeMergeScore(tracks.get(0).nodesByTime.get(ts),tracks.get(1).nodesByTime.get(ts),relativeNodeContact_referenceValue,relativeNodeDistance_referenceValue,relativeNodeContact_weight,relativeNodeDistance_weight)-1;
				mergeScoreTotalAllCommonTime+=mergeScore;
				mergeScoresByTime.put(ts, mergeScore);
				if (ts<firstCommonTime) {firstCommonTime=ts;}
				if (ts>lastCommonTime) {lastCommonTime=ts;}
			}
			
			int commonTimeLength = lastCommonTime-firstCommonTime+1;
			//double[] startTimeScoreAdjustment=new double[commonTimeLength];
			double[] endTimeScoreAdjustment=new double[commonTimeLength];
			double[] startTimeScoreAdjustment_bestBefore =new double[commonTimeLength];
			int[] startTimeScoreAdjustment_bestBefore_timeStep =new int[commonTimeLength];
			
			int bestStartTime = firstCommonTime;
			int bestEndTime = lastCommonTime;
			double bestScoreAdjustment = -Double.MAX_VALUE;
			

			// forward sweep, testing merge start times
			double mergeScoreCumForward=0;
			double bestStartTimeScoreAdjustment = -Double.MAX_VALUE;
			int bestStartTimeScoreAdjustment_timeStep=0;
			for (int ii=0;ii<commonTimeLength;ii++) {
				int ts = firstCommonTime+ii;
				double startTimeScoreAdjustment = -mergeScoreCumForward-matchScoreWeighting*matchDistAdjustment(tracks.get(0),tracks.get(1),ts,ts-1,logSizeWeight,matchThreshold);
				if (startTimeScoreAdjustment>bestStartTimeScoreAdjustment) {
					bestStartTimeScoreAdjustment=startTimeScoreAdjustment;
					bestStartTimeScoreAdjustment_timeStep=ts;
				}
				startTimeScoreAdjustment_bestBefore[ii] = bestStartTimeScoreAdjustment;
				startTimeScoreAdjustment_bestBefore_timeStep[ii] = bestStartTimeScoreAdjustment_timeStep;
				mergeScoreCumForward+=mergeScoresByTime.get(ts);
			}
			// backward sweep, testing merge end times
			double mergeScoreCumBackward=0;
			for (int ii=0;ii<commonTimeLength;ii++) {
				int ts = lastCommonTime-ii;
				endTimeScoreAdjustment[ii] = -mergeScoreCumBackward-matchScoreWeighting*matchDistAdjustment(tracks.get(0),tracks.get(1),ts,ts+1,logSizeWeight,matchThreshold);
				if (endTimeScoreAdjustment[ii] + startTimeScoreAdjustment_bestBefore[commonTimeLength-ii-1] > bestScoreAdjustment) {
					bestScoreAdjustment = endTimeScoreAdjustment[ii] + startTimeScoreAdjustment_bestBefore[commonTimeLength-ii-1];
					bestEndTime = ts;
					bestStartTime = startTimeScoreAdjustment_bestBefore_timeStep[commonTimeLength-ii-1];
				}
				mergeScoreCumBackward+=mergeScoresByTime.get(ts);
			}
			double finalScore = mergeScoreTotalAllCommonTime + bestScoreAdjustment;// -  matchScoreWeighting*(mergeMatch1 - existingMatchDist1 + mergeMatch2 - existingMatchDist2);
			
			
			// in each direction in turn, try possible extensions to the current merge that give better matches
			// only preceeding in the case where we have positive merge score, but it is outweighed by the match penalty (giving a negative total)
			// sometimes this is due to a tracking failure, and finding an additional track which matches well at the current endpoint
			// (if merged to the continuing track) may fix this issue
			
			// in order to consider possible extensions, need to break down finalScore into merge score and start/end match distance adjustments
			double matchDistanceAdjustmentStart = matchScoreWeighting*matchDistAdjustment(tracks.get(0),tracks.get(1),bestStartTime,bestStartTime-1,logSizeWeight,matchThreshold);
			double matchDistanceAdjustmentEnd = matchScoreWeighting*matchDistAdjustment(tracks.get(0),tracks.get(1),bestEndTime,bestEndTime+1,logSizeWeight,matchThreshold);
			double mergeScoreTotal = finalScore + matchDistanceAdjustmentStart + matchDistanceAdjustmentEnd;
			
			if(verbose) {
				println("Best merge of initial track pair: "+bestStartTime+"-"+bestEndTime+ " (common time "+firstCommonTime+"-"+lastCommonTime+ ")");
				println("  merge score "+mergeScoreTotal+", match distance adjustment "+ matchDistanceAdjustmentStart+", "+matchDistanceAdjustmentEnd+": adjusted score "+finalScore);
				println("Looking for extensions: current tracks"); for (Track tr:tracks) {println(tr.endTimes()[0]+"-"+tr.endTimes()[1]);}
			}
			for (boolean forward : new boolean[] {false,true}) {
				// only try extensions when current merge interval extends to the end of the common time period 
				if ((forward && (bestEndTime!=lastCommonTime)) || (!forward && (bestStartTime!=firstCommonTime))) {continue;}
				int tStep = forward ? 1 : -1;
				if(verbose) {println("Common time "+firstCommonTime+"-"+lastCommonTime+"; "+ (forward?"forward ":"backward ")+","+tStep);}
				while (finalScore<0 && mergeScoreTotal>0) {
					// first establish that exactly one track continues
					Track continuingTrack = null;
					Track terminatingTrack = null;
					int endTime = forward ? bestEndTime : bestStartTime;
					for (Track tr : tracks) {
						if (tr.nodesByTime.containsKey(endTime+tStep)) {
							assert(continuingTrack == null);
							continuingTrack = tr;
						} else if (tr.nodesByTime.containsKey(endTime)) {
							terminatingTrack = tr;
						}
					}
					if (continuingTrack == null) {break;}
					if (terminatingTrack==null) {
						println("Terminating track calculation failed");
						for (Track tr:tracks) {println(tr.endTimes()[0]+"-"+tr.endTimes()[1]);}
						println("Common time "+firstCommonTime+"-"+lastCommonTime+"; "+ (forward?"forward ":"backward ")+endTime+","+tStep);
						try {
							throw new Exception("Terminating track is null!"); //IllegalAccessException
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					// now consider the tracks adjacent to continuingTrack, as candidates for a continued merge
					// We never expect to have 2 extension tracks which both give improved matching at the next time step plus an overall
					// improvement in match/merge score, so we take the first one that works (if any)
					ArrayList<Track> adTracks = continuingTrack.adjacentTracks();
					boolean considerFurtherExtension=false;
					for (Track tryTrack : adTracks) {
						// a candidate extension track must start at the next time point and give an improvement in matching at that time
						if (tryTrack.nodesByTime.containsKey(endTime) || !tryTrack.nodesByTime.containsKey(endTime+tStep)) {continue;}
						double matchDistToTryTrackAdjustment = matchScoreWeighting*matchDistance(
								new Node(continuingTrack.nodesByTime.get(endTime),terminatingTrack.nodesByTime.get(endTime)),
								new Node(continuingTrack.nodesByTime.get(endTime+tStep),tryTrack.nodesByTime.get(endTime+tStep)),logSizeWeight)
								- (forward ? matchDistanceAdjustmentEnd : matchDistanceAdjustmentStart);
						if (matchDistToTryTrackAdjustment>=0) {continue;}
											
						// now we continue along until the end of either tryTrack or continuingTrack, and calculate the best time to end the merge
						// default stop is the current end point, with no score adjustment
						// at any subsequent time, the match distance adjustment must take account of matchDistToTryTrackAdjustment as well as the adjustment at the new proposed end
						int bestEndPoint = endTime;
						double bestMergeScoreDiff=0;
						double bestMatchDistDiff=0;
						
						int ts = endTime+tStep;
						double newMergeScoreSum=0;
						
						while (continuingTrack.nodesByTime.containsKey(ts) && tryTrack.nodesByTime.containsKey(ts) ) {						
							newMergeScoreSum += nodeMergeScore(continuingTrack.nodesByTime.get(ts),tryTrack.nodesByTime.get(ts),
									relativeNodeContact_referenceValue,relativeNodeDistance_referenceValue,relativeNodeContact_weight,relativeNodeDistance_weight)-1;
							double currentMatchDistanceAdjustment = matchDistToTryTrackAdjustment+matchScoreWeighting*matchDistAdjustment(continuingTrack,tryTrack,ts,ts+tStep,logSizeWeight,matchThreshold);
							
							if (newMergeScoreSum-currentMatchDistanceAdjustment > bestMergeScoreDiff - bestMatchDistDiff) {
								bestMergeScoreDiff = newMergeScoreSum;
								bestMatchDistDiff = currentMatchDistanceAdjustment;
								bestEndPoint = ts;
							}
							ts+=tStep;
						}
						if (bestEndPoint==endTime){continue;}
						
						// at this point we commit to an extension track (tryTrack) and new start/end point (bestEndPoint); time to make adjustments
						tracks.add(tryTrack);						
						mergeScoreTotal += bestMergeScoreDiff;
						//println("Added merge from "+endTime+" to " + bestEndPoint+";"+tStep);
						if (forward) {
							//println("bestEndTime: " + bestEndTime + "->" + bestEndPoint);
							bestEndTime = bestEndPoint;
							matchDistanceAdjustmentEnd += bestMatchDistDiff;
						} else {
							//println("bestStartTime: " + bestStartTime + "->" + bestEndPoint);
							bestStartTime = bestEndPoint;
							matchDistanceAdjustmentStart+=bestMatchDistDiff;
						}
						finalScore = mergeScoreTotal -  matchDistanceAdjustmentStart - matchDistanceAdjustmentEnd;
						
						// we consider further extension if we have added an extension (tryTrack) and merged to the end of either tryTrack or continuingTrack				
						considerFurtherExtension=!continuingTrack.nodesByTime.containsKey(bestEndPoint+tStep) || !tryTrack.nodesByTime.containsKey(bestEndPoint+tStep);
						break;
					}	
					if (!considerFurtherExtension) {break;}
				}
			}
			
			
			if (finalScore==Double.POSITIVE_INFINITY) {
				for (Track tr : tracks)
					tr.printNodeTable();
				if (haltOnInfiniteScore) {
				try {
					
					throw new IllegalAccessException("trackMergeScore gave an infinite value! Tracks equal: "+ (tracks.get(0)==tracks.get(1))); //  nds1.size()+","+nds2.size()+", matched pair " + x + ","+y+"!");
	            } catch (IllegalAccessException ex) {
	                System.err.println(ex);
	                System.exit(1);
	            }
				} else {
					println("trackMergeScore gave an infinite value! Tracks equal: "+ (tracks.get(0)==tracks.get(1)));
				}
			}
			
			// now work out which tracks need to be split at the ends, and record info in a TrackSetMergeScore object
			
			
			ArrayList<Track> tracksCutAtStart = new ArrayList<Track>();
			ArrayList<Track> tracksCutAtEnd = new ArrayList<Track>();
			for (boolean forward : new boolean[] {false,true}) {
				int endTime = forward ? bestEndTime : bestStartTime;
				int tStep = forward ? 1 : -1;
				ArrayList<Track> tracksAtEnd = new ArrayList<Track>();
				ArrayList<Track> continuingTracks = new ArrayList<Track>();
				for (Track tr : tracks) {
					if (tr.nodesByTime.containsKey(endTime)) {tracksAtEnd.add(tr);}
					if (tr.nodesByTime.containsKey(endTime+tStep)) {continuingTracks.add(tr);}
				}
				if (tracksAtEnd.size()!=2 || continuingTracks.size()>2) {
					try {
						throw new Exception("track count error at end of merge section in trackMergeScore " + tracksAtEnd.size() +"; "+continuingTracks.size() + "; " + forward + "; " + endTime ); //IllegalAccessException
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (continuingTracks.size()==0) {continue;}
				ArrayList<Track> tracksCut = new ArrayList<Track>();
				Node endNode = new Node(tracksAtEnd.get(0).nodesByTime.get(endTime), tracksAtEnd.get(1).nodesByTime.get(endTime));
				double matchDist1 = matchDistance(endNode,continuingTracks.get(0).nodesByTime.get(endTime+tStep),logSizeWeight);
				if (matchDist1>matchThreshold) {
					tracksCut.add(continuingTracks.get(0));
				}
				if (continuingTracks.size()==2) {
					double matchDist2 = matchDistance(endNode,continuingTracks.get(1).nodesByTime.get(endTime+tStep),logSizeWeight);		
					if (matchDist2>matchDist1) {
						tracksCut.add(continuingTracks.get(1));
					} else {
						if (!tracksCut.contains(continuingTracks.get(0))) {
							tracksCut.add(continuingTracks.get(0));
						}
					}
				}
				if (forward) {
					tracksCutAtEnd=tracksCut;
					} else {
						tracksCutAtStart=tracksCut;
					}

				
			}
			if(verbose) {
				println("Best final merge: "+bestStartTime+"-"+bestEndTime);
				println("  merge score "+mergeScoreTotal+", match distance adjustment "+ matchDistanceAdjustmentStart+", "+matchDistanceAdjustmentEnd+": adjusted score "+finalScore);
				if (tracks.size()>3) {println("Final included tracks:"); for (Track tr:tracks) {println(tr.endTimes()[0]+"-"+tr.endTimes()[1]);}}
			}
			// TrackSetMergeScore tsms = new TrackSetMergeScore(finalScore,bestStartTime,bestEndTime,tracks,tracksCutAtStart,tracksCutAtEnd);
			
			return new TrackSetMergeScore(finalScore,bestStartTime,bestEndTime,tracks,tracksCutAtStart,tracksCutAtEnd);
		}
	
	
	// get an overall score for merging 2 tracks (with possible extensions), by combining nodeMergeScore results on common times with change to match distance penalties
	// 0 = neutral, higher is evidence for merging
	// if initial score is<0 but >0 before adjustement, look for additional tracks that could be added (specifically looking for case where 2 tracks should be merged, but one is broken by a tracking failure 
	// additional tracks are added to tracks ArrayList
	// for extension, go with the first track which improves match score and merge score sum
	// important that no tracks are changed in this method - informative purposes only
	public static double[] trackMergeScoreOld2(ArrayList<Track> tracks, 
			double relativeNodeContact_referenceValue, double relativeNodeDistance_referenceValue, double relativeNodeContact_weight, double relativeNodeDistance_weight,
			double logSizeWeight, double matchThreshold, double matchScoreWeighting, boolean haltOnInfiniteScore) {
		double mergeScoreTotal=0;
		int firstCommonTime = Integer.MAX_VALUE;
		int lastCommonTime = Integer.MIN_VALUE;
		for (int ts : tracks.get(0).times()) {
			if (!tracks.get(1).nodesByTime.containsKey(ts)) {continue;}
			//commonTimeStepCount++;
			mergeScoreTotal+=nodeMergeScore(tracks.get(0).nodesByTime.get(ts),tracks.get(1).nodesByTime.get(ts),relativeNodeContact_referenceValue,relativeNodeDistance_referenceValue,relativeNodeContact_weight,relativeNodeDistance_weight)-1;
			if (ts<firstCommonTime) {firstCommonTime=ts;}
			if (ts>lastCommonTime) {lastCommonTime=ts;}
			// test - temp
//			if (ts==204) {
//				System.out.println(" dist/contact " +  relativeNodeCloseness(tr1.nodesByTime.get(ts),tr2.nodesByTime.get(ts)) +"/"+ relativeNodeContact(tr1.nodesByTime.get(ts),tr2.nodesByTime.get(ts)) + 
//						"; ref values " + relativeNodeDistance_referenceValue + "/"+relativeNodeContact_referenceValue + "; weights " + relativeNodeDistance_weight+"/"+relativeNodeContact_weight);
//			}
		}
		
		// now penalise score by change in total match distance, weighted by matchScoreWeighting; return -inf (always reject) if a new match exceeds threshold
		double mergeMatch1 = mergedMatchDist(new Track[] {tracks.get(0),tracks.get(1)},firstCommonTime-1,firstCommonTime,logSizeWeight);
		double mergeMatch2 = mergedMatchDist(new Track[] {tracks.get(0),tracks.get(1)},lastCommonTime,lastCommonTime+1,logSizeWeight);
		// if (mergeMatch1>matchThreshold || mergeMatch2>matchThreshold ) {return new double[] {Double.NEGATIVE_INFINITY,mergeScoreTotal};}
		double existingMatchDist1 = 0;
		double existingMatchDist2 = 0;
		for (Track tr : tracks) {
			Node eNode = tr.nodesByTime.get(firstCommonTime);
			if (eNode.preds.size()>0) {existingMatchDist1+=eNode.nextNodeDist.get(eNode.preds.get(0));}
			eNode = tr.nodesByTime.get(lastCommonTime);
			if (eNode.succs.size()>0) {existingMatchDist2+=eNode.nextNodeDist.get(eNode.succs.get(0));}
		}
		double finalScore = mergeScoreTotal -  matchScoreWeighting*(mergeMatch1 - existingMatchDist1 + mergeMatch2 - existingMatchDist2);
		// test - temp
		//ArrayList<Integer> tr1_ts = tr1.times(); ArrayList<Integer> tr2_ts = tr2.times();
		//System.out.println("track intervals " + tr1_ts.get(0) +" - " + tr1_ts.get(tr1_ts.size()-1)  + ", "+ tr2_ts.get(0) +" - " + tr2_ts.get(tr2_ts.size()-1));
		//System.out.println(mergeScoreTotal+" - "+ matchScoreWeighting +"*( "+ mergeMatch1+"+"+mergeMatch2 +"-"+ existingMatchDists+") = "+  (mergeScoreTotal -  matchScoreWeighting*(mergeMatch1 + mergeMatch2 - existingMatchDists))  );		
		
		// looking at possible extensions
		// println("Looking for extensions: current tracks"); for (Track tr:tracks) {println(tr.endTimes()[0]+"-"+tr.endTimes()[1]);}
		for (boolean forward : new boolean[] {false,true}) {
			int tStep = forward ? 1 : -1;
			// println("Common time "+firstCommonTime+"-"+lastCommonTime+"; "+ (forward?"forward ":"backward ")+","+tStep);
			while (finalScore<0 && mergeScoreTotal>0) {
				// try possible extensions to tracks that give better matches
				Track continuingTrack = null;
				Track terminatingTrack = null;
				int endTime = forward ? lastCommonTime : firstCommonTime;
				for (Track tr : tracks) {
					if (tr.nodesByTime.containsKey(endTime+tStep)) {
						assert(continuingTrack == null);
						continuingTrack = tr;
					} else if (tr.nodesByTime.containsKey(endTime)) {
						terminatingTrack = tr;
					}
				}
				if (continuingTrack == null) {break;}
				if (terminatingTrack==null) {
					println("Terminating track calculation failed");
					for (Track tr:tracks) {println(tr.endTimes()[0]+"-"+tr.endTimes()[1]);}
					println("Common time "+firstCommonTime+"-"+lastCommonTime+"; "+ (forward?"forward ":"backward ")+endTime+","+tStep);
					try {
						throw new Exception("Terminating track is null!"); //IllegalAccessException
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				ArrayList<Track> adTracks = continuingTrack.adjacentTracks();
				boolean addedNewTrack=false;
				for (Track tryTrack : adTracks) {
					if (tryTrack.nodesByTime.containsKey(endTime) || !tryTrack.nodesByTime.containsKey(endTime+tStep)) {continue;}
					double trialMatchDistAdjustment = mergedMatchDist(new Track[] {continuingTrack,terminatingTrack,tryTrack},endTime,endTime+tStep,logSizeWeight) 
							- mergedMatchDist(new Track[] {continuingTrack,terminatingTrack},endTime,endTime+tStep,logSizeWeight);
					if (trialMatchDistAdjustment>0) {continue;}
					double newMergeScoreSum=0;
					int ts = endTime+tStep;
					while (continuingTrack.nodesByTime.containsKey(ts) && tryTrack.nodesByTime.containsKey(ts) ) {
						
						newMergeScoreSum += nodeMergeScore(continuingTrack.nodesByTime.get(ts),tryTrack.nodesByTime.get(ts),
								relativeNodeContact_referenceValue,relativeNodeDistance_referenceValue,relativeNodeContact_weight,relativeNodeDistance_weight)-1;
						ts+=tStep;
					}
					if (newMergeScoreSum<0) {continue;}
					
					double newEndTrialMatchDist = mergedMatchDist(new Track[] {continuingTrack,tryTrack},ts,ts-tStep,logSizeWeight) ;
					double newEndCurrentMatchDist = mergedMatchDist(new Track[] {continuingTrack},ts,ts-tStep,logSizeWeight) + mergedMatchDist(new Track[] {tryTrack},ts,ts-tStep,logSizeWeight) ;
					// if (newEndTrialMatchDist > matchThreshold) {continue;}
					// Finally ready to commit to tryTrack
					tracks.add(tryTrack);
					
					mergeScoreTotal += newMergeScoreSum;
					println("Added merge from "+endTime+" to " + ts+";"+tStep);
					if (forward) {
						println("lastCommonTime: " + lastCommonTime + "->" + (ts-1));
						lastCommonTime = ts - 1;
						mergeMatch2 += trialMatchDistAdjustment + newEndTrialMatchDist;
						existingMatchDist2 += newEndCurrentMatchDist;
					} else {
						println("firstCommonTime: " + firstCommonTime + "->" + (ts+1));
						firstCommonTime = ts +1;
						mergeMatch1 += trialMatchDistAdjustment + newEndTrialMatchDist;
						existingMatchDist1 += newEndCurrentMatchDist;
					}
					finalScore = mergeScoreTotal -  matchScoreWeighting*(mergeMatch1 - existingMatchDist1 + mergeMatch2 - existingMatchDist2);
					addedNewTrack=true;
					break;
				}
				if (!addedNewTrack) {break;}
			}
		}
		
		
		if (finalScore==Double.POSITIVE_INFINITY) {
			for (Track tr : tracks)
				tr.printNodeTable();
			if (haltOnInfiniteScore) {
			try {
				
				throw new IllegalAccessException("trackMergeScore gave an infinite value! Tracks equal: "+ (tracks.get(0)==tracks.get(1))); //  nds1.size()+","+nds2.size()+", matched pair " + x + ","+y+"!");
            } catch (IllegalAccessException ex) {
                System.err.println(ex);
                System.exit(1);
            }
			} else {
				println("trackMergeScore gave an infinite value! Tracks equal: "+ (tracks.get(0)==tracks.get(1)));
			}
		}
		return new double[] {finalScore,mergeScoreTotal};
	}
	
	// get an overall score for merging 2 tracks by combining nodeMergeScore results on common times with change to match distance penalties
		// 0 = neutral, higher is evidence for merging
		public static double[] trackMergeScoreOld(Track tr1, Track tr2, double relativeNodeContact_referenceValue, double relativeNodeDistance_referenceValue, double relativeNodeContact_weight, double relativeNodeDistance_weight,
				double logSizeWeight, double matchThreshold, double matchScoreWeighting) {
			//int commonTimeStepCount=0; // needed?
			double mergeScoreTotal=0;
			int firstCommonTime = Integer.MAX_VALUE;
			int lastCommonTime = Integer.MIN_VALUE;
			for (int ts : tr1.times()) {
				if (!tr2.nodesByTime.containsKey(ts)) {continue;}
				//commonTimeStepCount++;
				mergeScoreTotal+=nodeMergeScore(tr1.nodesByTime.get(ts),tr2.nodesByTime.get(ts),relativeNodeContact_referenceValue,relativeNodeDistance_referenceValue,relativeNodeContact_weight,relativeNodeDistance_weight)-1;
				if (ts<firstCommonTime) {firstCommonTime=ts;}
				if (ts>lastCommonTime) {lastCommonTime=ts;}
				// test - temp
//				if (ts==204) {
//					System.out.println(" dist/contact " +  relativeNodeCloseness(tr1.nodesByTime.get(ts),tr2.nodesByTime.get(ts)) +"/"+ relativeNodeContact(tr1.nodesByTime.get(ts),tr2.nodesByTime.get(ts)) + 
//							"; ref values " + relativeNodeDistance_referenceValue + "/"+relativeNodeContact_referenceValue + "; weights " + relativeNodeDistance_weight+"/"+relativeNodeContact_weight);
//				}
			}
			
			
			// now penalise score by change in total match distance, weighted by matchScoreWeighting; return -inf (always reject) if a new match exceeds threshold
			double mergeMatch1 = mergedMatchDist(tr1,tr2,firstCommonTime-1,firstCommonTime,logSizeWeight);
			double mergeMatch2 = mergedMatchDist(tr1,tr2,lastCommonTime,lastCommonTime+1,logSizeWeight);
			if (mergeMatch1>matchThreshold || mergeMatch2>matchThreshold ) {return new double[] {Double.NEGATIVE_INFINITY,mergeScoreTotal};}
			double existingMatchDists = 0;
			for (Track tr : new Track[] {tr1,tr2}) {
				Node eNode = tr.nodesByTime.get(firstCommonTime);
				if (eNode.preds.size()>0) {existingMatchDists+=eNode.nextNodeDist.get(eNode.preds.get(0));}
				eNode = tr.nodesByTime.get(lastCommonTime);
				if (eNode.succs.size()>0) {existingMatchDists+=eNode.nextNodeDist.get(eNode.succs.get(0));}
			}
			
			
			double finalScore = mergeScoreTotal -  matchScoreWeighting*(mergeMatch1 + mergeMatch2 - existingMatchDists);
			// ArrayList<Integer> tr1_ts = tr1.times(); ArrayList<Integer> tr2_ts = tr2.times();
			//System.out.println("track intervals " + tr1_ts.get(0) +" - " + tr1_ts.get(tr1_ts.size()-1)  + ", "+ tr2_ts.get(0) +" - " + tr2_ts.get(tr2_ts.size()-1));
			//System.out.println(mergeScoreTotal+" - "+ matchScoreWeighting +"*( "+ mergeMatch1+"+"+mergeMatch2 +"-"+ existingMatchDists+") = "+  (mergeScoreTotal -  matchScoreWeighting*(mergeMatch1 + mergeMatch2 - existingMatchDists))  );		
			if (finalScore==Double.POSITIVE_INFINITY) {
				try {
					tr1.printNodeTable();
					tr2.printNodeTable();
					throw new IllegalAccessException("trackMergeScore gave an infinite value! Tracks equal: "+ (tr1==tr2)); //  nds1.size()+","+nds2.size()+", matched pair " + x + ","+y+"!");
	            } catch (IllegalAccessException ex) {
	                System.err.println(ex);
	                System.exit(1);
	            }
			}
			return new double[] {finalScore,mergeScoreTotal};
		}
	
		// similar to mergedMatchDist, but specific setup, tracks are not merged at firstTimeApart even if both still exists
	private static double matchDistAdjustment(Track tr1, Track tr2, int lastMergeTime, int firstTimeApart, double logSizeWeight, double matchDistanceMaximum) {
			
		Node mergedNode = new Node(tr1.nodesByTime.get(lastMergeTime),tr2.nodesByTime.get(lastMergeTime));
		double currentMatchDistance=0;
		double bestNewMergeDistance=matchDistanceMaximum;	
		int continuingTrackCount=0;
		for (Track tr : new Track[] {tr1,tr2}) {
			if (tr.nodesByTime.containsKey(firstTimeApart)) {
				continuingTrackCount++;
				currentMatchDistance+=matchDistance(tr.nodesByTime.get(lastMergeTime),tr.nodesByTime.get(firstTimeApart),logSizeWeight);
				double newMatchDist = matchDistance(mergedNode,tr.nodesByTime.get(firstTimeApart),logSizeWeight);
				if (newMatchDist<bestNewMergeDistance) {bestNewMergeDistance=newMatchDist;}
			}
		}
		double newMatchDistance =0;
		if (continuingTrackCount>0) {newMatchDistance+=bestNewMergeDistance;}
		if (continuingTrackCount>1) {newMatchDistance+=matchDistanceMaximum;}
		return newMatchDistance - currentMatchDistance;
	}
//	private static double matchDistAdjustment(Track continuingTrack, Track tr1, Track tr2, int t1, int t2, double logSizeWeight, double matchDistanceMaximum) {
//		return matchDistance(new Node(continuingTrack.nodesByTime.get(t1),tr1.nodesByTime.get(t1)),new Node(continuingTrack.nodesByTime.get(t2),tr2.nodesByTime.get(t2)),logSizeWeight) - 
//				matchDistance(continuingTrack.nodesByTime.get(t1),continuingTrack.nodesByTime.get(t2),logSizeWeight);
//	}
	
		
	// depracate mergedMatchDist - trying for clearer approach
	// note that mergedMatchDist doesn't not have matchDistanceMaximum 
	private static double mergedMatchDist(Track tr1, Track tr2,int t1, int t2, double logSizeWeight) {
		Node[] ndPr = new Node[] {null,null};
		int[] tms = new int[] {t1,t2};
		for (int i=0;i<2;i++) {
			if (tr1.nodesByTime.containsKey(tms[i])) {ndPr[i]=tr1.nodesByTime.get(tms[i]);}
			if (tr2.nodesByTime.containsKey(tms[i])) {
				ndPr[i]=ndPr[i]==null ? tr2.nodesByTime.get(tms[i]) :  new Node(ndPr[i],tr2.nodesByTime.get(tms[i]));
			}
		}
		if (ndPr[0]==null || ndPr[1]==null) {return  0.0;}
		return matchDistance(ndPr[0],ndPr[1],logSizeWeight);	
	}
	
	private static double mergedMatchDist(Track[] tracks,int t1, int t2, double logSizeWeight) {
		Node[] ndPr = new Node[] {null,null};
		int[] tms = new int[] {t1,t2};
		for (int i=0;i<2;i++) {
			for (Track tr : tracks) {
				if (tr==null) {
					println("Track is null! Number tracks in set is "+tracks.length);
				}
				if (tr.nodesByTime.containsKey(tms[i])) {
					ndPr[i]=ndPr[i]==null ? tr.nodesByTime.get(tms[i]) :  new Node(ndPr[i],tr.nodesByTime.get(tms[i]));
				}
			}
		}
		if (ndPr[0]==null || ndPr[1]==null) {return  0.0;}
		return matchDistance(ndPr[0],ndPr[1],logSizeWeight);
	}
	
	
	// method supports node merging on common timesteps, or just lengthwise joining
	//	although I return a new track, this is a destructive process: first track has nodes merged (if times overlap), and adjacency information changed in other nodes as part of this node merge
	// so not suitable for backtracking (need to rethink a little if I want to do that)
	// note that preds/succs info in each node is wiped, replaced by pred/succ node in track
	static Track addTracks(Track tr1, Track tr2,double logSizeWeight) {
		Track newTrack = new Track();
		newTrack.classId=tr1.classId;
		HashSet<Integer> tsUnordered = new HashSet<Integer>(tr1.nodesByTime.keySet());
		tsUnordered.addAll(tr2.nodesByTime.keySet());
		ArrayList<Integer> timeSteps = new ArrayList<Integer>(tsUnordered);
		//ArrayList<Integer> timeSteps = new ArrayList<Integer>(tr1.nodesByTime.keySet());
		//timeSteps.addAll(tr2.nodesByTime.keySet());
		Collections.sort(timeSteps);
		Node priorNode=null;
		for (int ts : timeSteps) {
			Node nd = tr1.nodesByTime.containsKey(ts) ? tr1.nodesByTime.get(ts) : null;
			if (tr2.nodesByTime.containsKey(ts)) {
				if (nd==null) {
					nd=tr2.nodesByTime.get(ts);
					} else {
						nd.mergeNode(tr2.nodesByTime.get(ts));
					}
			}
			newTrack.addNode(nd);
			nd.preds=new ArrayList<Node>(); 
			nd.succs=new ArrayList<Node>(); 
			nd.nextNodeDist = new HashMap<Node,Double>();
			if (priorNode!=null) {
				linkMatchedNodes(priorNode,nd,matchDistance(nd,priorNode,logSizeWeight));
			}
			priorNode=nd;
		}
		return(newTrack);
	}
	
	static Track addTracks(ArrayList<Track> tracks,double logSizeWeight) {
		Track newTrack = new Track();
		newTrack.classId=tracks.get(0).classId;
		HashSet<Integer> tsUnordered = new HashSet<Integer>();
		for (Track tr : tracks) {
			tsUnordered.addAll(tr.nodesByTime.keySet());
		}
		ArrayList<Integer> timeSteps = new ArrayList<Integer>(tsUnordered);
		Collections.sort(timeSteps);
		Node priorNode=null;
		for (int ts : timeSteps) {
			Node nd = null;
			for (Track tr:tracks) {
				if (tr.nodesByTime.containsKey(ts)) {
					if (nd==null) {
						nd=tr.nodesByTime.get(ts);
					} else {
						nd.mergeNode(tr.nodesByTime.get(ts));
					}
				}
			}
			newTrack.addNode(nd);
			nd.preds=new ArrayList<Node>(); 
			nd.succs=new ArrayList<Node>(); 
			nd.nextNodeDist = new HashMap<Node,Double>();
			if (priorNode!=null && priorNode.timeStep == nd.timeStep-1) {
				linkMatchedNodes(priorNode,nd,matchDistance(nd,priorNode,logSizeWeight));
			}
			priorNode=nd;
		}
		return(newTrack);
	}
	
	
	static Track[] splitTrack(Track tr, int endTimeTrack1) {
		Node nd1 = tr.nodesByTime.get(endTimeTrack1);
		Node nd2 = tr.nodesByTime.get(endTimeTrack1+1);
		nd1.succs = new ArrayList<Node>();
		nd2.preds = new ArrayList<Node>();
		nd1.nextNodeDist.remove(nd2);
		nd2.nextNodeDist.remove(nd1);
		Track[] nt = new Track[] {new Track(),new Track()};
		nt[0].classId = tr.classId;
		nt[1].classId = tr.classId;
		for (int ts : tr.times()) {
			if (ts<=endTimeTrack1) {
				nt[0].addNode(tr.nodesByTime.get(ts));
			} else {
				nt[1].addNode(tr.nodesByTime.get(ts));
			}
		}
		return nt;
	}
	
}


