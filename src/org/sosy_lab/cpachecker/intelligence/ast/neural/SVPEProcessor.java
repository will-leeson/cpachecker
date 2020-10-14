package org.sosy_lab.cpachecker.intelligence.ast.neural;

import java.util.ArrayList;
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

    protected List<IEdgeListener> attachPointerListener(PEGraph pPointerGraph, ShutdownNotifier pShutdownNotifier, List<IEdgeListener> listeners){
        List<IEdgeListener> copyList = new ArrayList<>(listeners);
        copyList.add(new PointerListener2(pPointerGraph, pShutdownNotifier));
        return copyList;
    }

    @Override
    public SVGraph process(CFA pCFA, ShutdownNotifier pShutdownNotifier)
            throws InterruptedException {

        SVGraph functionBody = new SVGraph();
        PEGraph pe = new PEGraph();
        functionBody.setGlobalOption(OptionKeys.INVOKED_FUNCS, new HashSet<>());
        functionBody.setGlobalOption(OptionKeys.POINTER_GRAPH, pe);
        List<IEdgeListener> listeners = listeners(functionBody, pShutdownNotifier);
        listeners = this.attachPointerListener(pe, pShutdownNotifier, listeners);
        CFAIterator it = new CFAIterator(listeners, pShutdownNotifier);

        it.iterate(pCFA);

        return functionBody;
    }
}
