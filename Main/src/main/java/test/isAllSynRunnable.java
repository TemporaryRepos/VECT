package test;

import JimpleMixer.blocks.BlockInfo;
import JimpleMixer.blocks.BlocksContainer;
import JimpleMixer.core.Configuration;
import JimpleMixer.core.JMUtils;
import JimpleMixer.core.MutationHelper;
import core.ClassInfo;
import core.MainConfiguration;
import core.MainHelper;
import core.SelectBlockHelper;
import dtjvms.*;
import dtjvms.analyzer.DiffCore;
import dtjvms.analyzer.JDKAnalyzer;
import dtjvms.executor.CFM.CFMExecutor;
import dtjvms.executor.CFM.JvmOutput;
import dtjvms.executor.ExecutorHelper;
import dtjvms.loader.DTLoader;
import soot.*;
import soot.options.Options;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class isAllSynRunnable {

    public static String projectName = "HotspotTests-Java";  // 提供种子文件的项目
    public static boolean projectPreDefineFlag = true;  // 此项目是否使用预定义类
    public static String mutationProviderProject = "HotspotTests-Java";  // 提供合成块文件的项目
    public static boolean providerProjectPreDefineFlag = true;  // 此项目是否使用预定义类

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
        while (seeds.size() > 0){
            BlockInfo ingredient = null;
            ClassInfo seed = seeds.get(random.nextInt(seeds.size()));
            System.out.println(seed.getOriginClassName());
            // Because test classes and normal classes do not behave quite the same, they are treated differently
            if (seed.isJunit()){
                Configuration.set_output_path(targetProject.getTestClassPath());
            } else {
                Configuration.set_output_path(targetProject.getSrcClassPath());
            }
            System.out.println("current: " + seed.getClassName());
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
            Map<String, Body> candidateMethods = new HashMap<>();
            List<SootMethod> seedMethods = seedClass.getMethods();
            // Gets the function body of each method
            for (SootMethod seedMethod : seedMethods) {
                if (!seedMethod.isAbstract()) {
                    try {
                        Body methodBody = seedMethod.retrieveActiveBody();
                        if (!seedMethod.isConstructor()) {
                            candidateMethods.put(seedMethod.getName(), methodBody);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            List<String> tmp = new ArrayList<>(candidateMethods.keySet());
            if (tmp.size() <= 0){
                seeds.remove(seed);
                Scene.v().removeClass(seedClass);
                continue;
            }
            Body methodBody = candidateMethods.get(tmp.get(random.nextInt(tmp.size())));
            List<Unit> originUnit = new ArrayList<>();
            List<Unit> mutantUnit = new ArrayList<>();
            if (methodBody != null){
                List<Unit> validUnits = MainHelper.getValidStmtsFromMethodBody(methodBody);
                targetUnit = MainHelper.getTargetUnitRandom(random, validUnits);
                ingredient = MainHelper.getIngredient(SelectBlockHelper.RANDOM_SELECT,random);
                int tryTimes = 0;
                while (ingredient == null && tryTimes < 10){
                    ingredient = MainHelper.getIngredient(SelectBlockHelper.RANDOM_SELECT,random);
                    tryTimes++;
                }
                if (ingredient != null){
                    String mutantClassName = ingredient.getClassName();
                    originUnit.addAll(methodBody.getUnits());
                    MutationHelper.insertBlockToMethod(seedClass, methodBody, targetUnit, BlocksContainer.getAllMutationMap().get(mutantClassName), ingredient);
                    mutantUnit.addAll(methodBody.getUnits());
                }
            }

            for (Unit unit : mutantUnit) {
                if (!originUnit.contains(unit)){

                }
            }
            if (JMUtils.saveSootClassToLocal(seedClass, Options.output_format_class)) {
                seed.mutationTimesIncrease();
                ClassInfo newMutateClass = new ClassInfo(seed.getOriginClassName(),
                        seed.getOriginClassPath(),
                        seed.generateMutateClassFilename(),
                        classFileFolder + DTPlatform.FILE_SEPARATOR + seed.generateMutateClassFilename(),
                        seed.isJunit(),
                        seed.getMutationOrder() + 1,
                        0,
                        seed.isLoop());
                newMutateClass.saveSootClassToFile(seedClass);
                DTGlobal.getInsertLogger().info("插入如下ingredients，生成的"+newMutateClass.getClassName());
                for(String s:DTGlobal.getInsertInfo()){
                    DTGlobal.getInsertLogger().info(s);
                }
                DTGlobal.getInsertInfo().clear();
                CFMExecutor.getInstance().dtSingleClassInProj(jvmCmds,
                        targetProject,
                        newMutateClass.getOriginClassName(),
                        newMutateClass.getClassName());
                HashMap<String, JvmOutput> results = CFMExecutor.getInstance().getResults();
                DiffCore diff = JDKAnalyzer.getInstance().analysis(newMutateClass.getClassName(), results);
                if(diff == null){
                    diff = new DiffCore(0,false,"None Difference");
                }
                ExecutorHelper.logJvmOutput(DTGlobal.getDiffLogger(), targetProject.getProjectName(), newMutateClass.getClassName(), diff ,results);


                File file = new File("03results"+DTPlatform.FILE_SEPARATOR+timeStamp+DTPlatform.FILE_SEPARATOR+projectName+DTPlatform.FILE_SEPARATOR+"SynResult.txt");
                FileWriter fileWriter = new FileWriter(file,true);
                for (String s : results.keySet()) {
                    JvmOutput jvmOutput = results.get(s);
                    fileWriter.write(jvmOutput.getFEEInfo()+"");
                }
                fileWriter.write("---"+newMutateClass.getClassName());
                fileWriter.write("\n");
                fileWriter.flush();
                fileWriter.close();

                String diffClassFolder = diffClassPath + DTPlatform.FILE_SEPARATOR + seed.getOriginClassName();
                MainHelper.createFolderIfNotExist(diffClassFolder);
                newMutateClass.saveSootClassToTargetPath(seedClass, diffClassFolder + DTPlatform.FILE_SEPARATOR + newMutateClass.getClassName());

                Scene.v().removeClass(seedClass);
            }
        }
    }
}
