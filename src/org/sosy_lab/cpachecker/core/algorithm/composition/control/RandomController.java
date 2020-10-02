// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jblas.util.Random;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class RandomController implements ICompositionController {

  private List<String> identifiers;
  private List<Integer> timeDistribution;

  public RandomController(
      Collection<String> pIdentifiers,
      Collection<Integer> pTimeDistribution) {
    identifiers = new ArrayList<>(pIdentifiers);
    timeDistribution = new ArrayList<>(pTimeDistribution);
  }

  @Override
  public void reportAlgorithmProgress(double progress, int timeConsumption) {

  }

  @Override
  public void reportCoverage(List<CFAEdge> coveredEdges) {

  }

  @Override
  public boolean hasNextControlAction() {
    return true;
  }

  @Override
  public AlgorithmControl nextControlAction() {

    int current = Random.nextInt(identifiers.size() * timeDistribution.size());

    int algorithm = current / timeDistribution.size();
    int time = current % timeDistribution.size();

    return new AlgorithmControl(
        identifiers.get(algorithm),
        timeDistribution.get(time)
    );
  }
}
