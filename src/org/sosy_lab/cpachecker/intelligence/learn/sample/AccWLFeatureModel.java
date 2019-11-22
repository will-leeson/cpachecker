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
package org.sosy_lab.cpachecker.intelligence.learn.sample;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Bytes;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.ast.base.CFAProcessor;
import org.sosy_lab.cpachecker.intelligence.ast.neural.SVPEProcessor;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.GraphAnalyser;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.NativeGraphAnalyser;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.blocked.BlockedGraphAnalyser;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.pointer.AliasAnalyser;
import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;
import org.sosy_lab.cpachecker.intelligence.graph.model.StructureGraph;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.SVGraph;
import org.sosy_lab.cpachecker.util.Pair;
import ove.crypto.digest.Blake2b;

public class AccWLFeatureModel implements IWLFeatureModel
{
    private static Map<String, String> relabelLabel;
    private CFA cfa;
    private int astDepth;
    private SVGraph graph;
    private GraphAnalyser analyser;
    private Map<String, Map<String, String>> relabel;
    private int iteration;

    private static Map<String, String> relabelLabel() {
        if (AccWLFeatureModel.relabelLabel == null) {
            final Map<String, String> map = new HashMap<String, String>();
            map.put("UNSIGNED_INT", "INT");
            map.put("LONG_UNSIGNED_INT", "LONG");
            map.put("LONG_INT", "LONG");
            map.put("LONGLONG_UNSIGNED_INT", "LONG");
            map.put("LONGLONG_INT", "LONG");
            map.put("LONG_UNSIGNED_LONG", "LONG");
            map.put("LONG_LONG", "LONG");
            map.put("UNSIGNED_CHAR", "CHAR");
            map.put("VOLATILE_LONG_LONG", "VOLATILE_LONG");
            map.put("VOLATILE_LONG_UNSIGNED_INT", "VOLATILE_LONG");
            map.put("VOLATILE_LONG_INT", "VOLATILE_LONG");
            map.put("VOLATILE_LONG_UNSIGNED_LONG", "VOLATILE_LONG");
            map.put("VOLATILE_UNSIGNED_INT", "VOLATILE_INT");
            map.put("CONST_UNSIGNED_INT", "CONST_INT");
            map.put("CONST_LONG_LONG", "CONST_LONG");
            map.put("CONST_LONG_UNSIGNED_LONG", "CONST_LONG");
            map.put("CONST_LONGLONG_UNSIGNED_LONGLONG", "CONST_LONG");
            map.put("CONST_LONGLONG_LONGLONG", "CONST_LONG");
            map.put("CONST_UNSIGNED_CHAR", "CONST_CHAR");
            map.put("INT_LITERAL_SMALL", "INT_LITERAL");
            map.put("INT_LITERAL_MEDIUM", "INT_LITERAL");
            map.put("INT_LITERAL_LARGE", "INT_LITERAL");
            AccWLFeatureModel.relabelLabel = (Map<String, String>)ImmutableMap.copyOf((Map)map);
        }
        return AccWLFeatureModel.relabelLabel;
    }

    public AccWLFeatureModel(final CFA pCFA, final int pAstDepth) {
        this.relabel = new HashMap<String, Map<String, String>>();
        this.iteration = -1;
        this.cfa = pCFA;
        this.astDepth = pAstDepth;
    }

    @Override
    public int getAstDepth() {
        return this.astDepth;
    }

    private Map<String, Integer> iteration0(final ShutdownNotifier pShutdownNotifier) throws InterruptedException {
        this.graph = this.getGraph(pShutdownNotifier);
        final Map<String, Integer> count = new HashMap<String, Integer>();
        for (final String n : this.graph.nodes()) {
            final GNode node = this.graph.getNode(n);
            if (pShutdownNotifier != null) {
                pShutdownNotifier.shutdownIfNecessary();
            }
            String label = node.getLabel();
            if (!count.containsKey(label)) {
                count.put(label, 0);
            }
            count.put(label, count.get(label) + 1);
            if (node.containsOption(OptionKeys.AST)) {
                final StructureGraph ast = node.getOption(OptionKeys.AST);
                for (final String a : ast.nodes()) {
                    label = ast.getNode(a).getLabel();
                    if (!count.containsKey(label)) {
                        count.put(label, 0);
                    }
                    count.put(label, count.get(label) + 1);
                }
            }
        }
        return count;
    }

    @Override
    public SVGraph getGraph(final ShutdownNotifier pShutdownNotifier) throws InterruptedException {
        if (this.graph == null) {
            this.graph = new SVPEProcessor().process(this.cfa, pShutdownNotifier);
        }
        return this.graph;
    }

    private GraphAnalyser getGraphAnalyser(final ShutdownNotifier pShutdownNotifier) throws InterruptedException {
        if (this.analyser == null) {
            final SVGraph g = this.getGraph(pShutdownNotifier);
            this.analyser = new BlockedGraphAnalyser(g, pShutdownNotifier, null);
        }
        return this.analyser;
    }

    private String hash(final String str) {
        final Blake2b blake2b = (Blake2b)Blake2b.Digest.newInstance();
        final byte[] bytes = blake2b.digest(str.getBytes(StandardCharsets.UTF_8));
        String h;
        for (h = new BigInteger(1, bytes).toString(16); h.length() < 128; h = '0' + h) {}
        return h;
    }

