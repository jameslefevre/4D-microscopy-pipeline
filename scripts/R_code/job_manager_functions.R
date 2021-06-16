# https://cran.r-project.org/web/packages/ssh/ssh.pdf

server_address = "uqjlefev@awoonga.qriscloud.org.au"
rdm_rt = "/RDS/Q0930/MachineLearning_ARC_Grant_work/"
tmp_rt = "/30days/uqjlefev/"
vis_rt = "/home/james/image_data/visualiser/"
# datasets=c(paste0("20190809/",c("Pre1","Post1","Pre2","Post2","Pre3","Post3","Pre4","Post4"),"/"),paste0("20190815_Yvette-Set2/",c("Set1-Pre","Set1-Post"),"/"))
# seg_name="d18_intAdj_rep1ds1gd_rf"
ob_fldr="objectAnalysis"


#### ********************************* file checking *********************************


countFilesRemoteDirectory <- function(path,sess){
  as.numeric(strsplit(rawToChar(ssh_exec_internal(sess, command = paste0("ls ",path," | wc"))$stdout),"\\s+")[[1]][2])
}
listFilesRemoteDirectory <- function(path,sess){
  strsplit(rawToChar(ssh_exec_internal(sess, command = paste0("ls ",path))$stdout),"\\s+")[[1]]
}

# uses tree to look in all subfolders for occurrences of specified file names
countSpecifiedFilenamesRemoteDirectory <- function(path,sess,checkNames){
  tokens <- strsplit(rawToChar(ssh_exec_internal(sess, command = paste0("tree ",path))$stdout),"\\s+")[[1]]
  cnts <- c()
  for (nm in checkNames){
    cnts <- c(cnts,sum(tokens==nm))
  }
  cnts
}

listFilesLocalDirectory <- function(path,wait=TRUE){
  
  system2("ls",c(path),stdout=TRUE,wait=wait)
}
# to count files, just use length(listFilesLocalDirectory(path)) 

# uses tree to look in all subfolders for occurrences of specified file names
countSpecifiedFilenamesLocalDirectory <- function(path,checkNames){
  tokens <- unlist(strsplit(system2("tree",path,stdout=TRUE),"\\s+"))
  cnts <- c()
  for (nm in checkNames){
    cnts <- c(cnts,sum(tokens==nm))
  }
  cnts
}

getStackNumFromFileName <- function(nm){
  as.numeric(strsplit(strsplit(nm,"-t")[[1]][2],"-e")[[1]][1])
}
#fldr="/RDS/Q0930/MachineLearning_ARC_Grant_work/20190830_LLSM_Yvette/Pre2/segmentation/d19_intAdj_rep1ds1gd_rf/objectAnalysis/"
#stackNumsExpected=1:300
getMissingstackNumbers <- function(fldr,stackNumsExpected=1:300){
  fls <- rawToChar(ssh_exec_internal(session, command = paste0("ls ",fldr))$stdout)
  fls <- strsplit(fls,"\n")[[1]]
  stNums <- unlist(lapply(fls,getStackNumFromFileName))
  stackNumsExpected[!stackNumsExpected %in% stNums]
}

#### ********************************* generate feature stack and seg locally *********************************


generate_feature_stack_local <- function(imageStackLocation,imageStackName,featureSavePath,cropBox,intensityScalingFactor,pixelSize_xy,pixelSize_z,modelName="d18_rep1ds1gd_rf",
                                   feature_model_table="/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/data_and_model_info/feature_model_table.txt",
                                   numberThreadsToUse=3,wait=FALSE){
  groovy_params <- paste0(
    'feature_model_table = "',feature_model_table,'",',
    'imageStackLocation = "',imageStackLocation,'",',
    'imageStackName = "',imageStackName,'",',
    'modelName = "',modelName,'",',
    'featureSavePath = "',featureSavePath,'",',
    'numberThreadsToUse = "',numberThreadsToUse,'",',
    'pixelSize_xy = "',pixelSize_xy,'",',
    'pixelSize_z = "',pixelSize_z,'",',
    'intensityScalingFactor = "',intensityScalingFactor,'",',
    'cropBox = "',cropBox,'"'
  )
  cat(groovy_params)
  system2('/home/james/software/Fiji.app/ImageJ-linux64',c("--ij2", "--headless", "--allow-multiple", "--run", "/home/james/work/eclipse-workspace/Segmented_Image_Analysis/src/scripts/generate_save_features.groovy",paste0("'",groovy_params,"'")),wait=wait)
}

# not finished - need to work out best way to get original file name
# generate_feature_stack_local_lookup <- function(ds_name,timeStep,saveName,lu,rt_fldr="/data/james/image_data/LLS/20190830_LLSM_Yvette/",
#                                                 pixelSize_xy=104,pixelSize_z=268.146,modelName="d18_rep1ds1gd_rf",
#                                                 feature_model_table="/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/data_and_model_info/feature_model_table.txt",
#                                                 intensityScaleOverride=NA,numberThreadsToUse=3,wait=FALSE){
#   dsNum <- which(lu$name==ds_name)
#   cb <- paste0(lu$cropBox[[dsNum]],collapse=",")
#   intFactor = ifelse(is.na(intensityScaleOverride),lu$intensityScalingFactor[dsNum]*lu$intensityScalingFactorTimeStep[dsNum]^timeStep,intensityScaleOverride)
#   generate_feature_stack_local(paste0(rt_fldr,ds_name, "/Decon_output/"),"c1-t200-et1044911_decon",paste0(rt_fldr,ds_name,"/",saveName,"/"),cropBox=cb,
#                                intensityScalingFactor=intFactor, pixelSize_xy=pixelSize_xy,pixelSize_z=pixelSize_z)
# }




generate_seg_from_feature_stack_local <- function(featurePath,saveName,savePath,
                                                  modelName="d18_rep1ds1gd_rf",modelPath="/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/data_and_model_info/",
                                                  saveProbabilityMaps=TRUE,pixelSize_unit="nm",pixelSize_xy,pixelSize_z,
                                                  feature_model_table="/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/data_and_model_info/feature_model_table.txt",
                                                  channel_grouping = "[[0],[1],[2,3]]",channels=4,
                                                  numberThreadsToUse=3,wait=FALSE){
  groovy_params <- paste0(
    'feature_model_table = "',feature_model_table,'",',
    'imageStackName = "',saveName,'",',
    'featurePath = "',featurePath,'",',
    'modelPath = "',modelPath,'",',
    'modelName = "',modelName,'",',
    'savePath = "',savePath,'",',
    'numberThreadsToUse = "',numberThreadsToUse,'",',
    'saveProbabilityMaps = "',ifelse(saveProbabilityMaps,"true","false"),'",',
    'pixelSize_xy = "',pixelSize_xy,'",',
    'pixelSize_z = "',pixelSize_z,'",',
    'channel_grouping = ',channel_grouping,',',
    'channels = ',channels
  )
  cat(groovy_params)
  system2('/home/james/software/Fiji.app/ImageJ-linux64',c("--ij2", "--headless", "--allow-multiple", "--run", "/home/james/work/eclipse-workspace/Segmented_Image_Analysis/src/scripts/apply_classifiers.groovy",paste0("'",groovy_params,"'")),wait=wait)
}

