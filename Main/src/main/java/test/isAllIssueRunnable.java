package test;

import JimpleMixer.blocks.BlockInfo;
import JimpleMixer.blocks.BlocksContainer;
import JimpleMixer.blocks.ChecksumHelper;
import JimpleMixer.blocks.LoopWrapHelper;
import JimpleMixer.core.Configuration;
import JimpleMixer.core.JMUtils;
import JimpleMixer.core.MutationHelper;
import core.*;
import core.ClazzUtils;
import dtjvms.*;
import dtjvms.analyzer.DiffCore;
import dtjvms.analyzer.JDKAnalyzer;
import dtjvms.executor.CFM.CFMExecutor;
import dtjvms.executor.CFM.JvmOutput;
import dtjvms.executor.ExecutorHelper;
import dtjvms.loader.DTLoader;
import org.apache.commons.cli.CommandLine;
import org.junit.Test;
import soot.*;
import soot.options.Options;

import java.io.*;
import java.sql.Time;
import java.util.concurrent.TimeUnit;
import java.util.*;

import static core.MainHelper.coverageTest;

public class isAllIssueRunnable {

    /**
     * Here you can set up the project you want to test
     */

    public static String projectName = "Openj9Test-Test";  // 提供种子文件的项目
    public static boolean projectPreDefineFlag = true;  // 此项目是否使用预定义类
    public static String mutationProviderProject = "HotspotTests-Java";  // 提供合成块文件的项目
    public static boolean providerProjectPreDefineFlag = true;  // 此项目是否使用预定义类

    public static boolean providerProjectAllDefineFlag = false;  // 此项目是否提取所有的可用类（当项目的application类过少时使用）
    public static String packagePath = "out"+DTPlatform.FILE_SEPARATOR+"production"+DTPlatform.FILE_SEPARATOR+"HotspotIssue";  // packagePath + packageName 指向项目的可用类
    public static String packageName = "Bug_triggering_input";

    public static int useClustering = CodeClusterHelper.NO_CLUSTER;

    public static int selectMethod = SelectBlockHelper.RANDOM_SELECT;

    public static boolean useVMOptions = false;

    public static boolean addLoopFlag = false; // 添加循环体的方式 选择： all、none

    public static boolean checksum = true; // 是否使用checksum

    public static boolean reshape = false;

    public static long exeTime = 60 * 60 * 9;

    public static boolean oneSeed = false;  // T 代表仅使用初始seed，不删除也不增加 F 原始代码
    public static  boolean oneDirected = false; // T 代表只使用一个插入点 F 原始代码

    public static boolean useTimeout = false;

    public static boolean covTest = false; //是否进行覆盖率测试
    public static String covJavaCmd = "/home/share/hotspot/openjdk/build/linux-x86_64-normal-server-release/jdk/bin/java"; //进行覆盖率测试的Java位置


    public static String defineClassesPath = "testcases.txt";
    public static int randomSeed = 1;
    public static String timeStamp;
    public static List<ClassInfo> seeds;
    public static boolean crossProject = false;


