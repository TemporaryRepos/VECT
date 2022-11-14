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
import dtjvms.executor.CFM.CFMExecutor;
import dtjvms.executor.ExecutorHelper;
import dtjvms.loader.DTLoader;
import fj.data.IO;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.junit.Test;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.options.Options;
import soot.util.Chain;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class test {

    public static String projectName = "temp";  // 提供种子文件的项目
    public static boolean projectPreDefineFlag = false;  // 此项目是否使用预定义类

    public static String timeStamp;
    public static List<ClassInfo> seeds;

    @Test
    public void test(){

        String root = "Z:\\JVM Archive\\可执行文件";
        for (File developer : Objects.requireNonNull(new File(root).listFiles())) {
            for (File version : Objects.requireNonNull(developer.listFiles())) {
                for (File rootDir : Objects.requireNonNull(version.listFiles())) {
                    if(rootDir.getName().contains("openj9")){
                        if(rootDir.getName().contains("8u")){
                            rootDir.renameTo(new File(version.getAbsolutePath()+"\\"+rootDir.getName().substring(rootDir.getName().length()-6)));
                        }else {
                            rootDir.renameTo(new File(version.getAbsolutePath()+"\\"+rootDir.getName().substring(rootDir.getName().length()-6)));
                        }
                    }else {
                        if(rootDir.getName().contains("8u")){
                            rootDir.renameTo(new File(version.getAbsolutePath()+"\\"+rootDir.getName().substring(4,rootDir.getName().length()-10)));
                        }else {
                            rootDir.renameTo(new File(version.getAbsolutePath()+"\\"+rootDir.getName().substring(4,rootDir.getName().length()-14)));
                        }
                    }

                }
            }
        }


//        File omr = new File("Z:\\JVM Archive\\压缩包\\OpenJ9\\release\\omr");
//        List<File> omrList = new ArrayList<>(Arrays.asList(omr.listFiles()));
//        File release = new File("Z:\\JVM Archive\\压缩包\\OpenJ9\\release\\openj9");
//        List<File> releaseList = new ArrayList<>(Arrays.asList(release.listFiles()));
//        for (File file : omrList) {
//            file.renameTo(new File("Z:\\JVM Archive\\压缩包\\OpenJ9\\release\\omr"+"\\"+file.getName().substring(1, file.getName().length() - 7) + ".tar.gz"));
//        }
//        for (File file : releaseList) {
//            file.renameTo(new File("Z:\\JVM Archive\\压缩包\\OpenJ9\\release\\openj9"+"\\"+file.getName().substring(14, file.getName().length() - 7) + ".tar.gz"));
//        }
//        List<String> omrUnique = new ArrayList<>();
//        List<String> releaseUnique = new ArrayList<>();
//        for (File file : omrList) {
//            omrUnique.add(file.getName().substring(18,file.getName().length() - 7));
//        }
//        for (File file : releaseList) {
//            String version = file.getName().substring(14,file.getName().length()-7);
//            if(omrUnique.contains(version)){
//                omrUnique.remove(version);
//            }else {
//                releaseUnique.add(version);
//            }
//        }
//        System.out.println("+++++++++++++++++++++++omr+++++++++++++++++++++++++++++++++");
//        for (String s : omrUnique) {
//            System.out.println(s);
//        }
//        System.out.println("+++++++++++++++++++++++release+++++++++++++++++++++++++++++++++");
//        for (String s : releaseUnique) {
//            System.out.println(s);
//        }
    }
    @Test
    public void rmSkipClass() throws IOException {
        File testcases = new File("../02Benchmarks/HotspotIssue/testcases.txt");
        BufferedReader testcaseReader = new BufferedReader(new FileReader(testcases));
        List<String> testcaseList = new ArrayList<>();
        String line = testcaseReader.readLine();
        while (line != null){
            testcaseList.add(line);
            line = testcaseReader.readLine();
        }
        File skipClass = new File("../02Benchmarks/HotspotIssue/skipclass.txt");
        BufferedReader skipReader = new BufferedReader(new FileReader(skipClass));
        List<String> skipList = new ArrayList<>();
        line = skipReader.readLine();
        while (line != null){
            skipList.add(line);
            line = skipReader.readLine();
        }
        testcaseList.removeAll(skipList);
        for (String s : testcaseList) {
            System.out.println(s);
        }

    }

    public static void main(String[] tempArgs) throws IOException {

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
        System.out.println(originProject.getSrcClassPath());
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
        String classFileFolder = null;

        BlocksContainer.initMutantsFromClasses(mutationClasses);

        for (ClassInfo seed : seeds) {
            SootClass seedClass = JMUtils.loadTargetClass(seed.getOriginClassName());
            for (SootMethod method : seedClass.getMethods()) {
                System.out.println(method.getName());
                for (Unit unit : method.retrieveActiveBody().getUnits()) {
                    System.out.println(unit);
                }
            }
        }


        System.exit(0);


        List<ClassInfo> tempSeeds = new ArrayList<>();
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


        List<ClassInfo> rmList = new ArrayList<>();
        for (ClassInfo seed : seeds) {
            String newName = seed.generateMutateClassFilename();
            ClassInfo newMutateClass = new ClassInfo(seed.getOriginClassName(),
                    seed.getOriginClassPath(),
                    newName,
                    classFileFolder + DTPlatform.FILE_SEPARATOR + newName,
                    seed.isJunit(),
                    seed.getMutationOrder() + 1,
                    0,
                    seed.isLoop());
            CFMExecutor.getInstance().dtSingleClassInProj(jvmCmds,
                    targetProject,
                    newMutateClass.getOriginClassName(),
                    newMutateClass.getClassName());
            if(CFMExecutor.getInstance().isDiffFound()){
                rmList.add(seed);
            }
        }
        System.out.println("++++++++++++++++++++++++");
        for (ClassInfo classInfo : rmList) {
            System.out.println(classInfo.getOriginClassName());
        }
        System.out.println("++++++++++++++++++++++++");
        seeds.removeAll(rmList);
        for (ClassInfo seed : seeds) {
            classFileFolder = mutationHistoryPath + DTPlatform.FILE_SEPARATOR + seed.getOriginClassName();
            MainHelper.createFolderIfNotExist(classFileFolder);
            SootClass sootClass = JMUtils.loadTargetClass(seed.getOriginClassName());
            if (sootClass == null){
                continue;
            }

            boolean skip = false;
            for (SootMethod method : sootClass.getMethods()) {
                if(skip){
                    break;
                }
                for (Unit unit : method.retrieveActiveBody().getUnits()) {
                    if(skip){
                        break;
                    }
                    if(unit.toString().toLowerCase(Locale.ROOT).contains("thread")){
                        System.out.println(seed.getOriginClassName());
                        skip = true;
                        break;
                    }
                    if(unit.toString().toLowerCase(Locale.ROOT).contains("bean")){
                        System.out.println(seed.getOriginClassName());
                        skip = true;
                        break;
                    }
                    if(unit.toString().toLowerCase(Locale.ROOT).contains("time") && !unit.toString().toLowerCase(Locale.ROOT).contains("runtime")){
                        System.out.println(seed.getOriginClassName());
                        skip = true;
                    }
                    if(unit.toString().toLowerCase(Locale.ROOT).contains("random")){
                        System.out.println(seed.getOriginClassName());
                        skip = true;
                        break;
                    }
                    if(unit.toString().toLowerCase(Locale.ROOT).contains("hashcode")){
                        System.out.println(seed.getOriginClassName());
                        skip = true;
                        break;
                    }
                    if(unit.toString().toLowerCase(Locale.ROOT).contains("port")){
                        System.out.println(seed.getOriginClassName());
                        skip = true;
                        break;
                    }

                }
            }
        }
    }

}
