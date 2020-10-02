// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition.control;

public class AlgorithmControl {

  private String algorithmIdentifier;
  private int timeLimit;

  public AlgorithmControl(String pAlgorithmIdentifier, int pTimeLimit) {
    algorithmIdentifier = pAlgorithmIdentifier;
    timeLimit = pTimeLimit;
  }

  public String getAlgorithmIdentifier() {
    return algorithmIdentifier;
  }

  public int getTimeLimit() {
    return timeLimit;
  }


}
