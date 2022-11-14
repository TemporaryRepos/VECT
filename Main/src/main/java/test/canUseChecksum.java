package test;

import JimpleMixer.blocks.BlockInfo;
import JimpleMixer.blocks.BlocksContainer;
import JimpleMixer.blocks.ChecksumHelper;
import JimpleMixer.core.Configuration;
import JimpleMixer.core.JMUtils;
import JimpleMixer.core.MutationHelper;
import core.ClassInfo;
import core.MainConfiguration;
import core.MainHelper;
import dtjvms.*;
import dtjvms.analyzer.DiffCore;
import dtjvms.analyzer.JDKAnalyzer;
import dtjvms.executor.CFM.CFMExecutor;
import dtjvms.executor.CFM.JvmOutput;
import dtjvms.executor.ExecutorHelper;
import dtjvms.loader.DTLoader;
import org.junit.Test;
import soot.*;
import soot.options.Options;

import java.io.*;
import java.util.*;

public class canUseChecksum {

    @Test
    public void getName() throws IOException {
        String path = "D:\\我的文件\\研究生\\JavaTailor\\CFSynthesisV2\\03results\\1656668770711\\HotspotTests-Java";
        File file = new File(path+"\\difference.log");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String line = bufferedReader.readLine();
        while (line!=null){
            if(line.contains("Difference found:")){
                System.out.println(line.substring(50,line.length()-25));
            }
            line = bufferedReader.readLine();
        }
    }
    public static String projectName = "HotspotTests-Java";  // 提供种子文件的项目
    public static boolean projectPreDefineFlag = true;  // 此项目是否使用预定义类
    public static String mutationProviderProject = "HotspotTests-Java";  // 提供合成块文件的项目
    public static boolean providerProjectPreDefineFlag = true;  // 此项目是否使用预定义类

    public static boolean providerProjectAllDefineFlag = false;  // 此项目是否提取所有的可用类（当项目的application类过少时使用）
    public static String packagePath = "";  // packagePath + packageName 指向项目的可用类
    public static String packageName = "Bug_triggering_input";

    public static String useClustering = "noCluster"; // 选择： noCluster、csv、kmeans、null
    public static int classNum = 10; // 聚类类数，仅对kmeans有效
    public static int ngram = 0; // 分词窗口，仅对kmeans与csv有效,特殊（ngram=0代表使用infercode得到的csv）

    public static String selectMethod = "RWS"; // 选择： random、softmax、RWS

    public static boolean oneSeed = false;  // T 代表仅使用初始seed，不删除也不增加 F 原始代码
    public static  boolean oneDirected = false; // T 代表只使用一个插入点 F 原始代码

    public static boolean useTimeout = false;

    public static String addLoopFlag = "noloop"; // 添加循环体的方式 选择： all、seed、ingredient、noloop

    public static boolean checksum = true; // 是否使用checksum

    public static List<String> projectNames = new ArrayList<String>();

    public static boolean covTest = false; //是否进行覆盖率测试
    public static String covJavaCmd = "/home/JVMTest/jvmCovTest/openjdk/build/linux-x86_64-normal-server-release/jdk/bin/java"; //进行覆盖率测试的Java位置


    public static String defineClassesPath = "testcases.txt";
    public static int randomSeed = 1;
    public static String timeStamp;
    public static List<ClassInfo> seeds;
    public static boolean crossProject = false;

