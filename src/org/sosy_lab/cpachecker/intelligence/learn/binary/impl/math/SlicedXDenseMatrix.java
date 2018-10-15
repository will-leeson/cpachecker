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

public class SlicedXDenseMatrix implements Matrix {

  private int beginSlice;
  private int endSlice;
  private Matrix matrix;

  public SlicedXDenseMatrix(
      int pBeginSlice,
      int pEndSlice,
      Matrix pMatrix) {
    beginSlice = pBeginSlice;
    endSlice = pEndSlice;
    matrix = pMatrix;
  }

  @Override
  public void set(int x, int y, double d) {
    if(x >= endSlice - beginSlice)
      throw new IllegalArgumentException(String.format("(%d, %d) is out of shape (%d, %d)", x, y, endSlice - beginSlice, matrix.getCols()));
    matrix.set(x + beginSlice, y, d);
  }

  @Override
  public double get(int x, int y) {
    if(x >= endSlice - beginSlice)
      throw new IllegalArgumentException(String.format("(%d, %d) is out of shape (%d, %d)", x, y, endSlice - beginSlice, matrix.getCols()));
    return matrix.get(x + beginSlice, y);
  }

  @Override
  public int getCols() {
    return matrix.getCols();
  }

  @Override
  public int getRows() {
    return endSlice - beginSlice;
  }

  @Override
  public Matrix transpose() {
    DenseMatrix m = new DenseMatrix(getCols(), getRows());

    for(int i = 0; i < getRows(); i++)
      for(int j = 0; j < getCols(); j++)
        m.set(j, i, this.get(i, j));

    return m;
  }

  @Override
  public Matrix sliceX(int begin, int end) {
    if(begin < 0 || end >= getRows())
      throw new IllegalArgumentException(String.format("Slice [%d : %d] is out of bounds 0-%d", begin, end, getRows()));
    return new SlicedXDenseMatrix(begin, end, matrix);
  }

  @Override
  public Matrix sliceY(int begin, int end) {
    if(begin < 0 || end >= getCols())
      throw new IllegalArgumentException(String.format("Slice [%d : %d] is out of bounds 0-%d", begin, end, getCols()));
    return new SlicedYDenseMatrix(begin, end, this);
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
}
