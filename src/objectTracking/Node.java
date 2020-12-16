package objectTracking;

import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;
import ij.measure.ResultsTable;

public class Node {
	static int[] probClasses; // = new Integer[] {1,2,3}; // exclude 0=background
	static int[] distChannels; // = new Integer[] {1,3};
	int id = -1; // use -1 as unset
	int classId=-1;
	int timeStep;
	Track track = null;
	ArrayList<Integer> objectIds = new ArrayList<Integer>();
	double[] position = new double[3];
	double[] positionCov = new double[6]; // variance and covariance of voxel positions: vx,vy,vz,cxy,cxz,cyz
	int voxels;
	double signalIntensity;
	double channelMean;
	double[] classProbs = new double[probClasses.length]; // excludes class/channel 0 = background
	double voxelVolume=0;
	ArrayList<Node> preds = new ArrayList<Node>();
	ArrayList<Node> succs = new ArrayList<Node>();
	HashMap<Node,Double> nextNodeDist = new HashMap<Node,Double>();
	HashMap<Node,Integer> adjacencies = new HashMap<Node,Integer>();
	double[] distMean=new double[distChannels.length];
	double[] distVar=new double[distChannels.length];
	double[] distMin=new double[distChannels.length];
	double[] distMax=new double[distChannels.length];
	boolean branchOrEnd;
	
	double volume() {
		return voxels*voxelVolume;
	}
	
	public String shortDescription() {
		return "Node "+id+", timeStep "+timeStep+", track"+ (track==null?"none":track.id + " (class "+ track.classId + ")") + "; " + voxels + " voxels, volume " + volume() + 
				"; position "+position[0]+","+position[1]+","+position[2]+"; pred/succ/adj count " + preds.size() +","+ succs.size() +","+ adjacencies.size();
	}
	
	static void set_probClasses_distChannels(ResultsTable rt) {
		String[] fields = rt.getHeadings();
		ArrayList<Integer> pc = new ArrayList<Integer>();
		ArrayList<Integer> dc = new ArrayList<Integer>();
		for (String fld : fields) {
			if (fld.startsWith("prob")) {
				Integer ch = Integer.parseInt(fld.substring(4));
				if (!pc.contains(ch)) {pc.add(ch);}
			}
			if (fld.startsWith("dMin")) {
				Integer ch = Integer.parseInt(fld.substring(4));
				if (!dc.contains(ch)) {dc.add(ch);}
			}
		}
		probClasses = new int[pc.size()];
		for (int ii=0;ii<pc.size();ii++) {
			probClasses[ii]=pc.get(ii);
		}
		distChannels = new int[dc.size()];
		for (int ii=0;ii<dc.size();ii++) {
			distChannels[ii]=dc.get(ii);
		}
	}
	
	Node(){}
	
	/**
	 * Construct a Node from tabular data.
	 * 
	 * @param rt		A table of data in which each row contains the data for a node, except for time step (expects that the table corresponds to a single time step).
	 * @param rowNum	The row number to use to construct this node
	 * @param timeStep_ The time step for the node.
	 */
	Node(ResultsTable rt, int rowNum, int timeStep_){
		objectIds.add((int) Math.round(rt.getValue("id", rowNum)));		
		classId = (int) Math.round(rt.getValue("class", rowNum));
		timeStep = timeStep_;
		position = new double[] {rt.getValue("x", rowNum),rt.getValue("y", rowNum),rt.getValue("z", rowNum)};
		voxels = (int) Math.round(rt.getValue("voxels", rowNum));
		if (rt.columnExists("intensity")) {
			signalIntensity = rt.getValue("intensity", rowNum);
		}
		if (rt.columnExists("channel")) {
			channelMean = rt.getValue("channel", rowNum);
		}
		if (rt.columnExists("vx")) {positionCov[0] = rt.getValue("vx", rowNum);}
		if (rt.columnExists("vy")) {positionCov[1] = rt.getValue("vy", rowNum);}
		if (rt.columnExists("vz")) {positionCov[2] = rt.getValue("vz", rowNum);}
		if (rt.columnExists("cxy")) {positionCov[3] = rt.getValue("cxy", rowNum);}
		if (rt.columnExists("cxz")) {positionCov[4] = rt.getValue("cxz", rowNum);}
		if (rt.columnExists("cyz")) {positionCov[5] = rt.getValue("cyz", rowNum);}
		
		for (int chIndex=0 ; chIndex<probClasses.length ; chIndex++) {
			String nm = "prob"+probClasses[chIndex];
			if (rt.columnExists(nm)) {
				classProbs[chIndex] = rt.getValue(nm, rowNum);
			}
		}
		for (int chIndex=0 ; chIndex<distChannels.length ; chIndex++) {
			String nm = "d"+distChannels[chIndex];
			if (rt.columnExists(nm)) {distMean[chIndex] = rt.getValue(nm, rowNum);}
			nm = "dv"+distChannels[chIndex];
			if (rt.columnExists(nm)) {distVar[chIndex] = rt.getValue(nm, rowNum);}
			nm = "dMin"+distChannels[chIndex];
			if (rt.columnExists(nm)) {distMin[chIndex] = rt.getValue(nm, rowNum);}
			nm = "dMax"+distChannels[chIndex];
			if (rt.columnExists(nm)) {distMax[chIndex] = rt.getValue(nm, rowNum);}
		}		
	}
	
	
	// 
	/**
	 * Create sum of 2 nodes with context stripped out and no side effects, used to check match score after possible merging; 
	 * not to be confused with mergeNode (permanent and with side effects)
	 * need to include any properties used to match nodes (and no others).
	 * 
	 * @param nd1
	 * @param nd2
	 */
	Node(Node nd1, Node nd2){
		voxels = nd1.voxels+nd2.voxels;	
		for (int i=0;i<3;i++) {
			position[i] = (nd1.position[i]*nd1.voxels+nd2.position[i]*nd2.voxels)/voxels;
		}
	}
	
