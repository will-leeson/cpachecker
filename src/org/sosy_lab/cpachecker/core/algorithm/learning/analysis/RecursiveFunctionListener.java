// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.learning.analysis;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.composition.learn.IAnalysisListener;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;

public class RecursiveFunctionListener implements IAnalysisListener {

  private Set<String> seenFunctionCalls = new HashSet<>();

  @Override
  public void listen(GEdge pGEdge) {
    GNode current = pGEdge.getSink();

    if(current.containsOption(OptionKeys.FUNC_CALL)) {

      String funcName = current.getOption(OptionKeys.FUNC_CALL);
      seenFunctionCalls.add(funcName);
    }

    if(current.getLabel().contains("FUNCTION_EXIT")){
      seenFunctionCalls.remove(current.getOption(OptionKeys.FUNC_NAME));
    }

    if(current.getLabel().contains("FUNCTION_START")){
      String funcName = current.getOption(OptionKeys.FUNC_NAME);
      if(seenFunctionCalls.contains(funcName))
        current.setLabel("FUNCTION_START_RECURSIVE");
    }


  }

  @Override
  public void reset() {
    seenFunctionCalls = new HashSet<>();
  }
}
