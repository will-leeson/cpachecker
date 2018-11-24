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
package org.sosy_lab.cpachecker.intelligence.learn.sample.backend.proto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.intelligence.learn.sample.FeatureRegistry;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.backend.ISampleBackend;
import org.sosy_lab.cpachecker.intelligence.learn.sample.backend.proto.Svcomp18Schema.Sample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.backend.proto.Svcomp18Schema.SampleBunch;
import org.sosy_lab.cpachecker.intelligence.learn.sample.backend.proto.Svcomp18Schema.Samples;
import org.sosy_lab.cpachecker.intelligence.util.PathFinder;

public class ProtoBackend implements ISampleBackend {

  private static final String DEFAULT_PROTO_FILES_DIR = "resources/%s.pr";

  private Path path;
  private FeatureRegistry registry;

  private Map<String, ProtoSample> backendStore = new HashMap<>();
  private Map<String, IProgramSample> localSample = new HashMap<>();

  public ProtoBackend(FeatureRegistry pFeatureRegistry, String pPath) throws IOException {
    registry = pFeatureRegistry;
    path = searchForProto(pPath);
  }

  public ProtoBackend(FeatureRegistry pFeatureRegistry) throws IOException {
    this(pFeatureRegistry, null);
  }

  private void init(){
    if(!backendStore.isEmpty())return;

    try {
      parseSamples(loadProto());
    } catch (IOException pE) {
      pE.printStackTrace();
    }
  }

  private Path searchForProto(String pPath) throws IOException {
    Path p;
    if(pPath != null){
      p = PathFinder.find("%s", pPath);
    }else{
      p = PathFinder.find(DEFAULT_PROTO_FILES_DIR, "svcomp18");
    }

    if(p == null){
      System.err.println("Couldn't find "+pPath+" or default.");
      return null;
    }

    return p;
  }

  private Samples loadProto() throws IOException {
    if(path == null)return Samples.newBuilder().build();
    return Samples.parseFrom(Files.newInputStream(path));
  }


  private void parseSamples(Samples pSamples){

    for(SampleBunch bunch: pSamples.getBunchesList()){
      for(Sample sample: bunch.getSamplesList()){
        String id = sample.getId();

        if(!backendStore.containsKey(id)){
          backendStore.put(id, new ProtoSample());
        }

        backendStore.get(id).addSerial(registry, sample);
      }
    }

    /*backendStore = pSamples.getBunches(0).getSamplesList().parallelStream().map(
        sample -> {
          ProtoSample protoSample = new ProtoSample();
          protoSample.addSerial(registry, sample);
          return protoSample;
        }
    ).collect(Collectors.toMap(
        ProtoSample::getID,
        Function.identity()
    ));

    for(int i = 1; i < pSamples.getBunchesCount(); i++){
      pSamples.getBunches(i).getSamplesList().parallelStream().forEach(
          sample -> backendStore.get(sample.getId()).addSerial(registry, sample)
      );
    }*/

  }


  @Override
  public IProgramSample loadSample(String id) {
    init();

    if(!backendStore.containsKey(id))
      return localSample.get(id);

    return backendStore.get(id);
  }

  @Override
  public void saveSample(
      String id, IProgramSample pIProgramSample) {
    localSample.put(id, pIProgramSample);
  }

  @Override
  public Set<String> listIds() {
    init();
    return backendStore.keySet();
  }

}
