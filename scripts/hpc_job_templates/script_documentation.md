## PBS scripts for deploying semantic segmentation and object analysis algorithms on a HPC cluster

### About 

This document provides information on the purpose, usage, and parameters of the template PBS job array submission scripts provided in the folder hpc_job_templates. These are intended to be copied, renamed and customised for each data set and possibly for multiple versions of the analysis with different parameters. It is recommended that these copies of the scripts are saved alongside the outputs as a record of the process and parameters used. Due to this repetition, the scripts are documented here rather than in headers.

The scripts call parameterised groovy scripts via a headless ImageJ process, and are designed for deploying the segmantic segmentation and object analysis process on a cluster with a PBS job submission system, allowing multiple image stacks to be processed in parallel. Much of the parameter information below is also included in the headers of the groovy scripts. The scripts form a pipeline, starting with semantic segmentation, then object splitting and analysis based on the segmentation, then generating meshes and skeleton representations of the output.

compile this doc in linux:  pandoc -o script_documentation.html script_documentation.md

### Preparing job scripts

For each task, copy and rename the template job script (or a template you have customised). Edit PBS options and paths as specified below, and consult detailed notes further down to customise parameters.

PBS options: consult manuals or support for your PBS cluster to correctly set these options. Resource requirements will depend on the system and image sizes, and may require trial and error. We recommend the job name (-N option) and script file name are chosen so they can be easily linked to each other and the key job information such as the data processed.

All paths are relative to your HPC cluster home directory, so the data, scripts, and Fiji (ImageJ) installation linked in these scripts must all be accessible from this location. Note the requirements for the Fiji installation.

In the call to the groovy script or scripts, you must specify the path to the ImageJ-linux64 file in your Fiji installation and the path to the groovy script (search in the scripts for the text ImageJ-linux64 and .groovy). This should only need to be done once for your setup.

Jobs are customised (selecting data and options) by editing parameter values in the scripts; see documentation of each script below. Note there is significant repetition between the documentation of scripts so that each can stand alone.

### Running jobs

On your HPC home directory, run the following command, or similar, depending on your system setup (consult documentation or support)

    qsub -q Short -J [first stack number]-[last stack number] [path to job script] 

This will create one job for each integer in the specified range. Consult HPC documentation or support for job monitoring and trouble shooting.

### Semantic segmentation

**template script:** segment_20190809_pre1_d18_intAdj_rep1ds1gd_rf  

Applies a trained Weka classification model to segment sequences of tiff stacks. Although based on the Trainable Weka ImageJ plugin, a customised process is used. Each job in the array segments one image stack.

This job calls the parameterised groovy scripts generate_save_features.groovy and apply_classifiers.groovy. Since these share many parameters, the parameters are defined separately at the top, which is a different setup to the scripts below.

**Parameters:**  

- feature_model_table: path to a text file containing a tab-separated table of feature and model information, which includes a column specifying the features used in the model to be used. The column heading must be the name of the model file (below). See ??? for details
- imageStackLocation: path to a directory containing single channel, 32-bit tiff stacks to be segmented  
- modelName: The file name of the Weka classification model to be used (not including the .model extension).
- modelPath: The directory containing the model file 
- featurePath: The path to a scratch data folder for a running job
- savePath: The direcory to save the segmented image stacks
- numberThreadsToUse: The number of threads that ImageJ should use
- saveProbabilityMaps: A string equal to 'true' or 'false'. Save the probability map associated with each segmentation; this gives the estimated probability that a given voxel is in each class, rather than just the predicted (most likely) class.
- stackNumberPrefix: A string identifying the start of the stack number in the filename (stack number may be left-padded with zeroes).
- stackNumberSuffix: A string identifying the end of the stack number in the filename. The script will look for a filename which contains a substring consisting of stackNumberPrefix, optional zeros, the stack number associated with the job, then stackNumberSuffix
- prefix: Only stack names starting with this string will be processed; leave blank (prefix='') to skip this filter. 
- pixelSize_unit: The name of the unit of measurement of the pixel dimensions; used to set the image properties in the segmented image stack
- pixelSize_xy: The size of the voxels in the image in the x and y dimension, in the units specified by pixelSize_unit (it is assumed that the x and y dimensions are equal) 
- pixelSize_z: The size of the voxels in the image in the z dimension, in the units specified by pixelSize_unit 
- intensityScalingFactor: An adjustment factor to account for differences in image fluorescence; the input image intensity is multiplied by this number before segmentation. 
- intensityScalingFactorTimeStep: An additional adjustment to account for fading of image fluorescence over a capture; the inout image intensity is further multiplied by this number to the power of the stack number. 
- cropBox: comma separated list of 6 integers describing a 3D crop to be applied to the input image prior to segmentation, or else an empty string to indicate no cropping. The 6 values are minX,maxX,minY,maxY,minZ,maxZ (voxel units, 0 indexed, endpoints included)

