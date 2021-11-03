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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.intelligence.learn.sample.SampleRegistry;
import org.sosy_lab.cpachecker.intelligence.oracle.predictor.IOracleLabelPredictor;
import org.sosy_lab.cpachecker.intelligence.oracle.predictor.PredictiveOracle;
import org.sosy_lab.cpachecker.intelligence.oracle.predictor.PredictorFactory;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.ManagedOracle;
import org.sosy_lab.cpachecker.intelligence.oracle.ranking.PackedPredictorFactory;

public class OracleFactory {

    private static OracleFactory instance;

    public static OracleFactory getInstance(){
        if(instance == null){
          instance = new OracleFactory();
        }
        return instance;
    }

    private static String mapConfigHeuristically(AnnotatedValue<Path> pConfig){

      Map<String, String> keyWords = Map.of(
          "valueAnalysis", "VA-NoCegar",
          "valueAnalysis-itp", "VA-Cegar",
          "predicateAnalysis", "PA",
          "kInduction", "KI",
          "bmc", "BMC",
          "symbolicExecution", "SymEx"
      );

      String fileName = pConfig.value().getFileName().toString();
      String longestMatch = "";
      String matchedName = "UNKNOWN";

      for(Entry<String, String> e : keyWords.entrySet()){
        if(fileName.contains(e.getKey()) && e.getKey().length() > longestMatch.length()){
          longestMatch = e.getKey();
          matchedName = e.getValue();
        }
      }

      int timelimit;
      String timeLimitStr = fileName.substring(
          fileName.indexOf(longestMatch) + longestMatch.length() + 1
      );
      timeLimitStr = timeLimitStr.replace(".properties", "");

      try{
        timelimit = Integer.parseInt(timeLimitStr);
      } catch (NumberFormatException e){
        timelimit = -1;
      }

      if(timelimit > 0){
        matchedName = matchedName+timelimit;
      }

      return matchedName;
    }


    public static Map<String, AnnotatedValue<Path>> initLabelToPath(List<AnnotatedValue<Path>> list){
      //TODO: Support loading

      Map<String, AnnotatedValue<Path>> labelToPath = new HashMap<>();

      for(AnnotatedValue<Path> config : list){
        String key = mapConfigHeuristically(config);

        if(!key.equalsIgnoreCase("UNKNOWN"))
          labelToPath.put(
              mapConfigHeuristically(config), config
          );
      }

      Map<String, String> revLabel = new HashMap<>();
      revLabel.put("pesco19--01-valueAnalysis.properties", "VA-NoCegar");
      revLabel.put("pesco19--02-valueAnalysis-itp.properties", "VA-Cegar");
      revLabel.put("pesco19--03-predicateAnalysis.properties", "PA");
      revLabel.put("pesco19--04-kInduction.properties", "KI");
      //revLabel.put("pesco19--recursion.properties", "BAM");
      revLabel.put("pesco19--bmc.properties", "BMC");
      revLabel.put("pesco20--kInduction-90.properties", "KI90");

      for(AnnotatedValue<Path> p: list){

        String n = p.value().getFileName().toString();

        if(!revLabel.containsKey(n)){
          continue;
        }

        labelToPath.put(revLabel.get(n), p);
      }

      return labelToPath;
    }

    private OracleFactory(){

    }

    public IConfigOracle create(String oracle, LogManager logger, Configuration config,
                                ShutdownNotifier pShutdownNotifier,
                                List<AnnotatedValue<Path>> configPaths,
                                SampleRegistry pSampleRegistry, CFA pCFA, String program)
        throws InvalidConfigurationException {

      if(oracle.equalsIgnoreCase("managed")){

        PackedPredictorFactory factory = new PackedPredictorFactory(
            logger, config, pShutdownNotifier, configPaths, pSampleRegistry, pCFA, PredictorFactory.getInstance()
        );
        return new ManagedOracle(config, factory, program);

      }

      IOracleLabelPredictor predictor = PredictorFactory.getInstance().create(
          oracle, logger, config, pShutdownNotifier, configPaths, pSampleRegistry, pCFA
      );

      if(predictor != null){
        return new PredictiveOracle(logger, config, predictor, configPaths);
      }

      return new DefaultOracle(configPaths);
    }


}
