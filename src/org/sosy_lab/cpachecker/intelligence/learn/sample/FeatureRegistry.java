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
package org.sosy_lab.cpachecker.intelligence.learn.sample;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FeatureRegistry {

  private int lastIndex = 0;
  private Map<String, IFeature> featureIndex = new HashMap<>();

  public IFeature index(String s){
    if(!featureIndex.containsKey(s)){
      featureIndex.put(s, new InnerFeature(lastIndex++, s));
    }
    return featureIndex.get(s);
  }


  private static class InnerFeature implements IFeature{
    int id;
    String feature;

    public InnerFeature(int pId, String pFeature) {
      id = pId;
      feature = pFeature;
    }

    @Override
    public String getFeatureName() {
      return feature;
    }

    @Override
    public int getFeatureId() {
      return id;
    }


    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      InnerFeature that = (InnerFeature) pO;
      return id == that.id &&
          Objects.equals(feature, that.feature);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, feature);
    }

    @Override
    public String toString(){
      return feature;
    }

  }

}
