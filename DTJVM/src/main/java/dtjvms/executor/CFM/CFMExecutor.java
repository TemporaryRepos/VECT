package dtjvms.executor.CFM;

import dtjvms.*;
import dtjvms.analyzer.DiffCore;
import dtjvms.analyzer.JDKAnalyzer;
import dtjvms.analyzer.UniqueFilter;
import dtjvms.executor.Executor;
import dtjvms.executor.ExecutorHelper;
import util.LoggerUtils;
import vmoptions.Option;


import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

public class CFMExecutor extends Executor {

    private JvmOutput currentOutput;
    private Process currentProcess;

    public static HashMap<String, Integer> allDiff = new HashMap<>();

    private boolean diffFound;
    private boolean disCard;

    public static CFMExecutor cfmExecutor;

    HashMap<String, JvmOutput> results = null;

    public static CFMExecutor getInstance(){

        if (cfmExecutor == null){
            cfmExecutor = new CFMExecutor();
        }
        return cfmExecutor;
    }

    @Override
    public JvmOutput execute(String cmd) {

        JvmOutput output = null;
        currentOutput = null;
        try {
            currentProcess = Runtime.getRuntime().exec(cmd);
            output = ExecutorHelper.getJvmOutput(currentProcess);
        } catch (IOException e) {
            e.printStackTrace();
        }
        currentOutput = output;
        return output;
    }

    public void shutDown(){
        if (currentProcess != null){
            currentProcess.destroy();
            currentProcess.destroyForcibly();
        }
    }

    public boolean dtSingleClassInProj(ArrayList<JvmInfo> jvmCmds,
                                       ProjectInfo currentProject,
                                       ProjectInfo mutateProvideProject,
                                       String executeClassName,
                                       String logClassName) throws IOException {

        diffFound = false;
        disCard = false;

        HashMap<String, JvmOutput> results = new HashMap<>();

        ArrayList<String> vmOptions = currentProject.getVmoptions();
        ArrayList<String> projOptions = currentProject.getProjoptions();
        String[] projOptionsArray = projOptions.toArray(new String[projOptions.size()]);
        String classPath = currentProject.getpClassPath() + DTPlatform.PATH_SEPARATOR + mutateProvideProject.getpClassPath();
        /**
         * 01 differential testing java application class
         */
        if (currentProject.getApplicationClasses().contains(executeClassName)){

            System.out.println("Project-application: "
                    + currentProject.getProjectName()
                    + "-"
                    + logClassName
                    + "...");
            results = dtSingleClass(jvmCmds, vmOptions, classPath, currentProject.getProjectName() , executeClassName, false, projOptionsArray);
        }
        /**
         * 02 differential testing junit test case
         */
        if (currentProject.getJunitClasses().contains(executeClassName)){

            System.out.println("Project-junit: "
                    + currentProject.getProjectName()
                    + "-"
                    + logClassName
                    + "...");
            results = dtSingleClass(jvmCmds, vmOptions, classPath, currentProject.getProjectName() ,executeClassName, true, projOptionsArray);
        }

        DiffCore diff = JDKAnalyzer.getInstance().analysis(logClassName, results);

        if (diff != null){
            diffFound = true;
            ExecutorHelper.logJvmOutput(DTGlobal.getDiffLogger(), currentProject.getProjectName(), logClassName, diff ,results);
        }
        disCard = JDKAnalyzer.getInstance().getDiscardFlag();
        return disCard;
    }

    public boolean dtSingleClassInProj(ArrayList<JvmInfo> jvmCmds,
                                    ProjectInfo currentProject,
                                    String executeClassName,
                                    String logClassName) throws IOException {

        diffFound = false;
        disCard = false;
        HashMap<String, JvmOutput> results = new HashMap<>();

        ArrayList<String> vmOptions = currentProject.getVmoptions();
        ArrayList<String> projOptions = currentProject.getProjoptions();
        String[] projOptionsArray = projOptions.toArray(new String[projOptions.size()]);
        String classPath = currentProject.getpClassPath();
        /**
         * 01 differential testing java application class
         */
        if (currentProject.getApplicationClasses().contains(executeClassName)){

            System.out.println("Project-application: "
                    + currentProject.getProjectName()
                    + "-"
                    + logClassName
                    + "...");
            results = dtSingleClass(jvmCmds, vmOptions, classPath, currentProject.getProjectName() , executeClassName, false, projOptionsArray);
        }
        /**
         * 02 differential testing junit test case
         */
        if (currentProject.getJunitClasses().contains(executeClassName)){

            System.out.println("Project-junit: "
                    + currentProject.getProjectName()
                    + "-"
                    + logClassName
                    + "...");
            results = dtSingleClass(jvmCmds, vmOptions, classPath, currentProject.getProjectName() ,executeClassName, true, projOptionsArray);
        }
        setResults(results);
        DiffCore diff = JDKAnalyzer.getInstance().analysis(logClassName, results);

        if (diff != null){
            diffFound = true;
            ExecutorHelper.logJvmOutput(DTGlobal.getDiffLogger(), currentProject.getProjectName(), logClassName, diff ,results);
        }
        disCard = JDKAnalyzer.getInstance().getDiscardFlag();
        return disCard;
    }

