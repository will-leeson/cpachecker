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
package org.sosy_lab.cpachecker.intelligence.ast.visitors;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.intelligence.ast.ASTNodeLabel;
import org.sosy_lab.cpachecker.intelligence.graph.StructureGraph;

/**
 * Created by zenscr on 01/12/15.
 */
public class CInitializerASTVisitor implements CInitializerVisitor<String, CPATransferException> {


  private final StructureGraph graph;
  private final int depth;

  public CInitializerASTVisitor(
      StructureGraph pGraph, int pDepth) {
    graph = pGraph;
    depth = pDepth;
  }

  @Override
  public String visit(CInitializerExpression pInitializerExpression)
      throws CPATransferException {
    return pInitializerExpression.getExpression().accept(new CExpressionASTVisitor( graph, depth));
  }

  @Override
  public String visit(CInitializerList pInitializerList)
      throws CPATransferException {

    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.INITIALIZER_LIST.name();
    graph.addNode(id, label);

    if(depth >= 1) {
      for (CInitializer initializer : pInitializerList.getInitializers()) {
        graph.addSEdge(initializer.accept(new CInitializerASTVisitor(
            this.graph, depth - 1
        )), id);
      }
    }

    return id;
  }

  @Override
  public String visit(CDesignatedInitializer pCStructInitializerPart)
      throws CPATransferException {
    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.DESIGNED_INITIALIZER.name();
    graph.addNode(id, label);
    return id;
  }
}
