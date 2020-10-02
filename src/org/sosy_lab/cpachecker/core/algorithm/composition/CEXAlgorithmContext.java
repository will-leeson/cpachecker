// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition;

import java.nio.file.Path;
import java.util.Optional;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.cpachecker.core.algorithm.learning.CEXRegistry;
import org.sosy_lab.cpachecker.core.algorithm.learning.CounterexampleStatistics;

public class CEXAlgorithmContext extends AlgorithmContext {

  private Optional<String> globalKey;
  private CounterexampleStatistics statistics;

  public CEXAlgorithmContext(AnnotatedValue<Path> pConfigFile, String pGlobalKey) {
    super(pConfigFile);
    this.globalKey = Optional.ofNullable(pGlobalKey);
  }

  public CounterexampleStatistics getCEXStatistics() {

    if(statistics == null && globalKey.isPresent()){
      String key = globalKey.get();

      Optional<CounterexampleStatistics> loaded = CEXRegistry.loadIfPresent(key);

      if(loaded.isPresent()){
        statistics = loaded.get();
      }

    }

    return statistics;
  }

  public void setCEXStatistics(CounterexampleStatistics pStatistics) {
    statistics = pStatistics;
  }


}
