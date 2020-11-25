/** Generates and saves a series of specified 3D image features using the ImageJ filters (Process -> Filters) and the ImageScience library, 
 *  accessed via the trainableSegmentation / Trainable Weka plugin.
 *  Feature selection is adapted from trainable weka 3D, with optional downsampling to accelerate the calculation of 
 *  features with larger sigma (scale parameter).
 *  
 *  The selection of features to compute is specified in a supplied file (the feature-model table - see feature_model_table below),
 *  using a model name to look up the desired set of features.
 *  
 *  Computed feature stacks are saved as image sequences named slice_xxxx.tif, where xxxx is the (padded) slice number, starting from zero. 
 *  Each is saved in root directory featureSavePath within a subdirector named for the feature (feature_name in feature_model_table).
 *  This is to allow all features for a given slice to be loaded at once (so a segmentation model can be applied) without loading 
 *  all features at once for the whole image stack, reducing the memory footprint.
 *  
 *  3D cropping and intensity scaling is optionally applied to the input prior to any computation, and the adjusted image
 *  is saved as "original" if this feature is selected in the feature-model table
 *  
 *  The input image must be single-channel tiff stack with floating point values. Multi-channel inputs require additional coding.
 *  
 *  
 * @param feature_model_table 
 * path to tsv file containing a table of information on features and which features to compute for the specified model,  
 * known as the feature-model table.
 * The feature_name column gives the label for each specific feature, which is used for the subdirectly name that the 
 * calculated feature is saved to. The next 5 columns specify the required computation, and are specified primarily 
 * for use by this script.
 * 
 * @param imageStackLocation
 * The path to the directory containing the input image.
 * 
 * @param imageStackName
 * The name of the input image file, excluding the file extension which is assumed to be .tif
 *  
 *  @param modelName 
 *  Intended as the name of a segmentation model to be applied, in this script it is simply a string used to look up the features 
 *  to generate in the feature-model table. This name must also match a column in feature_model_table, which contains
 *  a "1" for each feature used, with no entry in other rows.
 *  
 *  @param featureSavePath
 *  Path to the directory in which to save the feature stacks.
 *  
 *  @param numberThreadsToUse 
 *  number of threads that ImageJ is asked to use
 *  
 *  @param pixelSize_xy, pixelSize_z: Used instead of any image properties to convert from pixel dimensions to the real units used in 
 *  feature generation; the feature generation code actually uses the voxel x dimension as the unit, so only pixelSize_z/pixelSize_xy 
 *  will have an effect.
 *  
 *  @param intensityScalingFactor
 *  The image stack is multiplied by this value before features are generated
 *  
 *  @param cropBox 
 *  3D crop to be applied to image stack before features are generated;
 *  comma separated list of 6 integers, min,max for x,y,z (0 indexed, endpoints included) ; empty string means no cropping
 *  
 *  DETAILS ON FEATURE-MODEL TABLE AND FEATURE COMPUTATION
 *  
 *  The first column of the feature-model table (feature_name) provides a unique label for each specific image feature, 
 *  while the next five columns (operation, parameter, sigma, group, downsample) provide information specifying the computation 
 *  of each feature, primarily used here.
 *  
 *  group: computed features have group IJ_filter or ImageScience, indicating whether they are provided by the inbuilt image filters 
 *  or by the ImageScience library (via Trainable Weka interface). This dictates various aspects of implementation through the scripts.
 *  Other supported groups are "original" (input image to be used as a feature) and "derived" (to be ignored - indicates features that 
 *  can be easily calculated when needed from other features)
 *  
 *  sigma: A scale parameter to be applied in each dimension. Larger sigma means that the feature is calculated based on a larger 
 *  neighbourhood. The unit is the size of a voxel in the x/y direction (see note on scales below)
 *  
 *  downsample: Optional field of the form x_y_z. If specified, the original image is downsampled by the specified factor in each 
 *  dimension, then the feature is calculated with sigma (kernel size) reduced by the same factor in each dimension, and the 
 *  result upsampled to the original image size using bilinear interpolation. This provides a computationally cheaper approximation 
 *  to the feature run directly, due to both the smaller sigma and the reduced number of feature calculations.  
 *  Note that the feature-model table is used as a single source of truth to help ensure that the same downsampling pattern is 
 *  used for training and applying a model.
 *  
 *  operation, parameter: The particular operation with optional additional parameter with variable meaning. 
 *  These are combined with other information to form the API call for the specific feature computation.
 *  
 *  
 *  
 *  NOTE ON IMAGE SCALES
 *  
 *  In brief: this script handles non-isometric voxel scales in the z dimension (unequal x and y dimensions could be handled
 *  with minor modifications, but these are not usual), using parameters to specify the scales (image properties are ignored). 
 *  The size of the voxel in the x dimension is used as the unit when applying the scale parameter (sigma), so the ratio of the
 *  x/y dimension to the z dimension of the voxel is the only information used.
 *  
 *  The ImageJ filters work in voxel units when applying the scale parameter (sigma), and image properties (which specify the real 
 *  size represented by a voxel) are ignored.
 *  The FilterJ plugin, which is a front end for the image feature algorithms in the ImageScience library, works in the same way 
 *  (voxel units)
 *  However, the underlying ImageScience library works in terms of the units defined in the image properties, allowing adjustment 
 *  for non-isometric voxels but potentially working on a very different scale to imageJ filters using the same sigma.
 *  
 *  Trainable weka divides the voxel dimensions by the voxel x dimension before calculating features, so effectively the voxel x 
 *  dimension is the unit for sigma. This means that non-isometric voxels are taken into account, but the scale is still broadly 
 *  compatible with the ImageJ filters.
 *  (This was verified for Trainable_Segmentation-3.2.33, with ImageScience features used via the provided API 
 *  trainableSegmentation.ImageScience; this was not always the cases, and an earlier version did not reconcile the 
 *  ImageJ and ImageScience scaling)
 *  
 *  We follow the Trainable Weka system here, using the voxel x dimension as the unit for sigma. We also call the ImageJ filters 
 *  with a 3d sigma parameter, allowing us to apply the same interpretation of sigma as for the ImageScience features. However, 
 *  we specify the voxel dimensions as parameters (pixelSize_xy and pixelSize_z), due to the risk of image properties being lost 
 *  in file format changes etc.
 *  
 *  When calculating features on a downsampled image, ImageScience features can be calculated with unadjusted sigma, since necessary 
 *  adjustment is done via change in image properties. But for ImageJ filters, the 3d sigma values must be adjusted according to the 
 *  downsampling.
 */