	public void scaleSpatialCoords(double[] sf) {
		if (sf.length!=3) {
			try {
				throw new Exception("scaleSpatialCoords expects scaling factor vector of length 3 (x,y,z)");
			} catch (Exception ex) {
				System.err.println(ex);
				System.exit(1);
			}
		}
		for (int ii=0; ii<3; ii++) {
			position[ii] *= sf[ii];
			positionCov[ii] *= sf[ii]*sf[ii];
		}
		positionCov[3] = positionCov[3]*sf[0]*sf[1];
		positionCov[4] = positionCov[4]*sf[0]*sf[2];
		positionCov[5] = positionCov[5]*sf[1]*sf[2];
		voxelVolume = sf[0]*sf[1]*sf[2];
	}
	
	
	/**
	 * Permanently merge a Node into this one, with side effects: 
	 * Adjacency information is updated for adjacent Nodes
	 * Includes calculations for correctly combining various metrics.
	 * <p>
	 * succs, preds, nextNodeDist cannot be handled correctly without 
	 * additional information (logSizeWeight parameter, threshold, 
	 * possible consideration of other nodes), so these are not valid 
	 * for merged Node.
	 * 
	 * @param nd Node to be merged into this one.
	 */

	public void mergeNode(Node nd) {
		if (nd.timeStep != timeStep) {
			 try {
	                throw new IllegalAccessException("Nodes must have same time step to merge - have "+timeStep+", "+nd.timeStep);
	            } catch (IllegalAccessException ex) {
	                System.err.println(ex);
	                System.exit(1);
	            }
		}
		double v1=voxels; double v2=nd.voxels; double v=v1+v2;
		voxels=voxels+nd.voxels;
		double[] meanDiff = new double[3];
		for (int i=0;i<3;i++) {
			meanDiff[i] = position[i] - nd.position[i];
			position[i] = (position[i]*v1+nd.position[i]*v2)/v;
			double tmp=positionCov[i];
			positionCov[i] = (positionCov[i]*v1+nd.positionCov[i]*v2)/v +
					meanDiff[i]*meanDiff[i]*v1*v2/(v*v);
			if (positionCov[i]<0) {
				System.out.println("\nnegative variance!!!! "+i);
				for (double x : position) {System.out.println(" "+x);}
				for (double x : positionCov) {System.out.println(" "+x);}
				System.out.println("old value "+tmp);
				System.out.println( (tmp*v1+nd.positionCov[i]*v2)/v +", "+meanDiff[i]*meanDiff[i]*v1*v2/(v*v));
				System.out.println(meanDiff[i]+ ", "+nd.positionCov[i]+","+v1+","+v2+","+v+","+(v*v));
				System.out.println(1/0);
			}
		}
		positionCov[3] = (positionCov[3]*v1+nd.positionCov[3]*v2)/v +
				meanDiff[0]*meanDiff[1]*v1*v2/(v*v);
		positionCov[4] = (positionCov[4]*v1+nd.positionCov[4]*v2)/v +
				meanDiff[0]*meanDiff[2]*v1*v2/(v*v);
		positionCov[5] = (positionCov[5]*v1+nd.positionCov[5]*v2)/v +
				meanDiff[1]*meanDiff[2]*v1*v2/(v*v);
		
		signalIntensity = (signalIntensity*v1+nd.signalIntensity*v2)/v;
		channelMean = (channelMean*v1+nd.channelMean*v2)/v;
		
		for (int i=0;i<classProbs.length;i++) {
			classProbs[i] = (classProbs[i]*v1+nd.classProbs[i]*v2)/v;
		}
		for (int i=0;i<distChannels.length;i++) {
			double distDiff = distMean[i] - nd.distMean[i];
			distMean[i] = (distMean[i]*v1+nd.distMean[i]*v2)/v;
			distVar[i] = (distVar[i]*v1+nd.distVar[i]*v2)/v + distDiff*distDiff*v1*v2/(v*v);
			distMin[i] = distMin[i] < nd.distMin[i] ? distMin[i] : nd.distMin[i];
			distMax[i] = distMax[i] > nd.distMax[i] ? distMax[i] : nd.distMax[i];
		}
		
		

		objectIds.addAll(nd.objectIds);
		ArrayList<Node> ndAdj = new ArrayList<Node>(nd.adjacencies.keySet());
		for (Node adNd : ndAdj) {
			if (adNd==this) {continue;}
			int currentAd = adjacencies.containsKey(adNd) ? adjacencies.get(adNd) : 0;
			this.adjacencies.put(adNd,currentAd+nd.adjacencies.get(adNd));
			adNd.adjacencies.put(this, currentAd+nd.adjacencies.get(adNd));
			adNd.adjacencies.remove(nd);
		}
		adjacencies.remove(nd);
	}

