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

import java.util.Iterator;

public class DenseCol implements Iterable<Double> {

  private Matrix parent;
  private int coordY;

  DenseCol(Matrix pDenseMatrix, int x) {
    this.parent = pDenseMatrix;
    this.coordY = x;
  }

  public double get(int row) {
    return parent.get(row, coordY);
  }

  public void set(int row, double d) {
    parent.set(row, coordY, d);
  }

  public int getRows() {
    return parent.getRows();
  }


  @Override
  public Iterator<Double> iterator() {
    DenseCol col = this;
    return new Iterator<Double>() {
      int i = 0;

      @Override
      public boolean hasNext() {
        return i < col.getRows();
      }

      @Override
      public Double next() {
        return Double.valueOf(col.get(i++));
      }
    };
  }
}
