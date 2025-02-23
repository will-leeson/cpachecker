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
package org.sosy_lab.cpachecker.intelligence.oracle;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

public class OracleStatistics implements Statistics {

  private String oracleName;
  private Timer timer = new Timer();

  private List<String> order = new ArrayList<>();

  public OracleStatistics(String pOracleName) {
    oracleName = pOracleName;
    timer.start();
  }

  public void setOrder(List<String> pOrder) {
    order = pOrder;
  }

  public void stopTime(){
    timer.stopIfRunning();
  }

  public void reset(){
    this.order = new ArrayList<>();
    timer = new Timer();
    timer.start();
  }

  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {

    out.println("Number of algorithms provided:    " + order.size());
    out.println("Predicted sequence:     "+order.toString());
    out.println("Total time for prediction : " + timer);

  }

  @Nullable
  @Override
  public String getName() {
    return oracleName;
  }
}
