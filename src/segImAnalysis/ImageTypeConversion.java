package segImAnalysis;
import ij.IJ;

// import static Util.*;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileSaver;
import ij.process.StackStatistics;

public class ImageTypeConversion {
	// could add methods that use im.getDimensions()
	public static ImagePlus makeComposite(ImagePlus im, int numberChannels, int[] channelSelect){
		return(makeComposite(im.getStack(),numberChannels,channelSelect,im.getTitle() + "_composite"));
	}

	public static ImagePlus makeComposite(ImageStack imStack, int numberChannels, int[] channelSelect){
		return(makeComposite(imStack,numberChannels,channelSelect,"composite"));
	}

	public static ImagePlus makeComposite(ImageStack imStack, int numberChannels, int[] channelSelect, String title){
		int n = imStack.size();
		Util.println("Number of ImageStack slices: " + n);
		if (n % numberChannels != 0){
			Util.println("!!! Parameter numberChannels does not evenly divide the number of slices in the stack provided !!!");
			Util.println("Returning null");
			return(null);
		}
		int m = Math.round(n/numberChannels);
		Util.println("Composite image will have " + m + " slices");
		Util.println("Selecting slices " + channelSelect);
		ImageStack newStack = new ImageStack(imStack.getWidth(),imStack.getHeight() );
		for (int sl=1; sl<=m; sl++){
			for (int ch : channelSelect){
				newStack.addSlice(imStack.getProcessor((sl-1)*numberChannels+ch));
			}
		}
		Util.println("Number of slices in new stack: " + newStack.size());
		ImagePlus compImage = new ImagePlus("composite",newStack);
		compImage.setDimensions(channelSelect.length,m,1);
		return(new CompositeImage(compImage));
	}

	public static ImagePlus convertProbMapTo8bitRGB(ImagePlus probMap, int[] channelSelection){
		// if bit depth >8, assume that probMap stores probabilities properly, on [0,1] interval
		if (probMap.getBitDepth()>8){
			Util.println("Converting to 8-bit");
		  IJ.run(probMap,"Multiply...", "value=255 stack");
	      imageTypeChangeTrueValue(probMap,"8-bit");
		}
		//println(probMap.getDimensions())
	    probMap = makeComposite(probMap,4,channelSelection);
	    probMap.setDisplayMode(IJ.COMPOSITE);
	    //println(probMap.getDimensions())
	    return probMap;
	}

	public static void saveProbMapAs8bitRGB(ImagePlus probMap, String savePath, int[] channelSelection){
		convertProbMapTo8bitRGB(probMap,channelSelection);
	    new FileSaver( probMap ).saveAsTiff( savePath );
	}
	public static void saveProbMapAs8bitRGB(String probMapPath, String savePath, int[] channelSelection){
		ImagePlus probMap  = IJ.openImage(probMapPath);
		saveProbMapAs8bitRGB(probMap,savePath, channelSelection);
	}


	// convert between 8-bit, 16-bit, 32-bit, 8-bit color (source only)
	// want to prevent scaling, use true 8-bit color values (index) instead of LUT values, and check for overflow

	public static void imageTypeChangeTrueValue(ImagePlus im, String newType){
		imageTypeChangeTrueValue(im, newType, true);
	}

	public static void imageTypeChangeTrueValue(ImagePlus im, String newType,  boolean checkOverFlow){
	  	assert(im.getType() == ImagePlus.GRAY8 || im.getType() == ImagePlus.GRAY16 || im.getType() == ImagePlus.GRAY32);
	  	assert(newType == "8-bit" || newType == "16-bit" || newType == "32-bit");
	  	if (im.getBitDepth() <= 16){
	  		IJ.run(im, "Grays", "");
	  	}
	  	if (checkOverFlow){
	  		int max = (int) (Math.pow(2,im.getBitDepth())-1);
	  		if ((new StackStatistics(im)).max>max){
	  			Util.println("Conversion failed - maximum allowed value in target type insufficient to store maximum pixel value");
	  			return;
	  		}
	  	}
	  	IJ.run("Conversions...", " ");
	    IJ.run(im, newType, "");
	    IJ.run("Conversions...", "scale");
	}

}
