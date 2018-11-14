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
package org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.impl;

import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.EventuallyOperator;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.HCondition;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.HOperator;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.NextOperator;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.RankHeuristic;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.RankHeuristicOperation;

public class CegarHeuristic implements RankHeuristic {
  @Override
  public Set<RankHeuristicOperation> apply(
      int time, int precision, String label, Set<String> seen) {

    Set<RankHeuristicOperation> ops = new HashSet<>();
    if(label.equalsIgnoreCase("VA-NoCegar")){
      ops.add(new NextCegarOperation(time));
    }

    if(label.equalsIgnoreCase("VA-Cegar") && !seen.contains("VA-NoCegar")){
      ops.add(new EventuallyNoCegarOperation());
    }

    return ops;
  }


  private class EventuallyNoCegarOperation implements RankHeuristicOperation{

    @Override
    public HOperator getHeuristicOperator() {
      EventuallyOperator op = new EventuallyOperator();
      op.setConditon(new HCondition() {
        @Override
        public boolean satisfied(int time, int precision, String label) {
          return label.equalsIgnoreCase("VA-NoCegar");
        }
      });
      return op;
    }

    @Override
    public String apply(int time, int precision, String current, Set<String> seen) {
      return null;
    }
  }


  private class NextCegarOperation implements RankHeuristicOperation{

    public NextCegarOperation(int pCurrentTime) {
      operator = new NextOperator(pCurrentTime);
    }

    private NextOperator operator;


    @Override
    public HOperator getHeuristicOperator() {
      return operator;
    }

    @Override
    public String apply(int time, int precision, String current, Set<String> seen) {

      if(!seen.contains("VA-Cegar")){
        return "VA-Cegar";
      }

      return current;
    }
  }
}
