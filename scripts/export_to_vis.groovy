
/**
 * 
 * Bulk copy and convert image data to a form suitable for custom visualiser tool.
 * Input data is as tiff image stacks, with file names that can be parsed to get a stack number.
 * Output data is as png files, each representing a single z-slice; each 3D image stack is converted to a folder
 * (named for the stack) containing png files named slice0000.png to slice[max].png, where [max] is the number of slices 
 * minus 1, left padded with zeroes to width 4.
 * Expects 3 input types: see imageFolder, segFolder, probMapFolder documentation below.
 * Can choose to process some or all of these types.
 * 
 * @param imageFolder
 * Path to folder containing pre-processed microscopy image data (as a series of single-channel tiff stacks).
 * @param segFolder
 * Path to folder containing segmented image stacks (as a series of single-channel 8-bit tiff stacks).
 * @param probMapFolder
 * Path to folder containing the probability maps of the segmentations (as a series of multi-channel 8-bit tiff stacks).
 * @param imageSaveFolder
 * Root folder to save converted microscopy imaging.
 * @param segSaveFolder
 * Root folder to save converted segmentations.
 * @param probMapSaveFolder
 * Root folder to save converted segmentation probability maps.
 * @param alwaysSaveWithImageStackNames
 * If true, the converted segmentation and probability map stacks are saved under the name of the corresponding
 * microscopy image stack, rather than the (possibly different) name of the actual stack converted. This simplifies the later 
 * lookup process (the data can be distinguised since they are in different root folders: see imageSaveFolder, segSaveFolder, 
 * probMapSaveFolder)
 * @param stackNums
 * The time steps / stack numbers to convert, specified as a list of numbers, e.g. [1,101,201,301], or a range, e.g. 1..10
 * Specifying an excessive range will not cause an error; time steps where no tiff stack exists will simply be skipped.
 * @param stackNumberPrefix
 * A string identifying the start of the stack number in the filename (original image; stack number may be left-padded with zeroes).
 * @param stackNumberSuffix
 * A string identifying the end of the stack number in the filename (original image). The script will extract the text from the filename 
 * between stackNumberPrefix and stackNumberSuffix, and attempt to read it as an integer (leading zeros ignored), 
 * which identifies the stack.
 * @param stackNumberPrefixSeg
 * stackNumberPrefix, but for segmented images
 * @param stackNumberSuffixSeg
 * stackNumberSuffix, but for segmented images
 * @param stackNumberPrefixProb
 * stackNumberPrefix, but for probability map images
 * @param stackNumberSuffixProb
 * stackNumberSuffix, but for probability map images
 * @param convertRawSegProb
 * Array of 3 flags (true/false), e.g. [true,true,false], indicating which of the 3 image types 
 * (raw (deconvolved) image, segmentation, probability maps) should be exported as png stacks.
 * @param cropRawSegProb
 * Array of 3 flags (true/false) indicating which (if any) of the 3 image types should be cropped before conversion.
 * @param cropBox
 * Specification of 3D cropping area, for any cases where cropping is applied: 
 * [[min_x,max_x],[min_y,max_y],[min_z,max_z]], 0 indexed, endpoints included
 * @param intensityRange
 * [min,max] : range of intensities in microscopy image to represent in pngs (values below and above are mapped to 
 * 0 and max value respectively). The processed image will be 8-bit, generally much smaller than the original image 
 * (typically 32 bit), so truncation of the range is required due to the very high dynamic range often seen in microscopy data.
 * @param channelSelection
 * An array of up to 3 integers [red_channel,green_channel,blue_channel], each between 1 and the number of segmentation classes; 
 * specifies which probability map channels to map to the RGB channels. Values out of this range will cause a run-time error. Channels 
 * which are not included in this array will not be represented in the visualiser probability map.
 * Using RGB colouring, we can represent a probability map across at most 3 classes (technically 4, where the 4th is background/other,
 * represented by black or transparent). Note that this limitation does not apply to the actual segmentation, where the only limit to
 * the classes that can be represented is the number of colours that can be distinguished.
**/


#@ String imageFolder
#@ String segFolder
#@ String probMapFolder
#@ String imageSaveFolder
#@ String segSaveFolder
#@ String probMapSaveFolder
#@ String alwaysSaveWithImageStackNames
#@ String stackNums
#@ String stackNumberPrefix
#@ String stackNumberSuffix
#@ String stackNumberPrefix
#@ String stackNumberPrefixSeg
#@ String stackNumberSuffixSeg
#@ String stackNumberPrefixProb
#@ String stackNumberSuffixProb
#@ String convertRawSegProb
#@ String cropRawSegProb
#@ String cropBox
#@ String intensityRange
#@ String channelSelection


import ij.IJ
import ij.io.FileSaver
import java.io.File
import ij.ImagePlus
import ij.ImageStack
import ij.plugin.ChannelArranger
import ij.process.LUT


