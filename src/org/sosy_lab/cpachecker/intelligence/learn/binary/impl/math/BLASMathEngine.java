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

import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;

public class BLASMathEngine implements MathEngine{

  private MathEngine defaultEngine;

  BLASMathEngine(MathEngine pDefaultEngine) {
    defaultEngine = pDefaultEngine;
  }

  @Override
  public Vector zeroVector(int dim) {
    return new BLASVector(dim);
  }

  @Override
  public Matrix zeros(int rows, int columns) {
    return new BLASMatrix(rows, columns);
  }

  @Override
  public Vector dot(
      Matrix m, Vector v) throws Exception {
    if(!(m instanceof BLASMatrix) || !(v instanceof BLASVector))
      return defaultEngine.dot(m, v);
    return new BLASVector(((BLASMatrix) m).toNative().mmul(((BLASVector) v).toNative()));
  }

  @Override
  public Matrix dot(
      Matrix m, Matrix m2) throws Exception {
    if(!(m instanceof BLASMatrix) || !(m2 instanceof BLASMatrix))
      return defaultEngine.dot(m, m2);
    return new BLASMatrix(((BLASMatrix) m).toNative().mmul(((BLASMatrix) m2).toNative()));
  }

  @Override
  public Matrix add(
      Matrix m, Matrix m2) throws Exception {
    if(!(m instanceof BLASMatrix) || !(m2 instanceof BLASMatrix))
      return defaultEngine.add(m, m2);
    return new BLASMatrix(((BLASMatrix) m).toNative().add(((BLASMatrix) m2).toNative()));
  }

  @Override
  public Matrix mul(Matrix m, double d) throws Exception {
    if(!(m instanceof BLASMatrix))
      return defaultEngine.mul(m, d);
    return new BLASMatrix(((BLASMatrix) m).toNative().mul(d));
  }

  @Override
  public Vector add(Vector v1, Vector v2) throws Exception {
    if(!(v1 instanceof BLASVector) || !(v2 instanceof BLASVector))
      return defaultEngine.add(v1, v2);
    return new BLASVector(((BLASVector) v1).toNative().add(((BLASVector) v2).toNative()));
  }

  @Override
  public Vector mul(Vector v, double d) throws Exception {
    if(!(v instanceof BLASVector))
      return defaultEngine.mul(v, d);
    return new BLASVector(((BLASVector) v).toNative().mul(d));
  }

  @Override
  public Matrix stackH(
      Matrix m, Matrix m2) {
    if(!(m instanceof BLASMatrix) || !(m2 instanceof BLASMatrix))
      return defaultEngine.stackH(m, m2);
    return new BLASMatrix(DoubleMatrix.concatHorizontally(((BLASMatrix) m).toNative(), ((BLASMatrix) m2).toNative()));
  }

  @Override
  public Matrix stackV(
      Matrix m, Matrix m2) {
    if(!(m instanceof BLASMatrix) || !(m2 instanceof BLASMatrix))
      return defaultEngine.stackV(m, m2);
    return new BLASMatrix(DoubleMatrix.concatVertically(((BLASMatrix) m).toNative(), ((BLASMatrix) m2).toNative()));
  }

  @Override
  public Vector sign(Vector pDenseVector) {
    if(!(pDenseVector instanceof BLASVector))
      return defaultEngine.sign(pDenseVector);
    return new BLASVector(MatrixFunctions.signum(((BLASVector) pDenseVector).toNative()));
  }
}
