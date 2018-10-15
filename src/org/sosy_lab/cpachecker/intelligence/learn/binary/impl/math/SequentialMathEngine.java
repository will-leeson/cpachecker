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
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.math.SparseVector.SparseEntry;

public class SequentialMathEngine implements MathEngine {
  @Override
  public Vector zeroVector(int dim) {
    return new DenseVector(dim);
  }

  @Override
  public Matrix zeros(int rows, int columns) {
    return new DenseMatrix(rows, columns);
  }

  @Override
  public DenseVector dot(
      Matrix m, Vector v) throws Exception {

    DenseVector o = new DenseVector(m.getRows());

    int i = 0;
    for(DenseRow row: m) {
        o.set(i++, new RowVectorDot(row, v).call());
    }

    return o;
  }

  private Matrix dotIterative(
      Matrix m, Matrix m2) throws Exception {

    DenseMatrix o = new DenseMatrix(m.getRows(), m2.getCols());

    int i = 0;
    int j;

    for(DenseRow row: m){
      Iterator<DenseCol> col = m2.columnIterator();
      j = 0;
      while(col.hasNext()){
        DenseCol c = col.next();
        o.set(i, j, new RowColDot(row, c).call());
        j++;
      }

      i++;
    }


    return o;
  }



  @Override
  public Matrix dot(
      Matrix m, Matrix m2) throws Exception {

    if(m.getCols() != m2.getRows())
      throw new IllegalArgumentException("Columns and rows have to be equal ("+m.getCols()+", "+m2.getRows()+")");

    int max = Math.max(m.getRows(), Math.max(m.getCols(), m2.getCols()));

    if(max < 1000){
      return dotIterative(m, m2);
    }


    Matrix o;

    int split = max/2;
    if(max == m.getRows()){
      o = this.stackV(this.dot(m.sliceX(0, split), m2), this.dot(m.sliceX(split + 1, m.getRows()-1), m2));
    }else if(max == m2.getCols()){
      o = this.stackH(this.dot(m, m2.sliceY(0, split)), this.dot(m, m2.sliceY(split+1, m2.getCols()-1)));
    }else{
      Matrix o1 = this.dot(
                         m.sliceY(0, split), m2.sliceX(0, split)
                      );
      Matrix o2 = this.dot(
          m.sliceY(split+1, m.getCols()), m2.sliceX(split+1, m2.getRows())
      );

      o = this.add(o1, o2);
    }

    return o;
  }

  @Override
  public Matrix add(
      Matrix m, Matrix m2) {
    if(m.getRows() != m2.getRows() || m.getCols() != m2.getCols())
      throw new IllegalArgumentException(String.format("Shape (%d, %d) != (%d, %d)!", m.getRows(), m.getCols(), m2.getRows(), m2.getCols()));

    Matrix o = new DenseMatrix(m.getRows(), m.getCols());

    for(int i = 0; i < m.getRows(); i++)
      for(int j = 0; j < m.getCols(); j++)
        o.set(i, j, m.get(i, j) + m2.get(i, j));

    return o;
  }

  @Override
  public Matrix mul(Matrix m, double d) {
    Matrix o = new DenseMatrix(m.getRows(), m.getCols());

    for(int i = 0; i < m.getRows(); i++)
      for(int j = 0; j < m.getCols(); j++)
        o.set(i, j, m.get(i, j) * d);

    return o;
  }

  @Override
  public DenseVector add(
      Vector v1, Vector v2) {

    if(v1.getDim() != v2.getDim())
      throw new IllegalArgumentException(String.format("Dimensions %d != %d", v1.getDim(), v2.getDim()));

    DenseVector o = new DenseVector(v1.getDim());

    for(int i = 0; i < v1.getDim(); i++)
      o.set(i, v1.get(i) + v2.get(i));

    return o;
  }

  private SparseVector mulSparse(SparseVector v, double d){
    SparseVector vector = new SparseVector(v.getDim());
    for(SparseEntry e: v.data())
      vector.set(e.getPos(), e.getVal()*d);

    return vector;
  }

  @Override
  public Vector mul(Vector v, double d) {
    if(v instanceof SparseVector)
      return mulSparse((SparseVector)v, d);

    DenseVector o = new DenseVector(v.getDim());

    for(int i = 0; i < v.getDim(); i++)
      o.set(i, v.get(i) * d);

    return o;
  }

  @Override
  public Matrix stackH(
      Matrix m, Matrix m2) {

    if(m.getRows() != m2.getRows())
      throw new IllegalArgumentException(String.format("Rows have to match: %d != %d", m.getRows(), m2.getRows()));

    Matrix o = new DenseMatrix(m.getRows(), m.getCols() + m2.getCols());

    for(int i = 0; i < o.getRows(); i++){
      for(int j = 0; j < o.getCols(); j++){
          double d = 0;
          if(j < m.getCols()){
            d = m.get(i, j);
          }else{
            d = m2.get(i, j - m.getCols());
          }
          o.set(i, j, d);
      }
    }

    return o;
  }

  @Override
  public Matrix stackV(
      Matrix m, Matrix m2) {
    if(m.getCols() != m2.getCols())
      throw new IllegalArgumentException(String.format("Columns have to match: %d != %d", m.getCols(), m2.getCols()));

    Matrix o = new DenseMatrix(m.getRows() + m2.getRows(), m.getCols());

    for(int i = 0; i < o.getRows(); i++){
      for(int j = 0; j < o.getCols(); j++){
        double d = 0;
        if(i < m.getRows()){
          d = m.get(i, j);
        }else{
          d = m2.get(i - m.getRows(), j);
        }
        o.set(i, j, d);
      }
    }

    return o;
  }

  private SparseVector signSparse(SparseVector pSparseVector){
    SparseVector v = new SparseVector(pSparseVector.getDim());

    for(SparseEntry e: pSparseVector.data())
      v.set(e.getPos(), Math.signum(e.getVal()));

    return v;
  }


  @Override
  public DenseVector sign(Vector pDenseVector) {

    DenseVector vector = new DenseVector(pDenseVector.getDim());

    for(int i = 0; i < pDenseVector.getDim(); i++)
      vector.set(i, Math.signum(pDenseVector.get(i)));

    return vector;
  }
}
