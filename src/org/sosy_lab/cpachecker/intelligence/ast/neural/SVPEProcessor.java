package org.sosy_lab.cpachecker.intelligence.ast.neural;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.intelligence.ast.CFAIterator;
import org.sosy_lab.cpachecker.intelligence.ast.IEdgeListener;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.pointer.PEGraph;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.SVGraph;

import java.util.HashSet;
import java.util.List;

public class SVPEProcessor extends SVGraphProcessor {

    protected void attachPointerListener(PEGraph pPointerGraph, ShutdownNotifier pShutdownNotifier, List<IEdgeListener> listeners){
        listeners.add(new PointerListener2(pPointerGraph, pShutdownNotifier));
    }

    @Override
    public SVGraph process(CFA pCFA, ShutdownNotifier pShutdownNotifier)
            throws InterruptedException {

        SVGraph functionBody = new SVGraph();
        PEGraph pe = new PEGraph();
        functionBody.setGlobalOption(OptionKeys.INVOKED_FUNCS, new HashSet<>());
        functionBody.setGlobalOption(OptionKeys.POINTER_GRAPH, pe);
        List<IEdgeListener> listeners = listeners(functionBody, pShutdownNotifier);
        this.attachPointerListener(pe, pShutdownNotifier, listeners);
        CFAIterator it = new CFAIterator(listeners, pShutdownNotifier);

        it.iterate(pCFA);

        return functionBody;
    }
}
