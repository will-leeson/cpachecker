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

import java.util.concurrent.ExecutorService;

public interface MathEngine {

  static boolean blasAvailable(){
    try{
      Class.forName("org.jblas.DoubleMatrix");
      return true;
    } catch (ClassNotFoundException pE) {
      return false;
    }
  }

  static MathEngine instance(ExecutorService service){
    if(blasAvailable()){
      return new BLASMathEngine(new SequentialMathEngine());
    }
    if(service != null && !service.isShutdown()){
      return new ParrallelMathEngine(service);
    }
    return new SequentialMathEngine();
  }

  static MathEngine instance(){
    return instance(null);
  }


  public Vector zeroVector(int dim);

  public Matrix zeros(int rows, int columns);

  public Vector dot(Matrix m, Vector v) throws Exception;

  public Matrix dot(Matrix m, Matrix m2) throws Exception;

  public Matrix add(Matrix m, Matrix m2) throws Exception;

  public Matrix mul(Matrix m, double d) throws Exception;

  public Vector add(Vector v1, Vector v2) throws Exception;

  public Vector mul(Vector v, double d) throws Exception;

  public Matrix stackH(Matrix m, Matrix m2);

  public Matrix stackV(Matrix m, Matrix m2);

  public Vector sign(Vector pDenseVector);


}
