/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.intelligence.graph.analysis;

import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;
import org.sosy_lab.cpachecker.intelligence.graph.model.StructureGraph;

public class FunctionGraph extends StructureGraph {

  private int call_id = 0;

  public boolean addEdge(String source, String target, String pFuncCall){

    return super.addEdge(
        new CallEdge("call_"+(call_id++), pFuncCall, super.getNode(source), super.getNode(target))
    );

  }

  public class CallEdge extends GEdge {

    private String func_call;

    public CallEdge(String pID, String pFuncCall, GNode pSource, GNode pSink) {
      super(pID, pSource, pSink);
      this.func_call = pFuncCall;
    }

    public String getFunctionCall(){
      return func_call;
    }
  }

}
