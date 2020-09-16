package objectTracking;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static segImAnalysis.Util.println;


public class TrackingMethods {
	
	public static ArrayList<Track> generateTracksSingleClassAndInterval(
			HashMap<Integer,ArrayList<Node>> nodesByTime, // selected class only; remaining key is timeStep
			HashMap<Integer,int[][]> adjacencyData,
			double voxelVolume,
			int classNum,
			int[] timeSteps,
			double logSizeWeight,
			double matchThreshold,
			double relativeNodeContact_referenceValue, 
			double relativeNodeDistance_referenceValue, 
			double relativeNodeContact_weight, 
			//double relativeNodeDistance_weight,
			double matchScoreWeighting,
			boolean verbose
			){
		
		Arrays.sort(timeSteps);
		
		// match to make first version of tracks
				
		ArrayList<Track> tracks = new ArrayList<Track>();
		for (Node nd : nodesByTime.get(timeSteps[0])) {
			tracks.add(new Track(nd));
		}
		
		println("Initial matching: " + new Timestamp(System.currentTimeMillis()));
		for (int tsNum=1;tsNum < timeSteps.length;tsNum++) {
			println(" t " + timeSteps[tsNum] + " " + new Timestamp(System.currentTimeMillis()));
			TrackAndNodeOperations.matchNodes(nodesByTime.get(timeSteps[tsNum-1]),nodesByTime.get(timeSteps[tsNum]),logSizeWeight,matchThreshold,verbose);
			for (Node nd : nodesByTime.get(timeSteps[tsNum])) {
				if (nd.preds.size()==0) {
					tracks.add(new Track(nd));
				} else {
					nd.preds.get(0).track.addNode(nd);
				}
			}
		}
		// create lookups for track start and end nodes by timeStep
		HashMap<Integer,ArrayList<Node>> startNodesByTime = new HashMap<Integer,ArrayList<Node>>();
		HashMap<Integer,ArrayList<Node>> endNodesByTime = new HashMap<Integer,ArrayList<Node>>();
		for (Track tr : tracks) {
			int[] startEnd = tr.endTimes();
			if (!startNodesByTime.containsKey(startEnd[0])) {startNodesByTime.put(startEnd[0], new ArrayList<Node>());}
			if (!endNodesByTime.containsKey(startEnd[1])) {endNodesByTime.put(startEnd[1], new ArrayList<Node>());}
			startNodesByTime.get(startEnd[0]).add(tr.nodesByTime.get(startEnd[0]));
			endNodesByTime.get(startEnd[1]).add(tr.nodesByTime.get(startEnd[1]));
		}
		println("Add adjacencies " + new Timestamp(System.currentTimeMillis()));
		TrackingMethods.addAdjacenciesToTrackNodes(tracks,adjacencyData,timeSteps); // this only adds adjacencies to nodes within other members of tracks
		println("Get initial merge scores " + new Timestamp(System.currentTimeMillis()));
		ArrayList<TrackSetMergeScore>  trackMergeScores = getAllTrackMergeScores(tracks,relativeNodeContact_referenceValue, relativeNodeDistance_referenceValue, relativeNodeContact_weight,
				logSizeWeight, matchThreshold, matchScoreWeighting, 0);
		println("completed initial merge score calculations: commence merging");
		//double maxScore = trackMergeScores.get(0).score;
		int mergeCount=0;
		int extendedMergeCount=0;
		int truncatedMergeCount=0;
		int doubleTruncatedMergeCount=0;
		int trackCutCount=0;
		int followOnMatchCount=0;
		while (trackMergeScores.size()>0) {
			TrackSetMergeScore topMerge = trackMergeScores.get(0);
			for (TrackSetMergeScore mg : trackMergeScores) {
				if (mg.score>topMerge.score) {
					topMerge=mg;
				}
			}
			println("Best merge score "+topMerge.score);
			if (topMerge.score<0) {
				println("done merging");
				break;
			}
			mergeCount++;
			// check if truncated merge, just for the count
			int preTracks=0;
			int postTracks=0;
			for (Track tr:topMerge.tracks) {
				if (tr.nodesByTime.containsKey(topMerge.firstMergeTime-1)) {preTracks++;}
				if (tr.nodesByTime.containsKey(topMerge.lastMergeTime+1)) {postTracks++;}
			}
			if (preTracks==2 || postTracks==2) {truncatedMergeCount++;}
			if (preTracks==2 && postTracks==2) {doubleTruncatedMergeCount++;}
			// we now execute the merge described by topMerge
			// keep track of new and discarded tracks so that we can clean up at end in one step - 
			//   track list, merge scores (master list and within tracks), and start/end time lookups
						
			ArrayList<Track> newTracks = new ArrayList<Track>();
			ArrayList<Track> discardTracks = new ArrayList<Track>();
			
			// first step is to cut any tracks that extend beyond the merge period
			for (Track tr : topMerge.tracksCutAtEnd) {
				Track[] splitTracks = TrackAndNodeOperations.splitTrack(tr,topMerge.lastMergeTime);
				newTracks.add(splitTracks[1]);
				discardTracks.add(tr);
				topMerge.tracks.remove(tr);
				topMerge.tracks.add(splitTracks[0]);
				if (topMerge.tracksCutAtStart.contains(tr)) {
					topMerge.tracksCutAtStart.remove(tr);
					topMerge.tracksCutAtStart.add(splitTracks[0]);
				}
				trackCutCount++;
				println("split track at end of merge period: "+tr.endTimes()[0]+"-"+tr.endTimes()[1]+" -> "+splitTracks[0].endTimes()[0]+"-"+splitTracks[0].endTimes()[1]+" + "+splitTracks[1].endTimes()[0]+"-"+splitTracks[1].endTimes()[1]);
			}
			for (Track tr : topMerge.tracksCutAtStart) {
				Track[] splitTracks = TrackAndNodeOperations.splitTrack(tr,topMerge.firstMergeTime-1);
				newTracks.add(splitTracks[0]);
				discardTracks.add(tr);
				topMerge.tracks.remove(tr);
				topMerge.tracks.add(splitTracks[1]);
				trackCutCount++;
				println("split track at start of merge period: "+tr.endTimes()[0]+"-"+tr.endTimes()[1]+" -> "+splitTracks[0].endTimes()[0]+"-"+splitTracks[0].endTimes()[1]+" + "+splitTracks[1].endTimes()[0]+"-"+splitTracks[1].endTimes()[1]);
			}
			
			if (topMerge.tracks.size()>2) {
				println("Executing merge of " + topMerge.tracks.size() + " tracks");
				for (Track tr : topMerge.tracks) {
					println(tr.endTimes()[0] + " - " + tr.endTimes()[1]);
				}
				extendedMergeCount++;
			}
			// ready for main track merge - not necessary to specify start end end times since tracks have been trimmed
			Track newTrack = TrackAndNodeOperations.addTracks(topMerge.tracks,logSizeWeight);
			discardTracks.addAll(topMerge.tracks);
				
			// now check if new track can be extended by matching to tracks before or after
			for (boolean forward : new boolean[] {false,true}) {
				while(true) {
					int endTime = forward ? newTrack.endTimes()[1] : newTrack.endTimes()[0];
					Node endNode = newTrack.nodesByTime.get(endTime);
					Node bestMatchNode = null;
					double bestMatchDist = Double.NaN;
					for (Node matchNode : (forward ? 
							(startNodesByTime.containsKey(endTime+1) ? startNodesByTime.get(endTime+1) : new ArrayList<Node>()) : 
								(endNodesByTime.containsKey(endTime-1) ? endNodesByTime.get(endTime-1) : new ArrayList<Node>()) )){
						double dist = TrackAndNodeOperations.matchDistance(endNode, matchNode, logSizeWeight);
						if (bestMatchNode==null && dist<matchThreshold || bestMatchNode!=null && dist<bestMatchDist) {
							bestMatchNode = matchNode;
							bestMatchDist=dist;
						}
					}
					if (bestMatchNode==null) {
						break;
					} else {
						discardTracks.add(newTrack);
						discardTracks.add(bestMatchNode.track);
						newTrack = TrackAndNodeOperations.addTracks(newTrack,bestMatchNode.track,logSizeWeight);
						// tracks.add(newTrack);
						followOnMatchCount++;
					}
				}
			}
			
			//debug
//			if (newTrack.nodesByTime.containsKey(14) && newTrack.nodesByTime.get(14).objectIds.contains(590)) {
//				println("New track times " + newTrack.endTimes()[0] + "-" + newTrack.endTimes()[1] + "; combining tracks:");
//				for (Track tr : discardTracks) {
//					println("  " + tr.endTimes()[0] + "-" + tr.endTimes()[1]);
//				}
//			}

			// clean up old (merged) tracks:
			for (Track tr : discardTracks) {
				ArrayList<TrackSetMergeScore> trMergeScores = new ArrayList<TrackSetMergeScore>();
				trMergeScores.addAll(tr.mergeScores);
				for (TrackSetMergeScore tsms : trMergeScores) {
					trackMergeScores.remove(tsms);
					for (Track tr2 : tsms.tracks) {
						tr2.mergeScores.remove(tsms);
					}
				}
				int[] startEnd = tr.endTimes();
				// if (startNodesByTime.get(startEnd[0])==null) {println(startEnd[0]);}
				if (!startNodesByTime.containsKey(startEnd[0])) {startNodesByTime.put(startEnd[0], new ArrayList<Node>());}
				if (!endNodesByTime.containsKey(startEnd[1])) {endNodesByTime.put(startEnd[1], new ArrayList<Node>());}
				startNodesByTime.get(startEnd[0]).remove(tr.nodesByTime.get(startEnd[0]));
				endNodesByTime.get(startEnd[1]).remove(tr.nodesByTime.get(startEnd[1]));
				tracks.remove(tr);
			}
			// recalculate merge scores for new tracks, add start and end nodes to lookups
			newTracks.add(newTrack);
			for (Track tr : newTracks) {
				tracks.add(tr);
				trackMergeScores.addAll(getTrackSetMergeScores(tr,relativeNodeContact_referenceValue, relativeNodeDistance_referenceValue, relativeNodeContact_weight,
						logSizeWeight, matchThreshold, matchScoreWeighting, 0, false));
				int[] startEnd = tr.endTimes();
				if (!startNodesByTime.containsKey(startEnd[0])) {startNodesByTime.put(startEnd[0], new ArrayList<Node>());}
				if (!endNodesByTime.containsKey(startEnd[1])) {endNodesByTime.put(startEnd[1], new ArrayList<Node>());}
				startNodesByTime.get(startEnd[0]).add(tr.nodesByTime.get(startEnd[0]));
				endNodesByTime.get(startEnd[1]).add(tr.nodesByTime.get(startEnd[1]));
			}
			
			
		}	
		println("Finished track merging for class "+classNum+": "+mergeCount+" merges, "+ extendedMergeCount+ " of more than 2 tracks; "+truncatedMergeCount + " merges truncated, "+doubleTruncatedMergeCount+" at both ends");
		println("  "+trackCutCount+" tracks cut in 2 and "+ followOnMatchCount+ " extensions of merged tracks by matching with tracks before and after");
		
		return(tracks);
	}
	
