// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition.control;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public interface ICompositionController {

  public void reportAlgorithmProgress(double progress, int timeConsumption);
  
  public void reportCoverage(List<CFAEdge> coveredEdges);

  public boolean hasNextControlAction();
  
  public AlgorithmControl nextControlAction();

}
