package test;

import JimpleMixer.blocks.BlockInfo;
import JimpleMixer.blocks.BlocksContainer;
import dtjvms.DTGlobal;
import soot.Unit;
import dtjvms.analyzer.CosSimilarty;

import java.security.Key;
import java.util.*;

public class tempGetSame {
    public static void run() {
        Map<Integer,List<String>> lenMap = new TreeMap<>(new HashMap<>());
        for(String key: BlocksContainer.validMutationMap.keySet()) {
            List<BlockInfo> blockInfos = BlocksContainer.validMutationMap.get(key);
            for (BlockInfo blockInfo : blockInfos) {
                List<Unit> allStmtsInBlock = BlocksContainer.getAllStmtsInBlocks(BlocksContainer.getAllSuccessorBlockInBlocks(new ArrayList<>(), blockInfos, blockInfo));
                StringBuilder tempS = new StringBuilder();
                for (Unit unit : allStmtsInBlock) {
                    tempS.append(unit.toString());
                    tempS.append(";");
                }
                lenMap.computeIfAbsent(allStmtsInBlock.size(), k -> new ArrayList<>()).add(String.valueOf(tempS));

            }
        }
        lenMap = ((TreeMap)lenMap).descendingMap();
        for (Integer integer : lenMap.keySet()) {
            System.out.println(integer);
        }
        for(Integer key:lenMap.keySet()){
            if(key<5 || key > 8){
                continue;
            }
            List<String> ingredients = lenMap.get(key);
            for(int i=0;i<ingredients.size();i++){
                for(int j=i+1;j<ingredients.size();j++){
                    double similarty = CosSimilarty.run(ingredients.get(i),ingredients.get(j));
                    if(similarty>0.8){
                        DTGlobal.getInsertLogger().info("+++++++++++++++++++++++++++++++++++"+"\n"+ingredients.get(i)+"\n"+ingredients.get(j)+"\n");
                    }
                }
            }
        }
    }
}
