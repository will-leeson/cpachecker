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
package org.sosy_lab.cpachecker.intelligence.util;


import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathFinder {


  public static Path find(String template, String search){

    URL url = PathFinder.class.getClassLoader().getResource(search);

    if(url != null) {
      try {
        return Paths.get(url.toURI());
      } catch (URISyntaxException pE) {
      }
    }

    final String fileName = String.format(template, search);

    Path file = Paths.get(fileName);

    // look in current directory first
    if (Files.exists(file)) {
      return file;
    }

    // look relative to code location second
    Path codeLocation;
    try {
      codeLocation =
          Paths.get(
              PathFinder.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    } catch (SecurityException | URISyntaxException e) {
      System.err.println(
          "Cannot resolve paths relative to project directory of CPAchecker: " + e.getMessage());
      return null;
    }
    file = codeLocation.resolveSibling(fileName);
    if (Files.exists(file)) {
      return file;
    }

    return null;


  }

}
