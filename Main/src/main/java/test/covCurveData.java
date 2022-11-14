package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class covCurveData {
    public static void main(String[] args) throws IOException {
        File file = new File("D:\\我的文件\\研究生\\JavaTailor\\新测试结果\\覆盖率曲线实验");
        for (File listFile : file.listFiles()) {
            if(listFile.isDirectory()){
                continue;
            }
            List<Float> lines = new ArrayList();
            List<Float> functions = new ArrayList<>();
            List<Float> branches = new ArrayList<>();
            File inputFile = listFile;
            System.out.println(inputFile.getName());
            BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFile));
            String line = bufferedReader.readLine();
            while(line!=null){
                if(line.toLowerCase(Locale.ROOT).contains("%")){
                    float e = Float.parseFloat(line.substring(15, line.indexOf("%")));
                    if(line.toLowerCase(Locale.ROOT).contains("line")){
                        lines.add(e);
                    }
                    if(line.toLowerCase(Locale.ROOT).contains("function")){
                        functions.add(e);
                    }
                    if(line.toLowerCase(Locale.ROOT).contains("branch")){
                        branches.add(e);
                    }
                }
                line=bufferedReader.readLine();
            }
            System.out.println(lines);
            System.out.println(functions);
            System.out.println(branches);
        }
    }
}
