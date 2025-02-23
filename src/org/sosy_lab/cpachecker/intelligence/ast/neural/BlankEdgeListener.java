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

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.intelligence.ast.AEdgeListener;
import org.sosy_lab.cpachecker.intelligence.ast.ASTNodeLabel;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.SVGraph;

public class BlankEdgeListener extends AEdgeListener {


  public BlankEdgeListener(
      SVGraph pGraph,
      ShutdownNotifier pShutdownNotifier) {
    super(-1, pGraph, pShutdownNotifier);
  }

  public static String extractLabel(CFAEdge cfaEdge) {
    if(!(cfaEdge instanceof BlankEdge)) return  null;

    if(cfaEdge.getDescription() == "while" || cfaEdge.getDescription() == "for" || cfaEdge.getDescription() == "do")
      return ASTNodeLabel.LOOP_ENTRY.name();
    else if(cfaEdge.getDescription() == "Function start dummy edge")
      return ASTNodeLabel.FUNCTION_START.name();
    else if(cfaEdge.getDescription() == "skip")
      return ASTNodeLabel.SKIP.name();
    else if(cfaEdge.getDescription().startsWith("Goto"))
      return ASTNodeLabel.GOTO.name();
    else if(cfaEdge.getDescription().startsWith("Label"))
      return ASTNodeLabel.LABEL.name();
    else
      return ASTNodeLabel.BLANK.name();
  }

  @Override
  public void listen(CFAEdge cfaEdge) {
    if(cfaEdge instanceof BlankEdge){

      String label = extractLabel(cfaEdge);

      String id = "N"+cfaEdge.getPredecessor().getNodeNumber();
      String idS = "N"+cfaEdge.getSuccessor().getNodeNumber();
      graph.addNode(id, label);
      graph.addNode(idS);
      graph.addCFGEdge(id, idS);

      CFANode node = cfaEdge.getPredecessor();
      if(node instanceof CFunctionEntryNode){

        CFunctionEntryNode entryNode = (CFunctionEntryNode)node;

        Set<String> vars = new HashSet<>();

        for(CParameterDeclaration param : entryNode.getFunctionParameters()){

          vars.add(param.getQualifiedName());

        }

        graph.getNode(id).setOption(OptionKeys.DECL_VARS, vars);
        graph.getNode(id).setOption(OptionKeys.FUNC_NAME, entryNode.getFunctionName());


      }

    }
  }
}
