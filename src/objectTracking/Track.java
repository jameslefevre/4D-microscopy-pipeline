package objectTracking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class Track {
	// represents a simple class - one node per time - although nodes have capacity to record more than one pre/succ
	int id=-1;
	int classId=-1;
	//ArrayList<Node> nodes = new ArrayList<Node>();
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
	
	// depracated
	TrackSetMergeScore(double[] final_and_unadjusted_score_,ArrayList<Track> tracks_){
		score=final_and_unadjusted_score_[0];
		//unadjusted=final_and_unadjusted_score_[1];
		tracks=tracks_;
	}
}

// not sure if ordering works; sort was failing, then I realised it would be quicker just to run through and pick the max on each iteration of merging algorithm
//class TrackPairMergeScore implements Comparable<TrackPairMergeScore>{
//	double score;
//	double unadjusted;
//	Track track1;
//	Track track2;
//	TrackPairMergeScore(double[] final_and_unadjusted_score_,Track tr1,Track tr2){
//		score=final_and_unadjusted_score_[0];
//		unadjusted=final_and_unadjusted_score_[1];
//		track1=tr1;
//		track2=tr2;
//	}
//	@Override
//    public int compareTo(TrackPairMergeScore compareTo) {
//		// if (this.score==Double.POSITIVE_INFINITY)
//		if ((this.score - compareTo.score) < .00000000001)
//        return 0;
//    if (this.score > compareTo.score)
//        return -1;
//    else if (this.score < compareTo.score)
//        return 1;
//    return 0;
//    
//		//return (int) Math.ceil(compareTo.score - score);
//    }
//}
