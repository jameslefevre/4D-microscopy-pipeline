// TODO: write doc string; see old version notes; clean up

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
	if (splt_fn.size()<2){continue}
	int stNum = splt_fn[0].toInteger();
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

  imagePath = imageStackLocation + imageStackName + "/object_map2.tif";
  print("opening " + imagePath + ":   ");
  //ImagePlus objectMapImage2  = IJ.openImage( imagePath ); 
  ImagePlus objectMapImage2=null;
  for (int _try=0;_try<3;_try++){
	try{
		objectMapImage2  = IJ.openImage( imagePath ); 
	} catch(Exception e) {
    	println("Exception: ${e}")
    	objectMapImage2=null;
	}
	if (objectMapImage2!=null){break;}
	sleep(30000)
  }
  println(objectMapImage2==null ? "Failed" : "Succeeded");
  
 ImagePlus[] objectMapImages = [objectMapImage,objectMapImage2]

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












