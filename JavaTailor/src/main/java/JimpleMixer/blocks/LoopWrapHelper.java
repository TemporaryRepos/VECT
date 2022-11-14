package JimpleMixer.blocks;
import soot.*;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.internal.*;
import soot.util.Chain;

public class LoopWrapHelper {
    public static void loopWrapForClass(SootClass seedClass){
        Body methodBody = null;
        try {
            methodBody = seedClass.getMethod("void main(java.lang.String[])").retrieveActiveBody();
        }catch (Exception e){
            return;
        }


        assert methodBody != null;
        Unit endUnit = methodBody.getUnits().getLast();
        Unit startUnit = null;

        for (Unit unit : methodBody.getUnits()) {
            if(!unit.toString().contains("@")){
                startUnit = unit;
                break;
            }
        }

        if((endUnit instanceof JReturnVoidStmt || endUnit instanceof JReturnStmt) && startUnit!=null){

            // i0 = 0
            Local var1 = new JimpleLocal(getVarName("var1", methodBody.getLocals()),IntType.v());
            Value left = var1;
            methodBody.getLocals().add((Local) left);
            Value right = IntConstant.v(0);
            JAssignStmt jAssignStmt = new JAssignStmt(left,right);
            // if i0 >= 20000 goto return
            right = IntConstant.v(3);;
            Value condition = new JGeExpr(left, right);
            JIfStmt jIfStmt = new JIfStmt(condition, endUnit);
            // i0 = i0 + 1
            right = Jimple.v().newAddExpr(var1,IntConstant.v(1));
            JAssignStmt jAssignStmt1 = new JAssignStmt(left,right);
            // goto JIfStmt
            JGotoStmt jGotoStmt = new JGotoStmt(jIfStmt);
            methodBody.getUnits().insertBeforeNoRedirect(jAssignStmt1,endUnit);
            methodBody.getUnits().insertBeforeNoRedirect(jGotoStmt,endUnit);

            methodBody.getUnits().insertBeforeNoRedirect(jAssignStmt, startUnit);

            methodBody.getUnits().insertBeforeNoRedirect(jIfStmt, startUnit);
            if(endUnit instanceof JReturnVoidStmt){
                methodBody.getUnits().insertBeforeNoRedirect(new JReturnVoidStmt(),endUnit);
            }else {
                methodBody.getUnits().insertBeforeNoRedirect(new JReturnStmt(endUnit.getUseBoxes().get(0).getValue()),endUnit);
            }
        }
    }
    /**
     * 得到一个独一无二的变量名
     * @param targetName 基础目标名
     * @param locals
     * @return targetName+"_N"*n
     */
    private static String getVarName(String targetName, Chain<Local> locals){
        for (Local local : locals) {
            if(local.getName().contains(targetName)){
                targetName=targetName+"_N";
            }
        }
        return targetName;
    }
}
