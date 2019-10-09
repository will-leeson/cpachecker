package org.sosy_lab.cpachecker.intelligence.graph.analysis.blocked.datadep;

public class BlockDef implements IBlockUseDefEvent{

    private String defPos;
    private String var;
    private String block;

    public BlockDef(String defPos, String var, String block) {
        this.defPos = defPos;
        this.var = var;
        this.block = block;
    }

    public String getDefPos() {
        return defPos;
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
        return "BlockDef{" +
                "defPos='" + defPos + '\'' +
                ", var='" + var + '\'' +
                ", block='" + block + '\'' +
                '}';
    }
}