    public static void main(String[] args) throws IOException {

        timeStamp = String.valueOf(new Date().getTime());
        DTGlobal.setDiffLogger(timeStamp + DTPlatform.FILE_SEPARATOR + projectName, "difference");
        DTGlobal.setSelectLogger(timeStamp + DTPlatform.FILE_SEPARATOR + projectName, "selectInfo");
        DTGlobal.setInsertLogger(timeStamp + DTPlatform.FILE_SEPARATOR + projectName, "insertInfo");
        DTConfiguration.debug = false;
        DTConfiguration.setJvmDepensRoot("." + DTPlatform.FILE_SEPARATOR + "01JVMS");
        DTConfiguration.setProjectDepensRoot("." + DTPlatform.FILE_SEPARATOR + "sootOutput");
        System.out.println(DTPlatform.getInstance());
        ArrayList<JvmInfo> jvmCmds = DTLoader.getInstance().loadJvms();
        for (JvmInfo jvmCmd : jvmCmds) {
            System.out.println(jvmCmd);
        }
        ProjectInfo originProject = null;
        ProjectInfo targetProject = null;
        String mutationHistoryPath = MainConfiguration.mutationHistoryPath +
                DTPlatform.FILE_SEPARATOR + timeStamp +
                DTPlatform.FILE_SEPARATOR + "classhistory";

        String diffClassPath = MainConfiguration.mutationHistoryPath +
                DTPlatform.FILE_SEPARATOR + timeStamp +
                DTPlatform.FILE_SEPARATOR + "diffClass";
        MainHelper.createFolderIfNotExist(mutationHistoryPath);
        MainHelper.createFolderIfNotExist(diffClassPath);
        originProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("02Benchmarks", projectName, null, projectPreDefineFlag);
        targetProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("sootOutput", projectName, null, projectPreDefineFlag);

        System.out.println(originProject);
        System.out.println(targetProject);
        Configuration.initSootEnvWithClassPath(targetProject.getpClassPath());
        Configuration.set_output_path(targetProject.getSrcClassPath());
        // SootOutput may have been modified and overwritten before mutating
        List<String> seedClasses = originProject.getApplicationClasses();
        MainHelper.restoreBadClasses(seedClasses, originProject, targetProject);
        // As the files on the hard disk change, so do the files in memory
        targetProject.setApplicationClasses(new ArrayList<>(seedClasses));
        seeds = MainHelper.initialSeeds(seedClasses, targetProject.getSrcClassPath());
        // Since there are many private methods that cannot be accessed, we need to change them to the public flag
        List<String> mutationClasses = MainHelper.duplicateSeedsAndChangeModifiers(seedClasses);
        // initialize the SOOT environment
        G.reset();
        Configuration.initSootEnvWithClassPath(targetProject.getpClassPath());
        Configuration.set_output_path(targetProject.getSrcClassPath());
        // perform basic block analysis
//            System.out.println(mutationClasses);
        BlocksContainer.initMutantsFromClasses(mutationClasses);
        String classFileFolder = "";
        Unit targetUnit = null;
        Random random = new Random(randomSeed);
        Set<String> strings = new HashSet<>();
        ChecksumHelper.createChecksumFile(originProject.getSrcClassPath());
        ChecksumHelper.createChecksumFile(targetProject.getSrcClassPath());
        for (ClassInfo seed : seeds) {
//            System.out.println(seed.getOriginClassName());

            // Because test classes and normal classes do not behave quite the same, they are treated differently
            if (seed.isJunit()){
                Configuration.set_output_path(targetProject.getTestClassPath());
            } else {
                Configuration.set_output_path(targetProject.getSrcClassPath());
            }
//            System.out.println("current: " + seed.getClassName());
            // Soot class was loaded and converted into Soot method
            SootClass seedClass;
            if (seed.isOriginClass() && !seed.hasCovered()){
                seedClass = JMUtils.loadTargetClass(seed.getOriginClassName());
            } else {
                seed.storeToCoverOriginClass();
                seedClass = JMUtils.loadTargetClass(seed.getOriginClassName());
            }

            if (seedClass == null){
                continue;
            }
            boolean flag = false;
            for (SootMethod method : seedClass.getMethods()) {
                for (Unit unit : method.retrieveActiveBody().getUnits()) {
                    if(unit.toString().toLowerCase(Locale.ROOT).contains("currenttime")
                            || unit.toString().toLowerCase(Locale.ROOT).contains("random")
                            || unit.toString().toLowerCase(Locale.ROOT).contains("nanotime")
                            || unit.toString().toLowerCase(Locale.ROOT).contains("hash")
                            || unit.toString().toLowerCase(Locale.ROOT).contains("gettime")
                            ){
                        if(!(
                                unit.toString().toLowerCase(Locale.ROOT).contains("hashset")
                                ||unit.toString().toLowerCase(Locale.ROOT).contains("hashmap")
                                ||unit.toString().toLowerCase(Locale.ROOT).contains("random_arg")
                        )){
                            flag = true;
                        }

                    }
                    if (flag){
                        break;
                    }
                }
                if(flag){
                    break;
                }
            }
            if(flag){
                System.out.println(seed.getOriginClassName());
            }
//            ChecksumHelper.checksumForClass(seedClass);
//            if (JMUtils.saveSootClassToLocal(seedClass, Options.output_format_class)) {
//                seed.mutationTimesIncrease();
//                ClassInfo newMutateClass = new ClassInfo(seed.getOriginClassName(),
//                        seed.getOriginClassPath(),
//                        seed.generateMutateClassFilename(),
//                        classFileFolder + DTPlatform.FILE_SEPARATOR + seed.generateMutateClassFilename(),
//                        seed.isJunit(),
//                        seed.getMutationOrder() + 1,
//                        0,
//                        seed.isLoop());
//                newMutateClass.saveSootClassToFile(seedClass);
//                if (!CFMExecutor.getInstance().dtSingleClassInProj(jvmCmds,
//                        targetProject,
//                        newMutateClass.getOriginClassName(),
//                        newMutateClass.getClassName())){
//                }
//                if (CFMExecutor.getInstance().isDiffFound()){
//                    if(useTimeout || (!JvmOutput.timeoutFlag)){
//                        DTGlobal.getInsertInfo().clear();
//                        JvmOutput.timeoutFlag = false;
//                        String diffClassFolder = diffClassPath + DTPlatform.FILE_SEPARATOR + seed.getOriginClassName();
//                        MainHelper.createFolderIfNotExist(diffClassFolder);
//                        newMutateClass.saveSootClassToTargetPath(seedClass, diffClassFolder + DTPlatform.FILE_SEPARATOR + newMutateClass.getClassName());
//                    }
//                }else{
//                    DTGlobal.getSelectLogger().info("没有发现差异\n");
//                    DTGlobal.getInsertInfo().clear();
//                }
//            }
            Scene.v().removeClass(seedClass);
        }
    }
}
