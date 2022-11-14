import core.CSVUtils;
import test.test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class class2java {
    public static void main(String[] args) throws InterruptedException, IOException {
        List<String> nameList = CSVUtils.importStringCsv(new File("./className.csv"));
        int tempTimeFlag = 0;
        for(String name:nameList){
            tempTimeFlag++;
            System.out.print(tempTimeFlag);
//            if(System.getProperty("os.name").toUpperCase().contains("WIN")){
//                cmd = "cmd /c "+"jad.exe -p "+classFileFolder+ name+" > "+classFileFolder + name.replace(".class",".java");
//            }
//            if(System.getProperty("os.name").toUpperCase().contains("LINUX")){
//                cmd = "java -jar cfr-0.144.jar "+classFileFolder+ name+" > "+classFileFolder + name.replace(".class",".java");
//            }
            String cmd = "cmd /c "+"jad.exe -p "+".\\04SynthesisHistory\\template\\"+name+" > "+".\\04SynthesisHistory\\template\\"+name.replace(".class",".java");
            System.out.println(":"+cmd);
//            Process p = new ProcessBuilder(cmd).start();

            Process p = Runtime.getRuntime().exec(cmd);
//            p.waitFor();
        }
    }
}