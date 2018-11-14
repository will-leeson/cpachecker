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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SparseVector implements Vector{

  private Map<Integer, Double> data = new HashMap<>();
  private int dim;

  public SparseVector(int pDim) {
    dim = pDim;
  }


  @Override
  public void set(int x, double d){
    if(x < 0 || x >= dim)
      throw new IllegalArgumentException(String.format("%d is out of dimension %d", x, dim));
    data.put(x, d);
  }

  @Override
  public double get(int x){
    if(x < 0 || x >= dim)
      throw new IllegalArgumentException(String.format("%d is out of dimension %d", x, dim));
    if(!data.containsKey(x))
      return 0.0;
    return data.get(x);
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
    double[] o = new double[dim];

    for(int i = 0; i < dim; i++)
      o[i] = this.get(i);

    return o;
  }


  @Override
  public Iterator<Double> iterator() {
    return this.data.values().iterator();
  }

  public Set<SparseEntry> data(){
    return data.entrySet().stream().map(e -> new SparseEntry(e.getKey(), e.getValue())).collect(
        Collectors.toSet());
  }



  public class SparseEntry{

    private int pos;
    private double val;

    public SparseEntry(int pPos, double pVal) {
      pos = pPos;
      val = pVal;
    }

    public int getPos() {
      return pos;
    }

    public void setPos(int pPos) {
      pos = pPos;
    }

    public double getVal() {
      return val;
    }

    public void setVal(double pVal) {
      val = pVal;
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      SparseEntry that = (SparseEntry) pO;
      return pos == that.pos &&
          Double.compare(that.val, val) == 0;
    }

    @Override
    public int hashCode() {
      return Objects.hash(pos, val);
    }

  }


}
