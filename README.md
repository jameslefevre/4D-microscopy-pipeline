
# 4D-microscopy-pipeline

This project contains executables and source code for an automated microscopy data analysis pipeline based on Fiji/ImageJ.

Briefly, semantic segmentation is performed using a machine learning algorithm, then objects are separated and quantified within each class, and tracked. The pipeline is modular and designed for easy and flexible deployment.

A visualiser designed to be used with this pipeline is provided here:
<http://github.com/jameslefevre/visualiser-4D-microscopy-analysis>

Detailed protocols for use will be included with the planned publication "LLAMA: a robust and scalable machine learning pipeline for analysis of large scale 4D microscopy data: Analysis of cell ruffles and filopodia" (submitted to BMC Bioinformatics).


## Installation

1. Install [FIJI](https://imagej.net/Fiji) on the machine to be used for computation.
2. Install the additional plugins 3D_ImageJ_Suite (mcib3d-suite), ImageScience, and IJPB_plugins (MorphoLibJ). This is most easily done in Fiji via update sites: select "Update..." from the help menu (or in a new install, accept when prompted to check for updates). Click on "Manage update sites" in the popup window, select the 3 additional sites listed above, then continue with the update/install.
3. Move the file Segmented_Image_Analysis.jar into the jars folder within the FIJI folder, and the file 5_class_v1.lut into the luts folder (less important - this is just to assign colours to classes in segmented images).
4. Copy the contents of the scripts folder onto the analyis machine.

## Usage

The file Segmented_Image_Analysis.jar (source provided) contains ImageJ extension and glue code. Computations are carried out via the parameterised groovy scripts provided, designed for flexible deployment. They may be run directly in Fiji, with paths and other parameters set in the GUI, or run on a cluster or as a local background process using a headless ImageJ process. Script headers contain details on use, parameters, inputs and outputs.

The folder hpc_job_templates contains template job scripts for running computations on a cluster using a PBS job system, with detailed documentation in script_documentation.md. Template bash scripts for running computations directly are also provided, without documentation, in local_execution_scripts. The file job_manager_functions.R contains an ad-hoc and undocumented set of functions for running a computational pipeline via an R notebook (including creating and running PBS job scripts via SSH).

## Note on units

### Semantic segmentation

The machine learning segmentation algorithm expects non-isometric image data: often the distance between z-slices is different to (usually larger than) the x/y dimension of a voxel. This is corrected so that as far as possible the 3D image features used to segment the images are isometric; this requires knowledge of voxel dimensions. The voxel dimensions are typically specified in the tiff metadata (image properties in ImageJ/Fiji), but due to the risk of being lost in the image pre-processing, they must be specified in the parameters passed to the segmentation algorithm. The unit of the scale parameter (sigma) in the image feature computations is defined as the x-dimension of a voxel, so only the ratios between the x/y/z dimension are relevant to the segmentation. The supplied voxel dimensions and unit are also used to set the correct values in the outputs (segmentations and segmentation probability maps).

### object analysis and tracking

Object analysis is based on the segmented images, and uses the voxel size in the image properties. So the scale and units in any outputs will be those provided to the segmentation algorithm, or else voxels or a unit based on them (the adjacency score between 2 objects is the number of distinct pairs of adjacent voxels in which one voxel is in each object, where adjacency is defined in the 18-neighbour sense). Coordinates for object centres and mesh vertices are given in voxel coordinates, however skeleton representations use the supplied units for both coordinates and lengths. See split_object_analysis.groovy docstring for additional details.

The tracking algorithm is based on the object stats, however the key information used (object centre of mass coordinates, volume and adjacency) is provided in voxel units, so previous real units used are irrelevant. Object coordinates are converted into "real" units in the tracking algorithm using the voxel size parameter fieldScaling. The units used in this parameter must be consistent with the other distance related tracking parameters (logSizeWeight, matchThreshold, matchScoreWeighting). However, the coordinates are converted back into voxel units when saving the node positions (each node consists of one or more the input objects). All other fields that are passed through the tracking algorithm retain the scale used at input, except that the voxel position covariance metrics (varX, varY, varZ, covXY, covXZ, covYZ) 
are converted from voxel to real scale (using fieldScaling). See get_tracks_parameterised.groovy docstring for additional details.



