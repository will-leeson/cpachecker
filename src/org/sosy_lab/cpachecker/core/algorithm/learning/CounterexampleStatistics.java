// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.learning;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class CounterexampleStatistics {

  private int numSeenPaths = 0;
  private int numSeenEdges = 0;

  private Map<String, Counter> edgeCounter = new HashMap<>();

  private boolean isSetBased = false;
  private int ngramSize = 1;

  private String algorithmID;
  private SVPathProcessor pathProcessor;
  private PathAnalyser analyser;


  public CounterexampleStatistics(String pAlgorithmID){
    this(pAlgorithmID, 1, true);
  }

  public CounterexampleStatistics(String pAlgorithmID, int pNgramSize, boolean pIsSetBased){
    this.algorithmID = pAlgorithmID;
    this.ngramSize = pNgramSize;
    this.isSetBased = pIsSetBased;
    this.pathProcessor = new SVPathProcessor();
    this.analyser = null;
  }

  public int getNumSeenPaths(){
    return numSeenPaths;
  }

  public String getAlgorithmID() {
    return algorithmID;
  }

  public SVPathProcessor getPathProcessor() {
    return pathProcessor;
  }

  public void setAnalyser(PathAnalyser pAnalyser) {
    analyser = pAnalyser;
  }

  public PathAnalyser getAnalyser() {
    return analyser;
  }

  public boolean isSetBased() {
    return isSetBased;
  }

  public int getNgramSize() {
    return ngramSize;
  }

  public Set<String> getVocabulary(){
    return edgeCounter.keySet();
  }

  private ImmutableList<String> preprocessCounterexample(List<CFAEdge> counterexample){
      if(counterexample.isEmpty())return ImmutableList.of();

      PathGraph graph = this.pathProcessor.process(counterexample);

      if(this.analyser != null){
        CFAEdge targetEdge = counterexample.get(counterexample.size() - 1);
        this.analyser.analyse(graph, "N"+targetEdge.getSuccessor().getNodeNumber());
      }

      ImmutableList.Builder<String> builder = ImmutableList.builder();

      for(CFAEdge edge : counterexample){

        String currentNode = "N"+edge.getPredecessor().getNodeNumber();
        String label = graph.getNode(currentNode).getLabel();
        builder.add(label.length()==0?"EMPTY":label);

      }


      return builder.build();
  }

  private Collection<String> getOrConstructSet(List<String> list){
    if(isSetBased) return new HashSet<>(list);
    return list;
  }

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


  private void countFromTokens(List<String> tokens){

    List<String> ngrams = computeNgrams(tokens);

    for(String ngram : getOrConstructSet(ngrams)){

      if(!edgeCounter.containsKey(ngram)){
        edgeCounter.put(ngram, new Counter());
      }

      edgeCounter.get(ngram).increment();

      numSeenEdges += 1;
    }

    numSeenPaths += 1;

  }

  public void countFromCounterexample(List<CFAEdge> pCounterexample){
    List<String> tokens = preprocessCounterexample(pCounterexample);
    countFromTokens(tokens);
  }

  public CounterexampleStatistic generateCounterexampleStatistic(List<CFAEdge> pCounterexample){
    List<String> tokens = preprocessCounterexample(pCounterexample);

    ImmutableMap.Builder<String, Integer> mapBuilder = ImmutableMap.builder();

    List<String> ngrams = computeNgrams(tokens);

    for(String ngram : getOrConstructSet(ngrams)){
      int count = 0;
      if(edgeCounter.containsKey(ngram)){
        count = edgeCounter.get(ngram).getCount();
      }
      mapBuilder.put(ngram, count);
    }

    return new CounterexampleStatistic(
        numSeenPaths, numSeenEdges, mapBuilder.build()
    );


  }


  public void saveToFile(Path pPath) throws IOException {

    Path parent = pPath.getParent();

    if((parent != null && !Files.exists(parent)) || !Files.isDirectory(parent)){
      Files.createDirectory(parent);
    }

    try(BufferedWriter writer = Files.newBufferedWriter(pPath)) {

      writer.write(String.format("%s\t%d\n", "numSeenPaths", numSeenPaths));

      for(Entry<String, Counter> count : edgeCounter.entrySet()){
        writer.write(String.format("%s\t%d\n", count.getKey(), count.getValue().getCount()));
      }

    }

  }

  public void loadFromFile(Path pPath) throws IOException {


    if(!Files.exists(pPath)){
      throw new FileNotFoundException(pPath.toString());
    }

    String line;
    boolean foundNumPaths = false;

    try(BufferedReader reader = Files.newBufferedReader(pPath)){
      while((line = reader.readLine()) != null){
        List<String> elements = Splitter.on("\t").splitToList(line);

        Preconditions.checkState(elements.size() == 2);

        String key = elements.get(0);
        int count = Integer.parseInt(elements.get(1));

        if(!foundNumPaths && key.equals("numSeenPaths")){
          numSeenPaths = count;
          foundNumPaths = true;
        } else {
          edgeCounter.put(key, new Counter(count));
          numSeenEdges += count;
        }

      }
    }

  }

  private static class Counter {

    private int count;

    public Counter(int pCount){
      this.count = pCount;
    }

    public Counter(){
      this(0);
    }

    public void increment(){
      count += 1;
    }

    public int getCount(){
      return count;
    }
  }

  public static class CounterexampleStatistic {

    private int numSeenPath;
    private int numSeenTokens;
    private ImmutableMap<String, Integer> counterexampleTokens;

    public CounterexampleStatistic(
        int pNumSeenPath,
        int pNumSeenTokens,
        ImmutableMap<String, Integer> pCounterexampleTokens) {
      numSeenPath = pNumSeenPath;
      numSeenTokens = pNumSeenTokens;
      counterexampleTokens = pCounterexampleTokens;
    }

    public int getNumSeenPath() {
      return numSeenPath;
    }

    public int getNumSeenTokens() {
      return numSeenTokens;
    }

    public ImmutableMap<String, Integer> getCounterexampleTokens() {
      return counterexampleTokens;
    }



  }

}
