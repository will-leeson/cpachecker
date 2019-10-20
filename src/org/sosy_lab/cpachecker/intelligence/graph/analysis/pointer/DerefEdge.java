package org.sosy_lab.cpachecker.intelligence.graph.analysis.pointer;

import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;

public class DerefEdge extends GEdge {
    public DerefEdge(GNode pSource, GNode pSink) {
        super("d", pSource, pSink);
    }
}