segment_stack_local = function(
  imageStackLocation='/data/james/image_data/Fluo-C3DH-A549/01/',
  imageStackName='t000',
  savePath="/data/james/image_data/Fluo-C3DH-A549/01_seg/",
  tmpDirectory="/home/james/Desktop/temp/",
  modelName='F_C3DH_v1b',
  modelPath="/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/29_2021-03_postMDA231/Fluo-C3DH-A549/",
  feature_model_table='/home/james/work/machine_learning_organelle_selection_and_feature_detection/trainable_weka/data_and_model_info/feature_model_table.txt',
  numberThreadsToUse=2,
  pixelSize_xy=125.6983,
  pixelSize_z=1000.0,
  pixelSize_unit="nm",
  intensityScalingFactor=1,
  channel_grouping = "[[0],[1],[2]]",
  channels="3",
  cropBox="",
  saveProbabilityMaps=TRUE,
  fiji_path="/home/james/software/Fiji.app/ImageJ-linux64", #"~/Fiji.app/ImageJ-linux64",
  script_path_features="/home/james/work/eclipse-workspace/4D-microscopy-pipeline/scripts/generate_save_features.groovy", # "~/code/generate_save_features.groovy"
  script_path_classifier="/home/james/work/eclipse-workspace/4D-microscopy-pipeline/scripts/apply_classifiers.groovy"   #"~/code/apply_classifiers.groovy"
){
  
  exe_string = paste0(
    fiji_path," --ij2 --headless --run ",script_path_features," \\
\"feature_model_table='",feature_model_table,"',\"\\
\"imageStackLocation='",imageStackLocation,"',\"\\
\"imageStackName='",imageStackName,"',\"\\
\"modelName='",modelName,"',\"\\
\"featureSavePath='",tmpDirectory,"fs/",modelName,"/",imageStackName,"/',\"\\
\"numberThreadsToUse='",numberThreadsToUse,"',\"\\
\"pixelSize_xy='",pixelSize_xy,"',\"\\
\"pixelSize_z='",pixelSize_z,"',\"\\
\"intensityScalingFactor='",intensityScalingFactor,"',\"\\
\"cropBox='",cropBox,"'\"

",fiji_path," --ij2 --headless --run ",script_path_classifier," \\
\"feature_model_table='",feature_model_table,"',\"\\
\"imageStackName='",imageStackName,"',\"\\
\"featurePath='",tmpDirectory,"fs/",modelName,"/",imageStackName,"/',\"\\
\"modelPath='",modelPath,"',\"\\
\"modelName='",modelName,"',\"\\
\"savePath='",savePath,modelName,"/',\"\\
\"numberThreadsToUse='",numberThreadsToUse,"',\"\\
\"saveProbabilityMaps='",saveProbabilityMaps,"',\"\\
\"pixelSize_unit='",pixelSize_unit,"',\"\\
\"pixelSize_xy='",pixelSize_xy,"',\"\\
\"pixelSize_z='",pixelSize_z,"',\"\\
\"channel_grouping=",channel_grouping,",\"\\
\"channels='",channels,"'\"
")
  #exe_string
  system(exe_string)
}
# cat(segment_stack_local())


#### ********************************* crop and copy image stack *********************************

crop_and_copy <- function(imageLoadPath,imageSavePath,cropBox){
  groovy_params <- paste0(
    'imageLoadPath = "',imageLoadPath,'",',
    'imageSavePath = "',imageSavePath,'",',
    'cropBox = "[[',cropBox[1],',',cropBox[2],'],[',cropBox[3],',',cropBox[4],'],[',cropBox[5],',',cropBox[6],']]"')
  cat(groovy_params)
  system2('/home/james/software/Fiji.app/ImageJ-linux64',c("--ij2", "--headless", "--allow-multiple", "--run", "/home/james/work/eclipse-workspace/Segmented_Image_Analysis/src/scripts/crop_and_copy.groovy",paste0("'",groovy_params,"'")),wait=FALSE)
}




####  ********************************* running tracking algorithm *********************************
# changed to new tracking approach, commit "tracked new object info through tracking code, debugged"
# see git for original version of following code

track_objects <- function(main_path,
                          save_path,
                          timeSteps_specified = c(), # if empty/null, uses all available
                          breakPoints = c(),
                          useAlphabeticalPositionForStackNumber = FALSE,
                          stackNumPrefix = "-t",
                          stackNumSuffix = "-e",
                          trackedClasses=1:4,
                          voxelThresholds=c(2000,30,200,200), # c(5000,75,500,500)
                          #voxelThresholdsTracks=c(10000,150,1000,1000), # not currently applied in tracking code, done in analysis instead
                          fieldScaling = c(1.04,1.04,2.68),
                          logSizeWeight = c(90,22,22,22),
                          matchThreshold = c(120,20,20,20),
                          relativeNodeContact_referenceValue = c(0.06,0.02,0.04,0.04), # relativeNodeContact_referenceValue = c(0.1,0.02,0.04,0.04),
                          relativeNodeDistance_referenceValue = c(0.7,0.5,0.8,0.8), # relativeNodeDistance_referenceValue = c(1,0.5,0.8,0.8),
                          relativeNodeContact_weight = c(0.66,0.66,0.66,0.66),
                          matchScoreWeighting = c(0.35,0.25,0.25,0.25), # matchScoreWeighting = c(1,0.25,0.25,0.25),
                          trackingParams=list(), # optional list of parameters to overwrite
                          verbose = FALSE,
                          wait=FALSE)
{
  if ("logSizeWeight" %in% names(trackingParams)){logSizeWeight = trackingParams[["logSizeWeight"]]}
  if ("matchThreshold" %in% names(trackingParams)){matchThreshold = trackingParams[["matchThreshold"]]}
  if ("relativeNodeContact_referenceValue" %in% names(trackingParams)){relativeNodeContact_referenceValue = trackingParams[["relativeNodeContact_referenceValue"]]}
  if ("relativeNodeDistance_referenceValue" %in% names(trackingParams)){relativeNodeDistance_referenceValue = trackingParams[["relativeNodeDistance_referenceValue"]]}
  if ("relativeNodeContact_weight" %in% names(trackingParams)){relativeNodeContact_weight = trackingParams[["relativeNodeContact_weight"]]}
  if ("matchScoreWeighting" %in% names(trackingParams)){matchScoreWeighting = trackingParams[["matchScoreWeighting"]]}
  start_time = Sys.time()
  print(paste0("Starting track_objects at ", start_time))
  groovy_params <- paste0(
    'main_path = "',main_path,'",',
    'save_path = "',save_path,'",',
    'timeSteps_specified = [',paste0(timeSteps_specified,collapse=","),'],',
    'breakPoints = [',paste0(breakPoints,collapse=","),'],',
    'useAlphabeticalPositionForStackNumber = ',ifelse(useAlphabeticalPositionForStackNumber,"true","false"),',',
    'stackNumPrefix = "',stackNumPrefix,'",',
    'stackNumSuffix = "',stackNumSuffix,'",',
    'trackedClasses = [',paste0(trackedClasses,collapse=","),'],',
    'voxelThresholds = [',paste0(voxelThresholds,collapse=","),'],',
    'fieldScaling = [',paste0(fieldScaling,collapse=","),'],',
    'logSizeWeight = [',paste0(logSizeWeight,collapse=","),'],',
    'matchThreshold = [',paste0(matchThreshold,collapse=","),'],',
    'relativeNodeContact_referenceValue = [',paste0(relativeNodeContact_referenceValue,collapse=","),'],',
    'relativeNodeDistance_referenceValue = [',paste0(relativeNodeDistance_referenceValue,collapse=","),'],',
    'relativeNodeContact_weight = [',paste0(relativeNodeContact_weight,collapse=","),'],',
    'matchScoreWeighting = [',paste0(matchScoreWeighting,collapse=","),'],',
    'verbose = ',ifelse(verbose,"true","false")
  )
  cat(groovy_params)
  system2('/home/james/software/Fiji.app/ImageJ-linux64',c("--ij2", "--headless", "--allow-multiple", "--mem=20000M", "--run", "/home/james/work/eclipse-workspace/4D-microscopy-pipeline/scripts/get_tracks_parameterised.groovy",paste0("'",groovy_params,"'")),wait=wait)
  end_time = Sys.time()
  print(paste0("Completed track_objects at ", end_time, " elapsed time ", end_time-start_time, " minutes"))
  cat(groovy_params,"\n","start: ",start_time,"\nend: ",end_time,"\nelapsed: ",end_time-start_time, " minutes",file=paste0(strsplit(save_path,".",fixed=TRUE)[[1]][1],"_log.txt"))
}

track_objects_lookup <- function(datasetName,segName,lu,saveName="trackNodeTable.csv",objectFolder="objectAnalysis",trackingParams=list()){
  r = which(lu$name==datasetName)
  pth = paste0(vis_rt,datasetName,"/",segName,"/",objectFolder,"/")
  track_objects(pth,paste0(pth,saveName),trackingParams=trackingParams)
}

# saveName="trackNodeTable.csv"
# strsplit(".",saveName)
# strsplit(saveName,".",fixed=TRUE)[[1]][1]


#### ********************************* quantify segmenations *********************************

quantify_segmentations <- function(inputPath,outputPath,stackNumberPrefix="-t",stackNumberSuffix="-e",wait=FALSE){
  # written for the general case: invoke script quantify_segmentations.groovy with arbitrary params
  groovy_params <- paste0(
    'inputPath = "',inputPath,'",',
    'outputPath = "',outputPath,'",',
    'stackNumberPrefix = "',stackNumberPrefix,'",',
    'stackNumberSuffix = "',stackNumberSuffix,'"'
  )
  cat(groovy_params)
  system2('/home/james/software/Fiji.app/ImageJ-linux64',c("--ij2", "--headless", "--allow-multiple", "--run", "/home/james/work/eclipse-workspace/Segmented_Image_Analysis/src/scripts/quantify_segmentations.groovy",paste0("'",groovy_params,"'")),wait=wait)
}



####  ********************************* convert images for visualiser *********************************

convert_images_for_visualiser_old <- function(imageFolder,segFolder,saveFolder,stackNums,cropBox,intensityRange,modelName,segName,convertRawSegProb=c(TRUE,TRUE,TRUE),cropRawSegProb=c(TRUE,FALSE,FALSE),
                                          stackNumberPrefix="-t",stackNumberSuffix="-e",channelSelection=c(3,2,4),wait=FALSE){
  # written for the general case: invoke script export_to_vis.groovy with arbitrary params
  
  groovy_params <- paste0(
    'imageFolder = "',imageFolder,'",',
    'segFolder = "',segFolder,'",',
    'saveFolder = "',saveFolder,'",',
    'modelName = "',modelName,'",',
    'segName = "',segName,'",',
    'stackNums = "[',paste0(stackNums,collapse=","),']",',
    'stackNumberPrefix = "',stackNumberPrefix,'",',
    'stackNumberSuffix = "',stackNumberSuffix,'",',
    'convertRawSegProb = "[',paste0(ifelse(convertRawSegProb,"true","false"),collapse=","),']",',
    'cropRawSegProb = "[',paste0(ifelse(cropRawSegProb,"true","false"),collapse=","),']",',
    'cropBox = "[[',cropBox[1],',',cropBox[2],'],[',cropBox[3],',',cropBox[4],'],[',cropBox[5],',',cropBox[6],']]",', 
    'intensityRange = "[',paste0(intensityRange,collapse=","),']",',
    'channelSelection = "[',paste0(channelSelection,collapse=","),']"'
  )
  cat(groovy_params)
  system2('/home/james/software/Fiji.app/ImageJ-linux64',c("--ij2", "--headless", "--allow-multiple", "--run", "/home/james/work/eclipse-workspace/Segmented_Image_Analysis/src/scripts/export_to_vis.groovy",paste0("'",groovy_params,"'")),wait=wait)
}

convert_images_for_visualiser <- function(imageFolder,segFolder,probMapFolder,imageSaveFolder,segSaveFolder,probMapSaveFolder,stackNums,cropBox,intensityRange,
                                          alwaysSaveWithImageStackNames=TRUE,convertRawSegProb=c(TRUE,TRUE,TRUE),cropRawSegProb=c(TRUE,FALSE,FALSE),
                                          stackNumberPrefix="-t",stackNumberSuffix="-e", stackNumberPrefixSeg=NA,stackNumberSuffixSeg=NA, stackNumberPrefixProb=NA,stackNumberSuffixProb=NA,
                                          channelSelection=c(3,2,4),wait=FALSE){
  # written for the general case: invoke script export_to_vis.groovy with arbitrary params
  groovy_params <- paste0(
    'imageFolder = "',imageFolder,'",',
    'segFolder = "',segFolder,'",',
    'probMapFolder = "',probMapFolder,'",',
    'imageSaveFolder = "',imageSaveFolder,'",',
    'segSaveFolder = "',segSaveFolder,'",',
    'probMapSaveFolder = "',probMapSaveFolder,'",',
    'alwaysSaveWithImageStackNames = "',ifelse(alwaysSaveWithImageStackNames,"true","false"),'",',
    'stackNums = "[',paste0(stackNums,collapse=","),']",',
    'stackNumberPrefix = "',stackNumberPrefix,'",',
    'stackNumberSuffix = "',stackNumberSuffix,'",',
    'stackNumberPrefixSeg = "',ifelse(is.na(stackNumberPrefixSeg),stackNumberPrefix,stackNumberPrefixSeg),'",',
    'stackNumberSuffixSeg = "',ifelse(is.na(stackNumberSuffixSeg),stackNumberSuffix,stackNumberSuffixSeg),'",',
    'stackNumberPrefixProb = "',ifelse(is.na(stackNumberPrefixProb),stackNumberPrefix,stackNumberPrefixProb),'",',
    'stackNumberSuffixProb = "',ifelse(is.na(stackNumberSuffixProb),stackNumberSuffix,stackNumberSuffixProb),'",',
    'convertRawSegProb = "[',paste0(ifelse(convertRawSegProb,"true","false"),collapse=","),']",',
    'cropRawSegProb = "[',paste0(ifelse(cropRawSegProb,"true","false"),collapse=","),']",',
    'cropBox = "[[',cropBox[1],',',cropBox[2],'],[',cropBox[3],',',cropBox[4],'],[',cropBox[5],',',cropBox[6],']]",', 
    'intensityRange = "[',paste0(intensityRange,collapse=","),']",',
    'channelSelection = "[',paste0(channelSelection,collapse=","),']"'
  )
  cat("\necho groovy_params:\n")
  cat(groovy_params)
  cat("\nNow call groovy script:\n")
  system2('/home/james/software/Fiji.app/ImageJ-linux64',c("--ij2", "--headless", "--allow-multiple", "--run", "/home/james/work/eclipse-workspace/4D-microscopy-pipeline/scripts/export_to_vis.groovy",paste0("'",groovy_params,"'")),wait=wait)
}


convert_images_for_visualiser_short <- function(source_fldr,dest_fldr,timeSteps,cropBox,maxIntensity,convertRawSegProb=c(TRUE,TRUE,TRUE),
         segName="d18_intAdj_rep1ds1gd_rf",inputRootFolder = "/home/james/mnt/rdm_adam/MachineLearning_ARC_Grant_work/",wait=FALSE){
  
  convert_images_for_visualiser(paste0(inputRootFolder,source_fldr,"Decon_output/"),
                                paste0(inputRootFolder,source_fldr,"segmentation/",segName,"/segmented/"),
                                paste0(inputRootFolder,source_fldr,"segmentation/",segName,"/probability_maps/"),
                                paste0(vis_rt,dest_fldr,"deconv/"),
                                paste0(vis_rt,dest_fldr,segName,"/segmented/"),
                                paste0(vis_rt,dest_fldr,segName,"/probability_maps/"),
                                timeSteps,cropBox,c(0,maxIntensity),convertRawSegProb=convertRawSegProb,wait=wait)
}





convert_images_for_visualiser_lookup <- function(datasetName,timeSteps,lu,convertRawSegProb=c(TRUE,TRUE,TRUE),segName="d18_intAdj_rep1ds1gd_rf",
                                                     wait=FALSE, intensityAdjustmentFactor=1,inputRootFolder="/home/james/mnt/rdm_adam/MachineLearning_ARC_Grant_work/",
                                                 stackNumberPrefix='t',stackNumberSuffix='-e',
                                                 source_image_subfolder="Decon_output",source_segmentation_subfolder="segmentation", source_seg_extension="segmented", source_prob_extension="probability_maps",
                                                 vis_rt = "/home/james/image_data/visualiser/",dest_image_subfolder="deconv",dest_seg_extension="segmented", dest_prob_extension="probability_maps",
                                                 channelSelection=c(3,2,4)
                                                 ){
  r = which(lu$name==datasetName)
  dest_fldr=paste0(vis_rt,datasetName,"/")
  convert_images_for_visualiser(paste0(inputRootFolder,lu$path[r],source_image_subfolder,"/"),
                                paste0(inputRootFolder,lu$path[r],source_segmentation_subfolder,"/",segName,"/",source_seg_extension,"/"),
                                paste0(inputRootFolder,lu$path[r],source_segmentation_subfolder,"/",segName,"/",source_prob_extension,"/"),
                                paste0(dest_fldr,"deconv/"),
                                paste0(dest_fldr,segName,"/",dest_seg_extension,"/"),
                                paste0(dest_fldr,segName,"/",dest_prob_extension,"/"),
                                timeSteps,lu$cropBox[[r]],c(0,lu$cyto[r]*10/intensityAdjustmentFactor),
                                convertRawSegProb=convertRawSegProb,wait=wait,
                                stackNumberPrefix=stackNumberPrefix,stackNumberSuffix=stackNumberSuffix,
                                channelSelection=channelSelection)
}



convert_images_for_visualiser_short_old <- function(source_fldr,dest_fldr,timeSteps,cropBox,maxIntensity,convertRawSegProb=c(TRUE,TRUE,TRUE),modelName="d18_rep1ds1gd_rf",segName="d18_intAdj_rep1ds1gd_rf",wait=FALSE){
  rdmFldr <- "/home/james/mnt/rdm_adam/MachineLearning_ARC_Grant_work/"
  convert_images_for_visualiser(paste0(rdmFldr,source_fldr,"Decon_output/"),paste0(rdmFldr,source_fldr,"segmentation/"),paste0(vis_rt,dest_fldr),
                                modelName=modelName,segName=segName,
                                timeSteps,cropBox,c(0,maxIntensity),convertRawSegProb=convertRawSegProb,wait=wait)
}

convert_images_for_visualiser_lookup_old <- function(datasetName,timeSteps,lu,convertRawSegProb=c(TRUE,TRUE,TRUE),segName="d18_intAdj_rep1ds1gd_rf",
                                                     wait=FALSE, intensityAdjustmentFactor=1){
  r = which(lu$name==datasetName)
  convert_images_for_visualiser_short(lu$path[r],paste0(datasetName,"/"),timeSteps,lu$cropBox[[r]],lu$cyto[r]*10/intensityAdjustmentFactor,convertRawSegProb=convertRawSegProb,segName=segName,wait=wait)
}


# convert_images_for_visualiser("/home/james/mnt/rdm_adam/MachineLearning_ARC_Grant_work/20190809/Post1/Decon_output/",
#                               "/home/james/mnt/rdm_adam/MachineLearning_ARC_Grant_work/20190809/Post1/segmentation/",
#                               "/home/james/image_data/visualiser/20190809_pre2/",
#                               1:10,
#                               c(171,780,242,1021,0,150),
#                               c(0,3240),
#                               convertRawSegProb=c(TRUE,TRUE,TRUE),wait=TRUE
#                               )

# dataset_info

# convert_images_for_visualiser_short("20190809/Pre4/","20190809_pre4/",1:10,c(218,998,0,1023,0,150),3803)

# convert_images_for_visualiser_lookup("20190830_pre1",c(1,50,100,150,200,250,300),dataset_info,convertRawSegProb=c(TRUE,FALSE,FALSE))
# convert_images_for_visualiser_lookup("20190830_pos1",c(1,50,100,150,200,250,300),dataset_info,convertRawSegProb=c(TRUE,FALSE,FALSE))

####  ********************************* download *********************************

download_object_dat <- function(dataset_path, local_path, file_types=c("csv","txt","obj"),server=server_address,show_command_only=FALSE,wait=TRUE){
  "expect trailing / on both paths - selected contents of dataset_path go into local_path directory, with directory structure preserved"
  cmd = c('-ravzhe', 'ssh', '--include="*/"')
  for (ft in file_types){
    cmd = c(cmd,paste0('--include="*.',ft,'"'))
  }
  cmd = c(cmd,'--exclude="*"', paste0(server,':',dataset_path), local_path)
  cat(cmd)
  if(!show_command_only){
    system2('mkdir', c('-p',local_path))
    system2('rsync',cmd,wait=wait)
  }
}
download_object_dat_lookup <- function(datasetName,lu,wait=FALSE){
  r = which(lu$name==datasetName)
  
  download_object_dat(paste0(rdm_rt,lu$path[r],"segmentation/"),paste0(vis_rt,datasetName,"/"),wait=wait)
}
# ********************************* ********************************* *********************************
####  ********************************* generate and run jobs *********************************
#  ********************************* ********************************* *********************************

generate_max_projections_and_summary_stats <- function(dataset_name,path,sess,ram_GB=18,walltime="2:00:00",stackNumberPrefix='-t',stackNumberSuffix='-e',source_subfolder="Decon_output/",dest_subfolder=""){
  "processes about 200 stacks per hour"
  jobName = paste0("MIPS_",dataset_name)
  script = paste0(
    "#!/bin/bash\n#\n#PBS -N ",jobName,"\n#\n#PBS -e autojobs/",jobName,"/\n#PBS -o autojobs/",jobName,"/\n#PBS -A UQ-IMB\n#PBS -l select=1:ncpus=2:mpiprocs=2:mem=",ram_GB,"GB,walltime=",walltime,"\n\n",
    "~/Fiji.app/ImageJ-linux64 --ij2 --headless --allow-multiple --run ~/code/max_proj_and_summary.groovy \\\n",
    '"inputPath=\'',path,source_subfolder,'\',"\\\n',
    '"outputPath=\'',path,dest_subfolder,'\',"\\\n',
    '"stackNumberPrefix=\'',stackNumberPrefix,'\',"\\\n',
    '"stackNumberSuffix=\'',stackNumberSuffix,'\'"\\\n\n',
    'exit 0')
  
  
  #cat(script,"\n")
  cat(script,file=paste0(local_rt,"temp/",jobName))
  ssh_exec_wait(sess, command = paste0("mkdir ./autojobs/",jobName)) # switched from ~/ to ./ 2020-03 because scp_upload didn't like it, even though ssh_exec_wait is fine. Where does scp_upload think ~ is??
  scp_upload(sess, paste0(local_rt,"temp/",jobName), to = paste0("./autojobs/",jobName), verbose = TRUE)
  
  ssh_exec_wait(sess, command = paste0("qsub -q Short ./autojobs/",jobName,"/",jobName) )
  # cat(paste0("echo ",script," > ~/autojobs/",jobName,"/",jobName))
  # ssh_exec_wait(sess, command = paste0("echo ",script," > ~/autojobs/",jobName,"/",jobName))
}


generate_max_projections_and_summary_stats_with_image_correction <- function(dataset_name,path,sess,intensityAdjustment,ram_GB=18,walltime="2:00:00",stackNumberPrefix='-t',stackNumberSuffix='-e',source_subfolder="Decon_output/",dest_subfolder=""){
  "processes about 200 stacks per hour"
  jobName = paste0("MIPS_im_corr_",dataset_name)
  script = paste0(
    "#!/bin/bash\n#\n#PBS -N ",jobName,"\n#\n#PBS -e autojobs/",jobName,"/\n#PBS -o autojobs/",jobName,"/\n#PBS -A UQ-IMB\n#PBS -l select=1:ncpus=2:mpiprocs=2:mem=",ram_GB,"GB,walltime=",walltime,"\n\n",
    "~/Fiji.app/ImageJ-linux64 --ij2 --headless --allow-multiple --run ~/code/max_proj_and_summary_with_image_correction.groovy \\\n",
    '"inputPath=\'',path,source_subfolder,'\',"\\\n',
    '"outputPath=\'',path,dest_subfolder,'\',"\\\n',
    '"stackNumberPrefix=\'',stackNumberPrefix,'\',"\\\n',
    '"stackNumberSuffix=\'',stackNumberSuffix,'\',"\\\n',
    '"intensityAdjustment=\'',intensityAdjustment,'\'"\\\n\n',
    'exit 0')
  
  
  #cat(script,"\n")
  cat(script,file=paste0(local_rt,"temp/",jobName))
  ssh_exec_wait(sess, command = paste0("mkdir ./autojobs/",jobName)) # switched from ~/ to ./ 2020-03 because scp_upload didn't like it, even though ssh_exec_wait is fine. Where does scp_upload think ~ is??
  scp_upload(sess, paste0(local_rt,"temp/",jobName), to = paste0("./autojobs/",jobName), verbose = TRUE)
  
  ssh_exec_wait(sess, command = paste0("qsub -q Short ./autojobs/",jobName,"/",jobName) )
  # cat(paste0("echo ",script," > ~/autojobs/",jobName,"/",jobName))
  # ssh_exec_wait(sess, command = paste0("echo ",script," > ~/autojobs/",jobName,"/",jobName))
}

#scratchfolder="C:/temp/"

# timestamp()

####### ********************************* remote copying with rsync *********************************

sync_decon_to_cache = function(main_path,sess,jobName="sync_decon_to_cache",
                               source_root="/RDS/Q0930/MachineLearning_ARC_Grant_work/",
                               dest_root="/30days/uqjlefev/",
                               extension="Decon_output/",ram_GB=1,walltime="1:00:00",cpusRequested=2){
  script = paste0(
    "#!/bin/bash\n#\n#PBS -N ",jobName,"\n#PBS -A UQ-IMB\n#PBS -l select=1:ncpus=",cpusRequested,":mpiprocs=",cpusRequested,":mem=",ram_GB,"GB,walltime=",walltime,"\n\n\n",
    "mkdir -p '",dest_root,main_path,extension,"'\n",
    "rsync -rv --include='*/' --include='*.tif' --include='*.tiff' --exclude='*' ",source_root,main_path,extension," ",dest_root,main_path,extension,"\n\n",
    "exit 0")
  cat(script,"\n")

  localScriptPath = paste0(local_rt,"temp/",jobName)
  cat(script,file=localScriptPath)
  cat("Wrote job script to ",localScriptPath,"\n")
  ssh_exec_wait(sess, command = paste0("mkdir autojobs/",jobName))
  print(paste0("run scp_upload with ",localScriptPath," ", paste0("autojobs/",jobName)))
  scp_upload(sess, localScriptPath, to = paste0("autojobs/",jobName), verbose = TRUE)
  cat("Uploaded script to remote drive in ", paste0("autojobs/",jobName),"\n")
  cmnd = paste0("qsub -q Short ~/autojobs/",jobName,"/",jobName)
  cat("Submitting job:\n",cmnd,"\n")
  # ssh_exec_wait(session, command = cmnd )
  sub_response <- rawToChar(ssh_exec_internal(sess, command = paste0("cd ~/autojobs/",jobName," ; ",cmnd))$stdout)
  cat(sub_response)
  ssh_exec_wait(sess, command = paste0("echo '",timestamp(),"\n",cmnd,"\n",sub_response,"' >> ~/autojobs/",jobName,"/",jobName,"_job_submissions"))
}
# sync_decon_to_cache("20190809/Post2/")

sync_cache_to_decon = function(main_path,sess,jobName="sync_cache_to_decon",
                               home_root="/RDS/Q0930/MachineLearning_ARC_Grant_work/",
                               cache_root="/30days/uqjlefev/",
                               extension="segmentation/",ram_GB=1,walltime="1:00:00",cpusRequested=2){
  script = paste0(
    "#!/bin/bash\n#\n#PBS -N ",jobName,"\n#PBS -A UQ-IMB\n#PBS -l select=1:ncpus=",cpusRequested,":mpiprocs=",cpusRequested,":mem=",ram_GB,"GB,walltime=",walltime,"\n\n\n",
    "mkdir -p '",home_root,main_path,extension,"'\n",
    "rsync -rv --exclude='*object_map*' ",cache_root,main_path,extension," ",home_root,main_path,extension,"\n\n",
    "exit 0")
  cat(script,"\n")
  
  localScriptPath = paste0(local_rt,"temp/",jobName)
  cat(script,file=localScriptPath)
  cat("Wrote job script to ",localScriptPath,"\n")
  ssh_exec_wait(sess, command = paste0("mkdir autojobs/",jobName))
  print(paste0("run scp_upload with ",localScriptPath," ", paste0("autojobs/",jobName)))
  scp_upload(sess, localScriptPath, to = paste0("autojobs/",jobName), verbose = TRUE)
  cat("Uploaded script to remote drive in ", paste0("autojobs/",jobName),"\n")
  cmnd = paste0("qsub -q Short ~/autojobs/",jobName,"/",jobName)
  cat("Submitting job:\n",cmnd,"\n")
  # ssh_exec_wait(session, command = cmnd )
  sub_response <- rawToChar(ssh_exec_internal(sess, command = paste0("cd ~/autojobs/",jobName," ; ",cmnd))$stdout)
  cat(sub_response)
  ssh_exec_wait(sess, command = paste0("echo '",timestamp(),"\n",cmnd,"\n",sub_response,"' >> ~/autojobs/",jobName,"/",jobName,"_job_submissions"))
}



####### ********************************* segmentation *********************************

# could not specify error and output file path/name as I would like, think I can do what I want just by running a cd before qsub in same ssh_exec_wait call
#  \n#PBS -e autojobs/",jobName,"/$JOB_ID'_'$TASK_ID'.ER'\n
# \n#PBS -e autojobs/",jobName,"/",jobName,"_<$JOB_ID>_<$TASK_ID>.e\n#PBS -o autojobs/",jobName,"/",jobName,"_<$job_id>_<$task_id>.o

# http://gridscheduler.sourceforge.net/htmlman/htmlman1/qsub.html
# TODO: made stackNumberPrefix and stackNumberSuffix job parameters rather than putting them directly into the grep call - test
segment_stacks <- function(dataset_name,stackNums,input_path,output_path,sess,
                           intensityScalingFactor, intensityScalingFactorTimeStep, cropBox,
                           ram_GB=80,walltime="1:00:00",
                           feature_model_table_path='/home/uqjlefev/code/feature_model_table.txt',
                           modelName='d18_rep1ds1gd_rf',modelPath='models/',
                           numberThreadsToUse=3,cpusRequested=3,saveProbabilityMaps=TRUE,prefix='',
                           pixelSize_unit='nm',pixelSize_xy=104,pixelSize_z=268.146,
                           channel_grouping = "[[0],[1],[2,3]]",channels=4,
                           stackNumberPrefix='t',stackNumberSuffix='-e',jobname_extra=""
                           ){
  # cropBox is min,max for x,y,z (0 indexed, endpoints included)
  # stackNumberPrefix and stackNumberSuffix may cause problems if they have special meaning in grep
  jobName = paste0("segment_",dataset_name,jobname_extra)
  script = paste0(
    "#!/bin/bash\n#\n#PBS -N ",jobName,"\n#PBS -A UQ-IMB\n#PBS -l select=1:ncpus=",cpusRequested,":mpiprocs=",cpusRequested,":mem=",ram_GB,"GB,walltime=",walltime,"\n\n\n",
    "feature_model_table='",feature_model_table_path,"'\n",
    "imageStackLocation='",input_path,"'\n",
    "modelName='",modelName,"'\n",
    "modelPath='",modelPath,"'\n",
    "featurePath=$TMPDIR'/fs/'\n",
    "savePath='",output_path,"'\n",
    "numberThreadsToUse='",numberThreadsToUse,"'\n",
    "saveProbabilityMaps='",ifelse(saveProbabilityMaps,"true","false"),"'\n",
    "stackNumberPrefix='",stackNumberPrefix,"'\n",
    "stackNumberSuffix='",stackNumberSuffix,"'\n",
    "prefix='",prefix,"'\n",
    "pixelSize_unit='",pixelSize_unit,"'\n",
    "pixelSize_xy='",pixelSize_xy,"'\n",
    "pixelSize_z='",pixelSize_z,"'\n",
    "intensityScalingFactor='",intensityScalingFactor,"'\n",
    "intensityScalingFactorTimeStep='",intensityScalingFactorTimeStep,"'\n",
    "cropBox='",cropBox,"'\n",
    "channel_grouping='",channel_grouping,"'\n",
    "channels='",channels,"'\n\n",
    "intensityScalingFactor=$(echo \"$intensityScalingFactor $intensityScalingFactorTimeStep $PBS_ARRAY_INDEX\" | awk '{print $1*($2^($3-1));}')

echo \"PBS_ARRAY_INDEX (timestep) = \"$PBS_ARRAY_INDEX
echo \"Apply cropping box: \"$cropBox
echo \"Pixel dimensions: \"$pixelSize_xy\" (x,y); \"$pixelSize_z\" (z)\"
echo \"Adjusted intensityScalingFactor: \"$intensityScalingFactor

imageStackName=$(ls $imageStackLocation | grep \"$stackNumberPrefix\"0*$PBS_ARRAY_INDEX\"$stackNumberSuffix\" | grep \"^$prefix\" | cut -d. -f1)

# CI_path_check=$savePath\"Classified_Image_\"$imageStackName\"_\"$modelName\".tif\"
CI_path_check=$savePath\"segmented/\"$imageStackName\"_seg_\"$modelName\".tif\"
if [ -f $CI_path_check ]; then
   echo \"Segmented image already exists: exiting job\"
   exit 0
fi


(sleep $(( ($PBS_ARRAY_INDEX % 10) * 15 )))

~/Fiji.app/ImageJ-linux64 --ij2 --headless --run ~/code/generate_save_features.groovy \\
\"feature_model_table='$feature_model_table',\"\\
\"imageStackLocation='$imageStackLocation',\"\\
\"imageStackName='$imageStackName',\"\\
\"modelName='$modelName',\"\\
\"featureSavePath='$featurePath',\"\\
\"numberThreadsToUse='$numberThreadsToUse',\"\\
\"pixelSize_xy='$pixelSize_xy',\"\\
\"pixelSize_z='$pixelSize_z',\"\\
\"intensityScalingFactor='$intensityScalingFactor',\"\\
\"cropBox='$cropBox'\"

~/Fiji.app/ImageJ-linux64 --ij2 --headless --run ~/code/apply_classifiers.groovy \\
\"feature_model_table='$feature_model_table',\"\\
\"imageStackName='$imageStackName',\"\\
\"featurePath='$featurePath',\"\\
\"modelPath='$modelPath',\"\\
\"modelName='$modelName',\"\\
\"savePath='$savePath',\"\\
\"numberThreadsToUse='$numberThreadsToUse',\"\\
\"saveProbabilityMaps='$saveProbabilityMaps',\"\\
\"pixelSize_unit='$pixelSize_unit',\"\\
\"pixelSize_xy='$pixelSize_xy',\"\\
\"pixelSize_z='$pixelSize_z',\"\\
\"channel_grouping=$channel_grouping,\"\\
\"channels='$channels'\"

sleep 10s"
    )
  # cat(script,"\n")
  localScriptPath = paste0(local_rt,"temp/",jobName)
  cat(script,file=localScriptPath)
  cat("Wrote job script to ",localScriptPath,"\n")
  ssh_exec_wait(sess, command = paste0("mkdir autojobs/",jobName))
  print(paste0("run scp_upload with ",localScriptPath," ", paste0("autojobs/",jobName)))
  scp_upload(sess, localScriptPath, to = paste0("autojobs/",jobName), verbose = TRUE)
  cat("Uploaded script to remote drive in ", paste0("autojobs/",jobName),"\n")
  cmnd = paste0("qsub -q Short -J ",stackNums," ~/autojobs/",jobName,"/",jobName)
  cat("Submitting job:\n",cmnd,"\n")
  # ssh_exec_wait(session, command = cmnd )
  sub_response <- rawToChar(ssh_exec_internal(sess, command = paste0("cd ~/autojobs/",jobName," ; ",cmnd))$stdout)
  cat(sub_response)
  ssh_exec_wait(sess, command = paste0("echo '",timestamp(),"\n",cmnd,"\n",sub_response,"' >> ~/autojobs/",jobName,"/",jobName,"_job_submissions"))
}

segment_stacks_lookup <- function(dataset_name,stackNums,lu,sess,ram_GB=80,walltime="1:00:00",
                                  rt_fldr=rdm_rt,modelName='d18_rep1ds1gd_rf',segName="d18_intAdj_rep1ds1gd_rf",
                                  prefix='',stackNumberPrefix='t',stackNumberSuffix='-e',source_image_subfolder="Decon_output",jobname_extra=""){
  r = which(lu$name==dataset_name)
  segment_stacks(dataset_name,stackNums,
                 paste0(rt_fldr,lu$path[r],source_image_subfolder,"/"),
                 paste0(rt_fldr,lu$path[r],"segmentation/",segName,"/"),
                 sess,
                 lu$intensityScalingFactor[r],lu$intensityScalingFactorTimeStep[r],paste0(lu$cropBox[[r]],collapse=","),
                 modelName=modelName,prefix=prefix,stackNumberPrefix=stackNumberPrefix,stackNumberSuffix=stackNumberSuffix,jobname_extra=jobname_extra,
                 ram_GB=ram_GB,walltime=walltime)
}

# example calls

# segment_stacks("20190830_pre1","1-3",
#                paste0(rdm_rt,"20190830_LLSM_Yvette/Pre1/Decon_output/"),
#                paste0(rdm_rt,"20190830_LLSM_Yvette/Pre1/segmentation/d18_intAdj_rep1ds1gd_rf/"),
#                session,0.944822373393802,1.00154626565979,"314,761,282,801,12,129")
# 
# segment_stacks_lookup("20190830_pre1","1-3",dataset_info,session)

# used to have prefix='c1-'

segment_multi <- function(dataset_name,stackNums,input_path,output_path,sess,
                           intensityScalingFactor, intensityScalingFactorTimeStep, cropBox,
                           numberStacksPerJob=10,ram_GB=80,walltime="1:00:00",
                           feature_model_table_path='/home/uqjlefev/code/feature_model_table.txt',
                           modelName='d18_rep1ds1gd_rf',modelPath='models/',
                           numberThreadsToUse=3,cpusRequested=3,saveProbabilityMaps=TRUE,prefix='',
                           pixelSize_unit='nm',pixelSize_xy=104,pixelSize_z=268.146,
                           channel_grouping = "[[0],[1],[2,3]]",channels=4,
                           stackNumberPrefix='t',stackNumberSuffix='-e',jobname_extra=""
){
  # cropBox is min,max for x,y,z (0 indexed, endpoints included)
  # stackNumberPrefix and stackNumberSuffix may cause problems if they have special meaning in grep
  jobName = paste0("segment_multi_",dataset_name,jobname_extra)
  script = paste0(
    "#!/bin/bash\n#\n#PBS -N ",jobName,"\n#PBS -A UQ-IMB\n#PBS -l select=1:ncpus=",cpusRequested,":mpiprocs=",cpusRequested,":mem=",ram_GB,"GB,walltime=",walltime,"\n\n\n",
    "feature_model_table='",feature_model_table_path,"'\n",
    "imageStackLocation='",input_path,"'\n",
    "modelName='",modelName,"'\n",
    "modelPath='",modelPath,"'\n",
    "featurePath=$TMPDIR'/fs/'\n",
    "savePath='",output_path,"'\n",
    "numberThreadsToUse='",numberThreadsToUse,"'\n",
    "saveProbabilityMaps='",ifelse(saveProbabilityMaps,"true","false"),"'\n",
    "stackNumberPrefix='",stackNumberPrefix,"'\n",
    "stackNumberSuffix='",stackNumberSuffix,"'\n",
    "prefix='",prefix,"'\n",
    "pixelSize_unit='",pixelSize_unit,"'\n",
    "pixelSize_xy='",pixelSize_xy,"'\n",
    "pixelSize_z='",pixelSize_z,"'\n",
    "intensityScalingFactor='",intensityScalingFactor,"'\n",
    "intensityScalingFactorTimeStep='",intensityScalingFactorTimeStep,"'\n",
    "cropBox='",cropBox,"'\n",
    "numberStacksPerJob=",numberStacksPerJob,"\n",
    "channel_grouping='",channel_grouping,"'\n",
    "channels='",channels,"'\n\n

(sleep $(( ($PBS_ARRAY_INDEX % 10) * 15 )))

let firstStack=$numberStacksPerJob*$PBS_ARRAY_INDEX-$numberStacksPerJob+1
let lastStack=$numberStacksPerJob*$PBS_ARRAY_INDEX

echo \"job \"$PBS_ARRAY_INDEX\" stacks \"$firstStack\" - \"$lastStack
echo \"Apply cropping box: \"$cropBox
echo \"Pixel dimensions: \"$pixelSize_xy\" (x,y); \"$pixelSize_z\" (z)\"

for ((stackNum=$firstStack;stackNum<=$lastStack;stackNum++))
do
intensityScalingFactorAdjusted=$(echo \"$intensityScalingFactor $intensityScalingFactorTimeStep $stackNum\" | awk '{print $1*($2^($3-1));}')
echo \"stackNum = \"$stackNum
echo \"Adjusted intensityScalingFactor: \"$intensityScalingFactorAdjusted
imageStackName=$(ls $imageStackLocation | grep \"$stackNumberPrefix\"0*$stackNum\"$stackNumberSuffix\" | grep \"^$prefix\" | cut -d. -f1)

# CI_path_check=$savePath\"Classified_Image_\"$imageStackName\"_\"$modelName\".tif\"
CI_path_check=$savePath\"segmented/\"$imageStackName\"_seg_\"$modelName\".tif\"
if [ -f $CI_path_check ]; then
   echo \"Segmented image already exists: exiting job\"
   continue
fi

echo \"Clear feature stack:\"
rm -r $featurePath

~/Fiji.app/ImageJ-linux64 --ij2 --headless --run ~/code/generate_save_features.groovy \\
\"feature_model_table='$feature_model_table',\"\\
\"imageStackLocation='$imageStackLocation',\"\\
\"imageStackName='$imageStackName',\"\\
\"modelName='$modelName',\"\\
\"featureSavePath='$featurePath',\"\\
\"numberThreadsToUse='$numberThreadsToUse',\"\\
\"pixelSize_xy='$pixelSize_xy',\"\\
\"pixelSize_z='$pixelSize_z',\"\\
\"intensityScalingFactor='$intensityScalingFactorAdjusted',\"\\
\"cropBox='$cropBox'\"

~/Fiji.app/ImageJ-linux64 --ij2 --headless --run ~/code/apply_classifiers.groovy \\
\"feature_model_table='$feature_model_table',\"\\
\"imageStackName='$imageStackName',\"\\
\"featurePath='$featurePath',\"\\
\"modelPath='$modelPath',\"\\
\"modelName='$modelName',\"\\
\"savePath='$savePath',\"\\
\"numberThreadsToUse='$numberThreadsToUse',\"\\
\"saveProbabilityMaps='$saveProbabilityMaps',\"\\
\"pixelSize_unit='$pixelSize_unit',\"\\
\"pixelSize_xy='$pixelSize_xy',\"\\
\"pixelSize_z='$pixelSize_z',\"\\
\"channel_grouping=$channel_grouping,\"\\
\"channels='$channels'\"

done
sleep 10s
echo \"Finished\""
  )
  # cat(script,"\n")
  localScriptPath = paste0(local_rt,"temp/",jobName)
  cat(script,file=localScriptPath)
  cat("Wrote job script to ",localScriptPath,"\n")
  ssh_exec_wait(sess, command = paste0("mkdir autojobs/",jobName))
  print(paste0("run scp_upload with ",localScriptPath," ", paste0("autojobs/",jobName)))
  scp_upload(sess, localScriptPath, to = paste0("autojobs/",jobName), verbose = TRUE)
  cat("Uploaded script to remote drive in ", paste0("autojobs/",jobName),"\n")
  cmnd = paste0("qsub -q Short -J ",stackNums," ~/autojobs/",jobName,"/",jobName)
  cat("Submitting job:\n",cmnd,"\n")
  # ssh_exec_wait(session, command = cmnd )
  sub_response <- rawToChar(ssh_exec_internal(sess, command = paste0("cd ~/autojobs/",jobName," ; ",cmnd))$stdout)
  cat(sub_response)
  ssh_exec_wait(sess, command = paste0("echo '",timestamp(),"\n",cmnd,"\n",sub_response,"' >> ~/autojobs/",jobName,"/",jobName,"_job_submissions"))
}

segment_multi_lookup <- function(dataset_name,stackNums,lu,sess,numberStacksPerJob=10,ram_GB=80,walltime="3:00:00",
                                  rt_fldr=rdm_rt,modelName='d19_rep1ds1gd_rf',segName="d19_intAdj_rep1ds1gd_rf",
                                 prefix='',stackNumberPrefix='t',stackNumberSuffix='-e',source_image_subfolder="Decon_output",jobname_extra=""){
  r = which(lu$name==dataset_name)
  segment_multi(dataset_name,stackNums,
                 paste0(rt_fldr,lu$path[r],source_image_subfolder,"/"),
                 paste0(rt_fldr,lu$path[r],"segmentation/",segName,"/"),
                 sess,
                 lu$intensityScalingFactor[r],lu$intensityScalingFactorTimeStep[r],paste0(lu$cropBox[[r]],collapse=","),
                 numberStacksPerJob=numberStacksPerJob,modelName=modelName,prefix=prefix,
                stackNumberPrefix=stackNumberPrefix,stackNumberSuffix=stackNumberSuffix,jobname_extra=jobname_extra,
                 ram_GB=ram_GB,walltime=walltime)
}

####### ********************************* object analysis *********************************


# expect cropbox as vector of 6 integers
  

run_object_analysis <- function(jobName,jobNums,imageStackLocation,originalStackLocation,probStackLocation,savePath,sess,
                            intensityScalingFactor=1,cropBox=NA,numberStacksPerJob=20,ram_GB=80,walltime="3:00:00",cpu_request=2,
                            numberThreadsToUse=2,stackNumberPrefix="-t",stackNumberSuffix="-e",fileNamePrefix="c1-",
                            overwriteExisting=FALSE,classesToAnalyse=1:4,dynamic=c(200,20,20,20),minVoxExtraObjects=75,classNumsToFill=1:3,classLayers=c(1,2,2,1),
                            incorporatedChannels=list(1,2,3,2:3),channelsForDistanceMap=c(1,3),smoothingErosionForDistanceMap=c(300,20)){
  cropBox_string = ifelse(is.na(cropBox[1]),"",paste0("[[",cropBox[1],",",cropBox[2],"],[",cropBox[3],",",cropBox[4],"],[",cropBox[5],",",cropBox[6],"]]"))
  #cat(cropBox_string)
  script = paste0(
    "#!/bin/bash
#
#PBS -N ",jobName,"
#PBS -A UQ-IMB
#PBS -l select=1:ncpus=",cpu_request,":mpiprocs=",cpu_request,":mem=",ram_GB,"GB,walltime=",walltime,"

(sleep $(( ($PBS_ARRAY_INDEX % 10) * 15 )))

numberStacksPerJob=",numberStacksPerJob,"
lastStack=$(( ($numberStacksPerJob)*($PBS_ARRAY_INDEX) ))
firstStack=$(( ($lastStack)-($numberStacksPerJob)+1 ))

echo \"job \"$PBS_ARRAY_INDEX\" stacks \"$firstStack\" - \"$lastStack

~/Fiji.app/ImageJ-linux64 --ij2 --headless --allow-multiple --run ~/code/split_object_analysis.groovy \\
\"imageStackLocation='",imageStackLocation,"',\"\\
\"originalStackLocation='",originalStackLocation,"',\"\\
\"probStackLocation='",probStackLocation,"',\"\\
\"savePath='",savePath,"',\"\\
\"firstStackNumber='$firstStack',\"\\
\"lastStackNumber='$lastStack',\"\\
\"numberThreadsToUse='",numberThreadsToUse,"',\"\\
\"stackNumberPrefix='",stackNumberPrefix,"',\"\\
\"stackNumberSuffix='",stackNumberSuffix,"',\"\\
\"fileNamePrefix='",fileNamePrefix,"',\"\\
\"overwriteExisting='",ifelse(overwriteExisting,"true","false"),"',\"\\
\"classNumsToFill=[",paste0(classNumsToFill,collapse=","),"],\"\\
\"classesToAnalyse=[",paste0(classesToAnalyse,collapse=","),"],\"\\
\"classLayers=[",paste0(classLayers,collapse=","),"],\"\\
\"incorporatedChannels=[[",paste0(unlist(lapply(incorporatedChannels,function(v){paste0(v,collapse=",")})),collapse = "],["),"]],\"\\
\"dynamic=[",paste0(dynamic,collapse=","),"],\"\\
\"minVoxExtraObjects=",minVoxExtraObjects,",\"\\
\"channelsForDistanceMap=[",paste0(channelsForDistanceMap,collapse=","),"],\"\\
\"smoothingErosionForDistanceMap=[",paste0(smoothingErosionForDistanceMap,collapse=","),"],\"\\
\"intensityScalingFactor=",intensityScalingFactor,",\"\\
\"cropBox='",cropBox_string,"'\"
  
exit 0"
  )
  
  cat(script,"\n\n")
  #return()
  localScriptPath = paste0(local_rt,"temp/",jobName)
  cat(script,file=localScriptPath)
  cat("Wrote job script to ",localScriptPath,"\n")
  ssh_exec_wait(sess, command = paste0("mkdir ~/autojobs/",jobName))
  #scp_upload(sess, localScriptPath, to = paste0("~/autojobs/",jobName), verbose = TRUE)
  scp_upload(sess, localScriptPath, to = paste0("autojobs/",jobName), verbose = TRUE)
  cat("Uploaded script to remote drive in ", paste0("~/autojobs/",jobName),"\n")
  cmnd = paste0("qsub -q Short -J ",jobNums," ~/autojobs/",jobName,"/",jobName)
  cat("Submitting job:\n",cmnd,"\n")
  sub_response <- rawToChar(ssh_exec_internal(sess, command = paste0("cd ~/autojobs/",jobName," ; ",cmnd))$stdout)
  cat(sub_response)
  ssh_exec_wait(sess, command = paste0("echo '",timestamp(),"\n",cmnd,"\n",sub_response,"' >> ~/autojobs/",jobName,"/",jobName,"_job_submissions"))
}

#dataset_info
run_object_analysis_lookup <- function(dataset_name,jobNums,lu,sess,segName,objectAnalysis_folder="objectAnalysis",jobname_extra="", rt_fldr=rdm_rt,
                                       classesToAnalyse=1:4,dynamic=c(200,20,20,20),minVoxExtraObjects=75,channelsForDistanceMap=c(1,3),smoothingErosionForDistanceMap=c(300,20),
                                       numberStacksPerJob=20,ram_GB=80,walltime="3:00:00",cpu_request=2,numberThreadsToUse=2,
                                       stackNumberPrefix="-t",stackNumberSuffix="-e",fileNamePrefix="c1-",
                                       classNumsToFill=1:3,classLayers=c(1,2,2,1),
                                       incorporatedChannels=list(1,2,3,2:3),
                                       subfolder_original="Decon_output",subfolder_seg="segmented",subfolder_prob="probability_maps"){
  r = which(lu$name==dataset_name)
  run_object_analysis(paste0("objects_",dataset_name,jobname_extra),
                      jobNums,
                      paste0(rt_fldr,lu$path[r],"segmentation/",segName,"/",subfolder_seg,"/"),
                      paste0(rt_fldr,lu$path[r],subfolder_original,"/"),
                      paste0(rt_fldr,lu$path[r],"segmentation/",segName,"/",subfolder_prob,"/"),
                      paste0(rt_fldr,lu$path[r],"segmentation/",segName,"/",objectAnalysis_folder,"/"),
                      sess,lu$intensityScalingFactor[r],lu$cropBox[[r]],
                      numberStacksPerJob,ram_GB,walltime,cpu_request,
                      classesToAnalyse=classesToAnalyse,dynamic=dynamic,minVoxExtraObjects=minVoxExtraObjects,
                      classNumsToFill=classNumsToFill,classLayers=classLayers,
                      stackNumberPrefix=stackNumberPrefix,stackNumberSuffix=stackNumberSuffix,fileNamePrefix=fileNamePrefix,
                      incorporatedChannels=incorporatedChannels,channelsForDistanceMap=channelsForDistanceMap,smoothingErosionForDistanceMap=smoothingErosionForDistanceMap
                      
                      )
}

# run_object_analysis("objects_20190809_post1_d18_intAdj_rep1ds1gd_rf",
#                     "1:2",
#                     "/30days/uqjlefev/20190809/Post1/segmentation/d18_intAdj_rep1ds1gd_rf/segmented/",
#                     "/home/james/mnt/rdm_adam/MachineLearning_ARC_Grant_work/20190830_LLSM_Yvette/Pre1/Decon_output/",
#                     "/home/james/mnt/rdm_adam/MachineLearning_ARC_Grant_work/20190830_LLSM_Yvette/Pre1/segmentation/d19_intAdj_rep1ds1gd_rf/probability_maps/",
#                     "/30days/uqjlefev/20190809/Post1/segmentation/d18_intAdj_rep1ds1gd_rf/objectAnalysis/",
#                     session)


#incorporatedChannels=list(1,2,3,2:3)
#paste0("[[",paste0(unlist(lapply(incorporatedChannels,function(v){paste0(v,collapse=",")})),collapse = "],["),"]]")



run_mesh_generation <- function(jobName,jobNums,imageStackLocation,savePath,sess,
                                numberStacksPerJob=10,ram_GB=10,walltime="5:00:00",
                                numberThreadsToUse=2,stackNumberPrefix="-t",stackNumberSuffix="-e",fileNamePrefix="c1-",
                                overwriteExisting=FALSE,classesToAnalyse=1:4,targetMeshVertexReduction=c(0.96,0.0,0.8,0.8),meshSmoothingIterations=c(0,0,0,0)){
  cat(" started run_mesh_generation\n")
  script = paste0("#!/bin/bash
#
#PBS -N ",jobName,"
#PBS -A UQ-IMB
#PBS -l select=1:ncpus=2:mpiprocs=2:mem=",ram_GB,"GB,walltime=",walltime,"

(sleep $(( ($PBS_ARRAY_INDEX % 10) * 15 )))

numberStacksPerJob=",numberStacksPerJob,"
lastStack=$(( ($numberStacksPerJob)*($PBS_ARRAY_INDEX) ))
firstStack=$(( ($lastStack)-($numberStacksPerJob)+1 ))

echo \"job \"$PBS_ARRAY_INDEX\" stacks \"$firstStack\" - \"$lastStack

~/Fiji.app/ImageJ-linux64 --ij2 --headless --allow-multiple --run ~/code/get_meshes.groovy \\
\"imageStackLocation='",imageStackLocation,"',\"\\
\"savePath='",savePath,"',\"\\
\"classesToAnalyse='[",paste0(classesToAnalyse,collapse=","),"]',\"\\
\"firstStackNumber='$firstStack',\"\\
\"lastStackNumber='$lastStack',\"\\
\"numberThreadsToUse='",numberThreadsToUse,"',\"\\
\"stackNumberPrefix='",stackNumberPrefix,"',\"\\
\"stackNumberSuffix='",stackNumberSuffix,"',\"\\
\"fileNamePrefix='",fileNamePrefix,"',\"\\
\"overwriteExisting='",ifelse(overwriteExisting,"true","false"),"',\"\\
\"targetMeshVertexReduction='[",paste0(targetMeshVertexReduction,collapse=","),"]',\"\\
\"meshSmoothingIterations='[",paste0(meshSmoothingIterations,collapse=","),"]',\"

exit 0"
  )

  
  cat(script,"\n\n")
  #return()
  localScriptPath = paste0(local_rt,"temp/",jobName)
  cat(script,file=localScriptPath)
  cat("Wrote job script to ",localScriptPath,"\n")
  ssh_exec_wait(sess, command = paste0("mkdir autojobs/",jobName))
  scp_upload(sess, localScriptPath, to = paste0("autojobs/",jobName), verbose = TRUE)
  cat("Uploaded script to remote drive in ", paste0("autojobs/",jobName),"\n")
  cmnd = paste0("qsub -q Short -J ",jobNums," ~/autojobs/",jobName,"/",jobName)
  cat("Submitting job:\n",cmnd,"\n")
  sub_response <- rawToChar(ssh_exec_internal(sess, command = paste0("cd autojobs/",jobName," ; ",cmnd))$stdout)
  cat(sub_response)
  ssh_exec_wait(sess, command = paste0("echo '",timestamp(),"\n",cmnd,"\n",sub_response,"' >> autojobs/",jobName,"/",jobName,"_job_submissions"))
}

run_mesh_generation_lookup <- function(dataset_name,jobNums,lu,sess,segName,jobname_extra="", rt_fldr=rdm_rt,
                                       object_subfolder="objectAnalysis",
                                       numberStacksPerJob=10,ram_GB=10,walltime="5:00:00",
                                       numberThreadsToUse=2,stackNumberPrefix="-t",stackNumberSuffix="-e",fileNamePrefix="c1-",
                                       overwriteExisting=FALSE,verbose=FALSE,classesToAnalyse=1:4,targetMeshVertexReduction=c(0.96,0.0,0.8,0.8),meshSmoothingIterations=c(0,0,0,0)){
  if (verbose){cat(" started run_mesh_generation_lookup\n")}
  r = which(lu$name==dataset_name)

  if (verbose){print(lu$path[r])}
  fldr = paste0(rt_fldr,lu$path[r],"segmentation/",segName,"/",object_subfolder,"/")
  #print(fldr)
  run_mesh_generation(paste0("meshes_",dataset_name,"_",segName,jobname_extra),
                      jobNums,fldr,fldr,sess,numberStacksPerJob,ram_GB,walltime,
                      numberThreadsToUse,stackNumberPrefix,stackNumberSuffix,fileNamePrefix,
                      overwriteExisting,classesToAnalyse,targetMeshVertexReduction,meshSmoothingIterations)
}


run_skeleton_generation <- function(jobName,jobNums,imageStackLocation,savePath,sess,
                                numberStacksPerJob=100,ram_GB=10,walltime="1:00:00",
                                numberThreadsToUse=2,stackNumberPrefix="-t",stackNumberSuffix="-e",fileNamePrefix="c1-",
                                overwriteExisting=FALSE,classesToAnalyse=2,verbose=FALSE){
  if (verbose){cat(" started run_skeleton_generation\n")}
  script = paste0("#!/bin/bash
#
#PBS -N ",jobName,"
#PBS -A UQ-IMB
#PBS -l select=1:ncpus=2:mpiprocs=2:mem=",ram_GB,"GB,walltime=",walltime,"

(sleep $(( ($PBS_ARRAY_INDEX % 10) * 15 )))

numberStacksPerJob=",numberStacksPerJob,"
lastStack=$(( ($numberStacksPerJob)*($PBS_ARRAY_INDEX) ))
firstStack=$(( ($lastStack)-($numberStacksPerJob)+1 ))

echo \"job \"$PBS_ARRAY_INDEX\" stacks \"$firstStack\" - \"$lastStack

~/Fiji.app/ImageJ-linux64 --ij2 --headless --allow-multiple --run ~/code/get_skeletons.groovy \\
\"imageStackLocation='",imageStackLocation,"',\"\\
\"savePath='",savePath,"',\"\\
\"classesToAnalyse='[",paste0(classesToAnalyse,collapse=","),"]',\"\\
\"firstStackNumber='$firstStack',\"\\
\"lastStackNumber='$lastStack',\"\\
\"numberThreadsToUse='",numberThreadsToUse,"',\"\\
\"stackNumberPrefix='",stackNumberPrefix,"',\"\\
\"stackNumberSuffix='",stackNumberSuffix,"',\"\\
\"fileNamePrefix='",fileNamePrefix,"',\"\\
\"overwriteExisting='",ifelse(overwriteExisting,"true","false"),"',\"

exit 0"
  )
  
  
  if (verbose){cat(script,"\n\n")}
  #return()
  localScriptPath = paste0(local_rt,"temp/",jobName)
  cat(script,file=localScriptPath)
  if (verbose){cat("Wrote job script to ",localScriptPath,"\n")}
  ssh_exec_wait(sess, command = paste0("mkdir ~/autojobs/",jobName))
  scp_upload(sess, localScriptPath, to = paste0("autojobs/",jobName), verbose = TRUE)
  if (verbose){cat("Uploaded script to remote drive in ", paste0("~/autojobs/",jobName),"\n")}
  cmnd = paste0("qsub -q Short -J ",jobNums," ~/autojobs/",jobName,"/",jobName)
  if (verbose){cat("Submitting job:\n",cmnd,"\n")}
  sub_response <- rawToChar(ssh_exec_internal(sess, command = paste0("cd ~/autojobs/",jobName," ; ",cmnd))$stdout)
  cat(sub_response)
  ssh_exec_wait(sess, command = paste0("echo '",timestamp(),"\n",cmnd,"\n",sub_response,"' >> ~/autojobs/",jobName,"/",jobName,"_job_submissions"))
}
  

run_skeleton_generation_lookup <- function(dataset_name,jobNums,lu,sess,segName,jobname_extra="",rt_fldr=rdm_rt,
                                       object_subfolder="objectAnalysis",
                                       numberStacksPerJob=100,ram_GB=10,walltime="1:00:00",
                                       numberThreadsToUse=2,stackNumberPrefix="-t",stackNumberSuffix="-e",fileNamePrefix="c1-",
                                       overwriteExisting=FALSE,classesToAnalyse=2,verbose=FALSE){
  if (verbose){cat(" started run_skeleton_generation_lookup\n")}
  r = which(lu$name==dataset_name)
  
  if (verbose){print(lu$path[r])}
  fldr = paste0(rt_fldr,lu$path[r],"segmentation/",segName,"/",object_subfolder,"/")
  #print(fldr)
  run_skeleton_generation(paste0("skeletons_",dataset_name,"_",segName,jobname_extra),
                      jobNums,fldr,fldr,sess,numberStacksPerJob,ram_GB,walltime,
                      numberThreadsToUse,stackNumberPrefix,stackNumberSuffix,fileNamePrefix,
                      overwriteExisting,classesToAnalyse,verbose)
}



