// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.intelligence.ast.neural;

import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

public class LabelExtractorUtil {

  public static String extractLabel(CFAEdge pCFAEdge){

    if(pCFAEdge instanceof CAssumeEdge)
      return AssumptionListener.extractLabel(pCFAEdge);

    if(pCFAEdge instanceof BlankEdge)
      return BlankEdgeListener.extractLabel(pCFAEdge);

    if(pCFAEdge instanceof CDeclarationEdge)
      return DeclarationListener.extractLabel(pCFAEdge);

    if(pCFAEdge instanceof CFunctionCallEdge)
      return FunctionCallListener.extractLabel(pCFAEdge);

    if(pCFAEdge instanceof CFunctionReturnEdge)
      return ReturnFunctionListener.extractLabel(pCFAEdge);

    if(pCFAEdge instanceof CReturnStatementEdge)
      return ReturnStatementListener.extractLabel(pCFAEdge);

    if(pCFAEdge instanceof CStatementEdge)
      return StatementListener.extractLabel(pCFAEdge);

    return "UNKNOWN";
  }

}
