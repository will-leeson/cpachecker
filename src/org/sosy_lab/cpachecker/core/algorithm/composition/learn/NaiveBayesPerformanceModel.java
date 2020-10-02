// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition.learn;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class NaiveBayesPerformanceModel implements IPerformanceModel {

  private int noOfAlgorithms;
  private int ngramSize = 1;
  private int[] numSeenPaths;
  private int[] numSeenTokens;
  private boolean isSetBased;

  private Map<String, int[]> counter = new HashMap<>();

  public NaiveBayesPerformanceModel(int pNoOfAlgorithms){
    this(pNoOfAlgorithms, 1, true);
  }

  public NaiveBayesPerformanceModel(int pNoOfAlgorithms, int pNgramSize, boolean setBased){

    Preconditions.checkArgument(pNoOfAlgorithms > 1);
    Preconditions.checkArgument(pNgramSize >= 1);

    noOfAlgorithms = pNoOfAlgorithms;
    ngramSize = pNgramSize;
    isSetBased = setBased;
    numSeenPaths = new int[noOfAlgorithms];
    numSeenTokens = new int[noOfAlgorithms];
  }

  @Override
  public boolean isReady() {

    for(int i = 0; i < noOfAlgorithms; i++){
      if(numSeenPaths[i] == 0) return false;
    }

    return true;
  }

  @Override
  public void nextRound() { }

  private List<String> computeNgrams(List<String> elements){
    if(ngramSize == 1) return elements;

    List<String> ngrams = new ArrayList<>(elements);

    for(int windowSize = 2; windowSize <= ngramSize; windowSize++){

      for(int cursor = 0; cursor < elements.size() - windowSize; cursor++){

        List<String> window = elements.subList(cursor, cursor+windowSize);
        String ngram = String.join("_", window);
        ngrams.add(ngram);

      }

    }

    return ngrams;
  }

  private Collection<String> getOrConstructSet(List<String> list){
    if(isSetBased) return new HashSet<>(list);
    return list;
  }

  @Override
  public void learnPath(
      int algorithmId, List<String> pathTokens) {

    Preconditions.checkState(algorithmId < noOfAlgorithms);

    List<String> tokens = computeNgrams(pathTokens);

    // We count an ngram only once per path
    for(String ngram : getOrConstructSet(tokens)){
      if(!counter.containsKey(ngram)){
        counter.put(ngram, new int[noOfAlgorithms]);
      }
      counter.get(ngram)[algorithmId] += 1;
      numSeenTokens[algorithmId] += 1;
    }

    numSeenPaths[algorithmId] += 1;

  }

  // Laplace smoothed log probabilities
  private double[] logProbability(String token){
    double[] result = new double[noOfAlgorithms];
    int[] numOccurrence;

    if(counter.containsKey(token)){
      numOccurrence = counter.get(token);
    } else {
      numOccurrence = new int[noOfAlgorithms];
    }

    int vocabSize = counter.size();

    for(int algorithmId = 0; algorithmId < noOfAlgorithms; algorithmId++){
      int norm = numSeenTokens[algorithmId] + vocabSize;
      double conditional = (numOccurrence[algorithmId] + 1) / (double) norm;
      result[algorithmId] = Math.log(conditional);
    }

    return result;
  }

  @Override
  public List<Double> predictPerformance(List<String> pathTokens) {

    double[] result = new double[noOfAlgorithms];

    int norm = IntStream.of(numSeenPaths).sum();
    for(int i = 0; i < result.length; i++){
      result[i] = Math.log(numSeenPaths[i] / (double) norm);
    }

    List<String> tokens = computeNgrams(pathTokens);

    for(String ngram: getOrConstructSet(tokens) ){

      double[] tmpResults = logProbability(ngram);

      for(int i = 0; i < result.length; i++){
        result[i] += tmpResults[i];
      }

      //Since we apply a softmax distribution later one, we can shift the scores
      double minimum = DoubleStream.of(result).min().orElse(0.0);

      for(int i = 0; i < result.length; i++){
        result[i] -= minimum;
      }
    }

    //Softmax distribution

    for(int i = 0; i < result.length; i++){
      result[i] = Math.exp(result[i]);
    }

    double total = DoubleStream.of(result).sum();

    List<Double> resultList = new ArrayList<>(result.length);

    for(int i = 0; i < result.length; i++){
      resultList.add(result[i] / total);
    }

    return resultList;
  }
}
