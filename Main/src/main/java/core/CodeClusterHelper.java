package core;

import JimpleMixer.blocks.*;
import JimpleMixer.core.Configuration;
import JimpleMixer.core.JMUtils;
import JimpleMixer.core.MutationHelper;
import dtjvms.DTConfiguration;
import dtjvms.DTGlobal;
import dtjvms.DTPlatform;
import dtjvms.ProjectInfo;
import dtjvms.loader.DTLoader;
import soot.*;
import soot.options.Options;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CodeClusterHelper {
    public static void main(String[] args) throws IOException {
        class2java();
    }
    public static final int NO_CLUSTER = 1;
    public static final int INFER_CODE_CLUSTER = 2;
    public static final int CODE_BERT_CLUSTER = 3;
    public static final int CODE_GPT_CLUSTER = 4;
    public static final int CODE_T5_CLUSTER = 5;
    public static final int PL_BART_CLUSTER = 6;

    private static String csvFileName = "";
    /**
     * Gao Tianchang：通过method选择聚类方法
     * @param method
     */
    public static void clusteringFromBlock(int method) {
        if (method == NO_CLUSTER){
            clusteringFromBlockByNoCluster();
        }
        else {
            switch (method){
                case INFER_CODE_CLUSTER:
                    csvFileName = "InferCodeAssignment.csv";
                    break;
                case CODE_BERT_CLUSTER:
                    csvFileName = "CodeBERTAssignment.csv";
                    break;
                case CODE_GPT_CLUSTER:
                    csvFileName = "CodeGPTAssignment.csv";
                    break;
                case CODE_T5_CLUSTER:
                    csvFileName = "CodeT5Assignment.csv";
                    break;
                case PL_BART_CLUSTER:
                    csvFileName = "PlBartAssignment.csv";
                    break;
            }
            clusteringFromBlockByCsv();
        }
    }
    public static void jimple2class() throws IOException {
        String projectName = "templateClass";  // 提供种子文件的项目
        boolean projectPreDefineFlag = false;  // 此项目是否使用预定义类
        String mutationProviderProject = "HotspotTests-Java";  // 提供合成块文件的项目
        boolean providerProjectPreDefineFlag = true;  // 此项目是否使用预定义类
        boolean providerProjectAllDefineFlag = false;  // 此项目是否提取所有的可用类（当项目的application类过少时使用）
        String packagePath = "";  // packagePath + packageName 指向项目的可用类
        String packageName = "Bug_triggering_input";
        int randomSeed = 1;
        String timeStamp;
        List<ClassInfo> seeds;

        timeStamp = String.valueOf(new Date().getTime());
        DTGlobal.setDiffLogger(timeStamp + DTPlatform.FILE_SEPARATOR + projectName, "difference");
        DTGlobal.setSelectLogger(timeStamp + DTPlatform.FILE_SEPARATOR + projectName, "selectInfo");
        DTGlobal.setInsertLogger(timeStamp + DTPlatform.FILE_SEPARATOR + projectName, "insertInfo");
        DTConfiguration.debug = false;
        DTConfiguration.setJvmDepensRoot("." + DTPlatform.FILE_SEPARATOR + "01JVMS");
        DTConfiguration.setProjectDepensRoot("." + DTPlatform.FILE_SEPARATOR + "sootOutput");
        ProjectInfo originMutationProject = null;
        ProjectInfo mutationProject = null;

        String mutationHistoryPath = MainConfiguration.mutationHistoryPath +
                DTPlatform.FILE_SEPARATOR + timeStamp +
                DTPlatform.FILE_SEPARATOR + "classhistory";
        MainHelper.createFolderIfNotExist(mutationHistoryPath);

        // 加载ingredients项目
        originMutationProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("02Benchmarks", mutationProviderProject, null, true);
        mutationProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("sootOutput", mutationProviderProject, null, true);
        if(providerProjectAllDefineFlag){
            originMutationProject.setApplicationClasses((ArrayList<String>) ClazzUtils.run(originMutationProject.getSrcClassPath(),packagePath,packageName));
        }
        System.out.println(originMutationProject);
        System.out.println(mutationProject);
        // 设置soot环境
        Configuration.initSootEnvWithClassPath(mutationProject.getpClassPath());
        Configuration.set_output_path(mutationProject.getSrcClassPath());
        // 更新ingredients项目
        List<String> originMutateClasses = originMutationProject.getApplicationClasses();
        MainHelper.restoreBadClasses(originMutateClasses, originMutationProject, mutationProject);
        mutationProject.setApplicationClasses(new ArrayList<>(originMutateClasses));
        // 修改ingredients可访问性
        List<String> mutationClasses = MainHelper.duplicateSeedsAndChangeModifiers(originMutateClasses);
        // 加载seedclass项目
        ProjectInfo originProject = null;
        ProjectInfo targetProject = null;
        originProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("02Benchmarks", projectName, null, false);
        targetProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("sootOutput", projectName, null, false);
        System.out.println(originProject);
        System.out.println(targetProject);
        // 设置soot环境
        G.reset();
        Configuration.initSootEnvWithClassPath(targetProject.getpClassPath() + System.getProperty("path.separator") + mutationProject.getpClassPath());
        Configuration.set_output_path(targetProject.getSrcClassPath());
        // 更新seedclass项目
        List<String> seedClasses = originProject.getApplicationClasses();
        MainHelper.restoreBadClasses(seedClasses, originProject, targetProject);
        targetProject.setApplicationClasses(new ArrayList<>(seedClasses));
        List<String> seedJunits = originProject.getJunitClasses();
        MainHelper.restoreBadClasses(seedJunits, originProject, targetProject);
        targetProject.setJunitClasses(new ArrayList<>(seedJunits));
        // 生成ingredients
        BlocksContainer.initMutantsFromClasses(mutationClasses);
        // 加载seed class
        seeds = MainHelper.initialSeedsWithType(seedClasses, targetProject.getSrcClassPath(), false, mutationHistoryPath);
        seeds.addAll(MainHelper.initialSeedsWithType(seedJunits, targetProject.getTestClassPath(), true, mutationHistoryPath));

        Random random = new Random(randomSeed);
        ClassInfo seed = null;
        for (ClassInfo s : seeds) {
            if(s.getOriginClassName().equals("template")){
                seed = s;
                break;
            }
        }
        String classFileFolder = mutationHistoryPath + DTPlatform.FILE_SEPARATOR + seed.getOriginClassName();
        MainHelper.createFolderIfNotExist(classFileFolder);
        if (seed.isJunit()){
            Configuration.set_output_path(targetProject.getTestClassPath());
        } else {
            Configuration.set_output_path(targetProject.getSrcClassPath());
        }
        Map<String, List<BlockInfo>> ingredients = BlocksContainer.getValidMutationMap();
        Set<String> classCandidate = ingredients.keySet();
        List<StringBuilder> nameList = new ArrayList<>();
        for(String candidate:classCandidate){
            List<BlockInfo> methodCandidate = ingredients.get(candidate);
            for(BlockInfo ingredient: methodCandidate){

                SootClass seedClass;
                if (!seed.isOriginClass() || seed.hasCovered()) {
                    seed.storeToCoverOriginClass();
                }
                seedClass = JMUtils.loadTargetClass(seed.getOriginClassName());
                assert seedClass != null;
                for (SootMethod method : seedClass.getMethods()) {
                    if (!method.isAbstract()){
                        method.retrieveActiveBody();
                    }

                }

                SootMethod seedMethod = seedClass.getMethod("void main(java.lang.String[])");
                Body methodBody = seedMethod.retrieveActiveBody();
                List<Unit> validUnits = MainHelper.getValidStmtsFromMethodBody(methodBody);
                Unit targetUnit = validUnits.get(0);
                String mutantClass = ingredient.getClassName();
                MutationHelper.insertBlockToMethod(seedClass, methodBody, targetUnit, BlocksContainer.getAllMutationMap().get(mutantClass), ingredient);
                JMUtils.saveSootClassToLocal(seedClass, Options.output_format_class);
                seed.mutationTimesIncrease();
                String mutateClassFilename = seed.generateMutateClassFilename();
                ClassInfo newMutateClass = new ClassInfo(seed.getOriginClassName(),
                        seed.getOriginClassPath(),
                        seed.generateMutateClassFilename(),
                        classFileFolder + DTPlatform.FILE_SEPARATOR + mutateClassFilename,
                        seed.isJunit(),
                        seed.getMutationOrder() + 1,
                        0,
                        seed.isLoop());
                newMutateClass.saveSootClassToFile(seedClass);
                nameList.add(new StringBuilder(mutateClassFilename));
                Scene.v().removeClass(seedClass);
            }
        }
        CSVUtils.exportCsv(new File("./className.csv"),nameList);
    }
    public static void class2java() throws IOException {
        String path = ".\\out\\artifacts\\jimple2class_jar";
        List<String> nameList = CSVUtils.importStringCsv(new File(path+"\\className.csv"));
        int tempTimeFlag = 0;
        for(String name:nameList){
            tempTimeFlag++;
            System.out.print(tempTimeFlag);
            String cmd = "cmd /c "+
                    ".\\jad.exe -p "+
                    path+"\\template"+DTPlatform.FILE_SEPARATOR+name+
                    " > "+
                    path+"\\javaFile"+DTPlatform.FILE_SEPARATOR+name.replace(".class",".java");
            System.out.println(":"+cmd);
            Process p = Runtime.getRuntime().exec(cmd);
        }
    }



    private static void clusteringFromBlockByNoCluster() {
        BlocksContainer.allClusteringMutation = new ArrayList<>();
        for(String key:BlocksContainer.validMutationMap.keySet()) {
            List<BlockInfo> blockInfos = BlocksContainer.validMutationMap.get(key);
            for (BlockInfo blockInfo : blockInfos) {
                List<BlockInfo> blockInfoList = new ArrayList<>();
                blockInfoList.add(blockInfo);
                BlocksContainer.allClusteringMutation.add(blockInfoList);
            }
        }
    }




    private static void clusteringFromBlockByCsv() {
        BlocksContainer.allClusteringMutation = new ArrayList<>();
        // 临时存储与语句序列对应的blockInfo
        List<BlockInfo> allBlockInfo = new ArrayList<>();
        for(String key:BlocksContainer.validMutationMap.keySet()){
            List<BlockInfo> blockInfos = BlocksContainer.validMutationMap.get(key);
            allBlockInfo.addAll(blockInfos);
        }
        int [] assignments = CSVUtils.importIntCsv(new File("."+ DTPlatform.FILE_SEPARATOR+csvFileName));
        for(int i = 0; i<= Arrays.stream(assignments).max().getAsInt(); i++){
            BlocksContainer.allClusteringMutation.add(new ArrayList<>());
        }
        for(int i=0;i<assignments.length;i++){
            BlocksContainer.allClusteringMutation.get(assignments[i]).add(allBlockInfo.get(i));
        }
    }
}
