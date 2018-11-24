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
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.HCondition;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.HOperator;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.NextOperator;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.RankHeuristic;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.heuristics.RankHeuristicOperation;

public class BAMHeuristic implements RankHeuristic {
  @Override
  public Set<RankHeuristicOperation> apply(
      int time, int precision, String label, Set<String> seen) {

    Set<RankHeuristicOperation> ops = new HashSet<>();

    if(time == -1){
      ops.add(new NextNoBAMOperator());
    }


    return ops;
  }

  private class NextNoBAMOperator implements RankHeuristicOperation{

    private NextOperator operator;

    private HOperator operator(){
      if(operator == null){
        operator = new NextOperator(-1);
        operator.setConditon(new HCondition() {
          @Override
          public boolean satisfied(int time, int precision, String label) {
            return time == 0 && label.equalsIgnoreCase("BAM");
          }
        });
      }
      return operator;
    }

    @Override
    public HOperator getHeuristicOperator() {
      return operator();
    }

    @Override
    public String apply(int time, int precision, String current, Set<String> seen) {
      return "VA-NoCegar";
    }
  }
}
