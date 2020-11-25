
/**
 * Processes a folder containing tiff files, to produce maximum intensity projections (MIPs) and other summary information.
 * Expects input files to be single channel image stacks (3D), with file names that can be parsed to get a stack number.
 * The intended purpose is preliminary analysis of pre-processed image data.
 * Each stack is opened sequentially in alphanumerical order, a stack number is parsed from the filename, then several outputs 
 * are produced:
 * 
 * MIP: each stack is reduced to a single slice using a maximum intensity projection along the z-axis (so pixel (x,y) is assigned 
 * the maximum value over the voxels (x,y,1) ... (x,y,z_max), where 1 ... z_max are the slice numbers) 
 * MIP_y: same except projected along the y-axis
 * StackSummary: the mean, median, min, max, standard deviation and total voxel count for each stack
 * SliceSummary: the mean, median, min, max, and standard deviation for each z slice within each stack
 * StackHistsSqrt: A 1000-bin histogram of the square roots of the intensity values within each stack 
 * 
 * The MIP and MIP_y outputs are compiled into image stacks after primary processing (with dimensions x,y,time and x,z,time).
 * The folders of individual MIP slices should be deleted once the compiled stacks are verified.
 * Other data is aggregated across stacks into a single table for each of the 3 putput types, and saved as csv files. 
 * StackSummary and SliceSummary have column/field labels, and stack numbers are marked by the "time" field (SliceSummary also has a "slice" 
 * field.
 * StackHistsSqrt contains no header or any other explanatory information. Each row represents a stack, listed in the order processed.
 * The row contains 1000 values, representing voxel counts in 1000 bins, equally spaced from sqrt(min value) to sqrt(max value); the min 
 * and max for each stack must be obtained from StackSummary. The square root (equivalently, gamma transform with gamma=0.5) is designed 
 * to compress the intensity range produced from the LLS microscope to give histograms where the distibution in the majority of voxels 
 * is not obscured by a small number of very high intensity voxels.
 * 
 * 
 * @param inputPath
 * Absolute path to folder containing tif files (expects training "/")
 * @param outputPath
 * Absolute path of folder to save output files (expects training "/")
 * @param cacheFolder
 * Path to a temporary folder where the input tiff stack can be copied before opening, and the output
 * files can be kept until complete. This can be useful when the source data
 * is not local, as directly loading tiffs on a network connection may be slow, as can adding rows to tables.
 * If blank or not a valid path, tiff stacks are opened directly. 
 * @param stackNumberPrefix
 * A string identifying the start of the stack number in the filename (stack number may be left-padded with zeroes).
 * @param stackNumberSuffix
 * A string identifying the end of the stack number in the filename. The script will extract the text from the filename 
 * between stackNumberPrefix and stackNumberSuffix, and attempt to read it as an integer (leading zeros ignored), 
 * which identifies the stack.
 */

#@ String inputPath
#@ String outputPath
#@ String cacheFolder
#@ String stackNumberPrefix
#@ String stackNumberSuffix

import ij.IJ
import ij.io.FileSaver
import java.io.File
import ij.ImagePlus
import ij.ImageStack
import ij.process.StackStatistics
import ij.process.ImageStatistics
import ij.plugin.ZProjector
import ij.plugin.Slicer
import ij.plugin.FolderOpener

// input = "/home/james/mnt/rdm_adam/FromNick/LLSM/cell11/Decon_output/";
// output = "/data/james/image_data/LLS/20190513_cell11/";

println("cacheFolder parameter is "+cacheFolder)

localCache = cacheFolder != "" && new File(cacheFolder).exists()
savePath = localCache ? cacheFolder : outputPath

println("Using local cache? " + localCache)

MIPSfolder = savePath + "MIPS/";
MIPSfolderY = savePath + "MIPS_y/";

def folder = new File( outputPath )
if( !folder.exists() ) {
     folder.mkdirs() // Create all folders up-to and including folder
}
folder = new File( outputPath + "MIPS/" )
if( !folder.exists() ) {
 folder.mkdirs()
}
folder = new File( outputPath + "MIPS_y/" )
if( !folder.exists() ) {
  folder.mkdirs()
}

