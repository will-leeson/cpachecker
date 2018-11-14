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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.intelligence.learn.sample.FeatureRegistry;
import org.sosy_lab.cpachecker.intelligence.learn.sample.IFeature;

public class CINDecoder {

  private static int bytes_length(int b){
    int i = 0;
    int t = (1 << 7);
    while((b & t) > 0){
      i++;
      b = b << 1;
    }
    b = b << 1;
    if((b & t) > 0)
      return i;
    return 0;
  }

  private static boolean is_neg(int b, int l){
    int t = (1 << 7);
    b = b << (l+2);
    return (t & b) > 0;
  }

  private static int me_decode(byte[] B){
    int length = bytes_length(((int)B[0] & 0xff));
    if(length == 0){
      return ((int)B[0] & 0xff);
    }
    boolean neg = is_neg(B[0], length);
    int number = 0;
    for(int i = 1; i < B.length; i++){
      number += (((int)B[i] & 0xff) << 8*(B.length - i - 1));
    }
    if(neg){
      number = (-number) - 1;
    }
    return number;
  }



  public static double getFloat64(byte[] bytes)
  {
    return Double.longBitsToDouble(((bytes[0] & 0xFFL) << 56)
        | ((bytes[1] & 0xFFL) << 48)
        | ((bytes[2] & 0xFFL) << 40)
        | ((bytes[3] & 0xFFL) << 32)
        | ((bytes[4] & 0xFFL) << 24)
        | ((bytes[5] & 0xFFL) << 16)
        | ((bytes[6] & 0xFFL) << 8)
        | ((bytes[7] & 0xFFL) << 0));
  }

  private static List<List<Double>> decode_stream(InputStream stream, List<List<String>> featureIndex) throws IOException {
    int enc_i = 0;
    byte[] enc_b = null;
    List<List<Double>> out = new ArrayList<>();
    out.add(new ArrayList<>());
    List<Double> current = out.get(0);

    int b;
    while((b = stream.read()) != -1){
      if(enc_i == 0){
        if(enc_b != null){
          current.add((double) me_decode(enc_b));
          enc_b = null;
        }

        if(b == 255){
          out.add(new ArrayList<>());
          current = out.get(out.size() -1);

          if(out.size() > featureIndex.size()){
            break;
          }
        }else{
          enc_i = bytes_length(b);
          enc_b = new byte[enc_i + 1];
          enc_b[0] = (byte)b;
        }
      }else{
        enc_b[enc_b.length - enc_i] = (byte)b;
        enc_i--;
      }
    }

    boolean sw = true;

    while((b = stream.read()) != -1){
      if(enc_i == 0){
        if(enc_b != null) {
          if (sw) {
            //This is unavailable due to python struct
            current.add(900.0);
          } else {
            int D = me_decode(enc_b);
            boolean correct = (D & 1) > 0;
            current.add((double) (D >> 1));
            current.add(correct ? 1.0 : 0.0);
          }
          enc_b = null;
        }

        sw = !sw;

        if(sw){
          enc_i = 7;
        }else{
          enc_i = bytes_length(b);
        }
        enc_b = new byte[enc_i+1];
        enc_b[0] = (byte) b;

      }else{
        enc_b[enc_b.length - enc_i] = (byte)b;
        enc_i--;
      }
    }

    if(enc_b != null) {
      if (sw) {
        current.add(getFloat64(enc_b));
      } else {
        int D = me_decode(enc_b);
        boolean correct = (D & 1) > 0;
        current.add((double) (D >> 1));
        current.add(correct ? 1.0 : 0.0);
      }
    }

    return out;
  }


  public static List<String> decodeFI(String name, InputStream pStream){

    if(!name.startsWith("feature") || !name.endsWith(".list"))
      return new ArrayList<>();

    List<String> list = new ArrayList<>();
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(pStream));
      for(String line; (line = br.readLine()) != null; ) {
        list.add(line);
      }
    } catch (IOException pE) {
      pE.printStackTrace();
    }

    return list;
  }

  public static List<String> decodeLI(InputStream pStream){
    List<String> list = new ArrayList<>();
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(pStream));
      for(String line; (line = br.readLine()) != null; ) {
        list.add(line);
      }
    } catch (IOException pE) {
      pE.printStackTrace();
    }
    return list;
  }



  public static CINDecodingResult decode(InputStream pStream, List<List<String>> featureIndex, FeatureRegistry registry, List<String> labelIndex)
      throws IOException {


    List<Map<IFeature, Double>> bags = new ArrayList<>();

    InputStream stream = pStream;
    List<List<Double>> decodeStream = decode_stream(stream, featureIndex);

    for (int i = 0; i < decodeStream.size() - 1; i++) {
      List<String> fIndex = featureIndex.get(i);
      List<Double> st = decodeStream.get(i);
      Map<IFeature, Double> bag = new HashMap<>();
      bags.add(bag);

      IFeature f = null;

      for (int k = 0; k < st.size(); k++) {
        if (f != null) {
          bag.put(f, st.get(k));
          f = null;
        } else {
          int kI = st.get(k).intValue();
          if (kI < 0 || kI >= fIndex.size()) continue;
          f = registry.index(fIndex.get(kI));
        }
      }

    }

    Map<String, ProgramLabel> labels = new HashMap<>();

    List<Double> labelStream = decodeStream.get(decodeStream.size() - 1);
    String label = null;
    boolean correct = false;
    double time = -1;
    for(int i = 0; i < labelStream.size(); i++){
      if(label == null){
        int p = labelStream.get(i).intValue();
        if(p < labelIndex.size()){
          label = labelIndex.get(p);
        }
      }else if(time == -1){
        correct = labelStream.get(i) > 0;
        time = 0.0;
      }else{
        time = labelStream.get(i);
        labels.put(label, new ProgramLabel(correct, time));
        label = null;
        time = -1;
        correct = false;
      }
    }


    return new CINDecodingResult(bags, labels);

  }

  public static class CINDecodingResult{

    private List<Map<IFeature, Double>> bags;
    private Map<String, ProgramLabel> labels = new HashMap<>();


    public CINDecodingResult(
        List<Map<IFeature, Double>> pBags,
        Map<String, ProgramLabel> pLabels) {
      bags = pBags;
      labels = pLabels;
    }

    public List<Map<IFeature, Double>> getBags() {
      return bags;
    }

    public Map<String, ProgramLabel> getLabels() {
      return labels;
    }

  }

  public static class ProgramLabel{

    private boolean correctSolved;
    private double time;

    public ProgramLabel(boolean pCorrectSolved, double pTime) {
      correctSolved = pCorrectSolved;
      time = pTime;
    }

    public boolean isCorrectSolved() {
      return correctSolved;
    }

    public double getTime() {
      return time;
    }

  }
}
