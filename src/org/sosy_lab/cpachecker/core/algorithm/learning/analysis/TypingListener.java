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
import java.util.stream.Stream;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.cpachecker.core.algorithm.composition.learn.IAnalysisListener;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;
import org.sosy_lab.cpachecker.intelligence.graph.model.StructureGraph;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.SVGraph;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

public class TypingListener implements IAnalysisListener {

  private VariableClassification classification;

  public TypingListener(VariableClassification pVariableClassification){
    this.classification = pVariableClassification;
  }

  @Override
  public void listen(GEdge pGEdge) {

    GNode node = pGEdge.getSource();
    Set<String> handledVariables = new HashSet<>();

    if(node.containsOption(OptionKeys.DECL_VARS)){
      Set<String> declVars = node.getOption(OptionKeys.DECL_VARS);
      handledVariables.addAll(declVars);
    }

    if(node.containsOption(OptionKeys.VARS)){
        Set<String> usedVars = node.getOption(OptionKeys.VARS);
        handledVariables.addAll(usedVars);
    }

    handledVariables = Sets.intersection(handledVariables, classification.getRelevantVariables());

    if(handledVariables.isEmpty())return;

    Set<String> annotation = new HashSet<>();

    if(classification.getIntBoolVars().containsAll(handledVariables)){
      annotation.add("BOOL");
    }

    if(!Sets.intersection(handledVariables, classification.getAddressedVariables()).isEmpty()){
      annotation.add("ALIAS");
    }

    if(node.containsOption(OptionKeys.AST)){

      StructureGraph ast = node.getOption(OptionKeys.AST);

      Stream<String> labels = ast.nodes().stream().map(name -> ast.getNode(name).getLabel());

      if(labels.filter(label -> label.contains("ARRAY")).findAny().isPresent()){
        annotation.add("ARRAY");
      }

      labels = ast.nodes().stream().map(name -> ast.getNode(name).getLabel());

      if(labels.filter(label -> label.contains("FLOAT")).findAny().isPresent()){
        annotation.add("FLOAT");
      }

      labels = ast.nodes().stream().map(name -> ast.getNode(name).getLabel());

      if(labels.filter(label -> label.contains("POINTER")).findAny().isPresent()){
        annotation.add("POINTER");
      }
    }

    if(!annotation.isEmpty()){
      String extention = String.join("_", annotation);
      node.setLabel(node.getLabel()+"_"+extention);
    }

  }

  @Override
  public void reset() {

  }
}
