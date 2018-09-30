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

import java.io.Serializable;

public enum ASTNodeLabel implements Serializable {


  FUNCTION_START,
  SKIP,
  GOTO,
  LABEL,

  // New
  LOOP_ENTRY,
  INITIALIZER_LIST,
  INITIALIZER,
  DESIGNED_INITIALIZER,
  START,
  END,
  PROBLEM_TYPE,
  LOOP_CONDITION,
  BRANCH_CONDITION,
  ARGUMENTS,
  CONDITION,
  VARIABLE_DECL,
  TYPE_DECL,
  COMPLEX_TYPE_DECL,
  FUNCTION_DECL,
  PARAM_TYPES,
  LENGTH,
  COMPOSITE_TYPE,
  ELABORATED_TYPE,
  RETURN_TYPE,
  POINTER_TYPE,
  SIMPLE_TYPE,
  TYPEDEF_TYPE,
  REAL_TYPE,
  VOID_TYPE,
  BINARY_EXPRESSION,
  MULTIPLY,
  DIVIDE,
  PLUS,
  MINUS,
  EQUALS,
  NOT_EQUALS,
  LESS_THAN,
  GREATER_THAN,
  LESS_EQUAL,
  GREATER_EQUAL,
  BINARY_AND,
  BINARY_XOR,
  BINARY_OR,
  SHIFT_LEFT,
  SHIFT_RIGHT,
  MODULO,
  CAST_EXPRESSION,
  CAST_TYPE,
  OPERAND,
  CHAR_LITERAL,
  FLOAT_LITERAL,
  INT_LITERAL_SMALL,
  INT_LITERAL_MEDIUM,
  INT_LITERAL_LARGE,
  STRING_LITERAL,
  AMPER,
  TILDE,
  SIZEOF,
  ALIGNOF,
  COMPLEX_LITERAL,
  LABEL_ADDRESS,
  ARRAY_SUBSCRIPT_EXPRESSION,
  ARRAY_EXPRESSION,
  SUBSCRIPT_EXPRESSION,
  FIELD_REF,
  FIELD_POINTER_DEREF,
  POINTER_EXPRESSION,
  ASSIGNMENT,
  FUNC_CALL_ASSIGN,
  PARAMS,
  FUNC_CALL,
  GLOBAL,
  ENUM_TYPE,

  // Statements
  DECL,
  VAR,
  FUNC,
  ASSUME_TRUE,
  ASSUME_FALSE,
  CAST,
  ASSIGN,
  RETURN,
  BLANK,
  FUNC_RETURN,
  TYPE,
  STRUCT,
  UNION,
  ENUM,
  COMPLEX_TYPE,

  // Types
  VOLATILE,
  UNSIGNED,
  INT,
  DOUBLE,
  FLOAT,
  PTR,
  VOID,
  ARRAY,
  LONG,
  LONGLONG,
  FUNCTION_TYPE,
  SHORT,
  BOOL,
  CHAR,
  CONST,

  // Expression
  COMPARISON,
  ARITHMETIC,
  LOGICAL,
  BIT_OPERATION,
  LITERAL,
  ADDRESS,
  FIELD_REFERENCE,
  ID,
  COMPLEX_NUMBER,
  ARRAY_SUBSCRIPT,
  TYPE_ID,

  // Special
  VERIFIER_ASSERT,
  VERIFIER_ERROR,
  VERIFIER_ASSUME,
  VERIFIER_ATOMIC_BEGIN,
  VERIFIER_ATOMIC_END,
  INPUT,
  PTHREAD,
  MALLOC,
  FREE

}
