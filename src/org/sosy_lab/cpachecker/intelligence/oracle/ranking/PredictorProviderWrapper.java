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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.intelligence.oracle.predictor.IOracleLabelPredictor;

public class PredictorProviderWrapper implements IRankingProvider {

  private LogManager logger;

  private ProviderStatus status = ProviderStatus.INIT;

  public PredictorProviderWrapper(
      LogManager pLogger,
      IOracleLabelPredictor pPredictor) {
    logger = pLogger;
    predictor = pPredictor;
  }

  private IOracleLabelPredictor predictor;
  private List<String> predictorResult;
  private Iterator<String> iterator;


  @Override
  public String queryLabel() {
    if(getStatus() == ProviderStatus.RESULT_AVAILABLE) {
      if(iterator.hasNext()) {
        return  iterator.next();
      }
    }
    return null;
  }

  @Override
  public boolean reset() {
    if(getStatus() == ProviderStatus.RESULT_AVAILABLE) {
      iterator = predictorResult.iterator();
      return true;
    }
    return false;
  }

  @Override
  public ProviderStatus getStatus() {
    return status;
  }

  @Override
  public void initRanking(Consumer<IRankingProvider> callback) {

    status = ProviderStatus.PROCESSING;

    try{
      predictorResult = predictor.ranking();
    }catch (Exception e){
      logger.log(Level.WARNING, "Failed to predict a ranking ( "+predictor.getName()+" )", e);
    }

    if(predictorResult == null){
      predictorResult = new ArrayList<>();
    }

    if(predictorResult.isEmpty()){
      status = ProviderStatus.FAILED;
    }else{
      status = ProviderStatus.RESULT_AVAILABLE;
      reset();
    }

    callback.accept(this);

  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    this.predictor.collectStatistics(statsCollection);
  }
}
