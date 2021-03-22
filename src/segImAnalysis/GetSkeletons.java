package segImAnalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileSaver;
import ij.measure.ResultsTable;
import sc.fiji.analyzeSkeleton.SkeletonResult;
import sc.fiji.analyzeSkeleton.AnalyzeSkeleton_;
import sc.fiji.analyzeSkeleton.Edge;
import sc.fiji.skeletonize3D.Skeletonize3D_;
import static segImAnalysis.Util.mergeNumericalResultsTable;

public class GetSkeletons {
	
	
	/**
	 * 
	 * Skeletonise the objects in an object map, or a specified subset of them.
	 * Based on sc.fiji.skeletonize3D and sc.fiji.analyzeSkeleton
	 * 
	 * sc.fiji.skeletonize3D.Skeletonize3D_ requires a binary mask; in order to produce per-object skeletons boundary voxels are eroded (set to 0) to ensure all objects to
	 * be skeletonised are separated, while other objects are removed entirely from the mask.
	 * 
	 * @param objectMap						3D image with integer voxel values that denote the id of the object to which the voxel belongs, 0=background
	 * @param selectedObjectIds				Ids of objects to be skeletonised; if null, all objects are skeletonised
	 * @param optionalWorkingSaveFolder		If provided, intermediate stages of the analysis are saved to this location.
	 * @return	A comma separated table of edges describing the skeletonisation of each object (small objects may be excluded by the algorithm)
	 * 			  Table fields are: Skeleton ID,Branch length,V1 x,V1 y,V1 z,V2 x,V2 y,V2 z,Euclidean distance
	 * 				Skeleton ID matches the id of the original object, (V1 x,V1 y,V1 z) and (V2 x,V2 y,V2 z) are the start and end point of the edge,
	 * 				and Euclidean distance is the distance between them.
	 * 				Positions and distances are in the units defined in the objectMap image properties
	 */
	
	public static ResultsTable skeletoniseTouchingObjectsLayers(ImagePlus[] objectMaps, Set<Integer> selectedObjectIds, String optionalWorkingSaveFolder) {
		ResultsTable allSkels = null;
		for (ImagePlus om : objectMaps) {
			ResultsTable skel = skeletoniseTouchingObjects(om,selectedObjectIds,optionalWorkingSaveFolder);
			allSkels = mergeNumericalResultsTable(allSkels,skel);
		}
		return(allSkels);
	
	}
	
	public static ResultsTable skeletoniseTouchingObjects(ImagePlus objectMap, Set<Integer> selectedObjectIds, String optionalWorkingSaveFolder) {
		ImagePlus msk = objectMap.duplicate();
		int[] dims = msk.getDimensions();
		assert(dims[2]==1 && dims[4]==1);
		int dimX = dims[0] ; int dimY = dims[1] ; int dimZ = dims[3];
		
		// zero out all voxels in msk which are not in selectedObjectIds (if given), or are adjacent to a voxel in another object which is included
		ImageStack ims = msk.getImageStack();
		ImageStack ims_orig = objectMap.getImageStack();
		for (int z=0; z<dimZ; z++){
			for (int x=0; x<dimX; x++){
				for (int y=0; y<dimY; y++){
					int voxVal = (int) ims.getVoxel(x,y,z);
					if (voxVal==0) continue;
					if (selectedObjectIds!=null && !selectedObjectIds.contains(voxVal)) {
						ims.setVoxel(x, y, z, 0);
						continue;
					}
					testNeighbours:
					for (int kk=-1;kk<2;kk++){
						if (z+kk<0 || z+kk >=dimZ) {continue;}
						for (int ii=-1;ii<2;ii++){
							if (x+ii<0 || x+ii >=dimX) {continue;} 
							for (int jj=-1;jj<2;jj++){
								if (y+jj<0 || y+jj >=dimY) {continue;}
								int val2 = (int) ims_orig.getVoxel(x+ii,y+jj,z+kk);
								if (val2 != 0 && val2 != voxVal) {
									if (selectedObjectIds!=null && !selectedObjectIds.contains(val2)) {
										ims.setVoxel(x+ii,y+jj,z+kk, 0);
										continue;
									}
									ims.setVoxel(x, y, z, 0);
									ims.setVoxel(x+ii,y+jj,z+kk, 0); // save having to find adjacent pair for second time
									break testNeighbours;
								}
							}
						}
					}
				}
			}
		}
		
		// finish preparing msk
		// IJ.run(msk, "Macro...", "code=v=255*(v>0) stack"); 
		ImageStack imSt = msk.getStack();
		int width=imSt.getWidth();int height=imSt.getHeight();int slices=imSt.getSize();
		for (int x=0;x<width;x++) {for (int y=0;y<height;y++) {for (int z=0;z<slices;z++) {
			double v = imSt.getVoxel(x,y,z);
			imSt.setVoxel(x,y,z,v>0 ? 255 : 0 );
		}}}
		msk.setStack(imSt);
		
	  	ImageTypeConversion.imageTypeChangeTrueValue(msk, "8-bit");
	  	ImageTypeConversion.imageTypeChangeTrueValue(msk, "8-bit");
	  	if (optionalWorkingSaveFolder!=null) {new FileSaver( msk).saveAsTiff( optionalWorkingSaveFolder + "separatedObjects.tif");}
	  	
	  	// run skeletonisation algorithm
		skeletonise(msk);
		
		// run skeleton analysis and extract edge info
		AnalyzeSkeleton_ skel = new AnalyzeSkeleton_();
		skel.setup("", msk);
		SkeletonResult skelResult = skel.run();
		ImageStack ls = skel.getLabeledSkeletons();
		if (ls==null) {return(null);}
		ResultsTable skelDetails = edgeInfo(skelResult, msk);
		if (optionalWorkingSaveFolder!=null) {skelDetails.save(optionalWorkingSaveFolder + "branchDetails_uncorrected_id.csv");}	
		
		// correct skeleton ids to match object ids by comparing labeled skeleton image (ls) with oobjectMapImagesriginal object map
		// Assumes consistency, uses the first skeleton voxel of each id 
		HashMap<Integer,Integer> skelObMap = new HashMap<Integer,Integer>();
		for (int z=0; z<dimZ; z++){
			for (int x=0; x<dimX; x++){
				for (int y=0; y<dimY; y++){
					int skelVal = (int) ls.getVoxel(x,y,z);
					if (skelVal==0) {continue;}
					if (!skelObMap.containsKey(skelVal)) {
						skelObMap.put(skelVal, (int) ims_orig.getVoxel(x,y,z));
					}
				}
			}
		}
		for (int rr=0; rr<skelDetails.size(); rr++) {
			int skelId = (int) skelDetails.getValue("Skeleton ID",rr);
			skelDetails.setValue("Skeleton ID",rr,skelObMap.get(skelId));
		}
		
		if (optionalWorkingSaveFolder!=null) {
			new FileSaver( msk).saveAsTiff( optionalWorkingSaveFolder + "skeletonised.tif");
			if (ls!=null){new FileSaver( new ImagePlus("Labelled skeleton", ls) ).saveAsTiff( optionalWorkingSaveFolder + "skelTrees.tif"); }
			skelDetails.save(optionalWorkingSaveFolder + "branchDetails.csv");	
		}
		
		return(skelDetails);		
	}
	
