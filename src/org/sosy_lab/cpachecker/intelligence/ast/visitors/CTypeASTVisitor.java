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
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.intelligence.ast.ASTNodeLabel;
import org.sosy_lab.cpachecker.intelligence.graph.StructureGraph;



public class CTypeASTVisitor implements CTypeVisitor<String, CPATransferException> {

  private final StructureGraph graph;
  private final int depth;

  public CTypeASTVisitor(StructureGraph pGraph, int pDepth) {
    this.graph = pGraph;
    this.depth = pDepth;
  }


  @Override
  public String visit(CArrayType pArrayType) throws CPATransferException {

    if(depth <= 0) return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.ARRAY.name();

    if (pArrayType.isVolatile())
      label = ASTNodeLabel.VOLATILE.name() + "_" + label;
    if (pArrayType.isConst())
      label = ASTNodeLabel.CONST.name() + "_" + label;

    graph.addNode(id, label);

    String tId = graph.genId("A");

    if(depth >= 1) {
      graph.addNode(tId, ASTNodeLabel.TYPE.name());
      graph.addSEdge(tId, id);
    }

    if(depth >= 2) {
      String typeTree = pArrayType.getType().accept(
          new CTypeASTVisitor(graph, depth - 2));
      graph.addSEdge(typeTree, tId);
    }

    if (pArrayType.getLength() != null) {

      tId = graph.genId("A");

      if(depth >= 1) {
        graph.addNode(tId, ASTNodeLabel.LENGTH.name());
        graph.addSEdge(tId, id);
      }

      if(depth >= 2) {
        String lengthTree = pArrayType.getLength().accept(
            new CExpressionASTVisitor(graph, depth - 2));
        graph.addSEdge(lengthTree, tId);
      }

    }

    return id;
  }

  @Override
  public String visit(CCompositeType pCompositeType)
      throws CPATransferException {
    if(depth <= 0) return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.COMPOSITE_TYPE.name();

    if (pCompositeType.isVolatile())
      label = ASTNodeLabel.VOLATILE.name() + "_" + label;
    if (pCompositeType.isConst())
      label = ASTNodeLabel.CONST.name() + "_" + label;


    switch (pCompositeType.getKind()) {
      case ENUM:
        label = label + "_" + ASTNodeLabel.ENUM;
        break;
      case STRUCT:
        label = label + "_" + ASTNodeLabel.STRUCT;
        break;
      case UNION:
        label = label + "_" + ASTNodeLabel.UNION;
    }

    graph.addNode(id, label);

    if(depth >= 1) {
      for (CCompositeTypeMemberDeclaration decl : pCompositeType.getMembers()) {
        String compTypeMemberTypeTree =
            decl.getType().accept(new CTypeASTVisitor( graph, depth - 1));
        graph.addSEdge(compTypeMemberTypeTree, id);
      }
    }
    return id;
  }

  @Override
  public String visit(CElaboratedType pElaboratedType)
      throws CPATransferException {
    if(depth <= 0) return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.ELABORATED_TYPE.name();


    if (pElaboratedType.isVolatile())
      label = ASTNodeLabel.VOLATILE.name() + "_" + label;
    if (pElaboratedType.isConst())
      label = ASTNodeLabel.CONST.name() + "_" + label;

    switch (pElaboratedType.getKind()) {
      case ENUM:
        label = label + "_" + ASTNodeLabel.ENUM;
        break;
      case STRUCT:
        label = label + "_" + ASTNodeLabel.STRUCT;
        break;
      case UNION:
        label = label + "_" + ASTNodeLabel.UNION;
    }

    graph.addNode(id, label);

    return id;
  }

  @Override
  public String visit(CEnumType pEnumType) throws CPATransferException {
    if(depth <= 0) return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.ENUM_TYPE.name();
    graph.addNode(id, label);
    return id;

  }

  @Override
  public String visit(CFunctionType pFunctionType)
      throws CPATransferException {
    if(depth <= 0) return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.FUNCTION_TYPE.name();

    if (pFunctionType.isVolatile())
      label = ASTNodeLabel.VOLATILE.name() + "_" + label;
    if (pFunctionType.isConst())
      label = ASTNodeLabel.CONST.name() + "_" + label;

    graph.addNode(id, label);


    if (pFunctionType.getParameters().size() > 0) {
      String tId = graph.genId("A");

      if(depth >= 1){
        graph.addNode(tId, ASTNodeLabel.PARAM_TYPES.name());
        graph.addSEdge(tId, id);
      }

      if(depth >= 2) {
        for (CType type : pFunctionType.getParameters()) {
          String typeTree = type.accept(new CTypeASTVisitor(graph, depth - 2));
          graph.addSEdge(typeTree, tId);
        }
      }

    }

    String tId = graph.genId("A");

    if(depth >= 1){
      graph.addNode(tId, ASTNodeLabel.RETURN_TYPE.name());
      graph.addSEdge(tId, id);
    }

    if(depth >= 2) {
      String returnTypeTree = pFunctionType.getReturnType().accept(
          new CTypeASTVisitor(graph, depth - 2));
      graph.addSEdge(returnTypeTree, tId);
    }

    return id;
  }

