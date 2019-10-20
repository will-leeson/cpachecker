package org.sosy_lab.cpachecker.intelligence.graph.analysis.blocked.datadep;

public class UseDef implements IUseDefEvent{

    private String usePos;
    private String var;
    private String defPos;

    public UseDef(String usePos, String var, String defPos) {
        this.usePos = usePos;
        this.var = var;
        this.defPos = defPos;
    }

    public String getUsePos() {
        return usePos;
    }

    public String getDefPos() {
        return defPos;
    }

    @Override
    public String getVariable() {
        return var;
    }

    @Override
    public String toString() {
        return "UseDef{" +
                "usePos='" + usePos + '\'' +
                ", var='" + var + '\'' +
                ", defPos='" + defPos + '\'' +
                '}';
    }
}