#@ String feature_model_table
#@ String imageStackLocation
#@ String imageStackName
#@ String modelName
#@ String featureSavePath
#@ String numberThreadsToUse
#@ Float pixelSize_xy
#@ Float pixelSize_z
#@ Float intensityScalingFactor
#@ String cropBox 

import ij.IJ;
import ij.io.FileSaver;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.measure.ResultsTable;
import trainableSegmentation.ImageScience
import java.text.SimpleDateFormat
import groovy.time.TimeCategory 
import groovy.time.TimeDuration

println("starting generate_save_features.groovy");
duplicator = new Duplicator(); // since native imagej feature generation functions are destructive
sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss")

if (numberThreadsToUse != null){
	IJ.run("Memory & Threads...", "parallel="+ numberThreadsToUse +" run"); // assume IJ.setThreads(n) would do the same thing?
}

date = new Date(); println("Opening image stack " + imageStackName + ".tif  " + sdf.format(date));
String filePath = imageStackLocation+imageStackName + ".tif"
println(filePath)

// file loading start: use try/catch and retry in attempt to reduce failure rate when access to image stack unreliable ******************
ImagePlus image=null;
for (int _try=0;_try<3;_try++){
	try{
		image  = IJ.openImage( imageStackLocation+imageStackName + ".tif");
	} catch(Exception e) {
    	println("Exception: ${e}")
    	image=null;
	}
	if (image!=null){break;}
	println("wait and try again")
	sleep(30000)
}
if (image==null){
	date = new Date(); println("Failed to load " + imageStackName + ".tif ; terminating   " + sdf.format(date));
	System.exit(0)
}
//println("Image loaded")

