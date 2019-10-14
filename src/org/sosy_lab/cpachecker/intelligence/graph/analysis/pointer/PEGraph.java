package org.sosy_lab.cpachecker.intelligence.graph.analysis.pointer;


import org.sosy_lab.cpachecker.intelligence.graph.model.StructureGraph;

public class PEGraph extends StructureGraph {

    public boolean addDerefEdge(String source, String target){
        return addEdge(new DerefEdge(super.getNode(source), super.getNode(target)));
    }

    public boolean addAssignEdge(String source, String target){
        return addEdge(new AssignEdge(super.getNode(source), super.getNode(target)));
    }

    public boolean addMemoryAlias(String source, String target){
        return addEdge(new MAliasEdge(super.getNode(source), super.getNode(target)));
    }

    public boolean addValueAlias(String source, String target){
        return addEdge(new VAliasEdge(super.getNode(source), super.getNode(target)));
    }

    public boolean addTransition(String name, String source, String target){
        return addEdge(new TransEdge(name, super.getNode(source), super.getNode(target)));
    }

    public String toDot(){
        return super.toDot(
                n -> true,
                e -> {
                    if(e instanceof DerefEdge) {
                        return "red";
                    } else if(e instanceof AssignEdge){
                        return "black";
                    }else if(e instanceof MAliasEdge){
                        return "green";
                    }else{
                        return "opaque";
                    }
                },
                n -> "main"
        );
    }
}
