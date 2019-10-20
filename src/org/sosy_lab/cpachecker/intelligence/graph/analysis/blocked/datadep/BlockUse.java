package org.sosy_lab.cpachecker.intelligence.graph.analysis.blocked.datadep;

public class BlockUse implements IBlockUseDefEvent {

    private String usePos;
    private String var;
    private String block;

    public BlockUse(String usePos, String var, String block) {
        this.usePos = usePos;
        this.var = var;
        this.block = block;
    }

    public String getUsePos() {
        return usePos;
    }

    @Override
    public String getBlock() {
        return block;
    }

    @Override
    public String getVariable() {
        return var;
    }

    @Override
    public String toString() {
        return "BlockUse{" +
                "usePos='" + usePos + '\'' +
                ", var='" + var + '\'' +
                ", block='" + block + '\'' +
                '}';
    }
}
