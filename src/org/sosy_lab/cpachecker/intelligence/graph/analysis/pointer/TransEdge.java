package org.sosy_lab.cpachecker.intelligence.graph.analysis.pointer;

import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;

public class TransEdge extends GEdge {
    public TransEdge(String pID, GNode pSource, GNode pSink) {
        super(pID, pSource, pSink);
    }
}