println("\n*** Starting export_to_vis.groovy ***\n");
// conversion of structured parameters
int[] stackNums  = Eval.me(stackNums)
boolean[] convertRawSegProb = Eval.me(convertRawSegProb);
boolean[] cropRawSegProb = Eval.me(cropRawSegProb);
int[][] cb
if (cropBox=="" || cropBox==null){
	cb=null
	//println("cropBox not converted!")
	} else {
		cb = Eval.me(cropBox);
		//println("cropBox converted!")
		}
int[][] cropBox = cb
int[] intensityRange = Eval.me(intensityRange);
int[] channelSelection = Eval.me(channelSelection);
boolean alwaysSaveWithImageStackNames = Eval.me(alwaysSaveWithImageStackNames)

if (imageFolder[-1]!="/"){imageFolder=imageFolder+"/";}
if (segFolder[-1]!="/"){segFolder=segFolder+"/";}
if (probMapFolder[-1]!="/"){probMapFolder=probMapFolder+"/";}
if (imageSaveFolder[-1]!="/"){imageSaveFolder=imageSaveFolder+"/";}
if (segSaveFolder[-1]!="/"){segSaveFolder=segSaveFolder+"/";}
if (probMapSaveFolder[-1]!="/"){probMapSaveFolder=probMapSaveFolder+"/";}

// this is where I could test dimensions of structured parameters ...

nameSetImage = getFileNames(imageFolder, stackNumberPrefix, stackNumberSuffix, stackNums)

println("\nFound following file names to process: ");
println(nameSetImage);
// now cycle through the 3 image types

if (convertRawSegProb[0]){	
	println("convert original images");
	for (String nm : nameSetImage.values()){
		sourceFile = imageFolder + nm + ".tif"
		println("Opening " + sourceFile)
	    ImagePlus im  = IJ.openImage(sourceFile);
	    if (cropRawSegProb[0] && im !=null){
	    	st = im.getImageStack()
	    	if (st==null){println("null stack!!!");}
	    	//println("crop box: " + cropBox)	    
	    	//println("image dims: " + st.getWidth() + ", " + st.getHeight() + ", " + st.getSize())
	    	println("crop params: " + cropBox[0][0] + ", " + cropBox[1][0] + ", " + cropBox[2][0] + ", " + (cropBox[0][1]-cropBox[0][0]+1)  + ", " + (cropBox[1][1]-cropBox[1][0]+1) + ", " + (cropBox[2][1]-cropBox[2][0]+1))
	    	st = st.crop(cropBox[0][0], cropBox[1][0], cropBox[2][0], cropBox[0][1]-cropBox[0][0]+1 , cropBox[1][1]-cropBox[1][0]+1, cropBox[2][1]-cropBox[2][0]+1)    	
	    	//println("image dims2: " + st.getWidth() + ", " + st.getHeight() + ", " + st.getSize())
	    	im.setStack(st)
	    }
        saveStackToPngImageArray(im, imageSaveFolder + nm + "/",intensityRange[0],intensityRange[1])
    }
}

// segmented images
if (convertRawSegProb[1]){
	println("Doing seg images:");
	nameSet = getFileNames(segFolder, stackNumberPrefixSeg, stackNumberSuffixSeg, stackNums)
	println("\nSegmented image file names to process: ");
	println(nameSet);
	for (stackNum in nameSet.keySet()){
		nm = nameSet[stackNum]
		sourceFile = segFolder + nm + ".tif"
		//println("loading " + sourceFile)
		println("Opening " + sourceFile)
	    ImagePlus im  = IJ.openImage(sourceFile);
	    if (cropRawSegProb[1] && im !=null){
	    	st = im.getImageStack()
	    	st = st.crop(cropBox[0][0], cropBox[1][0], cropBox[2][0], cropBox[0][1]-cropBox[0][0]+1 , cropBox[1][1]-cropBox[1][0]+1, cropBox[2][1]-cropBox[2][0]+1)    	
	    	im.setStack(st)
	    }
	    if (alwaysSaveWithImageStackNames){nm = nameSetImage[stackNum]}
        saveStackToPngImageArray(im, segSaveFolder + nm + "/")
    }
}

