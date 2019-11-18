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

import com.google.common.base.Optional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.intelligence.ast.AEdgeListener;
import org.sosy_lab.cpachecker.intelligence.ast.ASTCollectorUtils;
import org.sosy_lab.cpachecker.intelligence.ast.ASTNodeLabel;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.ast.visitors.CAssignVariablesCollector;
import org.sosy_lab.cpachecker.intelligence.ast.visitors.CExpressionASTVisitor;
import org.sosy_lab.cpachecker.intelligence.ast.visitors.CVariablesCollectingVisitor;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.SVGraph;

public class FunctionCallListener extends AEdgeListener {


  public FunctionCallListener(
      SVGraph pGraph,
      ShutdownNotifier pShutdownNotifier) {
    super(-1, pGraph, pShutdownNotifier);
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

      //Summary edge
      String idN = "N"+call.getSummaryEdge().getSuccessor().getNodeNumber();
      graph.addNode(idN);
      graph.addCFGEdge(id, idN);

      GNode node = graph.getNode(id);
      node.setOption(OptionKeys.FUNC_CALL, call.getSuccessor().getFunctionName());
      node.setOption(OptionKeys.ARGS, call.getArguments());

      if(graph.getGlobalOption(OptionKeys.INVOKED_FUNCS) == null){
        graph.setGlobalOption(OptionKeys.INVOKED_FUNCS, new HashSet<>());
      }
      graph.getGlobalOption(OptionKeys.INVOKED_FUNCS).add(call.getSuccessor().getFunctionName());

      if(notifier != null)
        try{
          notifier.shutdownIfNecessary();
        }catch (InterruptedException pE){
          return;
        }

      Set<String> vars = new HashSet<>();
      for(CExpression exp : arguments)
        vars.addAll(exp.accept(new CVariablesCollectingVisitor(edge.getPredecessor())));
      node.setOption(OptionKeys.VARS, vars);

      if(!node.containsOption(OptionKeys.DECL_VARS)){
        node.setOption(OptionKeys.DECL_VARS, new HashSet<>());
      }

      CFunctionCall functionCall = call.getSummaryEdge().getExpression();

      if(functionCall instanceof CFunctionCallAssignmentStatement){
        CFunctionCallAssignmentStatement assign = (CFunctionCallAssignmentStatement)functionCall;

        try {
          node.getOption(OptionKeys.DECL_VARS).addAll(assign.accept(new CAssignVariablesCollector(edge.getPredecessor())));
        } catch (CPATransferException pE) {
        }

      }

      SVGraph ast = new SVGraph();
      ast.setGlobalOption(OptionKeys.REPLACE_ID, true);
      if(arguments.size() > 0) {
        String tId = ast.genId("A");

        ast.addNode(tId, ASTNodeLabel.ARGUMENTS.name());
        ast.addSEdge(tId, id);

        for (CExpression arg : arguments) {

          if(notifier != null)
            try{
              notifier.shutdownIfNecessary();
            }catch (InterruptedException pE){
              return;
            }

          try {
            String tree = arg.accept(new CExpressionASTVisitor(ast, Integer.MAX_VALUE));
            ast.addSEdge(tree, tId);
          } catch (CPATransferException pE) {
          }
        }
        node.setOption(OptionKeys.AST_ROOT, tId);

      }
      node.setOption(OptionKeys.AST, ast);

    }
  }
}
