// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.learning;

import com.google.common.collect.MapMaker;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;


//TODO This is not good solution
//However, I don't know currently how I should propagate the state up to the analysis
//Maybe save and load it from disk?

public class CEXRegistry {

  private static ConcurrentMap<String, CounterexampleStatistics> registry = new MapMaker()
                                                                                .weakValues()
                                                                                .makeMap();

  public static void register(String key, CounterexampleStatistics pCounterexampleStatistics){
    registry.putIfAbsent(key, pCounterexampleStatistics);
  }

  public static boolean containsKey(String key){
    return registry.containsKey(key);
  }

  public static Optional<CounterexampleStatistics> loadIfPresent(String key){
    try{
      return Optional.ofNullable(registry.get(key));
    } catch (Exception e){
      return Optional.empty();
    }
  }

}
