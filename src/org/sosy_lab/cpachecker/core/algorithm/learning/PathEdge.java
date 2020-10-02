// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.learning;

import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;

public class PathEdge extends GEdge {

  private int edgeID;

  public PathEdge(
      int pEdgeID,
      GNode pSource,
      GNode pSink) {
    super("path_"+pEdgeID, pSource, pSink);
    edgeID = pEdgeID;
  }

  public int getEdgeID() {
    return edgeID;
  }
}