	public static ResultsTable skeletoniseTouchingObjects(ImagePlus objectMap, String optionalWorkingSaveFolder) {
		return(skeletoniseTouchingObjects(objectMap,null,optionalWorkingSaveFolder));
		
	}
	
	public static void skeletonise(ImagePlus im){
		IJ.run(im, "8-bit", ""); // im.setType(0) doesn't quite work, so resort to macro command
		Skeletonize3D_ skel3D = new Skeletonize3D_();
		skel3D.setup("",im);
		skel3D.run(null);
	}

	// following method adapted from AnalyzeSkeleton_ source, from line 1433
	// https://github.com/fiji/AnalyzeSkeleton/blob/master/src/main/java/sc/fiji/analyzeSkeleton/AnalyzeSkeleton_.java 
	private static ResultsTable edgeInfo(SkeletonResult skelResult,ImagePlus im){
	  final String[] extra_head = {"Branch", "Skeleton ID", 
	                               "Branch length","V1 x", "V1 y",
	                               "V1 z","V2 x","V2 y", "V2 z", "Euclidean distance","running average length", 
	                               "average intensity (inner 3rd)", "average intensity"};
	  final ResultsTable extra_rt = new ResultsTable();
	  // Edge comparator (by branch length)
	  Comparator<Edge> comp = new Comparator<Edge>(){
	    public int compare(Edge o1, Edge o2)
	    {
	      final double diff = o1.getLength() - o2.getLength(); 
	      if(diff < 0)
	        return 1;
	      else if(diff == 0)
	        return 0;
	      else
	        return -1;
	    }
	    public boolean equals(Object o)
	    {
	      return false;
	    }
	  };
	  for(int i = 0 ; i < skelResult.getNumOfTrees(); i++)
	  {
	    final ArrayList<Edge> listEdges = skelResult.getGraph()[i].getEdges();    
	    Collections.sort(listEdges, comp); // Sort branches by length
	    for(final Edge e : listEdges)
	    {
	      extra_rt.incrementCounter();
	      extra_rt.addValue(extra_head[1], i+1);
	      extra_rt.addValue(extra_head[2], e.getLength());
	      double x1 = e.getV1().getPoints().get(0).x * im.getCalibration().pixelWidth;
	      double y1 = e.getV1().getPoints().get(0).y * im.getCalibration().pixelHeight;
	      double z1 = e.getV1().getPoints().get(0).z * im.getCalibration().pixelDepth;
	      double x2 = e.getV2().getPoints().get(0).x * im.getCalibration().pixelWidth;
	      double y2 = e.getV2().getPoints().get(0).y * im.getCalibration().pixelHeight;
	      double z2 = e.getV2().getPoints().get(0).z * im.getCalibration().pixelDepth;
	      
	      extra_rt.addValue(extra_head[3], x1);
	      extra_rt.addValue(extra_head[4], y1);
	      extra_rt.addValue(extra_head[5], z1);
	      extra_rt.addValue(extra_head[6], x2);
	      extra_rt.addValue(extra_head[7], y2);
	      extra_rt.addValue(extra_head[8], z2);
	      extra_rt.addValue(extra_head[9], Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2) + (z1-z2)*(z1-z2)) );

	      // last 3 entries from AnalyzeSkeleton_ code are pointless for binary image:
	      //extra_rt.addValue(extra_head[10], e.getLength_ra());
	      //extra_rt.addValue(extra_head[11], e.getColor3rd());
	      //extra_rt.addValue(extra_head[12], e.getColor());
	    }
	    
	  }
	  return(extra_rt);
	}
}
