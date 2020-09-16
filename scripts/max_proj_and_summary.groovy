
// open each tif/tiff file in specified folder in alphanum order, save max projection and image info
// produces tab separated text files StackSummary.csv, SliceSummary.csv, StackHistsSqrt.csv and folders MIPS and MIPS_y 
// containing max projections of each tif stack in the z and y axis respectively. These are compiled into stacks at the final stage.
// Either the folders of individual slices or the complied stacks should be deleted to avoid duplication.

// StackSummary.csv, SliceSummary.csv are tables with a header, and contain a "time" field extracted from the file name using specified 
// substrings which must bracket the stack number (stackNumberPrefix and stackNumberSuffix), so these must be customised to the file 
// format for the script to run correctly. 
// StackHistsSqrt.csv contains no explanetory information; rows are written in the order processed (alphanumerical), and each contains 1000 
// values representing a histogrom of the sqrt(voxel intensity) values over the stack, with bins between 0 and the square root of the maximum value specified in StackSummary
// The square root (a gamma transform in image processing terminology) compresses the range to make a more interpretable histogram

#@ String inputPath
#@ String outputPath
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

// The following parameters must match the file name format (substrings which bracket the stack number)

MIPSfolder = outputPath + "MIPS/";
MIPSfolderY = outputPath + "MIPS_y/";

def folder = new File( outputPath )
if( !folder.exists() ) {
     folder.mkdirs() // Create all folders up-to and including folder
}
folder = new File( MIPSfolder )
if( !folder.exists() ) {
 folder.mkdirs()
}
folder = new File( MIPSfolderY )
if( !folder.exists() ) {
  folder.mkdirs()
}
    

def inputDir = new File(inputPath);
files = inputDir.listFiles().sort();

File fl = new File(outputPath+"StackSummary.csv")
fw = fl.newWriter(false) // append
fw << ("time\tmean\tmedian\tmin\tmax\tstdev\tarea\thistMin\thistMax\n");
fw.close();

fl = new File(outputPath+"SliceSummary.csv")
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

	ImagePlus im = IJ.openImage((String) files[fnum]);

		
	maxProj = ZProjector.run(im,"max")
	new FileSaver( maxProj ).saveAsTiff( MIPSfolder + "MAX_"+fn + ".tif");

	sl = new Slicer()
	imY = sl.reslice(im)
	maxProj = ZProjector.run(imY,"max")
	new FileSaver( maxProj ).saveAsTiff( MIPSfolderY + "MAX_"+fn + ".tif");


	StackStatistics ss = new StackStatistics(im)

	fl = new File(outputPath+"StackSummary.csv")
	fw = fl.newWriter(true) // append
	fw << (stNum+"\t"+ss.mean+"\t"+ss.median+"\t"+ss.min+"\t"+ss.max+"\t"+ss.stdDev+"\t"+ss.area+"\t"+ss.histMin+"\t"+ss.histMax+"\n");
	fw.close();

	slNum = im.getNSlices()
	fl = new File(outputPath+"SliceSummary.csv")
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
        fl = new File(outputPath+"StackHistsSqrt.csv")
	fw = fl.newWriter(true) 
	fw << ss.histogram.join("\t")+"\n"
	fw.close();

	im.close()	
}

im = FolderOpener.open(MIPSfolder, "");
IJ.saveAs(im, "Tiff", outputPath + "MIPS.tif");

im = FolderOpener.open(MIPSfolderY, "");
IJ.saveAs(im, "Tiff", outputPath + "MIPS_y.tif");

println("done")

