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
package org.sosy_lab.cpachecker.intelligence.learn.binary;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.CoefInterModel;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.TrainModel;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.math.MathEngine;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.math.Matrix;
import org.sosy_lab.cpachecker.intelligence.util.PathFinder;

public class LinearPretrainedType implements IBinaryPredictorType {

  private static final String DEFAULT_RESOURCE_PATH = "resources/%s.json";

  private MathEngine engine = MathEngine.instance();
  private Matrix alpha;

  private Table<String, String, Integer> mapCombination = HashBasedTable.create();
  private Map<String, Integer> mapFeatures = new HashMap<>();

  private String path;


  public MathEngine getEngine() {
    return engine;
  }

  public Matrix getAlpha() {
    return alpha;
  }

  public Map<String, Integer> getMapFeatures(){
    return mapFeatures;
  }

  public Table<String, String, Integer> getMapCombination(){
    return mapCombination;
  }

  private void load(){
    if(!mapFeatures.isEmpty())return;
    if(!mapCombination.isEmpty())return;

    Path p;

    if(path == null){
      p = PathFinder.find(DEFAULT_RESOURCE_PATH, "Train_Spec");
    }else{
      p = PathFinder.find("%s", path);
    }

    if(p == null){
      System.err.println("Couldn't find "+path+" or default.");
      return;
    }

    GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(TrainModel.class, new JsonDeserializer<TrainModel>() {
      @Override
      public TrainModel deserialize(
          JsonElement pJsonElement,
          Type pType,
          JsonDeserializationContext pJsonDeserializationContext) throws JsonParseException {

        Table<String, String, CoefInterModel> table = HashBasedTable.create();

        int max = 0;

        if(pJsonElement instanceof JsonObject){
          JsonObject o = (JsonObject)pJsonElement;

          for(Entry<String, JsonElement> e: o.entrySet()){
            if(e.getValue() instanceof JsonObject){
              JsonObject subO = (JsonObject)e.getValue();

              for(Entry<String, JsonElement> subE: subO.entrySet()){
                CoefInterModel coefInterModel = pJsonDeserializationContext.deserialize(subE.getValue(), CoefInterModel.class);
                max = Math.max(max, coefInterModel.getCoef().size());
                table.put(e.getKey(), subE.getKey(), coefInterModel);
              }

            }
          }

        }

        return new TrainModel(table, max);
      }
    });
    Gson gson = builder.create();
    try {
      TrainModel model = gson.fromJson(Files.newBufferedReader(p.toFile().toPath(), Charset.forName("UTF-8")), TrainModel.class);

      alpha = engine.zeros(model.getTable().size(), model.getFeatureSize() + 1);

      for(Cell<String, String, CoefInterModel> cell: model.getTable().cellSet()){
        if(!mapCombination.contains(cell.getRowKey(), cell.getColumnKey())){
          mapCombination.put(cell.getRowKey(), cell.getColumnKey(), mapCombination.size());
        }
        int row = mapCombination.get(cell.getRowKey(), cell.getColumnKey());

        for(Entry<String, Double> e: cell.getValue().getCoef().entrySet()){
          String feature = e.getKey();
          if(!mapFeatures.containsKey(feature)){
            mapFeatures.put(feature, mapFeatures.size());
          }
          int col = mapFeatures.get(feature);
          alpha.set(row, col, e.getValue());
        }
        alpha.set(row, model.getFeatureSize(), cell.getValue().getIntercept());
      }


    } catch (FileNotFoundException pE) {
      System.out.println("File not found: "+p.toString());
    } catch (IOException pE) {
      System.out.println(pE.getMessage());
    }


  }


  public LinearPretrainedType(String pPath) {
    path = pPath;
    load();
  }

  @Override
  public IBinaryPredictor instantiate(String label1, String label2) {
    return null;
  }
}
