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
package org.sosy_lab.cpachecker.intelligence.oracle.predictor;

import java.lang.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.StringJoiner;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.nio.charset.Charset;
import java.net.URL;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.intelligence.learn.IRankLearner;
import org.sosy_lab.cpachecker.intelligence.learn.RPCLearner;
import org.sosy_lab.cpachecker.intelligence.learn.binary.LinearPretrainedType;
import org.sosy_lab.cpachecker.intelligence.learn.binary.PredictorBatchBuilder;
import org.sosy_lab.cpachecker.intelligence.learn.binary.exception.IncompleteConfigurationException;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.oracle.OracleStatistics;

@Options(prefix="GNNPredictor")
public class GNNLabelPredictor implements IOracleLabelPredictor {

  @Option(secure = true,
      description = "pretrained parameter of GNN")
  private String pretrained = null;

  private LogManager logger;
  private OracleStatistics statistics = new OracleStatistics(getName()+" Oracle");
  private IProgramSample currentSample;


  public GNNLabelPredictor(
      LogManager pLogger,
      Configuration pConfiguration,
      IProgramSample pCurrentSample) throws InvalidConfigurationException {
    pConfiguration.inject(this);
    logger = pLogger;
    currentSample = pCurrentSample;
  }

  @Override
  public String getName() {
    return "GNN";
  }

  @Override
  public List<String> ranking(String program) {
    statistics.reset();

    program = program.substring(1, program.length()-1);
    String fileLocation = getClass().getProtectionDomain().getCodeSource().getLocation().toString().substring(5);
    
    logger.log(Level.INFO, "Working Directory = " + System.getProperty("user.dir"));
    logger.log(Level.INFO, "python3", fileLocation+"../scripts/gnn/graves.py", program, fileLocation+"../scripts/gnn/model.pt");

    var pb = new ProcessBuilder("python3", fileLocation+"../scripts/gnn/graves.py", program, fileLocation+"../scripts/gnn/model.pt");
    String result = "";
    try{
      Process p = pb.start();
      final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      StringJoiner sj = new StringJoiner(System.getProperty("line.separator"));
      reader.lines().iterator().forEachRemaining(sj::add);
      result = sj.toString();
      var ret = p.waitFor();
      logger.log(Level.INFO, "Done ", ret);
    }
    catch (IOException | InterruptedException iE){
      logger.log(Level.WARNING, iE, "Use random sequence");
      return new ArrayList<>();
    }
    
    List<String> out = Arrays.asList(result.split("\n"));

    logger.log(Level.INFO, "Predicted ranking: "+out.toString());

    statistics.setOrder(out);
    statistics.stopTime();
    return out;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(statistics);
  }
}
