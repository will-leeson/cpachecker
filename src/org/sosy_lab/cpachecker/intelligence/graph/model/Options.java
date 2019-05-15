/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.intelligence.graph.model;

import java.util.HashMap;
import java.util.Map;

public class Options {

  private final Map<String, Object> map = new HashMap<>();

  public <T> T put(Key<T> key, T value){
    return (T) map.put(key.getName(), value);
  }

  public <T> T get(Key<T> key){
    return (T) map.get(key.getName());
  }


  public static class Key<T> {

    private final String name;

    public Key(String pName) {
      this.name = pName;
    }

    public String getName(){
      return name;
    }

  }
}