	// assumes all adjacencies are in same class, i.e. allowed merges
	
	// merge scores are added to adjacent tracks also; return list contains new merge scores only
		public static ArrayList<TrackSetMergeScore> getTrackSetMergeScores(Track tr,
				double relativeNodeContact_referenceValue, double relativeNodeDistance_referenceValue, double relativeNodeContact_weight,
				double logSizeWeight, double matchThreshold, double matchScoreWeighting,double minMergeScore, boolean verbose){
			ArrayList<TrackSetMergeScore> mergeScores = new ArrayList<TrackSetMergeScore>();
			ArrayList<Track> adTracks = tr.adjacentTracks();
			if (verbose) {
				println("Finding adjacent track merge scores for track "+tr.id);
			}
			for (Track adTr : adTracks) {
				if (verbose) {println("Calculating merge score with "+adTr.id);}
				// check if comparison already done
				boolean done=false;
				for (TrackSetMergeScore ms : tr.mergeScores) {
					if (ms.tracks.get(0)==adTr && ms.tracks.get(1)==tr){ // checking explicitly for this merge being calculated from the opposite direction
						if (verbose) {
							println("Score already calculated, but redoing; current value " + ms.score);
						} else {
							done=true;
						}
					}
				}
				if (!done) {
					ArrayList<Track> mergeTracks = new ArrayList<Track>();
					mergeTracks.add(tr); mergeTracks.add(adTr);
					TrackSetMergeScore ms = TrackAndNodeOperations.trackMergeScore(mergeTracks,relativeNodeContact_referenceValue, relativeNodeDistance_referenceValue, relativeNodeContact_weight, 1-relativeNodeContact_weight,
							logSizeWeight, matchThreshold, matchScoreWeighting,verbose,!verbose);
//					TrackSetMergeScore ms = new TrackSetMergeScore(
//							TrackAndNodeOperations.trackMergeScore(mergeTracks,relativeNodeContact_referenceValue, relativeNodeDistance_referenceValue, relativeNodeContact_weight, 1-relativeNodeContact_weight,
//									logSizeWeight, matchThreshold, matchScoreWeighting,!verbose)
//							,mergeTracks);
					for (Track mTr : mergeTracks) {
						mTr.mergeScores.add(ms);
					}
					if (ms.score>=minMergeScore) {mergeScores.add(ms);}
				}
			}		
			return mergeScores;
		}
		
