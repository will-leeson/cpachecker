/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.intelligence.ast.neural;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.intelligence.ast.CFAIterator;
import org.sosy_lab.cpachecker.intelligence.ast.IEdgeListener;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.CallGraph;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.SVGraph;

public class CallGraphProcessor {

  private List<IEdgeListener> listeners(SVGraph pGraph, ShutdownNotifier pShutdownNotifier){

    return new ArrayList<IEdgeListener>(){{
        add(new InitExitListener(pGraph, pShutdownNotifier));
        add(new AssumptionListener(pGraph, pShutdownNotifier));
        add(new BlankEdgeListener(pGraph, pShutdownNotifier));
        add(new DeclarationListener(pGraph, pShutdownNotifier));
        add(new FunctionCallListener(pGraph, pShutdownNotifier));
        add(new ReturnFunctionListener(pGraph, pShutdownNotifier));
        add(new ReturnStatementListener(pGraph, pShutdownNotifier));
        add(new StatementListener(pGraph, pShutdownNotifier));
    }};
  }


  public CallGraph process(CFA pCFA, ShutdownNotifier pShutdownNotifier)
      throws InterruptedException {

    CallGraph callGraph = new CallGraph();

    for(FunctionEntryNode functionEntryNode : pCFA.getAllFunctionHeads()) {
      SVGraph functionBody = new SVGraph();
      functionBody.setGlobalOption(OptionKeys.INVOKED_FUNCS, new HashSet<>());
      List<IEdgeListener> listeners = listeners(functionBody, pShutdownNotifier);
      CFAIterator it = new CFAIterator(listeners, pShutdownNotifier);

      it.iterate(functionEntryNode, false);

      System.out.println(functionEntryNode.getFunctionName()+"\n"+functionBody.toDot());

      callGraph.addFunction(functionEntryNode.getFunctionName(), functionBody);

      for(String call : functionBody.getGlobalOption(OptionKeys.INVOKED_FUNCS)){
        callGraph.addNode(call);
        callGraph.addCall(functionEntryNode.getFunctionName(), call);
      }

    }

    return callGraph;
  }




}
