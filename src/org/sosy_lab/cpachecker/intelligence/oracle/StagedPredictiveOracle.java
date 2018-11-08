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
package org.sosy_lab.cpachecker.intelligence.oracle;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;

@Options(prefix="stagedOracle")
public class StagedPredictiveOracle implements IConfigOracle{

  @Option(
      secure = true,
      description = "Number of analysis allowed to run till stage 2"
  )
  private int prerun = -1;


  private Set<String> seen = new HashSet<>();

  private boolean secondInit = false;
  private boolean secondFinish = false;

  private IConfigOracle first;
  private IConfigOracle second;

  StagedPredictiveOracle(
      Configuration pConfiguration,
      IConfigOracle pFirst,
      IConfigOracle pSecond) throws InvalidConfigurationException {

    pConfiguration.inject(this);

    first = pFirst;
    second = pSecond;
  }

  private void initProcess(){
      if(secondInit)return;
      StagedPredictiveOracle oracle = this;
      new Thread(new Runnable() {
        @Override
        public void run() {
          second.precomputeOracle(oracle::oracleCallback);
        }
      }).start();
      secondInit = true;
  }

  private void oracleCallback(IConfigOracle oracle){
    if(oracle == second){
      secondFinish = true;
    }
    synchronized (this) {
      this.notifyAll();
    }
  }

  private IConfigOracle getCurrent(){
    initProcess();
    if(prerun != -1 && seen.size() >= prerun && !secondFinish){
      synchronized (this) {
        while (!secondFinish) {
          try {
            this.wait();
          } catch (InterruptedException pE) {
          }
        }
      }
    }

    if(secondFinish){
      return second;
    }else{
      return first;
    }
  }


  @Override
  public AnnotatedValue<Path> peek() {
    IConfigOracle oracle = getCurrent();
    if(hasNext()){
      return oracle.peek();
    }
    throw new NoSuchElementException();
  }

  @Override
  public boolean hasNext() {

    IConfigOracle oracle = getCurrent();

    while(oracle.hasNext() && seen.contains(oracle.peek().value().toString())){
      oracle.next();
    }

    return oracle.hasNext();
  }

  @Override
  public AnnotatedValue<Path> next() {
    IConfigOracle oracle = getCurrent();
    if(hasNext()){
      AnnotatedValue<Path> n = oracle.next();
      seen.add(n.value().toString());
      return n;
    }
    throw new NoSuchElementException();
  }

  @Override
  public void remove() {
    throw new IllegalStateException();
  }


  @Override
  public void precomputeOracle(Consumer<IConfigOracle> callback) {
    initProcess();
    callback.accept(this);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    first.collectStatistics(statsCollection);
    second.collectStatistics(statsCollection);
  }
}
