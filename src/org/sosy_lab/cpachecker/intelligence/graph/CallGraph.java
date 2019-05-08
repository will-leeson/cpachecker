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
package org.sosy_lab.cpachecker.intelligence.graph;

import org.sosy_lab.cpachecker.intelligence.graph.Options.Key;

public class CallGraph extends StructureGraph {

  private final Key<SVGraph> BODY_KEY = new Key<>("function_body");

  public boolean addFunction(String id, SVGraph pGraph){
      super.addNode(id, id);
      GNode node = super.getNode(id);
      node.setOption(BODY_KEY, pGraph);
      return true;
  }

  public SVGraph getFunctionBody(String id){
    GNode node = super.getNode(id);

    if(node != null){
      return node.getOption(BODY_KEY);
    }

    return null;
  }

  public boolean addCall(String source_function, String target_function){
    return super.addEdge(new CallEdge(
        super.getNode(source_function), super.getNode(target_function)
    ));
  }

  private class CallEdge extends GEdge {

    public CallEdge(GNode pSource, GNode pSink) {
      super("call", pSource, pSink);
    }
  }

}
