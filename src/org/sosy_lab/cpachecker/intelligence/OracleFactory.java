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
package org.sosy_lab.cpachecker.intelligence;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.SampleRegistry;

public class OracleFactory {

    private static OracleFactory instance;

    public static OracleFactory getInstance(){
        if(instance == null){
          instance = new OracleFactory();
        }
        return instance;
    }

    private OracleFactory(){

    }

    public IConfigOracle create(String oracle, LogManager logger, Configuration config,
                                ShutdownNotifier pShutdownNotifier,
                                List<AnnotatedValue<Path>> configPaths,
                                SampleRegistry pSampleRegistry, CFA pCFA)
        throws InvalidConfigurationException {

      IProgramSample sample = pSampleRegistry.registerSample("testId", pCFA);

      if(oracle.equals("\"linear\"")){
        return new LinearPredictiveOracle(logger, config, pShutdownNotifier, configPaths, sample);
      }

      if(oracle.equals("\"jaccard\"")){
        return new JaccPredictiveOracle(logger, config, pShutdownNotifier, configPaths, sample, pSampleRegistry);
      }

      if(oracle.equals("\"staged\"")){
        return new StagedPredictiveOracle(
            config,
            new LinearPredictiveOracle(logger, config,pShutdownNotifier, configPaths, sample),
            new JaccPredictiveOracle(logger, config,pShutdownNotifier, configPaths, sample, pSampleRegistry)
        );
      }

      return new DefaultOracle(configPaths);
    }


}
