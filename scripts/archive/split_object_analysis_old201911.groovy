

// TODO: doc; see old versions up to 10 (?)
// maybe support changing intensity (only for object intensity field though)


#@ String imageStackLocation
#@ String originalStackLocation
#@ String probStackLocation
#@ String savePath
#@ String firstStackNumber
#@ String lastStackNumber
#@ String numberThreadsToUse
#@ String stackNumberPrefix
#@ String stackNumberSuffix
#@ String fileNamePrefix
#@ Boolean overwriteExisting
#@ String classesToAnalyse
#@ String dynamic
#@ String minVoxExtraObjects
#@ String classNumsToFill
#@ String classLayers
#@ String incorporatedChannels
#@ Float intensityScalingFactor
#@ String cropBox

 // cropBox: [[min_x,max_x],[min_y,max_y],[min_z,max_z]], 0 indexed, endpoints included
 // applied to original image if present (use empty string to indicate no cropping)
 // intensityScalingFactor also applied to original image if present (1 means no scaling)

import ij.IJ
import ij.ImagePlus
import ij.ImageStack
import java.io.File;
import java.util.Arrays;
import java.text.SimpleDateFormat
import segImAnalysis.*;

if (numberThreadsToUse != null){
	IJ.run("Memory & Threads...", "parallel="+ numberThreadsToUse +" run");
}

int firstStackNumber  = Eval.me(firstStackNumber)
int lastStackNumber  = Eval.me(lastStackNumber)
int[] classesToAnalyse  = Eval.me(classesToAnalyse)
int[] dynamic  = Eval.me(dynamic)
int minVoxExtraObjects  = Eval.me(minVoxExtraObjects)
int[] classNumsToFill  = Eval.me(classNumsToFill)
int[] classLayers  = Eval.me(classLayers)
int[][] incorporatedChannels  = Eval.me(incorporatedChannels)
int[][] cb = null
if (cropBox!=""){
	cb = Eval.me(cropBox);
} 
int[][] cropBox = cb 

sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")

String[] nameSet = getNameArray(imageStackLocation, firstStackNumber, lastStackNumber, stackNumberPrefix, stackNumberSuffix);

println(nameSet);
String[] nameSetRaw = (originalStackLocation != null && originalStackLocation != "") ? getNameArray(originalStackLocation, firstStackNumber, lastStackNumber, stackNumberPrefix, stackNumberSuffix) : null;
println(nameSetRaw);
String[] nameSetProb = (probStackLocation != null && probStackLocation != "") ? getNameArray(probStackLocation, firstStackNumber, lastStackNumber, stackNumberPrefix, stackNumberSuffix) : null;
println(nameSetProb);

//for (imageStackName in nameSet){
for (int st=firstStackNumber;st<=lastStackNumber;st++){
	int ind=st-firstStackNumber
	if (nameSet[ind]==null){continue;}
	if (nameSetRaw!=null && nameSetRaw[ind]==null){continue;}
	if (nameSetProb!=null && nameSetProb[ind]==null){continue;}
	classMapPath = imageStackLocation + nameSet[ind] + ".tif"
	rawImagePath = nameSetRaw==null ? null : originalStackLocation + nameSetRaw[ind] + ".tif"
	probImagePath = nameSetProb==null ? null : probStackLocation + nameSetProb[ind] + ".tif"
	
	saveFolder = savePath + nameSet[ind].split("_seg_")[0] + "/"  // if imageStackName contains _seg_, only use the portion before
	
	println("loading class map " + classMapPath + ";")
	println("saving results to " + saveFolder )

	if ( !overwriteExisting && (new File(saveFolder + "objectStats.txt")).exists() && (new File(saveFolder + "objectAdjacencyTable.txt")).exists() && (new File(saveFolder + "object_map.tif")).exists() && (new File(saveFolder + "object_map2.tif")).exists()  ){
		println("objectStats.txt, objectAdjacencyTable.txt, object_map.tif, object_map2.tif already exist in " + saveFolder + " : skipping ")
		continue;
	}

	// ImagePlus classMapImage  = IJ.openImage( classMapPath ); 
	// file loading start ******************
	ImagePlus classMapImage=null;
	for (int _try=0;_try<3;_try++){
		try{
			classMapImage  = IJ.openImage( classMapPath ); 
		} catch(Exception e) {
   		 	println("Exception: ${e}")
    		classMapImage=null;
		}
		if (classMapImage!=null){break;}
		sleep(30000)
		}
	if (classMapImage==null){
		date = new Date(); println("Failed to load " + classMapPath + " ; terminating   " + sdf.format(date));
		continue;
	}


	ImagePlus rawImage=null;
	if (rawImagePath!=null){
		println("Attempting to load " + rawImagePath)
		for (int _try=0;_try<3;_try++){
		try{
			rawImage  = IJ.openImage( rawImagePath ); 
		} catch(Exception e) {
   		 	println("Exception: ${e}")
    		rawImage=null;
		}
		if (rawImage!=null){break;}
		sleep(30000)
		}
		if (rawImage==null){
			date = new Date(); println("Failed to load " + rawImagePath + " ; terminating   " + sdf.format(date));
			continue;
		} else {println("loaded")}
	}
	ImagePlus probImage=null;
	if (probImagePath!=null){
		println("Attempting to load " + probImagePath)
		for (int _try=0;_try<3;_try++){
		try{
			probImage  = IJ.openImage( probImagePath ); 
		} catch(Exception e) {
   		 	println("Exception: ${e}")
    		probImage=null;
		}
		if (probImage!=null){break;}
		sleep(30000)
		}
		if (probImage==null){
			date = new Date(); println("Failed to load " + probImagePath + " ; terminating   " + sdf.format(date));
			continue;
		} else {println("loaded")}
	}

	// file loading end ******************

	if (cropBox!=null){
		println(cropBox)
		ImageStack rawStack = rawImage.getImageStack()
		rawStack = rawStack.crop(cropBox[0][0], cropBox[1][0], cropBox[2][0], cropBox[0][1]-cropBox[0][0]+1 , cropBox[1][1]-cropBox[1][0]+1, cropBox[2][1]-cropBox[2][0]+1)    	
		rawImage.setStack(rawStack)
	}
	IJ.run(rawImage,"Multiply...", "value="+ intensityScalingFactor +" stack");


	SegmentedImageAnalysis.splitObjectAnalysis(
		classMapImage,
		rawImage,
		probImage,
		saveFolder,
		classesToAnalyse,
		dynamic,
		minVoxExtraObjects,
		classNumsToFill,
        classLayers,
        incorporatedChannels,
        false)
}
println("Completed analysis for following segmented images:");
println(nameSet);
println("done")


String[] getNameArray(String path, int firstStackNumber, int lastStackNumber, String stackNumberPrefix, String stackNumberSuffix){
	String[] nameArray = new String[lastStackNumber-firstStackNumber+1]
	for (int _try=0;_try<3;_try++){
	try{
		files = (new File(path)).listFiles()
		} catch(Exception e) {
    		println("Exception: ${e}")
    		files=null;
			}
		if (files!=null){break;}
			sleep(30000)
	}
	if (files==null){
		date = new Date(); println("Failed to load file names in " + imageStackLocation + " ; terminating   " + sdf.format(date));
		System.exit(0)
	}
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
			nameArray[stNum-firstStackNumber] = fn;
		}
	}
	return(nameArray)
}