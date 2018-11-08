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
package org.sosy_lab.cpachecker.intelligence.oracle.ranking;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.intelligence.oracle.OracleStatistics;

public class DefaultProvider implements IRankingProvider {

  private static final String[] DEFAULT_RANKING = new String[]{
      "VA-NoCegar", "VA-Cegar", "PA", "KI", "BAM"
  };

  private Iterator<String> iterator = Arrays.asList(DEFAULT_RANKING).iterator();

  @Override
  public String queryLabel() {
    if(iterator.hasNext()) {
      return  iterator.next();
    }
    return null;
  }

  @Override
  public boolean reset() {
    iterator = Arrays.asList(DEFAULT_RANKING).iterator();
    return true;
  }

  @Override
  public ProviderStatus getStatus() {
    return ProviderStatus.RESULT_AVAILABLE;
  }

  @Override
  public void initRanking(Consumer<IRankingProvider> callback) {
    callback.accept(this);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    OracleStatistics statistics = new OracleStatistics("Default oracle");
    statistics.stopTime();
    statistics.setOrder(Arrays.asList(DEFAULT_RANKING));
    statsCollection.add(statistics);
  }
}
