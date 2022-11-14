import JimpleMixer.blocks.BlockInfo;
import JimpleMixer.blocks.BlocksContainer;
import JimpleMixer.core.Configuration;
import JimpleMixer.core.JMUtils;
import JimpleMixer.core.MutationHelper;
import core.ClassInfo;
import core.MainConfiguration;
import core.MainHelper;
import dtjvms.DTConfiguration;
import dtjvms.DTGlobal;
import dtjvms.DTPlatform;
import dtjvms.ProjectInfo;
import dtjvms.loader.DTLoader;
import soot.*;
import soot.toolkits.graph.DirectedGraph;
import soot.util.cfgcmd.CFGGraphType;

import java.io.IOException;
import java.util.*;

public class jimple2CDF {
    public static String projectName = "templateClass";  // 提供种子文件的项目
    public static boolean projectPreDefineFlag = true;  // 此项目是否使用预定义类
    public static String mutationProviderProject = "HotspotTests-Java";  // 提供合成块文件的项目
    public static boolean providerProjectPreDefineFlag = true;  // 此项目是否使用预定义类
    public static boolean providerProjectAllDefineFlag = false;  // 此项目是否提取所有的可用类（当项目的application类过少时使用）
    public static String packagePath = "target/classes";  // packagePath + packageName 指向项目的可用类
    public static String packageName = "org";
    public static String defineClassesPath = "testcases.txt";
    public static int randomSeed = 1;
    public static String timeStamp;
    public static List<ClassInfo> seeds;
    public static boolean crossProject = false;

    public static void main(String[] args) throws IOException, InterruptedException{
        List<Map<String,Set<String>>> transferMatrixList = new ArrayList<>();
        List<Integer> skipList = new ArrayList<>();
        timeStamp = String.valueOf(new Date().getTime());
        DTGlobal.setDiffLogger(timeStamp + DTPlatform.FILE_SEPARATOR + projectName, "difference");
        DTGlobal.setSelectLogger(timeStamp + DTPlatform.FILE_SEPARATOR + projectName, "selectInfo");
        DTGlobal.setInsertLogger(timeStamp + DTPlatform.FILE_SEPARATOR + projectName, "insertInfo");
        DTConfiguration.debug = false;
        DTConfiguration.setJvmDepensRoot("." + DTPlatform.FILE_SEPARATOR + "01JVMS");
        DTConfiguration.setProjectDepensRoot("." + DTPlatform.FILE_SEPARATOR + "sootOutput");


        String mutationHistoryPath = MainConfiguration.mutationHistoryPath +
                DTPlatform.FILE_SEPARATOR + timeStamp +
                DTPlatform.FILE_SEPARATOR + "classhistory";
        MainHelper.createFolderIfNotExist(mutationHistoryPath);

        crossProject = true;

        ProjectInfo originMutationProject = null;
        ProjectInfo mutationProject = null;
        originMutationProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("02Benchmarks", mutationProviderProject, null, providerProjectPreDefineFlag);
        mutationProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("sootOutput", mutationProviderProject, null, providerProjectPreDefineFlag);
        System.out.println(originMutationProject);
        System.out.println(mutationProject);

        ProjectInfo originProject = null;
        originProject = DTLoader.getInstance().loadTargetProjectWithGivenPath("02Benchmarks", projectName, null, projectPreDefineFlag);
        System.out.println(originProject);

//        BlocksContainer.initMutantsFromClasses(tempList);
        Configuration.initSootEnvWithClassPath(mutationProject.getpClassPath());
        Configuration.set_output_path(mutationProject.getSrcClassPath());

        List<String> originMutateClasses = originMutationProject.getApplicationClasses();
        MainHelper.restoreBadClasses(originMutateClasses, originMutationProject, mutationProject);
        mutationProject.setApplicationClasses(new ArrayList<>(originMutateClasses));

        List<String> mutationClasses = MainHelper.duplicateSeedsAndChangeModifiers(originMutateClasses);

        G.reset();

        Configuration.initSootEnvWithClassPath(originProject.getpClassPath() + System.getProperty("path.separator") + mutationProject.getpClassPath());
        Configuration.set_output_path(originProject.getSrcClassPath());
        BlocksContainer.initMutantsFromClasses(mutationClasses);
        List<String> seedClasses = originProject.getApplicationClasses();
        List<String> seedJunits = originProject.getJunitClasses();
        seeds = MainHelper.initialSeedsWithType(seedClasses, originProject.getSrcClassPath(), false, mutationHistoryPath);
        seeds.addAll(MainHelper.initialSeedsWithType(seedJunits, originProject.getTestClassPath(), true, mutationHistoryPath));
        Map<String, List<BlockInfo>> ingredients = BlocksContainer.getValidMutationMap();
        Set<String> classCandidate = ingredients.keySet();
        for(String candidate:classCandidate) {
            List<BlockInfo> methodCandidate = ingredients.get(candidate);
            for (BlockInfo ingredient : methodCandidate) {
//                System.out.println(stmtList);
                SootClass seedClass = JMUtils.loadTargetClass(seeds.get(0).getOriginClassName());
                assert seedClass != null;
                SootMethod seedMethod = seedClass.getMethods().get(1);
                Body methodBody = seedMethod.retrieveActiveBody();
                List<Unit> validUnits = MainHelper.getValidStmtsFromMethodBody(methodBody);
                Unit targetUnit = validUnits.get(0);
                String mutantClass = ingredient.getClassName();
                // 为什么实在这个函数里面增加
                MutationHelper.insertBlockToMethod(seedClass, methodBody, targetUnit, BlocksContainer.getAllMutationMap().get(mutantClass), ingredient);
                CFGGraphType graphtype;
                graphtype = CFGGraphType.getGraphType("BriefUnitGraph");
                DirectedGraph<Unit> graph = graphtype.buildGraph(methodBody);

                Map<String,Set<String>> transferMatrix = new HashMap<>();
                for (Unit unit : graph) { // 要剔除掉第一句的args 以及最后一句的return
                    if (unit.toString().contains("@parameter0: java.lang.String[]")){
                        continue;
                    }
                    if(!transferMatrix.keySet().contains(unit.getClass().getName())){
                        transferMatrix.put(unit.getClass().getName(),new HashSet<>());
                    }
                    List<Unit> tempList = graph.getSuccsOf(unit);
                    for (Unit unit1 : tempList) {
                        transferMatrix.get(unit.getClass().getName()).add(unit1.toString());
                    }
                }
                transferMatrixList.add(transferMatrix);
                Scene.v().removeClass(seedClass);
            }
        }
    }
}