    public boolean isDiffFound() {
        return diffFound;
    }

    public boolean isDisCard() {
        return disCard;
    }

    public void dtSingleClassMultiThread(ArrayList<JvmInfo> jvms,
                                         ArrayList<String> pOptions,
                                         String classpath,
                                         String projName,
                                         String className,
                                         boolean isJunit,
                                         String... args) {

        HashMap<String, JvmOutput> results = new HashMap<>();

        for (JvmInfo jvm : jvms) {

            String jvmId = jvm.getJvmId() != null ? jvm.getJvmId() : jvm.getFolderName();
            String cmdExecute = ExecutorHelper.assembleJavaCmd(jvm.getJavaCmd(), pOptions, classpath, className, isJunit, args);

            Thread ctester = new Thread(new Runnable() {
                @Override
                public void run() {
                    getInstance().execute(cmdExecute);
                }
            });
            ctester.start();
            try {
                TimeUnit.SECONDS.timedJoin(ctester, DTConfiguration.CLASS_MAX_RUNTIME);
                CFMExecutor.getInstance().shutDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (currentOutput != null){
                results.put(jvmId, currentOutput);
            } else {
                results.put(jvmId, new JvmOutput("JvmOutput-TIMEOUT"));
            }
        }

        if (JDKAnalyzer.getInstance().analysis(className, results) != null){
            ExecutorHelper.logJvmOutput(projName, className, results);
        }
    }


    public HashMap<String, JvmOutput> dtSingleClass(ArrayList<JvmInfo> jvms,
                                                    ArrayList<String> pOptions,
                                                    String classpath,
                                                    String projName,
                                                    String className,
                                                    boolean isJunit,
                                                    String... args) {
        JvmOutput.timeoutFlag = false;

        HashMap<String, JvmOutput> results = new HashMap<>();

        for (JvmInfo jvm : jvms) {

            if(jvm.getJvmName().equals("jvmCoverage")){


                String cmdExecute = ExecutorHelper.assembleJavaCmd(jvm.getJavaCmd(), pOptions, classpath, className, isJunit, args);
//                System.out.println(cmdExecute);
                Thread ctester = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        getInstance().execute(cmdExecute);
                    }
                });
                ctester.start();
                try {
                    TimeUnit.SECONDS.timedJoin(ctester, DTConfiguration.CLASS_MAX_RUNTIME);
                    CFMExecutor.getInstance().shutDown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            StringBuilder options = new StringBuilder();
            if(DTGlobal.useVMOptions){
                if(jvm.getJvmName().toLowerCase(Locale.ROOT).contains("hotspot")){
                    options = new StringBuilder(" -XX:CompileThreshold=2");
                    if(jvm.getVmOptions() != null){
                        options.append(" -Xcomp -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+IgnoreUnrecognizedVMOptions");
                        Set<Integer> indexSet = new HashSet<>();
                        for(int i=0;i<new Random().nextInt(jvm.getVmOptions().getOptions().size());i++){
                            indexSet.add(new Random().nextInt(jvm.getVmOptions().getOptions().size()));
                        }
                        for (Integer integer : indexSet) {
                            options.append(" ").append(jvm.getVmOptions().getOptions().get(integer).getCmd());
                        }
                    }
                }
                if(jvm.getJvmName().toLowerCase(Locale.ROOT).contains("openj9")){
                    options = new StringBuilder(" -Xjit:count=2");
                }
            }

            String jvmId = jvm.getJvmId() != null ? jvm.getJvmId() : jvm.getFolderName();
            String cmdExecute = ExecutorHelper.assembleJavaCmd(jvm.getJavaCmd()+options, pOptions, classpath, className, isJunit, args);
            System.out.println(cmdExecute);
            Thread ctester = new Thread(new Runnable() {
                @Override
                public void run() {
                    getInstance().execute(cmdExecute);
                }
            });
            ctester.start();
            try {
                TimeUnit.SECONDS.timedJoin(ctester, DTConfiguration.CLASS_MAX_RUNTIME);
                CFMExecutor.getInstance().shutDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (currentOutput != null){
                if(currentOutput.getStdout().contains("my_check_sum_value:")){
                    currentOutput.setStdout(currentOutput.getStdout().substring(currentOutput.getStdout().indexOf("my_check_sum_value:"))+"\n");
                    currentOutput.setStdout(currentOutput.getStdout().substring(0,currentOutput.getStdout().indexOf("\n")));
                }else {
                    currentOutput.setStdout("");
                }
                System.out.println("stdout:" + currentOutput.getStdout());
                System.out.println("stderr:" + currentOutput.getStderr());
                results.put(jvmId+options, currentOutput);

            } else {
                results.put(jvmId, new JvmOutput("JvmOutput-TIMEOUT"));
                JvmOutput.timeoutFlag = true;
            }
        }
        return results;
    }

    public JvmOutput getCurrentOutput() {
        return currentOutput;
    }

    public HashMap<String, JvmOutput> getResults() {
        return results;
    }

    public void setResults(HashMap<String, JvmOutput> results) {
        this.results = results;
    }
}
