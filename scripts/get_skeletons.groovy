
/**
 * Generates skeleton representations of objects. Each object is eroded in 3D down to a 
 * [topological skeleton](https://en.wikipedia.org/wiki/Topological_skeleton), and then reduced to summary information 
 * at the branch level (a branch is a segment of the skeleton between 2 branch or end points). For each branch, we 
 * save the start and end coordinates, and the curved branch length as well as the straight line distance. 
 * Coordinates and distances are in the units defined in the image properties. Skeletons are used primarily for 
 * visualisation, but may also be used for analysis. They are mostly useful for linear structures. As for meshes, this 
 * script is an add-on to the primary object analysis (see split_object_analysis.groovy), using some of its outputs. 
 * The required input is one or more 8- or 16-bit color tiff stacks named object_map.tif, object_map2.tif etc where 
 * the (integer) voxel values define object masks. 
 * In addition, we require a text file objectStats.txt containing a tab-separated table with integer-value 
 * columns named id and class (any other columns are allowed, but are not used). The IDs must match the voxel values in 
 * order for objects to be processed, and the class should be correct for the object as per the original segmentation and 
 * the primary object analysis.
 * 
 * This is a fairly light wrapper around Java code (segImAnalysis.GetSkeletons), which calls core functionality from 
 * sc.fiji.skeletonize3D and sc.fiji.analyzeSkeleton (Skeletonize and Analyse Skeleton plugins, standard with Fiji).
 * 
 * @param imageStackLocation
 * Path to a directory containing 8-bit color tiff stacks representing segmentation to be analysed.
 * @param savePath
 * The directory to save the output files, in subdirectories named for the image stacks processed. 
 * This should be the same as imageStackLocation, or at least have the same subdirectory names.
 * @param classesToAnalyse
 * A list of comma-separated integers between square brackets, e.g. '[1,2,3]', specifying which 
 * classes to analyse. Each object ID is associated with a class number in objectStats.txt, and the 
 * object will only be processed if its class number is in this list. 
 * @param firstStackNumber
 * The first stack number to process, where the stack number is parsed from the file name using stackNumberPrefix and 
 * stackNumberSuffix (see below).
 * @param lastStackNumber
 * The last stack number to process. Stacks will be processed in order from firstStackNumber to lastStackNumber, inclusive.
 * @param numberThreadsToUse
 * The number of threads that ImageJ is asked to use.
 * @param stackNumberPrefix
 * A string identifying the start of the stack number in the filename (stack number may be left-padded with zeroes).
 * @param stackNumberSuffix
 * A string identifying the end of the stack number in the filename. The script will look for a filename 
 * which contains a substring consisting of stackNumberPrefix, optional zeros, the stack number associated 
 * with the job, then stackNumberSuffix.
 * @param fileNamePrefix
 * Only stack names starting with this string will be processed; leave blank (prefix='') to skip this filter.
 * @param overwriteExisting
 * A string equal to 'true' or 'false'. If false, the job will check for the output file objectStats.txt in the 
 * expected location, and if present it will not process the stack. This is useful for easily redoing failed 
 * tasks while not repeating tasks that completed successfully.
 */
 
#@ String imageStackLocation
#@ String savePath
#@ String classesToAnalyse
#@ String firstStackNumber
#@ String lastStackNumber
#@ String numberThreadsToUse
#@ String stackNumberPrefix
#@ String stackNumberSuffix
#@ String fileNamePrefix
#@ Boolean overwriteExisting

import ij.IJ
import ij.ImagePlus
import ij.ImageStack;
import ij.measure.ResultsTable;
import java.io.File;
import java.text.SimpleDateFormat
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import segImAnalysis.GetSkeletons;

int[] classesToAnalyse  = Eval.me(classesToAnalyse)
int firstStackNumber  = Eval.me(firstStackNumber)
int lastStackNumber  = Eval.me(lastStackNumber)

sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")
println("starting get_skeletons.groovy:   " + sdf.format(new Date()));

