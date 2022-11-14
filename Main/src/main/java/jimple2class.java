import JimpleMixer.blocks.BlockInfo;
import JimpleMixer.blocks.BlocksContainer;
import core.*;
import JimpleMixer.core.Configuration;
import JimpleMixer.core.JMUtils;
import JimpleMixer.core.MutationHelper;
import core.ClassInfo;
import core.MainConfiguration;
import core.MainHelper;
import dtjvms.*;
import dtjvms.loader.DTLoader;
import soot.*;
import soot.options.Options;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class jimple2class {
    public static String projectName = "templateClass";  // 提供种子文件的项目
    public static boolean projectPreDefineFlag = false;  // 此项目是否使用预定义类
    public static String mutationProviderProject = "HotspotTests-Java";  // 提供合成块文件的项目
    public static boolean providerProjectPreDefineFlag = true;  // 此项目是否使用预定义类
    public static boolean providerProjectAllDefineFlag = false;  // 此项目是否提取所有的可用类（当项目的application类过少时使用）
    public static String packagePath = "";  // packagePath + packageName 指向项目的可用类
    public static String packageName = "Bug_triggering_input";
    public static String defineClassesPath = "testcases.txt";
    public static int randomSeed = 1;
    public static String timeStamp;
    public static List<ClassInfo> seeds;
    public static boolean crossProject = false;

    public static void main(String[] args) throws IOException, InterruptedException {
        //Generate the difference test log file, set the JVM and project output path, and load the JVM
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

        crossProject = true;
        originMutationProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("02Benchmarks", mutationProviderProject, null, providerProjectPreDefineFlag);
        mutationProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("sootOutput", mutationProviderProject, null, providerProjectPreDefineFlag);
        if(providerProjectAllDefineFlag){
            originMutationProject.setApplicationClasses((ArrayList<String>) ClazzUtils.run("02Benchmarks"+DTPlatform.FILE_SEPARATOR+mutationProviderProject,packagePath,packageName));
        }
        System.out.println(originMutationProject);
        System.out.println(mutationProject);

        ProjectInfo originProject = null;
        ProjectInfo targetProject = null;
        originProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("02Benchmarks", projectName, null, projectPreDefineFlag);
        targetProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("sootOutput", projectName, null, projectPreDefineFlag);
        System.out.println(originProject);
        System.out.println(targetProject);
        Configuration.initSootEnvWithClassPath(mutationProject.getpClassPath());
        Configuration.set_output_path(mutationProject.getSrcClassPath());

        List<String> originMutateClasses = originMutationProject.getApplicationClasses();
        MainHelper.restoreBadClasses(originMutateClasses, originMutationProject, mutationProject);
        mutationProject.setApplicationClasses(new ArrayList<>(originMutateClasses));

        List<String> originClasses = originProject.getApplicationClasses();
        MainHelper.restoreBadClasses(originClasses, originProject, targetProject);
        targetProject.setApplicationClasses(new ArrayList<>(originClasses));

        List<String> mutationClasses = MainHelper.duplicateSeedsAndChangeModifiers(originMutateClasses);

        G.reset();

        Configuration.initSootEnvWithClassPath(targetProject.getpClassPath() + System.getProperty("path.separator") + mutationProject.getpClassPath());
        Configuration.set_output_path(targetProject.getSrcClassPath());
        BlocksContainer.initMutantsFromClasses(mutationClasses);


        List<String> seedClasses = targetProject.getApplicationClasses();
        List<String> seedJunits = targetProject.getJunitClasses();
        seeds = MainHelper.initialSeedsWithType(seedClasses, targetProject.getSrcClassPath(), false, mutationHistoryPath);
        seeds.addAll(MainHelper.initialSeedsWithType(seedJunits, targetProject.getTestClassPath(), true, mutationHistoryPath));
        Random random = new Random(randomSeed);
        ClassInfo seed = seeds.get(0);
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
                SootClass seedClass = JMUtils.loadTargetClass(seed.getOriginClassName());
                assert seedClass != null;
                SootMethod seedMethod = seedClass.getMethods().get(1);
                Body methodBody = seedMethod.retrieveActiveBody();
                List<Unit> validUnits = MainHelper.getValidStmtsFromMethodBody(methodBody);
                Unit targetUnit = validUnits.get(0);
                String mutantClass = ingredient.getClassName();
                MutationHelper.insertBlockToMethod(seedClass, methodBody, targetUnit, BlocksContainer.getAllMutationMap().get(mutantClass), ingredient);
                for (SootMethod method : seedClass.getMethods()) {
                    method.retrieveActiveBody();
                }
                String mutateClassFilename = seed.generateMutateClassFilename();
                ClassInfo newMutateClass = new ClassInfo(seed.getOriginClassName(),
                        seed.getOriginClassPath(),
                        seed.generateMutateClassFilename(),
                        classFileFolder + DTPlatform.FILE_SEPARATOR + mutateClassFilename,
                        seed.isJunit(),
                        seed.getMutationOrder() + 1,
                        0,
                        seed.isLoop());


                if(!newMutateClass.saveSootClassToFile(seedClass)){
                    System.out.println("保存失败，信息如下:");
                    System.out.println(ingredient.getClass().getName());
                    for (Unit allStmt : ingredient.getAllStmts()) {
                        System.out.println(allStmt);
                    }
                }
                nameList.add(new StringBuilder(mutateClassFilename));
//                String cmd = "";
//                System.out.println();
//                if(System.getProperty("os.name").toUpperCase().contains("WIN")){
//                    cmd = "cmd /c "+"jad.exe -p "+classFileFolder + DTPlatform.FILE_SEPARATOR + mutateClassFilename+" > "+classFileFolder + DTPlatform.FILE_SEPARATOR + mutateClassFilename.replace(".class",".java");
//                }
//                if(System.getProperty("os.name").toUpperCase().contains("LINUX")){
//                    cmd = "java -jar cfr-0.144.jar "+classFileFolder + DTPlatform.FILE_SEPARATOR + mutateClassFilename+" > "+classFileFolder + DTPlatform.FILE_SEPARATOR + mutateClassFilename.replace(".class",".java");
//                }
//                System.out.println(cmd);
//                Process p = Runtime.getRuntime().exec(cmd);
//                p.waitFor();
                Scene.v().removeClass(seedClass);
            }
        }
        CSVUtils.exportCsv(new File("./className.csv"),nameList);
    }
}