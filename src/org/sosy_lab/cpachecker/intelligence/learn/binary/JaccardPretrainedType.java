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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.KernelCoef;
import org.sosy_lab.cpachecker.intelligence.learn.binary.impl.TrainJaccModel;
import org.sosy_lab.cpachecker.intelligence.util.PathFinder;

public class JaccardPretrainedType implements IBinaryPredictorType {

  private static final String DEFAULT_RESOURCE_PATH = "resources/%s.json";

  private String path;

  private Table<String, String, KernelCoef> config;


  public JaccardPretrainedType(String pPath) {
    path = pPath;
    load();
  }

  private void load() {
    if(config != null)return;

    Path p;

    if(path == null){
      p = PathFinder.find(DEFAULT_RESOURCE_PATH, "Train_Jacc");
    }else{
      p = PathFinder.find("%s", path);
    }

    if(p == null){
      System.err.println("Couldn't find "+path+" or default.");
      return;
    }

    GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(TrainJaccModel.class, new JsonDeserializer<TrainJaccModel>() {
      @Override
      public TrainJaccModel deserialize(
          JsonElement pJsonElement,
          Type pType,
          JsonDeserializationContext pJsonDeserializationContext) throws JsonParseException {

        Table<String, String, KernelCoef> table = HashBasedTable.create();

        int max = 0;

        if(pJsonElement instanceof JsonObject){
          JsonObject o = (JsonObject)pJsonElement;

          for(Entry<String, JsonElement> e: o.entrySet()){
            if(e.getValue() instanceof JsonObject){
              JsonObject subO = (JsonObject)e.getValue();

              for(Entry<String, JsonElement> subE: subO.entrySet()){
                KernelCoef coefInterModel = pJsonDeserializationContext.deserialize(subE.getValue(), KernelCoef.class);
                max = Math.max(max, coefInterModel.getCoef().size());
                table.put(e.getKey(), subE.getKey(), coefInterModel);
              }

            }
          }

        }

        return new TrainJaccModel(table, max);
      }
    });
    Gson gson = builder.create();

    try {
      TrainJaccModel model = gson.fromJson(new FileReader(p.toFile()), TrainJaccModel.class);
      config = model.getTable();
    }catch(JsonSyntaxException pE){
      pE.printStackTrace();
      config = HashBasedTable.create();
    } catch (FileNotFoundException pE) {
      pE.printStackTrace();
    }


  }

  public Table<String, String, KernelCoef> getConfig() {
    return config;
  }

  @Override
  public IBinaryPredictor instantiate(String label1, String label2) {
    return null;
  }
}
