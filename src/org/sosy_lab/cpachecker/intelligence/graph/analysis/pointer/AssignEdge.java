package org.sosy_lab.cpachecker.intelligence.graph.analysis.pointer;

import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;

public class AssignEdge extends GEdge {
    public AssignEdge(GNode pSource, GNode pSink) {
        super("a", pSource, pSink);
    }
}
