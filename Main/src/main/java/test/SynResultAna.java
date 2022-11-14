package test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class SynResultAna {
    public static void main(String[] args) throws IOException {
        Map<String,Integer> result2time = new HashMap<>();
        String path = "D:\\我的文件\\研究生\\JavaTailor\\CFSynthesisV2\\03results\\1656504543489\\HotspotTests-Java";
        File resultFile = new File(path + "\\SynResult.txt");
        BufferedReader resultReader = new BufferedReader(new FileReader(resultFile));
        String line = resultReader.readLine();
        while (line != null){
            line = line.split("---")[0];
            result2time.putIfAbsent(line, 0);
            result2time.put(line,result2time.get(line)+1);
            line = resultReader.readLine();
        }
        File timeFile = new File(path + "\\resultTime.txt");
        FileWriter fileWriter = new FileWriter(timeFile);
        for (String s : result2time.keySet()) {
            fileWriter.write(result2time.get(s)+"————"+s+"\n");
        }
        fileWriter.flush();
        fileWriter.close();
    }
}
