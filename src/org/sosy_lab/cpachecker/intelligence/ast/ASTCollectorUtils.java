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
package org.sosy_lab.cpachecker.intelligence.ast;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Map;

public final class ASTCollectorUtils {

  private static final Map<String, ASTNodeLabel> SPECIAL_FUNCTIONS;

  static {
    Builder<String, ASTNodeLabel> builder = ImmutableMap.builder();
    builder.put("pthread_create", ASTNodeLabel.PTHREAD);
    builder.put("pthread_exit", ASTNodeLabel.PTHREAD);
    builder.put("__VERIFIER_error", ASTNodeLabel.VERIFIER_ERROR);
    builder.put("__VERIFIER_assert", ASTNodeLabel.VERIFIER_ASSERT);
    builder.put("__VERIFIER_assume", ASTNodeLabel.VERIFIER_ASSUME);
    builder.put("__VERIFIER_atomic_begin", ASTNodeLabel.VERIFIER_ATOMIC_BEGIN);
    builder.put("__VERIFIER_atomic_end", ASTNodeLabel.VERIFIER_ATOMIC_END);
    builder.put("__VERIFIER_nondet", ASTNodeLabel.INPUT);
    builder.put("malloc", ASTNodeLabel.MALLOC);
    builder.put("free", ASTNodeLabel.FREE);
    SPECIAL_FUNCTIONS = builder.build();
  }

  public static Optional<ASTNodeLabel> getSpecialLabel(String functionName) {
    for(String key : ASTCollectorUtils.SPECIAL_FUNCTIONS.keySet()) {
      if(functionName.startsWith(key))
        return Optional.of(ASTCollectorUtils.SPECIAL_FUNCTIONS.get(key));
    }
    return Optional.absent();
  }

  private ASTCollectorUtils() { }
}
