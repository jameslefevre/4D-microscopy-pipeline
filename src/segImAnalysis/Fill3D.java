package segImAnalysis;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.ImageCalculator;
import mcib3d.image3d.ImageByte;
import mcib3d.image3d.processing.FillHoles3D;

public class Fill3D {
	public static ImagePlus fill3D(ImagePlus im){
		return(fill3D(im,4));
	}
	public static ImagePlus fill3D(ImagePlus im, int nCpus){
		ImageByte ib = new ImageByte(im);
		FillHoles3D.process(ib, 255, nCpus, true); // flag is "verbose"
		Calibration calib = im.getCalibration();
		ImagePlus im2 = new ImagePlus("filled",ib.getImageStack());
		im2.setCalibration(calib);
		return(im2);
	}

	// int[] classNumsToFill = [1,3,2]
	public static void fillHolesSpecifiedClasses(ImagePlus classMap, int[] classNumsToFill){
		for (int classNum : classNumsToFill){
			IJ.log(Integer.toString(classNum));
			ImagePlus classBinary = classMap.duplicate();
			SegmentedImageOperations.selectChannel(classBinary,classNum);
			//System.out.println("b");
			classBinary = fill3D(classBinary);
			// now to adjust classMap to remove any additional voxels in this class from any other classes
			// first zero out these voxels in classMap
			IJ.run(classBinary,"Macro...", "code=v=1*(v==0) stack");
			ImageCalculator ic = new ImageCalculator();
			ic.run("Multiply stack", classMap,classBinary);
			// now add the class values back
			IJ.run(classBinary,"Macro...", "code=v="+ classNum + "*(v==0) stack");
			ic.run("Add stack", classMap,classBinary);
		}
	}

}
