package org.sosy_lab.cpachecker.intelligence.graph.analysis.pointer;

import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;

public class MAliasEdge extends GEdge {
    public MAliasEdge(GNode pSource, GNode pSink) {
        super("M", pSource, pSink);
    }
}
