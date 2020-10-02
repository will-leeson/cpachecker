// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition.learn;

import java.util.List;

public interface IPerformanceModel {

  public boolean isReady();

  public void nextRound();

  public void learnPath(int algorithmId, List<String> pathTokens);

  public List<Double> predictPerformance(List<String> pathTokens);

}