// prob maps starting as 8 or 32 bit 4 channel composite (still have background)
if (convertRawSegProb[2]){	
	println("Doing prob images:");
	ChannelArranger channelArranger = new ChannelArranger()
	byte[] crusc = new byte[256]
	byte[] zs = new byte[256]
	for (int ii=0; ii<256; ii++){crusc[ii]=ii}
	LUT[] rgbLUTs = [new LUT(crusc,zs,zs), new LUT(zs,crusc,zs), new LUT(zs,zs,crusc)]
	
	nameSet = getFileNames(probMapFolder, stackNumberPrefixProb, stackNumberSuffixProb, stackNums)
	println("\nProb image file names to process: ");
	println(nameSet);
    for (stackNum in nameSet.keySet()){
    	nm = nameSet[stackNum]
    	sourceFile = probMapFolder + nm + ".tif"
    	println("opening " + sourceFile)
      ImagePlus probMap = IJ.openImage(sourceFile);
      	if (probMap==null){
		println("File not found (skip and move on)")
		continue
	  }
      if (probMap.getBitDepth()>8){
		println("Converting to 8-bit")
	    IJ.run(probMap,"Multiply...", "value=255 stack")
	    IJ.run("Conversions...", " ");
        IJ.run(im, "8-bit", "");
        IJ.run("Conversions...", "scale");
      }
      
      probMap = channelArranger.run(probMap, channelSelection)
      // println(probMap.getDimensions())
      probMap.setLuts(rgbLUTs) 
      probMap.setDisplayMode(IJ.COMPOSITE) // Sets the display mode of composite color images, where 'mode' should be IJ.COMPOSITE, IJ.COLOR or IJ.GRAYSCALE.
	    // want composite images in composite mode so that we get a single coloured png per slice
		IJ.run(probMap, "RGB Color", "slices");
      if (cropRawSegProb[2] && probMap !=null){
	    	st = probMap.getImageStack()
	    	st = st.crop(cropBox[0][0], cropBox[1][0], cropBox[2][0], cropBox[0][1]-cropBox[0][0]+1 , cropBox[1][1]-cropBox[1][0]+1, cropBox[2][1]-cropBox[2][0]+1)    	
	    	probMap.setStack(st)
	    }
	  if (alwaysSaveWithImageStackNames){nm = nameSetImage[stackNum]}
      saveStackToPngImageArray(probMap, probMapSaveFolder + nm + "/")
	}
}
println("done")
System.exit(0);

Map getFileNames(String folderPath, String stackNumberPrefix, String stackNumberSuffix, int[] stackNums){
	println(folderPath)
	def rawDir = new File(folderPath);

	// extract the file names from the imageFolder with stack numbers in the specified set
	files = rawDir.listFiles();
	println("Image folder contains " + files.size() + " files")
	//ArrayList<String> nameSet = new ArrayList<String>();
	nameSet = [:]
	for (String fn : files){
		x = fn.tokenize("/")[-1].tokenize(".")
		if (x.size()!=2){continue}
		if (x[1] != "tif" && x[1] != "tiff"){continue}
		fn = x[0]
		println(fn)
		fn_complete=fn
	
		// extract stack number
		if (stackNumberPrefix!=""){
		splt_fn = fn.split(stackNumberPrefix)
		//println(splt_fn.size())
		if (splt_fn.size()<2){continue}
		fn = splt_fn[1]
		} 
		if (stackNumberSuffix!=""){
		splt_fn = fn.split(stackNumberSuffix)
		//println(splt_fn.size())
		if (splt_fn.size()<2){continue}
		//println(splt_fn[0])
		fn=splt_fn[0]
		}
		int stNum = fn.toInteger();
		//println(stNum)
	
		if (stackNums.contains(stNum)){
			//nameSet.add(fn);
			nameSet[stNum]=fn_complete
		}
	}
	println(nameSet);
	return(nameSet);
}

void saveStackToPngImageArray(ImagePlus im, String saveFolder){	
	saveStackToPngImageArray(im,saveFolder,null,null);
}

void saveStackToPngImageArray(ImagePlus im, String saveFolder, Integer minIntensity, Integer maxIntensity){
	if (im==null){
		println("File not found (skip and move on)")
		return
	}
	println(im.getDimensions())
	println(im.getDisplayMode())
	if (im.isComposite()){
		im.setDisplayMode(IJ.COMPOSITE) // Sets the display mode of composite color images, where 'mode' should be IJ.COMPOSITE, IJ.COLOR or IJ.GRAYSCALE.
	    // want composite images in composite mode so that we get a single coloured png per slice
	    println(im.getDisplayMode())
		IJ.run(im, "RGB Color", "slices");
	}
	def folder = new File( saveFolder )
	if( !folder.exists() ) {
     // Create all folders up-to and including B
     folder.mkdirs()
    }
    if ((minIntensity != null) && (maxIntensity != null) && (minIntensity < maxIntensity)){
    	IJ.setMinAndMax(im, minIntensity, maxIntensity);
    	IJ.run(im, "8-bit", ""); 	
    }
    // IJ.run(im, "8-bit", "");
    println("saving to: " + saveFolder);
    // println("Check that im is not null: " + (im!=null));
    // new FileSaver( im ).saveAsTiff(saveFolder+".tif");
	IJ.run(im, "Image Sequence... ", "format=PNG name=slice save='" + saveFolder + "slice0000.png'");
}


