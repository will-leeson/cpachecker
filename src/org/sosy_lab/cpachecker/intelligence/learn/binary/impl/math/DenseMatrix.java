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

public class DenseMatrix implements Matrix {

  private double[][] data;
  private int rows;
  private int cols;

  public DenseMatrix(int pRows, int pCols, double fill){

    rows = pRows;
    cols = pCols;
    data = new double[rows][cols];

    if(fill != 0){
      for(int i = 0; i < data.length; i++)
        for(int j = 0; j < data[i].length; j++)
          data[i][j] = fill;
    }

  }

  public DenseMatrix(int pRows, int pCols){
    this(pRows, pCols, 0.0);
  }

  @Override
  public void set(int x, int y, double d){
    if(x >= rows || x < 0 || y < 0 || y >= cols)
      throw new IllegalArgumentException(String.format("(%d, %d) is out of shape (%d, %d)", x, y, rows, cols));
    data[x][y] = d;
  }

  @Override
  public double get(int x, int y){
    if(x >= rows || x < 0 || y < 0 || y >= cols)
      throw new IllegalArgumentException(String.format("(%d, %d) is out of shape (%d, %d)", x, y, rows, cols));
    return data[x][y];
  }

  @Override
  public int getCols() {
    return cols;
  }

  @Override
  public int getRows(){
    return rows;
  }

  @Override
  public DenseMatrix transpose(){
    DenseMatrix matrix = new DenseMatrix(cols, rows);

    for(int i = 0; i < rows; i++)
      for(int j = 0; j < cols; j++)
        matrix.set(j, i, this.get(i, j));

      return matrix;
  }

  @Override
  public Matrix sliceX(int begin, int end) {
    if(begin < 0 || end >= getRows())
      throw new IllegalArgumentException(String.format("Slice [%d : %d] is out of bounds 0-%d", begin, end, getRows()));
    return new SlicedXDenseMatrix(begin, end, this);
  }

  @Override
  public Matrix sliceY(int begin, int end) {
    if(begin < 0 || end >= getCols())
      throw new IllegalArgumentException(String.format("Slice [%d : %d] is out of bounds 0-%d", begin, end, getCols()));
    return new SlicedYDenseMatrix(begin, end, this);
  }

  @Override
  public Iterator<DenseRow> iterator() {
    DenseMatrix m = this;
    return new Iterator<DenseRow>() {
      int i = 0;

      @Override
      public boolean hasNext() {
        return i < rows;
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
    DenseMatrix m = this;
    return new Iterator<DenseCol>() {
      int i = 0;

      @Override
      public boolean hasNext() {
        return i < cols;
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
