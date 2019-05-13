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

import com.google.common.base.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclarationVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.intelligence.ast.ASTCollectorUtils;
import org.sosy_lab.cpachecker.intelligence.ast.ASTNodeLabel;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.SVGraph;

public class CSimpleDeclASTVisitor
    implements CSimpleDeclarationVisitor<String, CPATransferException> {

  private final SVGraph graph;
  private final int depth;

  public CSimpleDeclASTVisitor(SVGraph pGraph, int pDepth) {
    this.graph = pGraph;
    this.depth = pDepth;
  }

  @Override
  public String visit(CFunctionDeclaration pDecl)
      throws CPATransferException {
    if(depth <= 0)return "";
    Optional<ASTNodeLabel> specialLabel = ASTCollectorUtils.getSpecialLabel(pDecl.getName());


    String id = graph.genId("A");
    String label = ASTNodeLabel.FUNCTION_DECL.name();

    if(specialLabel.isPresent()){
        label = label +"_"+specialLabel.get();
    }

    graph.addNode(id, label);

    String tId = graph.genId("A");

    if(depth >= 1) {
      graph.addNode(tId, ASTNodeLabel.RETURN_TYPE.name());
      graph.addSEdge(tId, id);
    }

    if(depth >= 2) {
      String returnTypeTree = pDecl.getType().getReturnType()
          .accept(new CTypeASTVisitor( graph, depth - 2));
      graph.addSEdge(returnTypeTree, tId);
    }


    if(pDecl.getParameters().size() > 0) {
      tId = graph.genId("A");

      if(depth >= 1) {
        graph.addNode(tId, ASTNodeLabel.PARAM_TYPES.name());
        graph.addSEdge(tId, id);
      }

      if(depth >= 2) {
        for (CParameterDeclaration param : pDecl.getParameters()) {
          String typeTree =
              param.getType().accept(new CTypeASTVisitor( graph, depth - 2));
          graph.addSEdge(typeTree, tId);
        }
      }
    }

    return id;
  }

  @Override
  public String visit(CComplexTypeDeclaration pDecl)
      throws CPATransferException {
    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.COMPLEX_TYPE_DECL.name();
    graph.addNode(id, label);

    if(depth >= 1) {
      String typeTree = pDecl.getType().accept(new CTypeASTVisitor(graph, depth - 1));
      graph.addSEdge(typeTree, id);
    }
    return id;
  }


  @Override
  public String visit(CTypeDefDeclaration pDecl)
      throws CPATransferException {
    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.TYPE_DECL.name();
    graph.addNode(id, label);

    if(depth >= 1) {
      String typeTree = pDecl.getType().accept(new CTypeASTVisitor(graph, depth - 1));
      graph.addSEdge(typeTree, id);
    }
    return id;
  }

  @Override
  public String visit(CVariableDeclaration pDecl)
      throws CPATransferException {
    if(depth <= 0)return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.VARIABLE_DECL.name();
    graph.addNode(id, label);

    if(depth >= 1) {
      String typeTree = pDecl.getType().accept(new CTypeASTVisitor(graph, depth - 1));
      graph.addSEdge(typeTree, id);
    }

    if(pDecl.getInitializer() != null) {
      String tId = graph.genId("A");

      if(depth >= 1) {
        graph.addNode(tId, ASTNodeLabel.INITIALIZER.name());
        graph.addSEdge(tId, id);
      }

      if(depth >= 2) {
        String initializerTree =
            pDecl.getInitializer().accept(new CInitializerASTVisitor(this.graph, this.depth - 2));
        graph.addSEdge(initializerTree, tId);
      }

    }

    return id;
  }

  @Override
  public String visit(CParameterDeclaration pDecl)
      throws CPATransferException {
    return "";
  }

  @Override
  public String visit(CEnumerator pDecl) throws CPATransferException {
    return "";
  }
}
