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
import java.util.NoSuchElementException;
import org.jblas.DoubleMatrix;
import org.jblas.ranges.RangeUtils;

public class BLASMatrix implements Matrix {

  private DoubleMatrix matrix;

  BLASMatrix(int pRows, int pCols, double fill){
    matrix = new DoubleMatrix(pRows, pCols);

    if(fill != 0)
      for(int i = 0; i < pRows; i++)
        for(int j = 0; j < pCols; j++)
          matrix.put(i, j, fill);

  }

  public BLASMatrix(int pRows, int pCols){
    this(pRows, pCols, 0.0);
  }

  BLASMatrix(DoubleMatrix m){
    this.matrix = m;
  }

  @Override
  public void set(int x, int y, double d) {
    matrix.put(x, y, d);
  }

  @Override
  public double get(int x, int y) {
    return matrix.get(x, y);
  }

  @Override
  public int getCols() {
    return matrix.columns;
  }

  @Override
  public int getRows() {
    return matrix.rows;
  }

  @Override
  public Matrix transpose() {
    return new BLASMatrix(matrix.transpose());
  }

  @Override
  public Matrix sliceX(int begin, int end) {
    return new BLASMatrix(matrix.getRows(RangeUtils.interval(begin, end)));
  }

  @Override
  public Matrix sliceY(int begin, int end) {
    return new BLASMatrix(matrix.getColumns(RangeUtils.interval(begin, end)));
  }

  @Override
  public Iterator<DenseRow> iterator() {
    Matrix m = this;
    return new Iterator<DenseRow>() {
      int i = 0;

      @Override
      public boolean hasNext() {
        return i < getRows();
      }

      @Override
      public DenseRow next() {
        if(!hasNext())
          throw new NoSuchElementException();
        return new DenseRow(m, i++);
      }
    };
  }

  @Override
  public Iterator<DenseCol> columnIterator(){
    Matrix m = this;
    return new Iterator<DenseCol>() {
      int i = 0;

      @Override
      public boolean hasNext() {
        return i < getCols();
      }

      @Override
      public DenseCol next() {
        if(!hasNext())
          throw new NoSuchElementException();
        return new DenseCol(m, i++);
      }
    };
  }

  public DoubleMatrix toNative(){
    return this.matrix;
  }

}
