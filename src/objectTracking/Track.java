package objectTracking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Represents a simple track consisting of one Node per time.
 * (although Nodes have capacity to record more than one pre/succ)
 */
public class Track {
	int id=-1;
	int classId=-1;
	HashMap<Integer,Node> nodesByTime = new HashMap<Integer,Node>();
	ArrayList<TrackSetMergeScore> mergeScores = new ArrayList<TrackSetMergeScore>(); // TrackPairMergeScore
	Track(){}
	Track(Node node){
		classId=node.classId;
		addNode(node);
	}
	ArrayList<Integer> times(){
		ArrayList<Integer> ts = new ArrayList<Integer>(nodesByTime.keySet());
		Collections.sort(ts);
		return(ts);
	}
	int[] endTimes() {
		int[] ends = new int[] {Integer.MAX_VALUE,Integer.MIN_VALUE};
		for (int ts : nodesByTime.keySet()) {
			if (ts<ends[0]) {ends[0]=ts;}
			if (ts>ends[1]) {ends[1]=ts;}
		}
		return(ends);
	}
	ArrayList<Node> nodes(){
		return(new ArrayList<Node>(nodesByTime.values()));		
	}
	
	void addNode(Node node) {
		if (nodesByTime.containsKey(node.timeStep)) {
			try {
                throw new IllegalAccessException("Track already contains node at time "+node.timeStep+"!");
            } catch (IllegalAccessException ex) {
                System.err.println(ex);
                System.exit(1);
            }
		} else {
			nodesByTime.put(node.timeStep,node);
		}
		node.track=this;
	}
	
	ArrayList<Track> adjacentTracks(){
		ArrayList<Track> ad = new ArrayList<Track>();
		for (Node nd : nodesByTime.values()) {
			for (Node adNode : nd.adjacencies.keySet()) {
				if (adNode.track != null && !ad.contains(adNode.track) && adNode.track!=this) { // 
					ad.add(adNode.track);
				}
			}
		}
		return(ad);
	}
	
	void printNodeTable() {
		System.out.println(Node.tableHeader());
		for (int ts : times()) {
			System.out.println(nodesByTime.get(ts).toTableRow());
		}
	}

}

/**
 * Used to record the results after scoring 
 * a possible merging of two tracks.
 * Keeps tabs on the two tracks plus any other tracks 
 * that got pulled in, any cuts etc, as well as the calculated score.
 */
class TrackSetMergeScore{
	double score;
	int firstMergeTime;
	int lastMergeTime;
	ArrayList<Track> tracks; // first 2 tracks are the first 2 considered (useful to avoid redoing calculations)
	ArrayList<Track> tracksCutAtStart = new ArrayList<Track>();
	ArrayList<Track> tracksCutAtEnd = new ArrayList<Track>();
	
	TrackSetMergeScore(double score_,int firstMergeTime_,int lastMergeTime_,ArrayList<Track> tracks_, ArrayList<Track> tracksCutAtStart_, ArrayList<Track> tracksCutAtEnd_){
		score=score_;
		firstMergeTime=firstMergeTime_;
		lastMergeTime=lastMergeTime_;
		tracks=tracks_;
		tracksCutAtStart=tracksCutAtStart_;
		tracksCutAtEnd=tracksCutAtEnd_;
	}
}

