package segImAnalysis;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
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
			
			// now adjust classMap
			ImageStack classSt = classMap.getStack();
			ImageStack maskSt = classBinary.getStack();
			int width=classSt.getWidth();int height=classSt.getHeight();int slices=classSt.getSize();
			for (int x=0;x<width;x++) {for (int y=0;y<height;y++) {for (int z=0;z<slices;z++) {
				if (maskSt.getVoxel(x,y,z) > 0) {
					classSt.setVoxel(x,y,z,classNum);
				}
			}}}
			classMap.setStack(classSt);
		}
	}
}
