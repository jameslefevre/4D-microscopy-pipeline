// open each tif/tiff file in specified folder in alphanum order, extract the stack number, then count the voxels in every class (so table of frequencies)
// save results in table 

#@ String inputPath
#@ String outputPath
#@ String stackNumberPrefix
#@ String stackNumberSuffix

//String inputPath = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/23_2019-09_new_data/test_seg/"
//String outputPath = "/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/23_2019-09_new_data/test_seg/"
//String inputPath = "/home/james/mnt/rdm_adam/MachineLearning_ARC_Grant_work/20190830_LLSM_Yvette/Pos3/segmentation/d18_intAdj_rep1ds1gd_rf/segmented/"
//String outputPath = "/home/james/mnt/rdm_adam/MachineLearning_ARC_Grant_work/20190830_LLSM_Yvette/Pos3/segmentation/d18_intAdj_rep1ds1gd_rf/"
//String inputPath = "/home/james/mnt/rdm_adam/MachineLearning_ARC_Grant_work/20190830_LLSM_Yvette/Pos4/segmentation/d19_intAdj_rep1ds1gd_rf/segmented/"
//String outputPath = "/home/james/mnt/rdm_adam/MachineLearning_ARC_Grant_work/20190830_LLSM_Yvette/Pos4/segmentation/d19_intAdj_rep1ds1gd_rf/"
//String inputPath = "/home/james/mnt/rdm_adam/MachineLearning_ARC_Grant_work/20190917_LLSM_Yvette_CSF/sample1_pos/segmentation/d19_intAdj_rep1ds1gd_rf/segmented/"
//String outputPath = "/home/james/mnt/rdm_adam/MachineLearning_ARC_Grant_work/20190917_LLSM_Yvette_CSF/sample1_pos/segmentation/d19_intAdj_rep1ds1gd_rf/"
//String stackNumberPrefix = "-t"
//String stackNumberSuffix = "-e"

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

