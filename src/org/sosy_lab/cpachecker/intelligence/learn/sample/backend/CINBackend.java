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
package org.sosy_lab.cpachecker.intelligence.learn.sample.backend;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.sosy_lab.cpachecker.intelligence.learn.sample.EmptySample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.FeatureRegistry;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.util.PathFinder;
import scala.Int;

public class CINBackend implements ISampleBackend {

  private static int prefatch = 1000;
  private static final String DEFAULT_ZIP_FILES_DIR = "resources/%s.zip";

  private String path;
  private FeatureRegistry registry;
  private ZipFile zipFile;

  private Set<String> available;
  private List<List<String>> bagIndex;
  private List<String> labelIndex;

  private LoadingCache<String, IProgramSample> cache;

  public CINBackend(FeatureRegistry pFeatureRegistry, String pPath) throws IOException {
    registry = pFeatureRegistry;
    path = pPath;
    zipFile = searchForZip();
  }

  public CINBackend(FeatureRegistry pFeatureRegistry) throws IOException {
    this(pFeatureRegistry, null);
  }

  private ZipFile searchForZip() throws IOException {
    Path p;
    if(path != null){
      p = PathFinder.find("%s", path);
    }else{
      p = PathFinder.find(DEFAULT_ZIP_FILES_DIR, "svcomp18");
    }

    if(p == null){
      System.err.println("Couldn't find "+path+" or default.");
      return null;
    }

    return new ZipFile(p.toFile());
  }

  private void decodeFI(ZipEntry e){
    String name = e.getName();

    if(!name.startsWith("feature") || !name.endsWith(".list"))
      return;


    int number = Integer.parseInt(name.substring(8, name.length() - 5));

    List<String> list = new ArrayList<>();
    try(BufferedReader br = new BufferedReader(new InputStreamReader(zipFile.getInputStream(e)))) {
      for(String line; (line = br.readLine()) != null; ) {
        list.add(line);
      }
    } catch (IOException pE) {
      pE.printStackTrace();
    }

    while(bagIndex.size() <= number){
      bagIndex.add(new ArrayList<>());
    }
    bagIndex.set(number, list);
  }

  private void decodeLI(ZipEntry e){
    List<String> list = new ArrayList<>();
    try(BufferedReader br = new BufferedReader(new InputStreamReader(zipFile.getInputStream(e)))) {
      for(String line; (line = br.readLine()) != null; ) {
        list.add(line);
      }
    } catch (IOException pE) {
      pE.printStackTrace();
    }
    labelIndex = list;
  }


  private void decode(){
    if(bagIndex != null)return;
    if(zipFile == null)return;
    bagIndex = new ArrayList<>();

    Enumeration<? extends ZipEntry> en = zipFile.entries();

    available = new HashSet<>();

    while (en.hasMoreElements()){
      ZipEntry e = en.nextElement();

      if(e.getName().startsWith("feature")){
        decodeFI(e);
      }else  if(e.getName().startsWith("label")){
        decodeLI(e);
      }else{
        available.add(e.getName().substring(0, e.getName().length() - 3));
      }
    }

    cache = CacheBuilder.newBuilder().maximumSize(prefatch)
        .build(
            new CacheLoader<String, IProgramSample>() {
              @Override
              public IProgramSample load(String pS) throws Exception {
                ZipEntry e = zipFile.getEntry(pS+".ix");
                if(e != null){
                  return new CINSample(bagIndex, labelIndex, zipFile, e, registry);
                }
                return new EmptySample();
              }
            }
        );

  }


  @Override
  public IProgramSample loadSample(String id) {
    decode();
    try {
      return cache.get(id);
    } catch (ExecutionException pE) {
      return null;
    }
  }


  //IMPORTANT: Till now no serialization therefore samples will get lost
  @Override
  public void saveSample(
      String id, IProgramSample pIProgramSample) {
    decode();
    cache.put(id, pIProgramSample);
  }

  @Override
  public Set<String> listIds() {
    decode();
    return available;
  }
}