if (localCache){
	// set up the local copy of the save folders
	folder = new File( savePath )
	if( !folder.exists() ) {
	     folder.mkdirs()
	}
	folder = new File( MIPSfolder )
	if( !folder.exists() ) {
	 folder.mkdirs()
	}
	folder = new File( MIPSfolderY )
	if( !folder.exists() ) {
	  folder.mkdirs()
	}
}


def inputDir = new File(inputPath);
files = inputDir.listFiles().sort();

File fl = new File(savePath+"StackSummary.csv")
fw = fl.newWriter(false) // append
fw << ("time\tmean\tmedian\tmin\tmax\tstdev\tarea\thistMin\thistMax\n");
fw.close();

fl = new File(savePath+"SliceSummary.csv")
fw = fl.newWriter(false) // append
fw << ("time\tslice\tmean\tmedian\tmin\tmax\tstdev\n");
fw.close();

for (fnum=0;fnum<files.size();fnum++){ // files.size()
	println(files[fnum])
	x = ((String) files[fnum]).tokenize("/")[-1].tokenize(".")
	if (x.size()!=2){continue} // reject if filename has no extension
	if (x[1] != "tif" && x[1] != "tiff"){continue} // reject if not a tif/tiff file
	fn = x[0]
	println(fn)
	
	// code to extract stack number from filename - depends on format	
	// if this fails stack will not be processed
	splt_fn = fn.split(stackNumberPrefix)
	if (splt_fn.size()<2){continue}
	fn = splt_fn[1]
	splt_fn = fn.split(stackNumberSuffix)
	if (splt_fn.size()<2){continue}
	int stNum = splt_fn[0].toInteger();
	println(stNum)


	ImagePlus im = null;
	if (localCache){
		new File(cacheFolder+'temp.tif') << new File((String) files[fnum]).bytes
		im = IJ.openImage(cacheFolder+'temp.tif');
	} else {
		im = IJ.openImage((String) files[fnum]);
	} 
	
	maxProj = ZProjector.run(im,"max")
	new FileSaver( maxProj ).saveAsTiff( MIPSfolder + "MAX_"+fn + ".tif");

	sl = new Slicer()
	imY = sl.reslice(im)
	maxProj = ZProjector.run(imY,"max")
	new FileSaver( maxProj ).saveAsTiff( MIPSfolderY + "MAX_"+fn + ".tif");


	StackStatistics ss = new StackStatistics(im)

	fl = new File(savePath+"StackSummary.csv")
	fw = fl.newWriter(true) // append
	fw << (stNum+"\t"+ss.mean+"\t"+ss.median+"\t"+ss.min+"\t"+ss.max+"\t"+ss.stdDev+"\t"+ss.area+"\t"+ss.histMin+"\t"+ss.histMax+"\n");
	fw.close();

	slNum = im.getNSlices()
	fl = new File(savePath+"SliceSummary.csv")
	fw = fl.newWriter(true) // append
	for (int z=1; z<=slNum; z++){
		im.setSliceWithoutUpdate(z)
		ImageStatistics imStat = im.getStatistics()
		fw << (stNum+"\t"+z+"\t"+imStat.mean+"\t"+imStat.median+"\t"+imStat.min+"\t"+imStat.max+"\t"+imStat.stdDev+"\n");
	}	
	fw.close();

	IJ.run(im, "Square Root", "stack");
	ss = new StackStatistics(im)
	maxVal = ss.max
	ss = new StackStatistics(im,1000,0,maxVal)
        fl = new File(savePath+"StackHistsSqrt.csv")
	fw = fl.newWriter(true) 
	fw << ss.histogram.join("\t")+"\n"
	fw.close();

	im.close()	
}

im = FolderOpener.open(MIPSfolder, "");
IJ.saveAs(im, "Tiff", savePath + "MIPS.tif");

im = FolderOpener.open(MIPSfolderY, "");
IJ.saveAs(im, "Tiff", savePath + "MIPS_y.tif");

if (localCache){
	new File(outputPath + "MIPS.tif") << new File(savePath + "MIPS.tif").bytes
	new File(outputPath + "MIPS_y.tif") << new File(savePath + "MIPS_y.tif").bytes
	new File(outputPath + "StackSummary.csv") << new File(savePath + "StackSummary.csv").bytes
	new File(outputPath + "SliceSummary.csv") << new File(savePath + "SliceSummary.csv").bytes
	new File(outputPath + "StackHistsSqrt.csv") << new File(savePath + "StackHistsSqrt.csv").bytes
}

println("done")

