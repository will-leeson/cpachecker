// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.learning.analysis;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.composition.learn.IAnalysisListener;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;

public class UnusedVarListener implements IAnalysisListener {

  private Set<String> usedVars = new HashSet<>();

  @Override
  public void listen(GEdge pGEdge) {

    GNode node = pGEdge.getSource();

    if(node.containsOption(OptionKeys.DECL_VARS)){

      Set<String> declVars = node.getOption(OptionKeys.DECL_VARS);


      Set<String> declNotUsed = Sets.difference(declVars, usedVars);
      Set<String> declAndUsed = Sets.intersection(declVars, usedVars);

      if(!declNotUsed.isEmpty()){
        node.setLabel(node.getLabel()+"_UNUSED_VAR");
      }

      usedVars.removeAll(declAndUsed);

    }

    if(node.containsOption(OptionKeys.VARS)){

      usedVars.addAll(
          node.getOption(OptionKeys.VARS)
      );

    }



  }

  @Override
  public void reset() {
    usedVars = new HashSet<>();
  }
}