	public static String tableHeader() {
		String hdr = "id\tclass\ttrackId\ttimeStep\tx\ty\tz\tvoxels\tobjectIds\tlinkedPrev\tlinkedNext\tdistPrev\tdistNext\t" + 
				"adjacentNodes\tadjacentNodeContact\tadjacentTracks\tadjacentTrackClass\tsignalIntensity\tchannelMean\t" +
				"varX\tvarY\tvarZ\tcovXY\tcovXZ\tcovYZ\t";
		for (int ch : probClasses) {
			hdr+="classProb"+ch+"\t";
		}
		for (int ch : distChannels) {
			hdr+="distMean"+ch+"\tdistVar"+ch+"\tdistMin"+ch+"\tdistMax"+ch+"\t";
		}
		hdr+="branchOrEnd";
				
		return( hdr); // \tbranchesPrev\tbranchesNext\tbranchOffset
	}
	
	public String toTableRow() {
		String s = id + "\t" + track.classId + "\t" + track.id + "\t" + timeStep + "\t" + position[0] + "\t" + position[1] + "\t" + position[2] + "\t" + voxels + "\t";		
		s += StringUtils.join(objectIds,";");
		s += "\t";
		
		ArrayList<Integer> l1 = new ArrayList<Integer>();
		ArrayList<Double> l1d = new ArrayList<Double>();
		for (Node tn : preds) {l1.add(tn.id); l1d.add(nextNodeDist.get(tn));}
		ArrayList<Integer> l2 = new ArrayList<Integer>();
		ArrayList<Double> l2d = new ArrayList<Double>();
		for (Node tn : succs) {l2.add(tn.id); l2d.add(nextNodeDist.get(tn));}
		s += StringUtils.join(l1,";");
		s += "\t";
		s += StringUtils.join(l2,";");
		s += "\t";
		s += StringUtils.join(l1d,";");
		s += "\t";
		s += StringUtils.join(l2d,";");
		s += "\t";
		ArrayList<Integer> adId = new ArrayList<Integer>();
		ArrayList<Integer> adMag = new ArrayList<Integer>();
		ArrayList<Integer> adTrack = new ArrayList<Integer>();
		ArrayList<Integer> adClass = new ArrayList<Integer>();
		for (Node tn : adjacencies.keySet()) {
			adId.add(tn.id);
			adMag.add(adjacencies.get(tn));
			if (tn.track==null) {
				adTrack.add(-1);
				adClass.add(-1);
				System.out.println("node " + tn.id + " missing track!");
			} else {
			adTrack.add(tn.track.id);
			adClass.add(tn.track.classId);
			}
		}
		s += StringUtils.join(adId,";");
		s += "\t";
		s += StringUtils.join(adMag,";");
		s += "\t";
		s += StringUtils.join(adTrack,";");
		s += "\t";
		s += StringUtils.join(adClass,";");
		s += "\t";
		s += signalIntensity + "\t" + channelMean + "\t";
		for (double v : positionCov) {
			s+=v+"\t";
		}
		for (int chIndex=0; chIndex<classProbs.length ; chIndex++) {
			s += classProbs[chIndex] + "\t";
		}
		for (int i=0; i<distChannels.length; i++) {
			s+=distMean[i]+"\t"+distVar[i]+"\t"+distMin[i]+"\t"+distMax[i]+"\t";
		}
		s += (branchOrEnd ? "TRUE" : "FALSE");
		return(s);
	}

}
