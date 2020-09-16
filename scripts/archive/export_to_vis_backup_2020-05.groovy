
// TODO:  finish and test this doc; see old versions up to 6

#@ String imageFolder
#@ String segFolder
#@ String saveFolder
#@ String modelName
#@ String segName
#@ String stackNums
#@ String stackNumberPrefix
#@ String stackNumberSuffix
#@ String convertRawSegProb
#@ String cropRawSegProb
#@ String cropBox
#@ String intensityRange
#@ String channelSelection

/**
 * stackNums: e.g. [1,101,201,301] or 1..10
 * convertRawSegProb: array of 3 flags (true/false) indicating which of the 3 image types (raw (deconvolved) image, segmentation, probability maps) should be exported as png stacks
 * cropRawSegProb: array of 3 flags (true/false) indicating which of the 3 image types should be cropped before conversion
 * cropBox: [[min_x,max_x],[min_y,max_y],[min_z,max_z]], 0 indexed, endpoints included
 * intensityRange: [min,max] : range of intensities in raw image to represent in pngs (values below and above are mapped to 0 and max value respectively)
 * channelSelection: [red_channel,green_channel,blue_channel]; which prob map channels to map to the RGB channels
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
int[][] cropBox = Eval.me(cropBox);
int[] intensityRange = Eval.me(intensityRange);
int[] channelSelection = Eval.me(channelSelection);

// this is where I should test dimensions of structured parameters ...

new File(saveFolder).mkdirs() 
println(imageFolder)
def rawDir = new File(imageFolder);

// extract the file names from the imageFolder with stack numbers in the specified set
files = rawDir.listFiles();
println("Image folder contains " + files.size() + " files")
ArrayList<String> nameSet = new ArrayList<String>();
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
		nameSet.add(fn);
	}
}
println(nameSet);

// now cycle through the 3 image types

if (convertRawSegProb[0]){	
	for (String nm : nameSet){
		sourceFile = imageFolder + nm + ".tif"
		println("Opening " + sourceFile)
	    ImagePlus im  = IJ.openImage(sourceFile);
	    if (cropRawSegProb[0] && im !=null){
	    	st = im.getImageStack()
	    	st = st.crop(cropBox[0][0], cropBox[1][0], cropBox[2][0], cropBox[0][1]-cropBox[0][0]+1 , cropBox[1][1]-cropBox[1][0]+1, cropBox[2][1]-cropBox[2][0]+1)    	
	    	im.setStack(st)
	    }
        saveStackToPngImageArray(im, saveFolder + "deconv/" + nm + "/",intensityRange[0],intensityRange[1])
    }
}

// segmented images
if (convertRawSegProb[1]){

	for (String nm : nameSet){
		sourceFile = segFolder + segName + "/segmented/" + nm + "_seg_" + modelName + ".tif"
		//println("loading " + sourceFile)
		println("Opening " + sourceFile)
	    ImagePlus im  = IJ.openImage(sourceFile);
	    if (cropRawSegProb[1] && im !=null){
	    	st = im.getImageStack()
	    	st = st.crop(cropBox[0][0], cropBox[1][0], cropBox[2][0], cropBox[0][1]-cropBox[0][0]+1 , cropBox[1][1]-cropBox[1][0]+1, cropBox[2][1]-cropBox[2][0]+1)    	
	    	im.setStack(st)
	    }  
        saveStackToPngImageArray(im, saveFolder + segName + "/segmented/" + nm + "/")
    }
}

// prob maps starting as 8 or 32 bit 4 channel composite (still have background)
if (convertRawSegProb[2]){	
	
	ChannelArranger channelArranger = new ChannelArranger()
	byte[] crusc = new byte[256]
	byte[] zs = new byte[256]
	for (int ii=0; ii<256; ii++){crusc[ii]=ii}
	LUT[] rgbLUTs = [new LUT(crusc,zs,zs), new LUT(zs,crusc,zs), new LUT(zs,zs,crusc)]
	

    for (String nm : nameSet){
    	sourceFile = segFolder + segName + "/probability_maps/" + nm + "_prob_" + modelName + ".tif"
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
      // saveStackToPngImageArray(probMap, saveFolder + segName + "/probMap/" + nm + "/")
      saveStackToPngImageArray(probMap, saveFolder + segName + "/probability_maps/" + nm + "/")
	}
}
println("done")

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


