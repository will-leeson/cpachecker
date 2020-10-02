// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public interface TargetReportingAlgorithm extends ProgressReportingAlgorithm {

  public Set<CFAEdge> getProgressedTargets();

}