  @Override
  public String visit(CPointerType pPointerType)
      throws CPATransferException {

    if(depth <= 0) return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.POINTER_TYPE.name();

    if (pPointerType.isVolatile())
      label = ASTNodeLabel.VOLATILE.name() + "_" + label;
    if (pPointerType.isConst())
      label = ASTNodeLabel.CONST.name() + "_" + label;


    graph.addNode(id, label);

    if(depth >= 1) {
      String typeTree =
          pPointerType.getType().accept(new CTypeASTVisitor( graph, depth - 1));
      graph.addSEdge(typeTree, id);
    }

    return id;
  }

  @Override
  public String visit(CProblemType pProblemType)
      throws CPATransferException {
    if(depth <= 0) return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.PROBLEM_TYPE.name();
    graph.addNode(id, label);
    return id;
  }

  @Override
  public String visit(CSimpleType pSimpleType)
      throws CPATransferException {

    if(depth <= 0) return "";

    String id = graph.genId("A");
    List<String> labels = new ArrayList<>();

    if (pSimpleType.isConst())
      labels.add(ASTNodeLabel.CONST.name());
    if (pSimpleType.isVolatile())
      labels.add(ASTNodeLabel.VOLATILE.name());
    if (pSimpleType.isLong())
      labels.add(ASTNodeLabel.LONG.name());
    if (pSimpleType.isLongLong())
      labels.add(ASTNodeLabel.LONGLONG.name());
    if (pSimpleType.isUnsigned())
      labels.add(ASTNodeLabel.UNSIGNED.name());

    switch (pSimpleType.getType()) {
      case BOOL:
        labels.add(ASTNodeLabel.BOOL.name());
        break;
      case CHAR:
        labels.add(ASTNodeLabel.CHAR.name());
        break;
      case INT:
        labels.add(ASTNodeLabel.INT.name());
        break;
      case FLOAT:
        labels.add(ASTNodeLabel.FLOAT.name());
        break;
      case DOUBLE:
        labels.add(ASTNodeLabel.DOUBLE.name());
        break;
      default:
        if (pSimpleType.isLong()) {
          labels.add(ASTNodeLabel.LONG.name());
          break;
        }
        if (pSimpleType.isLongLong()) {
          labels.add(ASTNodeLabel.LONGLONG.name());
          break;
        }
        if (pSimpleType.isUnsigned()) {
          labels.add(ASTNodeLabel.UNSIGNED.name());
          break;
        }
        if (pSimpleType.isShort()) {
          labels.add(ASTNodeLabel.SHORT.name());
          break;
        }
    }
    graph.addNode(id, String.join("_", labels));
    return id;
  }

  @Override
  public String visit(CTypedefType pTypedefType)
      throws CPATransferException {
    if(depth <= 0) return "";

    String id = graph.genId("A");
    graph.addNode(id, ASTNodeLabel.TYPEDEF_TYPE.name());

    String tId = graph.genId("A");

    if(depth >= 1){
      graph.addNode(tId, ASTNodeLabel.REAL_TYPE.name());
      graph.addSEdge(tId, id);
    }

    if(depth >= 2) {
      String realType =
          pTypedefType.getRealType().accept(new CTypeASTVisitor( graph, depth - 2));
      graph.addSEdge(realType, tId);
    }


    return id;
  }

  @Override
  public String visit(CVoidType pVoidType)
      throws CPATransferException {
    if(depth <= 0) return "";

    String id = graph.genId("A");
    String label = ASTNodeLabel.VOID_TYPE.name();

    if (pVoidType.isVolatile())
      label = ASTNodeLabel.VOLATILE.name() + "_" + label;
    if (pVoidType.isConst())
      label = ASTNodeLabel.CONST.name() + "_" + label;

    graph.addNode(id, label);

    return id;
  }

  @Override
  public String visit(CBitFieldType pCBitFieldType) throws CPATransferException {
    return "";
  }
}