    /**
     * Here we go！
     * @param args
     */
    public static void main(String[] args) throws IOException {
        run(args);
    }
    public static void run(String[] args)  throws IOException {

        // Read your Settings from the command line
        CommandLine options = MainHelper.parseArgs(args);

        if (options != null){
            if (options.hasOption("help")) { // seed class project
                System.out.println("You can use the following parameters:");
                System.out.println("-t or -timestamp to set timeStamp");
                System.out.println("          for example,1659665267170");
                System.out.println("-s or -seed to set seed class project");
                System.out.println("          for example,HotspotTests-Java");
                System.out.println("-p or -provide to set ingredients provide project");
                System.out.println("          for example,HotspotTests-Java");
                System.out.println("-f or -filePredefined to set predefined classes file");
                System.out.println("          for example,testcases.txt");
                System.out.println("-r or -randomSeed to set random seed");
                System.out.println("          for example,0 or 1 or 2...");
                System.out.println("-sl or -select to set select method");
                System.out.println("          for example,random or softmax or rws or mab");
                System.out.println("-cl or -cluster to set cluster method");
                System.out.println("          for example,no_cluster or infercode or codebert or codet5 or codegpt");
                System.out.println("-v or -vmoption to set open or close vm option");
                System.out.println("          for example,true or false");
                System.out.println("-ch or -checksum to set open or close checksum");
                System.out.println("          for example,true or false");
                System.out.println("-l or -loop to set open or close loop wrap");
                System.out.println("          for example,true or false");
                System.exit(0);
            }
            if (options.hasOption("t")) { // time stamp
                timeStamp = options.getOptionValue("t");
            } else {
                timeStamp = String.valueOf(new Date().getTime());
            }
            if (options.hasOption("s")) { // seed class project
                projectName = options.getOptionValue("s");
            }
            if (options.hasOption("p")) { // provide project
                mutationProviderProject = options.getOptionValue("p");
            }
            if (options.hasOption("f")) { // predefined classes file
                defineClassesPath = options.getOptionValue("f");
            }
            if (options.hasOption("r")) { // random seed
                randomSeed = Integer.parseInt(options.getOptionValue("r"));
            }
            if(options.hasOption("sl")){ // select method
                String s = options.getOptionValue("sl");
                if (s.toLowerCase(Locale.ROOT).equals("random")) {
                    selectMethod = SelectBlockHelper.RANDOM_SELECT;
                }
                if (s.toLowerCase(Locale.ROOT).equals("softmax")) {
                    selectMethod = SelectBlockHelper.SOFTMAX_SELECT;
                }
                if (s.toLowerCase(Locale.ROOT).equals("rws")) {
                    selectMethod = SelectBlockHelper.RWS_SELECT;
                }
                if (s.toLowerCase(Locale.ROOT).equals("mab")) {
                    selectMethod = SelectBlockHelper.MAB_SELECT;
                }
            }
            if(options.hasOption("cl")){ // cluster method
                String s = options.getOptionValue("cl");
                if(s.toLowerCase(Locale.ROOT).equals("no_cluster")){
                    useClustering = CodeClusterHelper.NO_CLUSTER;
                }
                if(s.toLowerCase(Locale.ROOT).equals("infercode")){
                    useClustering = CodeClusterHelper.INFER_CODE_CLUSTER;
                }
                if(s.toLowerCase(Locale.ROOT).equals("codebert")){
                    useClustering = CodeClusterHelper.CODE_BERT_CLUSTER;
                }
                if(s.toLowerCase(Locale.ROOT).equals("codet5")){
                    useClustering = CodeClusterHelper.CODE_T5_CLUSTER;
                }
                if(s.toLowerCase(Locale.ROOT).equals("codegpt")){
                    useClustering = CodeClusterHelper.CODE_GPT_CLUSTER;
                }
                if(s.toLowerCase(Locale.ROOT).equals("plbart")){
                    useClustering = CodeClusterHelper.PL_BART_CLUSTER;
                }
            }
            if(options.hasOption("v")){ // vm option
                if (options.getOptionValue("v").toLowerCase(Locale.ROOT).equals("true")) {
                    useVMOptions = true;
                }
                if (options.getOptionValue("v").toLowerCase(Locale.ROOT).equals("false")) {
                    useVMOptions = false;
                }
            }
            if(options.hasOption("ch")){ // checksum
                if (options.getOptionValue("ch").toLowerCase(Locale.ROOT).equals("true")) {
                    checksum = true;
                }
                if (options.getOptionValue("ch").toLowerCase(Locale.ROOT).equals("false")) {
                    checksum = false;
                }
            }
            if(options.hasOption("l")){ // loop wrap
                if (options.getOptionValue("l").toLowerCase(Locale.ROOT).equals("true")) {
                    addLoopFlag = true;
                }
                if (options.getOptionValue("l").toLowerCase(Locale.ROOT).equals("false")) {
                    addLoopFlag = false;
                }
            }
            if(options.hasOption("re")){
                if (options.getOptionValue("re").toLowerCase(Locale.ROOT).equals("true")) {
                    reshape = true;
                }
                if (options.getOptionValue("re").toLowerCase(Locale.ROOT).equals("false")) {
                    reshape = false;
                }
            }
            if(options.hasOption("et")) { // execute time
                exeTime = Integer.parseInt(options.getOptionValue("et"));
            }
        }
        if(randomSeed == -1){
            randomSeed = (int) System.currentTimeMillis();
        }


        SelectBlockHelper.reshape = reshape;
        DTGlobal.useVMOptions = useVMOptions;
        timeStamp = String.valueOf(new Date().getTime());
        //Generate the difference test log file, set the JVM and project output path, and load the JVM
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
        ProjectInfo originMutationProject = null;
        ProjectInfo mutationProject = null;

        String mutationHistoryPath = MainConfiguration.mutationHistoryPath +
                DTPlatform.FILE_SEPARATOR + timeStamp +
                DTPlatform.FILE_SEPARATOR + "classhistory";

        String diffClassPath = MainConfiguration.mutationHistoryPath +
                DTPlatform.FILE_SEPARATOR + timeStamp +
                DTPlatform.FILE_SEPARATOR + "diffClass";

        /*
         *用来确定是谁（的ingredient）插入谁（的seedClass）的结果
         */
        String projectFlag = MainConfiguration.mutationHistoryPath +
                DTPlatform.FILE_SEPARATOR + timeStamp +
                DTPlatform.FILE_SEPARATOR + mutationProviderProject +"--"+projectName;
        MainHelper.createFolderIfNotExist(projectFlag);
        /*
         *To here
         */
        MainHelper.createFolderIfNotExist(mutationHistoryPath);
        MainHelper.createFolderIfNotExist(diffClassPath);

        // Load the original project and Mutant output project, initialize the SOOT environment, and perform basic block analysis
        if ( projectName == null || projectName.equals(mutationProviderProject) ){
            // Load the original project and Mutant output project
            crossProject = false;
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

            if(providerProjectAllDefineFlag){
                originProject.setApplicationClasses((ArrayList<String>) core.ClazzUtils.run("02Benchmarks"+DTPlatform.FILE_SEPARATOR+projectName,packagePath,packageName));
                seedClasses = originProject.getApplicationClasses();
                MainHelper.restoreBadClasses(seedClasses, originProject, targetProject);
                targetProject.setApplicationClasses(new ArrayList<>(seedClasses));
            }

            // Since there are many private methods that cannot be accessed, we need to change them to the public flag
            List<String> mutationClasses = MainHelper.duplicateSeedsAndChangeModifiers(seedClasses);
            // initialize the SOOT environment
            G.reset();

            Configuration.initSootEnvWithClassPath(targetProject.getpClassPath());
            Configuration.set_output_path(targetProject.getSrcClassPath());
            BlocksContainer.initMutantsFromClasses(mutationClasses);
        } else {

            crossProject = true;
            originProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("02Benchmarks", projectName, null, projectPreDefineFlag);
            targetProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("sootOutput", projectName, null, projectPreDefineFlag);

            originMutationProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("02Benchmarks", mutationProviderProject, null, providerProjectPreDefineFlag);
            mutationProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("sootOutput", mutationProviderProject, null, providerProjectPreDefineFlag);
            if(providerProjectAllDefineFlag){
                originMutationProject.setApplicationClasses((ArrayList<String>) ClazzUtils.run("02Benchmarks"+DTPlatform.FILE_SEPARATOR+mutationProviderProject,packagePath,packageName));
            }

            System.out.println(originProject);
            System.out.println(targetProject);
            System.out.println(originMutationProject);
            System.out.println(mutationProject);

            Configuration.initSootEnvWithClassPath(mutationProject.getpClassPath());
            Configuration.set_output_path(mutationProject.getSrcClassPath());

            List<String> originMutateClasses = originMutationProject.getApplicationClasses();
            MainHelper.restoreBadClasses(originMutateClasses, originMutationProject, mutationProject);
            mutationProject.setApplicationClasses(new ArrayList<>(originMutateClasses));

            List<String> mutationClasses = MainHelper.duplicateSeedsAndChangeModifiers(originMutateClasses);

            G.reset();
            Scene.v().addBasicClass("java.lang.StringBuilder");
            Scene.v().addBasicClass("java.io.PrintStream");
            Scene.v().addBasicClass("java.lang.System");
            Scene.v().addBasicClass("java.lang.String");
            Scene.v().addBasicClass("com.sun.crypto.provider.Cipher.AEAD.ReadWriteSkip$SkipTest");
            Configuration.initSootEnvWithClassPath(targetProject.getpClassPath() + System.getProperty("path.separator") + mutationProject.getpClassPath());
            Configuration.set_output_path(targetProject.getSrcClassPath());

            List<String> seedClasses = originProject.getApplicationClasses();
            MainHelper.restoreBadClasses(seedClasses, originProject, targetProject);
            targetProject.setApplicationClasses(new ArrayList<>(seedClasses));

            List<String> seedJunits = originProject.getJunitClasses();
            MainHelper.restoreBadClasses(seedJunits, originProject, targetProject);
            targetProject.setJunitClasses(new ArrayList<>(seedJunits));

            seeds = MainHelper.initialSeedsWithType(seedClasses, targetProject.getSrcClassPath(), false, mutationHistoryPath);
            seeds.addAll(MainHelper.initialSeedsWithType(seedJunits, targetProject.getTestClassPath(), true, mutationHistoryPath));

            BlocksContainer.initMutantsFromClasses(mutationClasses);
        }
        if(projectName.contains("jython-dacapo")){
            ArrayList<String> s = new ArrayList<>();
            for (String projoption : originProject.getProjoptions()) {
                s.add(projoption.replace("jython-dacapo",projectName));
            }
            originProject.setProjoptions(s);
            targetProject.setProjoptions(s);
        }
        if(projectName.contains("jython_new")){
            ArrayList<String> s = new ArrayList<>();
            for (String projoption : originProject.getProjoptions()) {
                s.add(projoption.replace("jython_new",projectName));
            }
            originProject.setProjoptions(s);
            targetProject.setProjoptions(s);
        }


//        test.tempGetSame.run();
//        ChecksumHelper.createChecksumFile(originProject.getSrcClassPath());
        ChecksumHelper.createChecksumFile(targetProject.getSrcClassPath());
//        if(originMutationProject != null){
//            ChecksumHelper.createChecksumFile(originMutationProject.getSrcClassPath());
//        }
//        if(mutationProject != null){
//            ChecksumHelper.createChecksumFile(mutationProject.getSrcClassPath());
//        }
        ChecksumHelper.setSkipClass("."+DTPlatform.FILE_SEPARATOR+"02Benchmarks"+DTPlatform.FILE_SEPARATOR+projectName+DTPlatform.FILE_SEPARATOR+"skipclass.txt");

        /*
         *判断是否进行聚类，以及通过useClustering字符串确定聚类方法
         */
//        if(selectMethod != SelectBlockHelper.RANDOM_SELECT){
//            CodeClusterHelper.clusteringFromBlock(useClustering);
//        }
        CodeClusterHelper.clusteringFromBlock(useClustering);
        /*
         *判断是否使用hotspot计算覆盖率
         */
        if(covTest){
            coverageTest(covJavaCmd,jvmCmds,targetProject,seeds);
        }

        /*
         * 为种子文件插入一个loop，以此加大触发jit的概率
         */
        List<ClassInfo> tempSeeds = new ArrayList<>();
        String classFileFolder = "";
        Random random = new Random(randomSeed);

        // 新建历史文件对应文件夹
        for(ClassInfo seed :seeds){
            classFileFolder = mutationHistoryPath + DTPlatform.FILE_SEPARATOR + seed.getOriginClassName();
            MainHelper.createFolderIfNotExist(classFileFolder);
        }


        if(checksum){
            tempSeeds.clear();
            for(ClassInfo seed :seeds){
                classFileFolder = mutationHistoryPath + DTPlatform.FILE_SEPARATOR + seed.getOriginClassName();
                MainHelper.createFolderIfNotExist(classFileFolder);
                SootClass seedClass = JMUtils.loadTargetClass(seed.getOriginClassName());
                try {
                    ChecksumHelper.checksumForClass(seedClass);
                }catch (Exception e){
                    e.printStackTrace();
                    tempSeeds.add(seed);
                    Scene.v().removeClass(seedClass);
                    continue;
                }

                String newName = seed.generateMutateClassFilename();
                ClassInfo newMutateClass = new ClassInfo(seed.getOriginClassName(),
                        seed.getOriginClassPath(),
                        newName,
                        classFileFolder + DTPlatform.FILE_SEPARATOR + newName,
                        seed.isJunit(),
                        seed.getMutationOrder() + 1,
                        0,
                        seed.isLoop());
                newMutateClass.saveSootClassToFile(seedClass);
                if(JMUtils.saveSootClassToLocal(seedClass, Options.output_format_class)){
                    tempSeeds.add(newMutateClass);
                }else {
                    tempSeeds.add(seed);
                }

                Scene.v().removeClass(seedClass);

            }
            seeds.clear();
            seeds.addAll(tempSeeds);
        }
        //为 种子文件 添加循环
        if(addLoopFlag){
            for(ClassInfo seed :seeds) {
                classFileFolder = mutationHistoryPath + DTPlatform.FILE_SEPARATOR + seed.getOriginClassName();
                MainHelper.createFolderIfNotExist(classFileFolder);
                if (seed.isJunit()){
                    Configuration.set_output_path(targetProject.getTestClassPath());
                } else {
                    Configuration.set_output_path(targetProject.getSrcClassPath());
                }
                SootClass seedClass = JMUtils.loadTargetClass(seed.getOriginClassName());
                LoopWrapHelper.loopWrapForClass(seedClass);
                String newName = seed.generateMutateClassFilename();
                ClassInfo newMutateClass = new ClassInfo(seed.getOriginClassName(),
                        seed.getOriginClassPath(),
                        newName,
                        classFileFolder + DTPlatform.FILE_SEPARATOR + newName,
                        seed.isJunit(),
                        seed.getMutationOrder() + 1,
                        0,
                        seed.isLoop());
                newMutateClass.saveSootClassToFile(seedClass);
                if(JMUtils.saveSootClassToLocal(seedClass, Options.output_format_class)){

                    tempSeeds.add(newMutateClass);
                }else {
                    tempSeeds.add(seed);
                }
                Scene.v().removeClass(seedClass);
            }
            seeds.clear();
            seeds.addAll(tempSeeds);
        }
        int sumDiffFindCount = 0;
        Unit targetUnit = null;
        int randomIndex = -1;
        long originTime = System.currentTimeMillis();
        long startTime = System.currentTimeMillis();
        for (ClassInfo seed : seeds) {
            classFileFolder = mutationHistoryPath + DTPlatform.FILE_SEPARATOR + seed.getOriginClassName();
            MainHelper.createFolderIfNotExist(classFileFolder);
            // Because test classes and normal classes do not behave quite the same, they are treated differently
            if (seed.isJunit()){
                Configuration.set_output_path(targetProject.getTestClassPath());
            } else {
                Configuration.set_output_path(targetProject.getSrcClassPath());
            }


            System.out.println("current: " + seed.getClassName());


            // Soot class was loaded and converted into Soot method
            SootClass seedClass;
            if (!seed.isOriginClass() || seed.hasCovered()) {
                seed.storeToCoverOriginClass();
            }
            seedClass = JMUtils.loadTargetClass(seed.getOriginClassName());

            //Save the SootClass local
            if (JMUtils.saveSootClassToLocal(seedClass, Options.output_format_class)) {
                seed.mutationTimesIncrease();
                String newName = seed.generateMutateClassFilename();
                ClassInfo newMutateClass = new ClassInfo(seed.getOriginClassName(),
                        seed.getOriginClassPath(),
                        newName,
                        classFileFolder + DTPlatform.FILE_SEPARATOR + newName,
                        seed.isJunit(),
                        seed.getMutationOrder() + 1,
                        0,
                        seed.isLoop());
                newMutateClass.saveSootClassToFile(seedClass);
                if (crossProject){
                    CFMExecutor.getInstance().dtSingleClassInProj(jvmCmds,
                            targetProject,
                            mutationProject,
                            newMutateClass.getOriginClassName(),
                            newMutateClass.getClassName());
                } else {
                    CFMExecutor.getInstance().dtSingleClassInProj(jvmCmds,
                            targetProject,
                            newMutateClass.getOriginClassName(),
                            newMutateClass.getClassName());
                }
                if (CFMExecutor.getInstance().isDiffFound()){
                    String diffClassFolder = diffClassPath + DTPlatform.FILE_SEPARATOR + seed.getOriginClassName();
                    MainHelper.createFolderIfNotExist(diffClassFolder);
                    newMutateClass.saveSootClassToTargetPath(seedClass, diffClassFolder + DTPlatform.FILE_SEPARATOR + newMutateClass.getClassName());
                }
            }
            Scene.v().removeClass(seedClass);

        }
    }
    @Test
    public void test() throws IOException {
        File file = new File("D:\\我的文件\\研究生\\JavaTailor\\CFSynthesisV2\\03results\\1663055633956\\Openj9Test-Test\\difference.log");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String line = bufferedReader.readLine();
        while (line!=null){
            if(line.toLowerCase(Locale.ROOT).contains("difference")){
                System.out.println(line.substring(48,line.length()-25));
            }
            line = bufferedReader.readLine();
        }
    }
    @Test
    public void test2() throws IOException {
        String rootPath = "D:\\我的文件\\研究生\\JavaTailor\\CFSynthesisV2\\02Benchmarks\\Openj9Test-Test";
        File skipFile = new File(rootPath+"\\skipclass.txt");
        File preDefineFile = new File(rootPath+"\\testcases.txt");
        List<String> list = new ArrayList<>();
        BufferedReader skipReader = new BufferedReader(new FileReader(skipFile));
        BufferedReader preDefinReader = new BufferedReader(new FileReader(preDefineFile));
        String line = preDefinReader.readLine();
        while (line != null){
            list.add(line);
            line = preDefinReader.readLine();
        }
        line = skipReader.readLine();
        while (line != null){
            list.remove(line);
            line = skipReader.readLine();
        }
        for (String s : list) {
            System.out.println(s);
        }
    }
}