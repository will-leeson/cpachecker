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
package org.sosy_lab.cpachecker.intelligence.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GNode {

  private String id;
  private String label;
  private Options typedOptions = new Options();

  public GNode(String pId, String pLabel) {
    id = pId;
    label = pLabel;
  }

  public String getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String pLabel){
    this.label = pLabel;
  }

  public <T> void setOption(Options.Key<T> key, T option){
    typedOptions.put(key, option);
  }

  public <T> T getOption(Options.Key<T> key){
    return typedOptions.get(key);
  }

  public <T> boolean containsOption(Options.Key<T> key){
    return getOption(key) != null;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    GNode gNode = (GNode) pO;
    return Objects.equals(id, gNode.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }


}