ArrayList<String> nameSet = new ArrayList<String>();
files = (new File(imageStackLocation)).listFiles()
Arrays.sort(files);
for (String fn : files){
	fn = fn.tokenize("/")[-1].tokenize(".")[0]

	// extract stack number
	splt_fn = fn.split(stackNumberPrefix)
	if (splt_fn.size()<2){continue}
	part_fn = splt_fn[1]
	splt_fn = part_fn.split(stackNumberSuffix)
	// if (splt_fn.size()<2){continue} // if suffix is at end of filename, size will be 1 and that is fine; no match will also give size 1
	int stNum;
	try{
		stNum = splt_fn[0].toInteger();
	} catch(Exception e) {continue;}
		
	//int stNum = splt_fn[0].toInteger();
	// println(stNum)
	
	if (firstStackNumber <= stNum && lastStackNumber >= stNum ){
		nameSet.add(fn);
	}
}

for (imageStackName in nameSet){
  String saveName = savePath + imageStackName + "/objectSkeletons.csv";
  if ( !overwriteExisting && (new File(saveName)).exists()){
	println(saveName + " already exists : skipping ")
	continue;
  }
  
  String imagePath = imageStackLocation + imageStackName + "/object_map.tif";
  print("opening " + imagePath + ":   ");
  // ImagePlus objectMapImage  = IJ.openImage( imagePath ); 
  ImagePlus objectMapImage=null;
  for (int _try=0;_try<3;_try++){
	try{
		objectMapImage  = IJ.openImage( imagePath ); 
	} catch(Exception e) {
    	println("Exception: ${e}")
    	objectMapImage=null;
	}
	if (objectMapImage!=null){break;}
	sleep(30000)
  }
  println(objectMapImage==null ? "Failed" : "Succeeded");
  if (objectMapImage==null) continue;

  ArrayList<ImagePlus> objectMapImageList = new ArrayList<ImagePlus>();
  objectMapImageList.add(objectMapImage);

  int omNum=1
  while(true){
  	objectMapImage=null;
  	omNum+=1;
  	imagePath = imageStackLocation + imageStackName + "/object_map"+omNum+".tif";
  	print("opening " + imagePath + ":   ");
  	for (int _try=0;_try<3;_try++){
	  try{
	  	objectMapImage  = IJ.openImage( imagePath ); 
  	  } catch(Exception e) {
      	 println("Exception: ${e}")
    	 objectMapImage=null;
	  }
	  if (objectMapImage!=null){break;}
	  sleep(30000)
    }
    println(objectMapImage==null ? "Failed" : "Succeeded");
    if (objectMapImage==null) break;
    objectMapImageList.add(objectMapImage);
  }

 ImagePlus[] objectMapImages = objectMapImageList.toArray(); 

  String objectStatsPath = imageStackLocation + imageStackName + "/objectStats.txt";
  print("opening " + objectStatsPath + ":   ");
  ResultsTable objectStats = ResultsTable.open(objectStatsPath);
  println(objectStats==null ? "Failed" : "Succeeded");
  int maxId = (int) Math.round(objectStats.getValue("id",objectStats.size()-1))
  println("Max object id " + maxId)

// get set of ids to skeletonise
  Set<Integer> idsToSkeletonise = new HashSet<Integer>();
  for (int ii=0; ii<objectStats.size(); ii++){
    int id = (int) Math.round(objectStats.getValue("id",ii));
    int classNum = (int) Math.round(objectStats.getValue("class",ii))
    if (id==0){continue;}
    if (classesToAnalyse.contains(classNum)){
      idsToSkeletonise.add(id);
    }
  }
  println("Found " + idsToSkeletonise.size() + " object ids to skeletonise    " + sdf.format(new Date())); 
  
  ResultsTable skelDetails = GetSkeletons.skeletoniseTouchingObjectsLayers(objectMapImages, idsToSkeletonise, null);
  
  println("Attempting to save skeletons as " + saveName + "    " + sdf.format(new Date()));
  new File(savePath + imageStackName).mkdirs();
  skelDetails.save(saveName);
}

println("done")












