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
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;

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

    public IConfigOracle create(String oracle, LogManager logger, Configuration config, List<AnnotatedValue<Path>> configPaths, CFA pCFA)
        throws InvalidConfigurationException {

      if(oracle.equals("\"linear\"")){
        return new LinearPredictiveOracle(logger, config, configPaths, pCFA);
      }

      return new DefaultOracle(configPaths);
    }

}