// following shouldn't be necessary, feature_model_table is small file that should be stored locally (eg in home directory on cluster)
ResultsTable model_features=null;
for (int _try=0;_try<3;_try++){
	try{
		model_features = ResultsTable.open(feature_model_table);
	} catch(Exception e) {
    	println("Exception: ${e}")
    	model_features=null;
	}
	if (model_features!=null){break;}
	sleep(30000)
}
if (model_features==null){
	date = new Date(); println("Failed to load " + feature_model_table + "; terminating   " + sdf.format(date));
	System.exit(0)
}
//println("feature_model_table loaded")
// file loading end ******************


if (cropBox!=null && cropBox.split(",").size()==6){
	cb = cropBox.split(",")
	int[] x = [0,0,0,0,0,0]
	allParsed=true
	for (int ii=0;ii<6;ii++){
		if (cb[ii].isInteger()){
			x[ii] = cb[ii] as int
		} else {
			allParsed = false
		}
	}
	if (allParsed){
		st = image.getImageStack()
	    st = st.crop(x[0], x[2], x[4], x[1]-x[0]+1, x[3]-x[2]+1, x[5]-x[4]+1)  	
	    image.setStack(st)
	} else {
		println("Failed to parse cropBox parameter into list of 6 integers - no cropping applied")
	}
}

// image conversion and scaling:
IJ.run("Conversions...", " ");
IJ.run(image, "32-bit", "");
IJ.run("Conversions...", "scale");
IJ.run(image,"Multiply...", "value="+ intensityScalingFactor +" stack");

println("Image scaled")

// set up calibration based on voxel dimensions
// these factors will be modified by currentDownsampling 
vx = pixelSize_xy ; vy = pixelSize_xy ; vz = pixelSize_z ; 
println("Voxel dimensions " + vx + "x" + vy + "x" + vz);
mult_y = vx/vy ; mult_z = vx/vz // scaling factor for ImageJ filter sigma; invert because larger dimension means fewer pixels in given distance

// now we have factors for ImageJ filters (which don't directly use the image properties), we set the image properties so that ImageScience works as we want
calib = image.getCalibration();
calib.pixelHeight = vy/vx
calib.pixelDepth = vz/vx
calib.pixelWidth = 1
image.setCalibration(calib);

date = new Date();  println("Calculating features for model " + modelName + "  " + sdf.format(date))

// for the hessian and structure features, one operation gives 3 features (eigenvalues); so persist matrix and the sigma it was calculated for
ArrayList<ImagePlus> hess
int hess_sigma = 0
ArrayList<ImagePlus>  struct_1
int struct_1_sigma = 0
ArrayList<ImagePlus>  struct_3
int struct_3_sigma = 0

/**
 * with downsampling, persist downsampled source image for efficiency
 * assumes features are ordered by downsampling factor, so no need to save additional downsampled versions
 * code will still work with features unordered by downsampling factors, but additional downsampling operations required
 */
ImagePlus currentImage = duplicator.run(image);
int[] currentDownsampling = [1,1,1]

