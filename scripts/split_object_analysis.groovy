
/**
 * Defines and quantifies objects in a semantic segmentation of a 3D image (provided as an 
 * 8-bit color tiff stack). For each class to be analysed, 3D hole filling 
 * is applied, followed by a watershed split algorithm to separate touching objects. 
 * Then various metrics are calculated for each object, and saved in a table. 
 * If available, the original image and the segmentation probability map within each object are included 
 * in the metric calculations. 
 * A separate output table quantifies contact between objects, whether they 
 * are in the same class or not. 
 * This code also supports a class hierarchy, where additional 
 * classes can be defined as the combination of one or more of the original classes, and 
 * analysed as such; these must be arranged in layers so that each original class is used 
 * at most once in each layer.
 * 
 * An object map image stack is also saved for each layer. This is a 16-bit integer valued tiff stack where 
 * the voxel values correspond to the object ids in the output tables, and 0 indicates background or no object in the layer. 
 * This allows further computation such as object meshes.
 * 
 * Apart from adjacency calculations, analysis is performed separately for each specified class, which is 
 * defined as one or more of the image channels (corresponding to classes in the original segmentation). 
 * The default case is to analyse each channel as a class, excluding the background (generally channel 0). 
 * Multiple groupings of channels are possible, but each class must be assigned to a layer so that each 
 * channel occurs at most once in a layer, and the layers will affect the adjaceny calculations. 
 * Each class is assigned its own set of analysis parameters.
 * 
 * The adjacency metric between 2 objects in the same layer is the number of neighbouring voxel pairs where one voxel is in 
 * each of the two objects. We define voxels to be neighbours if they differ in each coordinate by at most 1 position, but 
 * excluding the case where they differ by 1 in all 3 coordinates; by this definition, each (interior) voxel has 18 neighbours. 
 * The contact area between the  objects in voxels is approximately one fifth this value, although the discrete, non-smooth 
 * surfaces mean this is an approximation. The adjacency metric between 2 objects in different layers is the number of overlapping 
 * voxels (voxels with the same coordinates). Note that these two adjacency metrics measure different things and are not comparable.
 * 
 * OUTPUT DETAILS
 * 
 * The outputs for each image stack are saved within the specified root folder (savePath), in a subfolder named for the stack 
 * The 3 output types are linked by consistent object naming (using positive integer ids).
 * The object maps described above are saved as object_map.tif, object_map2.tif etc (depending on the number of layers).
 * The object adjacency data is saved as csv file objectAdjacencyTable.txt: each line contains 2 object ids then the adjacency score. 
 * The object stats are saved as a tab separated text table objectStats.txt, with header. The fields present will depend on the parameters 
 * channelsForDistanceMap and probStackLocation. Units (noted in each case) are either voxels (voxel [2,3,4] is next to [2,3,5], 
 * distance between them is 1) or the units obtained from the image properties ("real units"). Details follow:
 * 	id: object id, a non-negative integer that is unique across all classes; 0 is the background object
 * 	class: object class, based on 1 or more of the original segmentation classes. Sequential integers from 0, which by convention is background
 * 	within_class_id: Sequential numbering of objects within class, not important
 * 	x,y,z: Object centre of mass, in voxel units
 *  voxels: The number of voxels in the object
 *  channel: The average segmentation voxel value over the object; for composite classes formed from more than one of the original segmentation
 *  	classes, this gives some information on the breakdown. 
 *  intensity: The average value of the source image after intensity scaling, averaged over the object
 *  	Present if the original image stack corresponding to the segmentation is provided (parameter originalStackLocation)
 *  vx,vy,vz: Within the object, the variance of the x,y,z coordinates respectively, in voxel units.
 *  cxy,cxz,cyz: Within each object, the covariance between the given coordinate pairs, in voxel units.
 *  	The variance and covariance data is provided as a possible source of approximate shape information.
 *  prob0, prob1,.. : Present if the parameter probStackLocation is provided, linking to the probability map for the segmentation.
 *  	These are the mean intensities of each channel within the object.
 *  	The number of fields is equal to the number of channels in the prob map (including 0=background), and will add to 255
 *	d1,dv1,dMin1,dMax1 etc: 1 is as example, these four fields will be present for each channel listed in channelsForDistanceMap
 * 		These are all based on the same calculated metric, the distance map for the named channel (note that by channel we indicate one
 * 		of the original segmentation classes, potentially distinct from the classes analysed).
 * 		The values are respectively the mean, variance, min and max of this metric over the object voxels, measured in real units.
 * 		The metric is the distance of each voxel from the smoothed surface around the voxels of the specified channel; the smoothing
 * 		level is controlled by the corresponding value in parameter smoothingErosionForDistanceMap
 * 
 * @param imageStackLocation
 * Path to a directory containing 8-bit color tiff stacks representing segmentation to be analysed.
 * @param originalStackLocation
 * Optional path to a directory containing the source images (single channel, 32-bit tiff stacks), so that the original image 
 * intensity can be quantified in each object.
 * @param probStackLocation
 * Optional path to a directory containing the probability maps corresponding to the segmentations 
 * above (multi-channel, 8-bit tiff stacks), so that segmentation confidence and class confusion can 
 * be quantified in each object.
 * @param savePath
 * The root directory to save the output files, using subdirectories named for the image stacks processed.
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
 * @param classNumsToFill
 * The channels for which 3D hole filling should be applied. A list of comma-separated integers within square brackets, 
 * which should be selected from the class numbers in the segmentations (the channel index values when loading 
 * the segmented images in ImageJ). This is not necessarily the same as the classes in classesToAnalyse below. 
 * For each listed class in turn, a 3D hole-filling algorithm is applied to the segmentation. While often useful 
 * for removing small pockets of mis-segmentation and improving the function of the watershedding algorithm, 
 * care should be taken: if a legitimate structure is entirely enveloped (in 3 dimensions) in a region of another 
 * class, then the structure will be removed and replaced with the surrounding class. The background class should 
 * not be included in this list, as that will typically result in the removal of all structures (treated as 
 * "holes" in the background).
 * @param classesToAnalyse
 * A list of comma-separated integers between square brackets, e.g. '[1,2,3]'. Specifies which classes to 
 * analyse; in the simple case these correspond directly to the colours (segmentation classes) in the input 
 * segmentations, but may be more complex if class aggregation or hierarchy is used (see below). Typically 
 * class 0 is black and represents background, and should not be included in the analysis.
 * @param classLayers
 * A list of comma-separated integers within square brackets, which should have the same length as and correspond 
 * to classesToAnalyse. Gives a layer to which each class is allocated. Layers are numbered sequentially from 1, and 
 * each layer must contain at least one class. In the simple (non-hierachical) case, all numbers should be 1.
 * @param incorporatedChannels
 * An array of arrays; within the outer square brackets there is a list of subarrays, which has the same length as 
 * and corresponds to classesToAnalyse. Each sub-array (list of comma-separated numbers between square brackets) 
 * gives one or more of the original classes to be combined to form the corresponding class in classesToAnalyse. 
 * In the simple cases (no merging of classes), this should be set to e.g. '[[1],[2],[3]]'. The values need to 
 * be considered in combination with the parameter classLayers: each of the original classes can occur at most 
 * once in each layer (this system is designed to allow efficient representations of every object for adjacency 
 * quantification and other purposes). Classes can be aggregated using this parameter without having more than 
 * one layer, if you do not need to also analyse the classes separately. 
 * @param dynamic
 * A list of comma-separated integers within square brackets, which should have the same length as and correspond 
 * to classesToAnalyse. This is the watershed parameter used to separate touching objects, specified for each class. 
 * Given two objects or masses which are connected by a narrower section, consider the distance to the surface of 
 * the structure at the centre of each of the two masses versus the distance at the centre of the narrow section. 
 * If the difference is greater than the "dynamic" parameter in both cases, then the two masses are split into 
 * separate objects by dividing across the narrow section. This parameter has the same units as the image 
 * (see voxel dimensions), and generally speaking the parameter value should be in proportion to the scale of 
 * the structures in the class. Lower values result in more aggressive splitting.
 * @param minVoxExtraObjects
 * An integer. The watershedding algorithm will remove objects altogether if the distance from the centre of 
 * the object to the surface is smaller than the "dynamic" parameter. This process allows the optional recovery 
 * of these objects; any object that was removed but has at least this number (minVoxExtraObjects) of voxels 
 * will be replaced.
 * @param channelsForDistanceMap
 * A list of comma-separated integers between square brackets, corresponding to selected segmentation classes 
 * (original, not aggregated). This parameter allows the optional quantification of the distance from the edge 
 * of a specified class or classes, to every object. For example, it allows the calculation of the minimum, mean 
 * and maximum distance of a structure from the surface of the main cell body.
 * @param smoothingErosionForDistanceMap
 * A list of comma-separated integers between square brackets, of the same length as and corresponding to 
 * channelsForDistanceMap. This indicates how much smoothing of the corresponding channel to do before 
 * calculating the distance transform, in order to give more stable results. Higher numbers give more smoothing, 
 * 0 means no smoothing. 
 * @param intensityScalingFactor
 * The original image intensity is multiplied by this value before quantifying the fluoroscence intensity 
 * within each object; set to 1 to disable intensity adjustment.
 * @param cropBox
 * An array of the form '[[minX,maxX],[minY,maxY],[minZ,maxZ]]' defining a 3D cropping region to be applied 
 * to the original image only. The 6 values are integers indicating a number of voxels (see semantic segmentation 
 * section). If the original image was cropped before segmenting, this allows the cropping to be replicated so that 
 * the images correspond, before quantifying the fluoroscence intensity within each object; 
 * cropBox='' indicates no cropping.
 */

