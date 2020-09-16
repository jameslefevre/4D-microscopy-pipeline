package objectTracking;

import static segImAnalysis.Util.println;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import ij.measure.ResultsTable;

public class DataLoadingMethods {
	
	public static int[] loadObjectData(
			String pth, 
			//String[] fields, 
			//double[] fieldScaling, // scaling factor on data load for each field; so must be same length as fields
			//Integer fieldDropIfZero, // if the value at this index is zero, the row in the object stats table will be skipped (designed to skip objects of size 0)
			boolean dropIfNoVoxels,
			int[] trackedClasses, 
			String objectStats_filename,
			String adjacencyTable_filename,
			int[] timeSteps,
			boolean useAlphabeticalPositionForStackNumber,
			String stackNumPrefix,
			String stackNumberSuffix,
			HashMap<Integer,HashMap<Integer,ArrayList<Node>>> objectData, // keys class, timeStep
			HashMap<Integer,int[][]> allClassAdjacencies
			) throws IOException {
		

		// check data structure and start set up
		assert(objectData.size()==0);		
		assert(allClassAdjacencies.size()==0);		
		for (int jj=0; jj< trackedClasses.length ; jj++){
			int classnum = trackedClasses[jj];
			objectData.put(classnum, new HashMap<Integer,ArrayList<Node>>());
		}

		// sort out time steps and file names

		HashMap<Integer,String> fileNames = new HashMap<Integer,String>();

		if (useAlphabeticalPositionForStackNumber) {
			File[] files = (new File(pth)).listFiles();
			println("Found " + files.length + " files");
			ArrayList<String> folders = new ArrayList<String>();
			for (int ii=0; ii<files.length; ii++) {
				if (files[ii].isDirectory()) {
					folders.add(files[ii].getName());
				}
			}
			println("Found " + folders.size() + " folders");
			Collections.sort(folders);
			if (timeSteps==null) {
				timeSteps = new int[folders.size()];
				for (int ii=0; ii<folders.size(); ii++) {timeSteps[ii]=ii;}
			}
			for (int ts : timeSteps) {
				fileNames.put(ts,folders.get(ts));
			}

		} else {
			File[] files = (new File(pth)).listFiles();
			println("Found " + files.length + " files");
			for (File fl : files){
				String fn = fl.getName();
				String[] splitName = fn.split(stackNumPrefix);
				if (splitName.length<2) {continue;}
				splitName = splitName[1].split(stackNumberSuffix);
				if (splitName.length<1) {continue;}
				int stNum = Integer.valueOf(splitName[0]);
				fileNames.put(stNum, fn);
			}
			if (timeSteps==null) {
				Integer[] ts = fileNames.keySet().toArray(new Integer[fileNames.keySet().size()]);
				Arrays.sort(ts);
				timeSteps = new int[ts.length];
				for (int ii=0; ii<ts.length; ii++) {
					timeSteps[ii] = (int) ts[ii];
				}
			}
		}
		
		// main data loading loop through time steps

		for (int ts : timeSteps){
			String folderName = pth + fileNames.get(ts) + "/";	
			// load data for time step / stack
			HashMap<Integer,ArrayList<Node>> ndsByClass = loadObjectDataByClassFromTable(folderName+ objectStats_filename,ts,dropIfNoVoxels);
			for (int classNum : ndsByClass.keySet()) {
				if (objectData.containsKey(classNum)) {
					objectData.get(classNum).put(ts, ndsByClass.get(classNum));
				}
			}
			
			allClassAdjacencies.put(ts, csvToIntArray(folderName+adjacencyTable_filename));
		}
	
		return(timeSteps);
		
	}
	
	static HashMap<Integer,ArrayList<Node>> loadObjectDataByClassFromTable(String pth, int timeStep, boolean dropIfNoVoxels) throws IOException{
		HashMap<Integer,ArrayList<Node>> nds = new HashMap<Integer,ArrayList<Node>>();
		ResultsTable rt = ResultsTable.open(pth);
		Node.set_probClasses_distChannels(rt);
		for (int rowNum =0; rowNum<rt.size(); rowNum++) {
			Node nd = new Node(rt,rowNum,timeStep);
			if (dropIfNoVoxels && nd.voxels==0) {continue;}
			if (!nds.containsKey(nd.classId)) {
				nds.put(nd.classId, new ArrayList<Node>());
			}
			nds.get(nd.classId).add(nd);
		}
		return(nds);
	}
	
	// Node(Integer objectId ,int classId_, double[] posSize_, double voxelVolume_, int timeStep_)
	
	
	// file reading **************************************************

		private static String readFile(String path) {
			String st = "";
			try {
				st =readFile(path, Charset.defaultCharset());
			} catch (IOException e) {
				e.printStackTrace();
				return(null);
			}
			return(st);
		}
		private static String readFile(String path, Charset encoding) 
				throws IOException 
		{
			byte[] encoded = Files.readAllBytes(Paths.get(path));
			return new String(encoded, encoding);
		}
	private static int[][] csvToIntArray(String fn){
		String fl = readFile(fn);
		if (fl == null) {
			println("File does not exist");
			return null;
		}		
		String[] lines = fl.split( "\n" );
		int[][] rt = new int[lines.length][];			
		for (int ii=0; ii< lines.length; ii++){
			String ln = lines[ii];
			String[] nums = ln.split(",");
			rt[ii] = new int[nums.length];
			for (int jj=0; jj< nums.length; jj++){
				rt[ii][jj] = Integer.parseInt(nums[jj]);
			}
			//println(ln)
		}
		return(rt);
	}

}
