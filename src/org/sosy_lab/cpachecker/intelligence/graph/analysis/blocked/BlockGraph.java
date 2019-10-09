package org.sosy_lab.cpachecker.intelligence.graph.analysis.blocked;

import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;
import org.sosy_lab.cpachecker.intelligence.graph.model.StructureGraph;

import java.util.ArrayList;
import java.util.List;

public class BlockGraph extends StructureGraph {

    private int conn_id = 0;

    public boolean addEdge(String source, String target){

        return super.addEdge(
                new CFGEdge("conn_"+(conn_id++), super.getNode(source), super.getNode(target))
        );

    }

    public boolean addBlockNode(String name, String label, List<String> sequence){
        return super.addNode(new BlockNode(name, label, sequence));
    }

    public boolean addBlockNodeFrom(GNode node){

        BlockNode blockNode = new BlockNode(node.getId(), node.getLabel(), new ArrayList<>());
        if(node.containsOption(OptionKeys.PARENT_FUNC))
            blockNode.setOption(OptionKeys.PARENT_FUNC, node.getOption(OptionKeys.PARENT_FUNC));

        return super.addNode(blockNode);
    }


    public boolean addBlockNode(String name,  List<String> sequence){
        return addBlockNode(name, "", sequence);
    }

    public class CFGEdge extends GEdge {

        public CFGEdge(String pID, GNode pSource, GNode pSink) {
            super(pID, pSource, pSink);
        }
    }

    public class BlockNode extends GNode {

        private List<String> sequence;

        public BlockNode(String pId, String label, List<String> sequence) {
            super(pId, label);
            this.sequence = sequence;
        }

        public BlockNode(String pId,  List<String> sequence) {
            this(pId, "", sequence);
        }

        public List<String> getSequence(){
            return sequence;
        }
    }

}
