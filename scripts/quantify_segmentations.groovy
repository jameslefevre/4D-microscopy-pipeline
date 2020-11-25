/**
 * Simple script to quantify segmentations, by counting the number of voxels in each class.
 * Expects a folder containing multiple 8-bit 3D tif files, with file names that can be parsed to get a stack number.  
 * Outputs a single table as a csv file saved to a specified path, with each row representing a stack number and each column a class. 
 * The values are voxel counts. Opens each tif/tiff file in specified folder in alphanumerical order, extracts the stack number, 
 * then count the voxels in every class
 * 
 * @param inputPath
 * Absolute path to folder containing tif files (segmentations)
 * @param outputPath
 * Absolute path of csv file to write with results
 * @param stackNumberPrefix
 * A string identifying the start of the stack number in the filename (stack number may be left-padded with zeroes).
 * @param stackNumberSuffix
 * A string identifying the end of the stack number in the filename. The script will extract the text from the filename 
 * between stackNumberPrefix and stackNumberSuffix, and attempt to read it as an integer (leading zeros ignored), 
 * which identifies the stack.
 */

#@ String inputPath
#@ String outputPath
#@ String stackNumberPrefix
#@ String stackNumberSuffix

int maxValueHeader = 3
// assumed max class number used in heading; if larger number found totals will be printed, extending line

import ij.IJ
import ij.io.FileSaver
import java.io.File
import ij.ImagePlus
import ij.ImageStack
import ij.process.StackStatistics
import ij.process.ImageStatistics
import ij.plugin.FolderOpener

def folder = new File( outputPath )
if( !folder.exists() ) {
     folder.mkdirs() // Create all folders up-to and including folder
}

def inputDir = new File(inputPath);
files = inputDir.listFiles().sort();

fl = new File(outputPath+"voxCountsByClass_d19.csv")
fw = fl.newWriter(false) // append
fw << ("stackNumber")
for (int classNum = 0; classNum<=maxValueHeader; classNum++){
	fw << "\tclass_" + classNum
}
fw << "\n"

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
	fw << stNum + "\t"

	ImagePlus im = IJ.openImage((String) files[fnum]);
	ss = new StackStatistics(im)
	int maxVal = (int) ss.max
	if (maxVal<maxValueHeader){
		maxVal=maxValueHeader
	}
	ss = new StackStatistics(im,maxVal+1,0,maxVal)
	hist = ss.histogram	
	fw << ss.histogram.join("\t")+"\n"
}

fw.close();
println("done")

