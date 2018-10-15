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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ParrallelMathEngine implements MathEngine {

  private ExecutorService service;
  private MathEngine defaultEngine = MathEngine.instance();

  public ParrallelMathEngine(ExecutorService pService) {
    service = pService;
  }

  @Override
  public Vector zeroVector(int dim) {
    return defaultEngine.zeroVector(dim);
  }

  @Override
  public Matrix zeros(int rows, int columns) {
    return defaultEngine.zeros(rows, columns);
  }

  @Override
  public DenseVector dot(
      Matrix m, Vector v) throws Exception {

    if(m.getCols() != v.getDim())
      throw new IllegalArgumentException("Columns and dimension have to be equal ("+m.getCols()+", "+v.getDim()+")");

    List<Future<Double>> futures = new ArrayList<>();

    for(DenseRow row: m) {
      futures.add(service.submit(
          new RowVectorDot(row, v)
      ));
    }

    DenseVector o = new DenseVector(m.getRows());

    for(int i = 0; i < o.getDim(); i++){
      o.set(i, futures.get(i).get());
    }

    return o;
  }



  @Override
  public Matrix dot(
      Matrix m, Matrix m2) throws Exception {
    return defaultEngine.dot(m, m2);
  }

  @Override
  public Matrix add(
      Matrix m, Matrix m2) throws Exception {
    return defaultEngine.add(m, m2);
  }

  @Override
  public Matrix mul(Matrix m, double d) throws Exception {
    return defaultEngine.mul(m, d);
  }

  @Override
  public Vector add(
      Vector v1, Vector v2) throws Exception {
    return defaultEngine.add(v1, v2);
  }

  @Override
  public Vector mul(Vector v, double d) throws Exception {
    return defaultEngine.mul(v, d);
  }

  @Override
  public Matrix stackH(
      Matrix m, Matrix m2) {
    return defaultEngine.stackH(m, m2);
  }

  @Override
  public Matrix stackV(
      Matrix m, Matrix m2) {
    return defaultEngine.stackV(m, m2);
  }

  @Override
  public Vector sign(Vector pDenseVector) {
    return defaultEngine.sign(pDenseVector);
  }
}