		public static ArrayList<TrackSetMergeScore> getAllTrackMergeScores(
				ArrayList<Track> tracks,
				double relativeNodeContact_referenceValue, double relativeNodeDistance_referenceValue, double relativeNodeContact_weight,
				double logSizeWeight, double matchThreshold, double matchScoreWeighting, double minMergeScore){
			ArrayList<TrackSetMergeScore> mergeScores = new ArrayList<TrackSetMergeScore>();
			for (Track tr : tracks) {
				ArrayList<TrackSetMergeScore> mergeScoresNew = getTrackSetMergeScores(tr,relativeNodeContact_referenceValue,relativeNodeDistance_referenceValue,relativeNodeContact_weight,logSizeWeight,matchThreshold,matchScoreWeighting,minMergeScore,false);
				mergeScores.addAll(mergeScoresNew);
			}		
			return mergeScores;
		}

	public static void addAdjacenciesToTrackNodes(
			ArrayList<Track> classTracks, 
			HashMap<Integer,int[][]> allClassAdjacencies,
			int[] timeSteps) {
		for (int ts : timeSteps) {
			int[][] ad = allClassAdjacencies.get(ts);
			// set up mapping from object id to TrackNode, and reset adjacencies for each TrackNode
						HashMap<Integer,Node> objectNodeMap = new HashMap<Integer,Node>();
						for (Track tr : classTracks) {
								if (!tr.nodesByTime.containsKey(ts)) {continue;}
								Node tn = tr.nodesByTime.get(ts);
									for (int id : tn.objectIds) {
										objectNodeMap.put(id, tn);
									}
									tn.adjacencies = new HashMap<Node,Integer>();						
						}
						for (int[] oa : ad) {
							if (objectNodeMap.containsKey(oa[0]) && objectNodeMap.containsKey(oa[1])) {
								// println("Adjacency: " + oa[0] + ", " + oa[1] + ", " + oa[2]);
								Node tn1 = objectNodeMap.get(oa[0]);
								Node tn2 = objectNodeMap.get(oa[1]);
								if (tn1!=tn2) {
									TrackAndNodeOperations.addNodeAdjacency(tn1, tn2, oa[2]);
								}
							} 
						}
		}
	}
	
