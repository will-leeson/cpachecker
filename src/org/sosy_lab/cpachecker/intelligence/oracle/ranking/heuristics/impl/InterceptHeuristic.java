// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.impl;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.EventuallyOperator;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.HOperator;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.NextOperator;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.RankHeuristic;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.RankHeuristicOperation;

public class InterceptHeuristic implements RankHeuristic {
  @Override
  public Set<RankHeuristicOperation> apply(
      int time, int precision, String label, Set<String> seen) {
    Set<RankHeuristicOperation> ops = new HashSet<>();

    if(time == 0){
      ops.add(new InterceptHeuristicOperator(time));
    }


    return ops;
  }

  private static class InterceptHeuristicOperator implements RankHeuristicOperation{

    private int timestep;
    private HOperator operator;

    public InterceptHeuristicOperator(int pTimestep){
      timestep = pTimestep;
    }

    @Override
    public HOperator getHeuristicOperator() {

      if(operator == null){
        operator = new NextOperator(timestep);
      }

      return operator;
    }

    @Override
    public String apply(int time, int precision, String current, Set<String> seen) {
        return "intercept-unknown";
    }
  }
}
