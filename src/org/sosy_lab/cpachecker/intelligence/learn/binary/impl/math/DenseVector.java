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

import java.util.Arrays;
import java.util.Iterator;

public class DenseVector implements Vector {

  private double[] data;
  private int dim;

  public DenseVector(int pDim, double fill){
    data = new double[pDim];
    dim = pDim;

    if(fill != 0){
      for(int i = 0; i < dim; i++)
        data[i] = fill;
    }

  }

  public DenseVector(int pDim){
    this(pDim, 0);
  }

  @Override
  public void set(int x, double d){
    if(x < 0 || x >= dim)
      throw new IllegalArgumentException(String.format("%d is out of dimension %d", x, dim));
    data[x] = d;
  }

  @Override
  public double get(int x){
    if(x < 0 || x >= dim)
      throw new IllegalArgumentException(String.format("%d is out of dimension %d", x, dim));
    return data[x];
  }

  @Override
  public int getDim(){
    return dim;
  }

  @Override
  public DenseMatrix toMatrix(){
    DenseMatrix matrix = new DenseMatrix(dim, 1);
    for(int i = 0; i < dim; i++)
      matrix.set(i, 0, this.get(i));
    return matrix;
  }

  @Override
  public double[] toArray(){
    return Arrays.copyOf(data, data.length);
  }


  @Override
  public Iterator<Double> iterator() {
    DenseVector vector = this;
    return new Iterator<Double>() {
      int i = 0;
      @Override
      public boolean hasNext() {
        return i < vector.getDim();
      }

      @Override
      public Double next() {
        return Double.valueOf(vector.get(i++));
      }
    };
  }
}