// main loop through features
startTime = new Date();
float elapsedMinutesImageJfilters = 0.0;
println() ; println("feature	operation	parameter	sigma	group	start	end	duration_minutes")
for (int r=0; r<model_features.size(); r++){
	if (model_features.getStringValue(modelName,r) != "1"){continue} // test if feature given in row r is used by specified model 
	String fname = model_features.getStringValue("feature_name",r)
	String operation = model_features.getStringValue("operation",r)
	float parameter = model_features.getValue("parameter",r)
	float s = model_features.getValue("sigma",r)
	String group = model_features.getStringValue("group",r)
	if (group == "derived"){continue;}

	// get downsampling factor and determine whether it has changed from previous feature (or default [1,1,1] if first feature)
	// If so, redo downsampling from original image 
	String downSampleFactor = model_features.getStringValue("downsample",r)
	boolean changedDownSampleFactor = false
	int[] dsf = [1,1,1]
	if (downSampleFactor != ""){
		String[] dsfs = downSampleFactor.split("_");
		dsf = [dsfs[0].toInteger(),dsfs[1].toInteger(),dsfs[2].toInteger()]
	}
	if (dsf != currentDownsampling){
		currentDownsampling = dsf;
		changedDownSampleFactor = true
		println("Changing down sampling factor to " + dsf);
		currentImage = duplicator.run(image);
		if (currentDownsampling != [1,1,1]){
			IJ.run(currentImage, "Size...", 
			"width=" + Math.round(image.getWidth()/currentDownsampling[0]) +
			" height=" + Math.round(image.getHeight()/currentDownsampling[1]) + 
			" depth=" + Math.round(image.getNSlices()/currentDownsampling[2]) + " average interpolation=Bilinear");
		}
	}
	
	fStartTime = new Date();
	ImagePlus newImage 
	if (group == "original"){
		newImage = duplicator.run(currentImage);
	}
	if (group=="IJ_filter"){
		newImage = duplicator.run(currentImage);
		IJ.run(newImage, operation, "x=" + s/ ((float) currentDownsampling[0]) + 
                                           " y=" + s*mult_y /  ((float) currentDownsampling[1]) + 
                                           " z=" + s*mult_z /  ((float) currentDownsampling[2]));
	}
	if (operation=="Hessian"){
		if ( (hess == null) || (hess_sigma != s) || changedDownSampleFactor){
		  hess = ImageScience.computeHessianImages(s, true, currentImage)
		  hess_sigma = s
		}
		int index = Math.round(parameter)
		newImage = hess[index]
	}
	if (operation=="Derivatives"){
		int ii = Math.round(parameter)
		newImage = ImageScience.computeDifferentialImage(s, ii, ii, ii, currentImage)
	}
	if (operation=="Laplacian"){
		newImage = ImageScience.computeLaplacianImage(s,currentImage)
	}
	if (operation=="Edges"){
		newImage = ImageScience.computeEdgesImage(s,currentImage)
	}
	if (operation=="Structure_1"){
		if ( (struct_1 == null) || (struct_1_sigma != s) || changedDownSampleFactor){
			struct_1 = ImageScience.computeEigenimages(s,1.0,currentImage)
		    struct_1_sigma = s
		}
		int index = Math.round(parameter)
		newImage = struct_1[index]
	}
	if (operation=="Structure_3"){
		if ( (struct_3 == null) || (struct_3_sigma != s) || changedDownSampleFactor){
			struct_3 = ImageScience.computeEigenimages(s,3.0,currentImage)
		    struct_3_sigma = s
		}
		int index = Math.round(parameter)
		newImage = struct_3[index]
	}

	if (newImage != null){
		if (currentDownsampling != [1,1,1]){
			IJ.run(newImage, "Size...", "width=" + image.getWidth() +
			" height=" + image.getHeight() + 
			" depth=" + image.getNSlices() + " average interpolation=Bilinear");
		}
		new File(featureSavePath + fname).mkdirs() 
		IJ.run(newImage, "Image Sequence... ", "format=TIFF name=slice_ save=" + featureSavePath + fname + "/slice_0000.png");
	}
	fEndTime = new Date();
	float fDurationMinutes = TimeCategory.minus( fEndTime, fStartTime ).toMilliseconds()/60000
	if (group=="IJ_filter"){elapsedMinutesImageJfilters+=fDurationMinutes}
	print(fname + "\t" + operation + "\t" + parameter + "\t" + s + "\t" + group + "\t")
	println(sdf.format(fStartTime) + "\t" + sdf.format(fEndTime) + "\t" + fDurationMinutes)
}

endTime = new Date();
float durationMinutes = TimeCategory.minus( endTime, startTime ).toMilliseconds()/60000

println("ImageJ_filters_total" + "\t\t\t\t\t\t\t" + elapsedMinutesImageJfilters)
println("ImageScience_total" + "\t\t\t\t\t\t\t" + (durationMinutes-elapsedMinutesImageJfilters))
println("Total" + "\t\t\t\t\t" + sdf.format(startTime) + "\t" + sdf.format(endTime) + "\t" + durationMinutes)
println()