### Primary object analysis

**template script:** objects_20190830_pos3_d19

Defines and quantifies objects in a semantic segmentation of a 3D image (provided as an 8-bit color tiff stack). See documentation for split_object_analysis.groovy in code_and_file_descriptions for full information. For each channel (tissue type) to be analysed, 3D hole filling is applied, followed by a watershed split algorithm to separate touching objects. Then various metrics are calculated for each object, and saved in a table. The original image and the segmentation probability map are used for this purpose if available. A separate output table quantifies contact between objects, whether they are in the same class or not. This code also supports a class hierarchy, where additional classes can be defined as the combination of one or more of the original classes, and analysed as such; these must be arranged in layers so that each original class is used at most once in each layer.

This job calls the parameterised groovy script split_object_analysis.groovy 

There is a special parameter numberStacksPerJob which specifies how many stacks a single job will process consecutively; it may be set to any positive integer, allowing for the duration of individual jobs to be adjusted for efficient cluster performance. It is located after the PBS options but before the main call with the remaining parameters, and the value is used in combination with the job array index to automatically set the parameters firstStackNumber and lastStackNumber.
Job array index i will process stacks $numberStacksPerJob*(i-1)+1$ to $numberStacksPerJob*i$. For example, if numberStacksPerJob is 2, the following generates jobs to process stacks 1 to 20:
  
    qsub -q Short -J 1-10 [path to job script] 


**Parameters:**  

- imageStackLocation: path to a directory containing 8-bit color tiff stacks representing segmentation to be analysed
- originalStackLocation: optional path to a directory containing the source images (single channel, 32-bit tiff stacks), so that the original image intensity can be quantified in each object.
- probStackLocation: optional path to a directory containing the probability maps corresponding to the segmentations above (multi-channel, 8-bit tiff stacks), so that segmentation confidence and class confusion can be quantified in each object.
- savePath: The directory to save the output files, in subdirectories named for the image stacks processed.
- numberThreadsToUse: The number of threads that ImageJ should use.
- stackNumberPrefix: A string identifying the start of the stack number in the filename (stack number may be left-padded with zeroes).
- stackNumberSuffix: A string identifying the end of the stack number in the filename. The script will look for a filename which contains a substring consisting of stackNumberPrefix, optional zeros, the stack number associated with the job, then stackNumberSuffix
- fileNamePrefix: Only stack names starting with this string will be processed; leave blank (prefix='') to skip this filter. 
- overwriteExisting: A string equal to 'true' or 'false'. If false, the job will check for the output file objectStats.txt in the expected location, and if present it will not process the stack. This is useful for easily redoing failed tasks while not repeating tasks that completed successfully.
- classNumsToFill: A list of comma-separated integers within square brackets, which should be selected from the class numbers in the segmentations (the index values when loading in ImageJ). This is not necessarily the same as the classes in classesToAnalyse below. For each listed class in turn, a 3D hole-filling algorithm is applied to the segmentation. While often useful for removing small pockets of mis-segmentation and improving the function of the watershedding algorithm, care should be taken: if a legitimate structure is entirely enveloped (in 3 dimensions) in a region of another class, then the structure will be removed and replaced with the surrounding class. The background class should not be included in this list, as that will typically result in the removal of all structures (treated as "holes" in the background).
- classesToAnalyse: A list of comma-separated integers between square brackets, e.g. '[1,2,3]'. Specifies which classes to analyse; in the simple case these correspond directly to the colours (segmentation classes) in the input segmentations, but may be more complex if class aggregation or hierarchy is used (see below). Typically class 0 is black and represents background, and should not be included in analysis.
- classLayers: A list of comma-separated integers within square brackets, which should have the same length as and correspond to classesToAnalyse. Gives a layer to which each class is allocated. Layers are numbered sequentially from 1, and each layer must contain at least one class. In the simple (non-hierachical) case, all numbers should be 1.
- incorporatedChannels: An array of arrays; within the outer square brackets there is a list of subarrays, which has the same length as and corresponds to classesToAnalyse. Each sub-array (list of comma-separated numbers between square brackets) gives one or more of the original classes to be combined to form the corresponding class in classesToAnalyse. In the simple cases (no merging of classes), this should be set to e.g. '[[1],[2],[3]]'. The values need to be considered in combination with the parameter classLayers: each of the original classes can occur at most once in each layer (this system is designed to allow efficient representations of every object for adjacency quantification and other purposes). Classes can be aggregated using this parameter without having more than one layer, if you do not need to also analyse the classes separately. 
- dynamic: A list of comma-separated integers within square brackets, which should have the same length as and correspond to classesToAnalyse. This is the watershed parameter used to separate touching objects, specified for each class. Given two objects or masses which are connected by a narrower section, consider the distance to the surface of the structure at the centre of the two masses versus the distance at the centre of the narrow section. If the difference is greater than the "dynamic" parameter, then the two masses are split into separate objects. This parameter has the same units as the image (see voxel dimensions), and generally speaking the parameter value should be in proportion to the scale of the structures in the class. Lower values result in more aggressive splitting.
- minVoxExtraObjects: An integer. The watershedding algorithm will remove objects altogether if the distance from the centre of the object to the surface is smaller than the "dynamic" parameter. This process allows the optional recovery of these objects; any object that was removed but has at least this number (minVoxExtraObjects) of voxels will be replaced.
- channelsForDistanceMap: A list of comma-separated integers between square brackets, corresponding to selected segmentation classes (original, not aggregated). This parameter allows the optional quantification of the distance from the edge of a specified class or classes, to every object. For example, it allows the calculation of the minimum, mean and maximum distance of a structure from the surface of the main cell body.
- smoothingErosionForDistanceMap: A list of comma-separated integers between square brackets, of the same length as and corresponding to channelsForDistanceMap. This indicates how much smoothing of the corresponding channel to do before calculating the distance transform, in order to give more stable results. Higher numbers give more smoothing, 0 means no smoothing. 
- intensityScalingFactor: The original image intensity is multiplied by this value before quantifying the fluoroscence intensity within each object.
- cropBox: An array of the form '[[minX,maxX],[minY,maxY],[minZ,maxZ]]' defining a 3D cropping region to be applied to the original image only. The 6 values are integers indicating a number of voxels (see semantic segmentation section). If the original image was cropped before segmenting, this allows the cropping to be replicated so that the images correspond, before quantifying the fluoroscence intensity within each object; cropBox='' indicates no cropping.


