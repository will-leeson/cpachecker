package org.sosy_lab.cpachecker.intelligence.graph.analysis.pointer;

import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;

public class VAliasEdge extends GEdge {
    public VAliasEdge( GNode pSource, GNode pSink) {
        super("v", pSource, pSink);
    }
}
