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
package org.sosy_lab.cpachecker.intelligence.graph;

import java.util.Objects;

public class GEdge {

  private String id;
  private GNode source;
  private GNode sink;

  public GEdge(String pID, GNode pSource, GNode pSink) {
    source = pSource;
    sink = pSink;
    id = pID;
  }

  public String getId(){
    return id;
  }

  public GNode getSource() {
    return source;
  }

  public GNode getSink() {
    return sink;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    GEdge gEdge = (GEdge) pO;
    return Objects.equals(id, gEdge.id) &&
        Objects.equals(source, gEdge.source) &&
        Objects.equals(sink, gEdge.sink);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, source, sink);
  }


}