	public static void addAdjacenciesToTrackNodesMultiClass(
			HashMap<Integer, ArrayList<Track>> tracks, 
			HashMap<Integer,int[][]> allClassAdjacencies,
			int[] timeSteps,
			int[] trackedClasses) {
		ArrayList<Track> allTracks = new ArrayList<Track>();
		for (int classNum : trackedClasses) {
			allTracks.addAll(tracks.get(classNum));
		}
		addAdjacenciesToTrackNodes(allTracks,allClassAdjacencies,timeSteps);
	}
	
//	public static void addAdjacenciesToTrackNodes(
//			HashMap<Integer, ArrayList<Track>> tracks, 
//			HashMap<Integer,int[][]> allClassAdjacencies,
//			int[] timeSteps,
//			int[] trackedClasses) {
//		for (int ts : timeSteps) {
//			// println("Calculate node adjacencies at time step " + ts);
//			int[][] ad = allClassAdjacencies.get(ts);
//			
//			// set up mapping from object id to TrackNode, and reset adjacencies for each TrackNode
//			HashMap<Integer,Node> objectNodeMap = new HashMap<Integer,Node>();
//			for (int classNum : trackedClasses) {
//				for (Track tr : tracks.get(classNum)) {
//					if (!tr.nodesByTime.containsKey(ts)) {continue;}
//					//for (Node tn : tr.nodesByTime.get(ts)) {
//					Node tn = tr.nodesByTime.get(ts);
//						for (int id : tn.objectIds) {
//							objectNodeMap.put(id, tn);
//						}
//						tn.adjacencies = new HashMap<Node,Integer>();
//					//}
//				}
//			}
//			for (int[] oa : ad) {
//				if (objectNodeMap.containsKey(oa[0]) && objectNodeMap.containsKey(oa[1])) {
//					// println("Adjacency: " + oa[0] + ", " + oa[1] + ", " + oa[2]);
//					Node tn1 = objectNodeMap.get(oa[0]);
//					Node tn2 = objectNodeMap.get(oa[1]);
//					if (tn1!=tn2) {
//						TrackAndNodeOperations.addNodeAdjacency(tn1, tn2, oa[2]);
//					}
//				} 
//			}
//		}
//	}
	
