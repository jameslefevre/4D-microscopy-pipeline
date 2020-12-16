package objectTracking;

import static segImAnalysis.Util.println;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
public class Tracking {

	// objectData structure is class:{timeStep:{id:{x,y,z,size}}}
	// allClassAdjacencies structure is timeStep:{[][id1,id2,connectionSize]} ; 
	
	
	//*** removePlateAdjacentObjectsParams; has not been implemented for rewrite of tracking code; following notes describe how it used to work (and might again)
	// except for 1st component (angle in degrees), must be in same units as fieldScaling
	//An array of parameters used to filter out objects prior to tracking based on a clipping plane which can move (but not rotate) at a fixed rate per time step
	// angle between x axis and true horizontal, estimated position of plate in transformed axis at time 0, change in posiiton each time step, then distance threshold for each tracked class
    // track level size filter code removed in commit "removed voxelThresholdsTracks from generateSaveTracks" (already disabled)
	
	
	public static void generateSaveTracks(
			String main_path,
			String save_path,
			
			int[] timeSteps_specified, // if null, uses all available
			int[] breakPoints, // null==none
			boolean useAlphabeticalPositionForStackNumber,
			String stackNumPrefix,
			String stackNumberSuffix,
			int[] trackedClasses,
			int[] voxelThresholds,
			double[] fieldScaling,
			double[] logSizeWeight,
			double[] matchThreshold,
			double[] relativeNodeContact_referenceValue,
			double[] relativeNodeDistance_referenceValue,
			double[] relativeNodeContact_weight, 
			double[] matchScoreWeighting,
			boolean verbose
			) throws IOException {
		// String[] field_names = {"x","y","z","voxels"};
		HashMap<Integer,HashMap<Integer,ArrayList<Node>>>  objectData = new HashMap<Integer,HashMap<Integer,ArrayList<Node>>> (); // keys class, timeStep
		HashMap<Integer,int[][]> allClassAdjacencies = new HashMap<Integer,int[][]>();
		int[] timeSteps = DataLoadingMethods.loadObjectData(main_path, true, trackedClasses,
				"objectStats.txt", "objectAdjacencyTable.txt",
				timeSteps_specified, useAlphabeticalPositionForStackNumber, stackNumPrefix, stackNumberSuffix, objectData, allClassAdjacencies);
		println("Data loaded");
		Arrays.sort(timeSteps);
		int timeBlockCount = breakPoints==null ? 1 : breakPoints.length + 1;
		
		// apply size filter and position scaling to object data
		// TODO? remove missing objects from allClassAdjacencies?
		
		for (int classIndex=0; classIndex<trackedClasses.length; classIndex++){
			int classnum = trackedClasses[classIndex];
			int voxThreshold = voxelThresholds[classIndex]; 
			for (int ts : objectData.get(classnum).keySet()) {
				ArrayList<Node> keptNodes = new ArrayList<Node>();
				for (Node tn : objectData.get(classnum).get(ts)) {
					if (tn.voxels < voxThreshold) {continue;}
					keptNodes.add(tn);
					tn.scaleSpatialCoords(fieldScaling);
				}
				objectData.get(classnum).put(ts,keptNodes);
			}
		}
		
		
		HashMap<Integer, ArrayList<Track>> tracks = new HashMap<Integer, ArrayList<Track>>();
		for (int classIndex=0; classIndex<trackedClasses.length; classIndex++) {		
			int classNum = trackedClasses[classIndex];
			println("Generating tracks for class " + classNum + " " + new Timestamp(System.currentTimeMillis()));
			
			ArrayList<Track> classTracks = new ArrayList<Track>();
			for (int timeBlock = 0; timeBlock<timeBlockCount ; timeBlock++) {
				int[] currentTimeSteps = timeSteps;
				if (timeBlock>0) {
					final int ind = timeBlock-1;
					currentTimeSteps = Arrays.stream(currentTimeSteps).filter(x -> x>breakPoints[ind]).toArray();
				}
				if (timeBlock<timeBlockCount-1) {
					final int ind = timeBlock;
					currentTimeSteps = Arrays.stream(currentTimeSteps).filter(x -> x<breakPoints[ind]).toArray();
				}
				Arrays.sort(currentTimeSteps);

				classTracks.addAll(TrackingMethods.generateTracksSingleClassAndInterval(
						objectData.get(classNum),
						allClassAdjacencies,
						fieldScaling[0] * fieldScaling[1] * fieldScaling[2],
						classNum,
						currentTimeSteps,
						logSizeWeight[classIndex],
						matchThreshold[classIndex], 
						relativeNodeContact_referenceValue[classIndex], 
						relativeNodeDistance_referenceValue[classIndex], 
						relativeNodeContact_weight[classIndex], 
						matchScoreWeighting[classIndex],
						verbose));
			}
			
			
			
			tracks.put(classNum, classTracks);
			
		}
		println("Track summaries before size filter");
		TrackingMethods.printTrackSummary(tracks);

		
		TrackingMethods.addAdjacenciesToTrackNodesMultiClass(tracks,allClassAdjacencies,timeSteps,trackedClasses); // overwrites same-class only adjacencies used deeper in code
		
		// assign global track,node ids, 
				int nextTrack=1;
				int nextNode=1;
				for (int classNum : trackedClasses) {
					for (Track tr : tracks.get(classNum)) {
						//tr.classId = classNum;
						tr.id=nextTrack++;
						for (Node nd : tr.nodes()) {
							nd.id=nextNode++;
						}
					}
				}
				
		// showNodePairComparison(601,901,tracks.get(1)); 
				
		// test				
		int[] trackIdsMergeCheck = new int[] {}; // 28842 , 28750
		for (int trackId : trackIdsMergeCheck) {
			Track tr = null;
			for (int classIndex=0; classIndex<trackedClasses.length; classIndex++) {		
				int classNum = trackedClasses[classIndex];
			
				if (tr!=null) {break;}
				for (Track tstTr : tracks.get(classNum)) {
					if (tstTr.id==trackId) {
						tr=tstTr;
						ArrayList<TrackSetMergeScore> trackSetMergeScores = TrackingMethods.getTrackSetMergeScores(tr,
								relativeNodeContact_referenceValue[classIndex],
								relativeNodeDistance_referenceValue[classIndex],
								relativeNodeContact_weight[classIndex],
								logSizeWeight[classIndex],
								matchThreshold[classIndex],
								matchScoreWeighting[classIndex],
								Double.NEGATIVE_INFINITY,
								true);
						
						for (TrackSetMergeScore tsms : trackSetMergeScores) {
							for (Track mtr : tsms.tracks) {
								System.out.print(mtr.id+" ");
							}
							println(tsms.score);
						}
						
						break;
					}
				}
			}
			if (tr==null) {
				println("Track "+trackId+" not found");
			}
		}
		
				
		// convert node positions back to original voxel scale (undo scaling factor applied at load); calculated distances will still reflect fieldScaling
				for (ArrayList<Track> trSet : tracks.values()) {
					for (Track tr : trSet) {
						for (Node tn : tr.nodes()) {
							for (int ii=0; ii<tn.position.length; ii++) {
								tn.position[ii] /= fieldScaling[ii];
							}
						}
					}
				}
				
		
		// save tracks
		PrintWriter out = new PrintWriter(save_path);
		out.println(Node.tableHeader());
		for (int classNum : trackedClasses) {
			for (Track tr : tracks.get(classNum)) {
				for (Node tn : tr.nodes()) {
					out.println(tn.toTableRow());
				}
			}
		}
		out.close();
	}
	
	// tests
	
	// 
	public static void showNodePairComparison(int id1, int id2, ArrayList<Track> classTracks) {
		Node nd1 = null;Node nd2 = null;
		for (Track tr : classTracks) { for (Node nd : tr.nodes()) {
			if (nd.id==id1) {nd1=nd;}
			if (nd.id==id2) {nd2=nd;}
		}}
		if (nd1!=null && nd2!=null) {
			println(nd1.shortDescription());
			println(nd2.shortDescription());
			println("distanceEuclidean " + TrackAndNodeOperations.distanceEuclidean(nd1,nd2));
			println("relativeNodeDistance " + TrackAndNodeOperations.relativeNodeCloseness(nd1,nd2));
			println("relativeNodeContact " + TrackAndNodeOperations.relativeNodeContact(nd1,nd2));
		} else {
			println("Could not locate both nodes");
		}
	}

}
