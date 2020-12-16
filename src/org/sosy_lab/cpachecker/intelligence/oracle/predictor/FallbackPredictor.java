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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.configuration.*;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.intelligence.util.PathFinder;

@Options(prefix="fallbackPredictor")
public class FallbackPredictor implements IOracleLabelPredictor {

  @Option(secure = true,
          description = "Configurations to fallback to")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<AnnotatedValue<Path>> components = new ArrayList<>();

  public FallbackPredictor(Configuration pConfiguration) throws InvalidConfigurationException {
    pConfiguration.inject(this);
  }

  @Override
  public String getName() {
    return "Fallback";
  }

  @Override
  public List<String> ranking(String program) {

    List<String> out = new ArrayList<>();

    for(AnnotatedValue<Path> c : components) {
      Path path = PathFinder.find("%s", c.value().toString());
      if (path != null && Files.exists(path)) {
        out.add(path.toString());
      }
    }

    return out;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
  }
}
