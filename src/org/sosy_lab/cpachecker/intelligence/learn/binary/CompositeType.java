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
package org.sosy_lab.cpachecker.intelligence.learn.binary;

import com.google.common.collect.Table;

public class CompositeType implements IBinaryPredictorType {

  private IBinaryPredictorType defaultType;
  private Table<String, String, IBinaryPredictorType> definedTypes;

  CompositeType(
      IBinaryPredictorType pDefaultType,
      Table<String, String, IBinaryPredictorType> pDefinedTypes) {
    defaultType = pDefaultType;
    definedTypes = pDefinedTypes;
  }

  @Override
  public IBinaryPredictor instantiate(String label1, String label2) {
    if(definedTypes.contains(label1, label2)){
      return definedTypes.get(label1, label2).instantiate(label1, label2);
    }
    return defaultType.instantiate(label1, label2);
  }
}
