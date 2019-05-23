/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.intelligence.ast.neural;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;
import org.sosy_lab.cpachecker.intelligence.graph.model.StructureGraph;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.SVGraph;
import org.sosy_lab.cpachecker.util.Pair;

public class GraphWriter {

  private SVGraph graph;

  public GraphWriter(
      SVGraph pGraph) {
    graph = pGraph;
  }

  private String getId(GNode pGNode, GNode pParent){
    String id;
    if(pParent == null)
      id = pGNode.getId();
    else{
      id = pParent.getId() + "_"+ pGNode.getId().substring(1);
    }
    return id;
  }

  private void processNode(GNode pGNode, GNode pParent, Consumer<String> appender){
    appender.accept("\""+getId(pGNode, pParent)+"\":\""+pGNode.getLabel()+"\"");
  }

  private void processEdge(GEdge pGEdge, GNode pParent, Consumer<String> appender){
    appender.accept(String.format("[\"%s\", \"%s|>\", \"%s\"]",
                    getId(pGEdge.getSource(), pParent),
                    pGEdge.getId(),
                    getId(pGEdge.getSink(), pParent)));
  }

  private List<Pair<GNode, GNode>> nodes(){

    List<Pair<GNode, GNode>> out = new ArrayList<>();

    for(String n : graph.nodes()){
      GNode node = graph.getNode(n);
      out.add(Pair.of(node, null));

      if(node.containsOption(OptionKeys.AST)){
        StructureGraph g = node.getOption(OptionKeys.AST);

        for(String s : g.nodes()){
          GNode snode = g.getNode(s);
          out.add(Pair.of(snode, node));
        }

      }

    }


    return out;

  }

  private List<Pair<GEdge, GNode>> astEdges(){

    List<Pair<GEdge, GNode>> out = new ArrayList<>();

    for(String n : graph.nodes()){
      GNode node = graph.getNode(n);

      if(node.containsOption(OptionKeys.AST)){
        StructureGraph g = node.getOption(OptionKeys.AST);

        for(GEdge edge : g.edgeStream().collect(Collectors.toSet())){
          out.add(Pair.of(edge, node));
        }

      }

    }


    return out;

  }

  private List<GEdge> edges(){
    return graph.edgeStream().collect(Collectors.toList());
  }

  public void writeTo(Path pPath) throws IOException {

    BufferedWriter writer = Files.newBufferedWriter(pPath);
    try{
      writer.write("{\"nodes\":{");


      List<Pair<GNode, GNode>> nodes = nodes();
      for(int i = 0; i < nodes.size(); i++){

        Pair<GNode, GNode> pair = nodes.get(i);

        final int f = i;
        processNode(pair.getFirst(), pair.getSecond(),
            x -> {
              try {
                writer.write(x+(f == nodes.size()-1?"":", "));
              } catch (IOException pE) {
              }
            });

      }
      writer.write("},\"edges\":[");

      List<Pair<GEdge, GNode>> ast = astEdges();
      for (int i = 0; i < ast.size(); i++) {

        Pair<GEdge, GNode> astEdge = ast.get(i);

        final int f = i;
        processEdge(astEdge.getFirst(), astEdge.getSecond(),
            x -> {
              try {
                writer.write(x + (f == nodes.size() - 1 ? "" : ", "));
              } catch (IOException pE) {
              }
            });

      }


      List<GEdge> edges = edges();
      if(edges.size() > 0 && ast.size() == 0)writer.write(", ");

      for(int i = 0; i < edges.size(); i++){
        GEdge edge = edges.get(i);

        final int f = i;
        processEdge(edge, null,
            x -> {
              try {
                writer.write(x + (f == edges.size() - 1 ? "" : ", "));
              } catch (IOException pE) {
              }
            });
      }

      writer.write("]}");
    }finally {
      writer.close();
    }


  }


  public void writeToNIO(Path pPath) throws IOException {

    StringBuilder sb = new StringBuilder();

    sb.append("{\"nodes\":{");


    List<Pair<GNode, GNode>> nodes = nodes();
    for(int i = 0; i < nodes.size(); i++){

      Pair<GNode, GNode> pair = nodes.get(i);

      final int f = i;
      processNode(pair.getFirst(), pair.getSecond(),
          x -> {
              sb.append(x+(f == nodes.size()-1?"":", "));
          });

    }
    sb.append("},\"edges\":[");

    List<Pair<GEdge, GNode>> ast = astEdges();
    for (int i = 0; i < ast.size(); i++) {

      Pair<GEdge, GNode> astEdge = ast.get(i);

      final int f = i;
      processEdge(astEdge.getFirst(), astEdge.getSecond(),
          x -> {
              sb.append(x + (f == nodes.size() - 1 ? "" : ", "));
          });

    }


    List<GEdge> edges = edges();
    if(edges.size() > 0 && ast.size() > 0)sb.append(", ");

    for(int i = 0; i < edges.size(); i++){
      GEdge edge = edges.get(i);

      final int f = i;
      processEdge(edge, null,
          x -> {
            sb.append(x + (f == edges.size() - 1 ? "" : ", "));
          });
    }

    sb.append("]}");

    writeWithNIO(pPath, sb.toString());
  }

  private void writeWithNIO(Path pPath, String text)
      throws IOException {

    RandomAccessFile file = null;
    FileChannel channel = null;

    try {
      file = new RandomAccessFile(pPath.toString(), "rw");
      channel = file.getChannel();

      byte[] bytes = text.getBytes("UTF-8");
      ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
      byteBuffer.put(bytes);
      byteBuffer.flip();
      channel.write(byteBuffer);
    }finally {
      if(file != null)file.close();
      if(channel != null)channel.close();
    }

  }


}
