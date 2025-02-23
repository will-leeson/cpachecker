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
package org.sosy_lab.cpachecker.intelligence.oracle;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;

public class DefaultOracle implements IConfigOracle {

  private PeekingIterator<AnnotatedValue<Path>> iterator;

  public DefaultOracle(List<AnnotatedValue<Path>> pList) {
    iterator = Iterators.peekingIterator(pList.iterator());
  }

  @Override
  public AnnotatedValue<Path> peek() {
    return iterator.peek();
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  @Override
  public AnnotatedValue<Path> next() {
    return iterator.next();
  }

  @Override
  public void remove() {
    iterator.remove();
  }


  @Override
  public void precomputeOracle(Consumer<IConfigOracle> callback) {
    callback.accept(this);
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {

  }
}
