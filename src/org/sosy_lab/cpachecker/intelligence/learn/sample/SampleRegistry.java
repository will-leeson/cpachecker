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
package org.sosy_lab.cpachecker.intelligence.learn.sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.intelligence.learn.sample.backend.ISampleBackend;
import org.sosy_lab.cpachecker.intelligence.learn.sample.backend.InMemBackend;

public class SampleRegistry {

  private int maxIteration;
  private int astDepth;
  private FeatureRegistry registry;
  private ISampleBackend backend;
  private boolean accelerated = false;

  public SampleRegistry(FeatureRegistry pFeatureRegistry, int pMaxIteration, int pAstDepth,
                        ISampleBackend pISampleBackend, boolean pAccelerated) {
    this.registry = pFeatureRegistry;
    maxIteration = pMaxIteration;
    astDepth = pAstDepth;
    this.backend = pISampleBackend;
    this.accelerated = pAccelerated;
  }

  public SampleRegistry(FeatureRegistry pFeatureRegistry, int pMaxIteration, int pAstDepth,
                        ISampleBackend pISampleBackend) {
    this(pFeatureRegistry, pMaxIteration, pAstDepth, pISampleBackend, false);
  }


  public SampleRegistry(FeatureRegistry pFeatureRegistry, int pMaxIteration, int pAstDepth){
    this(pFeatureRegistry, pMaxIteration, pAstDepth, new InMemBackend());
  }

  public IProgramSample registerSample(String id, CFA pCFA){

    IWLFeatureModel featureModel;

    if(accelerated){
      featureModel = new AccWLFeatureModel(pCFA, this.astDepth);
    }else{
      featureModel = new WLFeatureModel(pCFA, this.astDepth);
    }

    RealProgramSample sample = new RealProgramSample(
        id, this.maxIteration, featureModel, registry
    );

    backend.saveSample(id, sample);

    return sample;
  }

  public List<IProgramSample> getSamples(Collection<String> ids){
    List<IProgramSample> samples = new ArrayList<>();

    for(String id: ids)
      samples.add(this.getSample(id));

    return samples;
  }

  public IProgramSample getSample(String id){
      IProgramSample sample = backend.loadSample(id);
      if(sample == null)
        sample = new EmptySample();
      return sample;
  }

  public Set<String> listSampleIds(){
    return backend.listIds();
  }


}