    private String relabel(final String source, final List<String> neighbours) {
        Collections.sort(neighbours);
        neighbours.add(0, source);
        final String text = String.join("_", neighbours);
        return this.hash(text);
    }

    private String loadLabel(final GNode node, final Map<String, String> lookup) {
        if (node == null) {
            return "";
        }
        final String id = node.getId();
        if (lookup.containsKey(id)) {
            return lookup.get(id);
        }
        return node.getLabel();
    }

    private Map<String, String> relabelAST(final StructureGraph ast, final String start, final Map<String, String> lookup) {
        if (ast == null || start == null) {
            return new HashMap<String, String>();
        }
        final Map<String, String> nextLabel = new HashMap<>();
        final Queue<Pair<String, Integer>> astSearch = new ArrayDeque<>();
        astSearch.add(Pair.of(start, 1));
        while (!astSearch.isEmpty()) {
            final Pair<String, Integer> p = astSearch.poll();
            final String nodeId = p.getFirst();
            final int depth = p.getSecond();
            final GNode astNode = ast.getNode(nodeId);
            if (astNode == null) {
                continue;
            }
            final List<String> neighLabels = new ArrayList<>();
            if (depth + 1 <= this.astDepth) {
                for (final GEdge edge : ast.getIngoing(nodeId)) {
                    final GNode in = edge.getSource();
                    neighLabels.add(String.join("_", "s", this.loadLabel(in, lookup)));
                    astSearch.add(Pair.of(in.getId(), depth + 1));
                }
            }
            nextLabel.put(astNode.getId(), this.relabel(this.loadLabel(astNode, lookup), neighLabels));
        }
        return nextLabel;
    }

    private Map<String, String> relabelNode(final String nodeId, final Map<String, Map<String, String>> relabelIndex) {
        final Map<String, String> relabels = relabelIndex.getOrDefault(nodeId, new HashMap<String, String>());
        final GNode node = this.graph.getNode(nodeId);
        Map<String, String> astRelabel = new HashMap<String, String>();
        String astCore = null;
        if (node.containsOption(OptionKeys.AST)) {
            final StructureGraph ast = node.getOption(OptionKeys.AST);
            if (!ast.isEmpty()) {
                astRelabel = this.relabelAST(node.getOption(OptionKeys.AST), node.getOption(OptionKeys.AST_ROOT), relabels);
                astCore = this.loadLabel(node.getOption(OptionKeys.AST).getNode(node.getOption(OptionKeys.AST_ROOT)), relabels);
            }
        }
        final List<String> neighLabels = new ArrayList<String>();
        for (final GEdge incoming : this.graph.getIngoing(nodeId)) {
            final GNode in = incoming.getSource();
            final String label = this.loadLabel(in, relabelIndex.getOrDefault(in.getId(), new HashMap<String, String>()));
            neighLabels.add(String.join("_", incoming.getId(), label));
        }
        if (astCore != null) {
            neighLabels.add(String.join("_", "s", astCore));
        }
        final String text = this.relabel(this.loadLabel(node, relabels), neighLabels);
        astRelabel.put(nodeId, text);
        return astRelabel;
    }

    @Override
    public Map<String, Integer> iterate() {
        try {
            return this.iterate(null);
        }
        catch (InterruptedException pE) {
            return new HashMap<String, Integer>();
        }
    }

    @Override
    public Map<String, Integer> iterate(final ShutdownNotifier pShutdownNotifier) throws InterruptedException {
        ++this.iteration;
        final GraphAnalyser localAnalyser = this.getGraphAnalyser(pShutdownNotifier);
        if (this.iteration == 0) {
            localAnalyser.simplify();
            localAnalyser.recursionDetection();
            return this.iteration0(pShutdownNotifier);
        }
        if (this.iteration == 1) {
            Map<String, Set<String>> aliases = null;
            if (this.graph.getGlobalOption(OptionKeys.POINTER_GRAPH) != null) {
                AliasAnalyser aliasAnalyser = new AliasAnalyser(this.graph.getGlobalOption(OptionKeys.POINTER_GRAPH), pShutdownNotifier);
                aliases = aliasAnalyser.getAliases();
                this.graph.setGlobalOption(OptionKeys.POINTER_GRAPH, null);
                aliasAnalyser = null;
            }
            localAnalyser.applyDD(aliases);
            localAnalyser.disconnectFunctionsViaDependencies();
            localAnalyser.applyCD();
        }
        if (pShutdownNotifier != null) {
            pShutdownNotifier.shutdownIfNecessary();
        }
        final Map<String, Map<String, String>> oldRelabel = this.relabel;
        this.relabel = new HashMap<String, Map<String, String>>();
        final Map<String, Integer> count = new HashMap<String, Integer>();
        for (final String n : this.graph.nodes()) {
            if (pShutdownNotifier != null) {
                pShutdownNotifier.shutdownIfNecessary();
            }
            final Map<String, String> labels = this.relabelNode(n, oldRelabel);
            this.relabel.put(n, labels);
            for (final String label : labels.values()) {
                if (!count.containsKey(label)) {
                    count.put(label, 0);
                }
                count.put(label, count.get(label) + 1);
            }
        }
        return count;
    }

    static {
        AccWLFeatureModel.relabelLabel = null;
    }
}

