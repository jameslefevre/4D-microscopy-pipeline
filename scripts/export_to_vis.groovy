
// TODO:  finish and test this doc; see old versions up to 6

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
#@ String convertRawSegProb
#@ String cropRawSegProb
#@ String cropBox
#@ String intensityRange
#@ String channelSelection

/**
 * alwaysSaveWithImageStackNames: true/false flag; if true, the converted tiff stacks for the segmentation and probability maps 
 * 		at a given time will be saved under the filename of the corresponding original image stack. This allows consistent naming to be restored between image types.
 * stackNums: the time steps / stack numbers to convert, specified as a list of numbers, e.g. [1,101,201,301], or a range, e.g. 1..10
 * 		Specifying an excessive range will not cause an error; time steps where no tiff stack exists will simply be skipped
 * stackNumberPrefix: A string preceding the stack number in each file name.
 * stackNumberSuffix: A string following the stack number in each file name. It is assumed that the section of the filename between these two strings (text snippets)
 * 		always contains the stack number (leading zeroes allowed).
 * convertRawSegProb: array of 3 flags (true/false), e.g. [true,true,false], indicating which of the 3 image types 
 * 		(raw (deconvolved) image, segmentation, probability maps) should be exported as png stacks
 * cropRawSegProb: array of 3 flags (true/false) indicating which of the 3 image types should be cropped before conversion
 * cropBox: specification of 3D cropping area, if cropping is applied: [[min_x,max_x],[min_y,max_y],[min_z,max_z]], 0 indexed, endpoints included
 * intensityRange: [min,max] : range of intensities in raw image to represent in pngs (values below and above are mapped to 0 and max value respectively)
 * channelSelection: An array of 3 integers [red_channel,green_channel,blue_channel], each between zero and the number of segmentation classes minus 1; 
 * 		specifies which probability map channels to map to the RGB channels
**/

import ij.IJ
import ij.io.FileSaver
import java.io.File
import ij.ImagePlus
import ij.ImageStack
import ij.plugin.ChannelArranger
import ij.process.LUT

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

// now cycle through the 3 image types

if (convertRawSegProb[0]){	
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
	nameSet = getFileNames(segFolder, stackNumberPrefix, stackNumberSuffix, stackNums)
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
	ChannelArranger channelArranger = new ChannelArranger()
	byte[] crusc = new byte[256]
	byte[] zs = new byte[256]
	for (int ii=0; ii<256; ii++){crusc[ii]=ii}
	LUT[] rgbLUTs = [new LUT(crusc,zs,zs), new LUT(zs,crusc,zs), new LUT(zs,zs,crusc)]
	
	nameSet = getFileNames(probMapFolder, stackNumberPrefix, stackNumberSuffix, stackNums)
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
	
		// extract stack number
		splt_fn = fn.split(stackNumberPrefix)
		if (splt_fn.size()<2){continue}
		part_fn = splt_fn[1]
		splt_fn = part_fn.split(stackNumberSuffix)
		if (splt_fn.size()<2){continue}
		int stNum = splt_fn[0].toInteger();
		// println(stNum)
	
		if (stackNums.contains(stNum)){
			//nameSet.add(fn);
			nameSet[stNum]=fn
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
    
    println("saving to: " + saveFolder);
	IJ.run(im, "Image Sequence... ", "format=PNG name=slice save='" + saveFolder + "slice0000.png'");
}


