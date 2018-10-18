/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.intelligence.ast;

import com.google.common.base.Optional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.intelligence.ast.visitors.CExpressionASTVisitor;
import org.sosy_lab.cpachecker.intelligence.ast.visitors.CVariablesCollectingVisitor;
import org.sosy_lab.cpachecker.intelligence.graph.StructureGraph;

public class FunctionCallListener extends AEdgeListener {


  public FunctionCallListener(int pDepth, StructureGraph pGraph) {
    super(pDepth, pGraph);
  }

  @Override
  public void listen(CFAEdge edge) {
    if(edge instanceof CFunctionCallEdge){
      CFunctionCallEdge call = (CFunctionCallEdge)edge;

      List<CExpression> arguments = call.getArguments();
      String callFunctionName = call.getDescription();

      Optional<ASTNodeLabel> specialLabel = ASTCollectorUtils.getSpecialLabel(callFunctionName);
      String label = ASTNodeLabel.FUNC_CALL.name();

      if(specialLabel.isPresent())
        label = label + "_" + specialLabel.get();

      String id = "N"+edge.getPredecessor().getNodeNumber();
      String idS = "N"+edge.getSuccessor().getNodeNumber();
      graph.addNode(id, label);
      graph.addNode(idS);
      graph.addCFGEdge(id, idS);


      if(arguments.size() > 0) {
        String tId = graph.genId("A");

        if(depth >= 1){
          graph.addNode(tId, ASTNodeLabel.ARGUMENTS.name());
          graph.addSEdge(tId, id);
        }

        if(depth >= 2) {
          for (CExpression arg : arguments) {
            try {
              String tree = arg.accept(new CExpressionASTVisitor(graph, depth - 2 ));
              graph.addSEdge(tree, tId);
            } catch (CPATransferException pE) {
            }
          }
        }
      }


      Set<String> vars = new HashSet<>();
      for(CExpression exp : arguments)
        vars.addAll(exp.accept(new CVariablesCollectingVisitor(edge.getPredecessor())));
      graph.getNode(id).getOptions().put("variables", vars);

    }
  }
}