	static void printTrackSummary(HashMap<Integer, ArrayList<Track>> tracks) {
		for (int classNum : tracks.keySet()) {
			println("Class " + classNum + " track summary:");
			int trackCount = tracks.get(classNum).size();
			int nodeCount=0;
			double distSum = 0;
			double distCount = 0;
			double maxDist=0;
			double minDist = Double.MAX_VALUE;
			HashMap<Integer,Integer> trackLengthTable = new HashMap<Integer,Integer>();
			for (Track tr : tracks.get(classNum)) {
				int l = tr.nodes().size();
				nodeCount+=l;
				int lengthCount = trackLengthTable.containsKey(l) ? trackLengthTable.get(l)+1 : 1;
				trackLengthTable.put(l,lengthCount);
				for (Node nd : tr.nodes()) {
					for (Node succ : nd.succs) { // expect 1 distance for each node except the last
						double dist = nd.nextNodeDist.get(succ);
						distCount++;
						distSum+=dist;
						maxDist = Math.max(maxDist, dist);
						minDist = Math.min(minDist, dist);
					}
				}
			}
			println("  "+nodeCount+" nodes in "+trackCount+" tracks: mean length "+(nodeCount/trackCount) );
			println("  min, mean, max matching distance " + minDist + ", "+(distSum/distCount)+", "+maxDist);
			// int[] lengths = trackLengthTable.keySet().toArray();
			for (int length : trackLengthTable.keySet()) {
				println("    " + length+": "+trackLengthTable.get(length));
			}
			
		}
	}
	
}



