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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.sosy_lab.cpachecker.intelligence.learn.sample.EmptySample;
import org.sosy_lab.cpachecker.intelligence.learn.sample.FeatureRegistry;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IProgramSample;
import org.sosy_lab.cpachecker.intelligence.util.PathFinder;


public class OnePassCINBackend implements ISampleBackend {

  private static int prefatch = 1000;
  private static final String DEFAULT_ZIP_FILES_DIR = "resources/%s.zip";

  private String path;
  private FeatureRegistry registry;

  private Set<String> available;
  private List<List<String>> bagIndex;
  private List<String> labelIndex;

  private Map<String, IProgramSample> cache;

  public OnePassCINBackend(FeatureRegistry pFeatureRegistry, String pPath) throws IOException {
    registry = pFeatureRegistry;
    path = pPath;
    try{
      decode();
    }catch (IOException e){
      e.printStackTrace();
      System.exit(0);
    }
  }

  public OnePassCINBackend(FeatureRegistry pFeatureRegistry) throws IOException {
    this(pFeatureRegistry, null);
  }

  private ZipInputStream searchForZip() throws IOException {
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

    return new ZipInputStream(new FileInputStream(p.toFile()));
  }

  private void decodeFI(String name, InputStream pStream){

    if(!name.startsWith("feature") || !name.endsWith(".list"))
      return;


    int number = Integer.parseInt(name.substring(8, name.length() - 5));

    List<String> list = CINDecoder.decodeFI(name, pStream);

    while(bagIndex.size() <= number){
      bagIndex.add(new ArrayList<>());
    }
    bagIndex.set(number, list);

  }

  private void decodeLI(InputStream pStream){
    labelIndex = CINDecoder.decodeLI(pStream);
  }


  private void decode() throws IOException {
    if(bagIndex != null)return;

    bagIndex = new ArrayList<>();

    ZipInputStream stream = searchForZip();

    ZipEntry entry;
    while ((entry = stream.getNextEntry()) != null){
      if(entry.getName().startsWith("feature")){
        decodeFI(entry.getName(), stream);
      }else  if(entry.getName().startsWith("label")){
        decodeLI(stream);
      }
    }

    stream.close();
    stream = searchForZip();

    cache = new HashMap<>();

    while ((entry = stream.getNextEntry()) != null){
      String name = entry.getName();

      if(!name.startsWith("feature") && !name.startsWith("label")) {

        long time = System.currentTimeMillis();

        cache.put(name.substring(0, name.length()-3), new OnePassCINSample(
            bagIndex, labelIndex, name.substring(0, name.length()-3), stream, registry
        ) );

        System.out.println("Loaded "+name+"( "+(System.currentTimeMillis() - time)+" ms)");

      }
    }

    stream.close();


  }


  @Override
  public IProgramSample loadSample(String id) {
    return cache.get(id);
  }


  //IMPORTANT: Till now no serialization therefore samples will get lost
  @Override
  public void saveSample(
      String id, IProgramSample pIProgramSample) {
    cache.put(id, pIProgramSample);
  }

  @Override
  public Set<String> listIds() {
    return available;
  }
}