### Generate object meshes

**template script:** meshes_20190830_pos3_d19

Generates meshes in the [OBJ format](https://en.wikipedia.org/wiki/Wavefront_.obj_file) defining object surfaces. Coordinates are in voxel units. Meshes are used primarily for visualisation, but may also be used for analysis. This script is an add-on to the primary object analysis above, using some of its outputs. The required input is one or more 8- or 16-bit color tiff stacks named object_map.tif, object_map2.tif etc where the (integer) voxel values define object masks. In addition, we require a text file objectStats.txt containing a tab-separated table with integer-value columns named id and class (any other columns are allowed, but are not used). The IDs must match the voxel values in order for objects to be processed, and the class should be correct for the object as per the original segmentation and the primary object analysis.

This job calls the parameterised groovy script get_meshes.groovy, which adapts code from the mcib3d core library for the 3D ImageJ Suite (https://github.com/mcib3d/mcib3d-core).

There is a special parameter numberStacksPerJob which specifies how many stacks a single job will process consecutively; it may be set to any positive integer, allowing for the duration of individual jobs to be adjusted for efficient cluster performance. It is located after the PBS options but before the main call with the remaining parameters, and the value is used in combination with the job array index to automatically set the parameters firstStackNumber and lastStackNumber.
Job array index i will process stacks $numberStacksPerJob*(i-1)+1$ to $numberStacksPerJob*i$. For example, if numberStacksPerJob is 2, the following generates jobs to process stacks 1 to 20:
  
    qsub -q Short -J 1-10 [path to job script] 

**Parameters:**  

- imageStackLocation: path to a directory containing subdirectories (named for the original image stacks) which each contain the inputs specified above.
8-bit color tiff stacks representing segmentation to be analysed
- savePath: The directory to save the output files, in subdirectories named for the image stacks processed. This should be the same as imageStackLocation, or at least have the same subdirectory names.
- classesToAnalyse: A list of comma-separated integers between square brackets, e.g. '[1,2,3]', specifying which classes to analyse. Each object ID is associated with a class number in objectStats.txt, and the object will only be processed if its class number is in this list. 
- numberThreadsToUse: The number of threads that ImageJ should use.
- stackNumberPrefix: A string identifying the start of the stack number in the subdirectory name (stack number may be left-padded with zeroes).
- stackNumberSuffix: A string identifying the end of the stack number in the subdirectory name. The script will look for a filename which contains a substring consisting of stackNumberPrefix, optional zeros, the stack number associated with the job, then stackNumberSuffix
- fileNamePrefix: Only subdirectory names starting with this string will be processed; leave blank (prefix='') to skip this filter. 
- overwriteExisting: A string equal to 'true' or 'false'. If false, the job will check for the output file objectMeshes.obj in the expected location, and if present it will not process the stack. This is useful for easily redoing failed tasks while not repeating tasks that completed successfully.
- targetMeshVertexReduction: An array of numbers (i.e. a list of comma-separated numbers between square brackets) between 0 and 1 corresponding to classes 1,2, etc. After meshes have been constructed with the marching cubes algorithm, it is generally recommended to prune the meshes, reducing the number of faces while retaining a good representation of the surface, in order to save resources (disk and RAM space plus processing speed). This number is the target proportion of mesh nodes that should be removed (the target may not be obtainable). Pruning may reduce the level of detail in the surface, so this parameter should reflect the size and complexity of structures in the class. Small structures may be best without pruning (parameter value 0) while larger and simpler structures may benefit from a aprameter value approaching 1.
- meshSmoothingIterations: An array of integers corresponding to classes 1,2, etc, giving the number of smoothing iterations to perform on the pruned meshes. Each iteration adjusts the mesh node positions slightly to give a smoother surface rendering, so higher numbers give more smoothing, and 0 indicates no smoothing. Again, the parameter choice should reflect the characteristics of structures of the class, and the desired tradeoff between smooth surfaces and faithfulness to the segmentation.


### Generate object skeletons

**template script:** skeletons_20190830_pos3_d19

Generates skeleton representations of objects. Each object is eroded in 3D down to a [topological skeleton](https://en.wikipedia.org/wiki/Topological_skeleton), and then reduced to summary information at the branch level (a branch is a segment of the skeleton between 2 branch or end points). For each branch, we save the start and end coordinates, and the curved branch length as well as the straight line distance. Coordinates and distances are in the units defined in the image properties. Skeletons are used primarily for visualisation, but may also be used for analysis. They are mostly useful for linear structures. As for meshes, this script is an add-on to the primary object analysis above, using some of its outputs. The required input is one or more 8- or 16-bit color tiff stacks named object_map.tif, object_map2.tif etc where the (integer) voxel values define object masks. In addition, we require a text file objectStats.txt containing a tab-separated table with integer-value columns named id and class (any other columns are allowed, but are not used). The IDs must match the voxel values in order for objects to be processed, and the class should be correct for the object as per the original segmentation and the primary object analysis.

This job calls the parameterised groovy script get_skeletons.groovy, which adapts code from the mcib3d core library for the 3D ImageJ Suite (https://github.com/mcib3d/mcib3d-core).

There is a special parameter numberStacksPerJob which specifies how many stacks a single job will process consecutively; it may be set to any positive integer, allowing for the duration of individual jobs to be adjusted for efficient cluster performance. It is located after the PBS options but before the main call with the remaining parameters, and the value is used in combination with the job array index to automatically set the parameters firstStackNumber and lastStackNumber.
Job array index i will process stacks $numberStacksPerJob*(i-1)+1$ to $numberStacksPerJob*i$. For example, if numberStacksPerJob is 2, the following generates jobs to process stacks 1 to 20:
  
    qsub -q Short -J 1-10 [path to job script] 


**Parameters:**  

- imageStackLocation: path to a directory containing subdirectories (named for the original image stacks) which each contain the inputs specified above.
- savePath: The directory to save the output files, in subdirectories named for the image stacks processed. This should be the same as imageStackLocation, or have the same subdirectory names.
- classesToAnalyse: A list of comma-separated integers between square brackets, e.g. '[1,2,3]', specifying which classes to analyse. Each object ID is associated with a class number in objectStats.txt, and the object will only be processed if its class number is in this list. 
- numberThreadsToUse: The number of threads that ImageJ should use.
- stackNumberPrefix: A string identifying the start of the stack number in the subdirectory name (stack number may be left-padded with zeroes).
- stackNumberSuffix: A string identifying the end of the stack number in the subdirectory name. The script will look for a filename which contains a substring consisting of stackNumberPrefix, optional zeros, the stack number associated with the job, then stackNumberSuffix
- fileNamePrefix: Only subdirectory names starting with this string will be processed; leave blank (prefix='') to skip this filter. 
- overwriteExisting: A string equal to 'true' or 'false'. If false, the job will check for the output file objectMeshes.obj in the expected location, and if present it will not process the stack. This is useful for easily redoing failed tasks while not repeating tasks that completed successfully.




