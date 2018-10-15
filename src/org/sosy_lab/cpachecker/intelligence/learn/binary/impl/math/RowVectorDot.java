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
package org.sosy_lab.cpachecker.intelligence.learn.binary.impl.math;

import java.util.concurrent.Callable;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.math.SparseVector.SparseEntry;

public class RowVectorDot implements Callable<Double> {

  private DenseRow row;
  private Vector vector;

  RowVectorDot(
      DenseRow pRow,
      Vector pVector) {
    row = pRow;
    vector = pVector;
  }

  private double dotDense(){
    double d = 0.0;

    for(int i = 0; i < row.getCols(); i++)
      d += row.get(i)*vector.get(i);

    return d;
  }

  private double dotSparse(){
    double d = 0.0;

    for(SparseEntry e: ((SparseVector)vector).data()){
      d += e.getVal()*row.get(e.getPos());
    }

    return d;
  }


  @Override
  public Double call() throws Exception {
    if(row.getCols() != vector.getDim())
      throw new IllegalArgumentException("Columns and Dims have to be equal ("+row.getCols()+", "+vector.getDim()+")");

    if(vector instanceof SparseVector){
      return dotSparse();
    }else{
      return dotDense();
    }

  }
}