#@ String imageStackLocation
#@ String originalStackLocation
#@ String probStackLocation
#@ String savePath
#@ int firstStackNumber
#@ int lastStackNumber
#@ String numberThreadsToUse
#@ String stackNumberPrefix
#@ String stackNumberSuffix
#@ String fileNamePrefix
#@ Boolean overwriteExisting
#@ int[] classNumsToFill
#@ int[] classesToAnalyse
#@ int[] classLayers
#@ int[][] incorporatedChannels
#@ int[] dynamic
#@ int minVoxExtraObjects
#@ int[] channelsForDistanceMap
#@ int[] smoothingErosionForDistanceMap
#@ Float intensityScalingFactor
#@ String cropBox

import ij.IJ
import ij.ImagePlus
import ij.ImageStack
import java.io.File;
import java.util.Arrays;
import java.text.SimpleDateFormat
import segImAnalysis.SegmentedImageAnalysis;

if (numberThreadsToUse != null){
	IJ.run("Memory & Threads...", "parallel="+ numberThreadsToUse +" run");
}

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

	if (rawImage != null){

	if (cropBox!=null){
		println(cropBox)
		ImageStack rawStack = rawImage.getImageStack()
		rawStack = rawStack.crop(cropBox[0][0], cropBox[1][0], cropBox[2][0], cropBox[0][1]-cropBox[0][0]+1 , cropBox[1][1]-cropBox[1][0]+1, cropBox[2][1]-cropBox[2][0]+1)    	
		rawImage.setStack(rawStack)
	}
	IJ.run(rawImage,"Multiply...", "value="+ intensityScalingFactor +" stack");
	}


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
	channelsForDistanceMap,
	smoothingErosionForDistanceMap,
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
