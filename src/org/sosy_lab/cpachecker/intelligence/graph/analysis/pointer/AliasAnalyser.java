package org.sosy_lab.cpachecker.intelligence.graph.analysis.pointer;

import com.google.common.collect.ImmutableTable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;

import java.util.*;
import java.util.stream.Collectors;

public class AliasAnalyser {

    public static final ImmutableTable<String, String, String> phaseOne = ImmutableTable.<String, String, String>builder()
            .put("a", "M", "v_1")
            .put("a", "v_1", "v_1")
            .put("a", "vp_1", "v_1")
            .put("d", "M", "dp_1")
            .put("d", "v_1", "dp_1")
            .put("d", "vp_1", "dp_1")
            .put("M", "M", "x")
            .put("M", "v_1", "vp_1")
            .put("M", "vp_1", "x")
            .build();

    public static final ImmutableTable<String, String, String> phaseTwo = ImmutableTable.<String, String, String>builder()
            .put("a", "dp_1", "d_2")
            .put("a", "d_1", "d_2")
            .put("a", "dp_2", "d_2")
            .put("a", "d_2", "d_2")
            .put("ap", "dp_1", "d_1")
            .put("ap", "d_1", "d_1")
            .put("ap", "dp_2", "x")
            .put("ap", "d_2", "x")
            .put("d", "dp_1", "M")
            .put("d", "d_1", "M")
            .put("d", "dp_2", "M")
            .put("d", "d_2", "M")
            .put("M", "dp_1", "x")
            .put("M", "d_1", "dp_1")
            .put("M", "dp_2", "x")
            .put("M", "d_2", "dp_2")
            .build();


    private PEGraph graph;
    private ShutdownNotifier shutdownNotifier;

    public AliasAnalyser(PEGraph graph, ShutdownNotifier shutdownNotifier) {
        this.graph = graph;
        this.shutdownNotifier = shutdownNotifier;
    }

    private boolean isNeeded(){
        for(String n: graph.nodes()){
            GNode node = graph.getNode(n);
            if(node.getLabel().startsWith("*")){
                return true;
            }
        }
        return false;
    }

    private void memoryAlias() throws InterruptedException {

        Queue<Triple<String, String, String>> workList = new ArrayDeque<>();

        for(GEdge e : graph.edgeStream().collect(Collectors.toList())){
            if(e instanceof DerefEdge){
                String sink = e.getSink().getId();
                workList.add(Triple.of(sink, "M", sink));
                graph.addMemoryAlias(sink, sink);
            }
        }

        while (!workList.isEmpty()){

            if(shutdownNotifier != null)
                shutdownNotifier.shutdownIfNecessary();

            Triple<String, String, String> triple = workList.poll();
            String u = triple.getFirst();
            String X = triple.getSecond();
            String v = triple.getThird();

            if(Set.of("M", "v_1", "vp_1").contains(X)){
                for(String alpha: List.of("a", "d", "M")){
                    String trans = phaseOne.get(alpha, X);
                    if(trans.equals("x"))continue;
                    for(String w : graph.getOutgoingTypedStream(v, alpha).map(e -> e.getSink().getId()).collect(Collectors.toSet())){
                        if(graph.getEdge(u, w, trans) == null){
                            if(trans.equals("M")){
                                graph.addMemoryAlias(u, w);
                                graph.addMemoryAlias(w, u);
                                workList.add(Triple.of(w, trans, u));
                            }else{
                                graph.addTransition(trans, u, w);
                            }
                            workList.add(Triple.of(u, trans, w));
                        }
                    }
                }
            }else{

                for(String alpha : List.of("a", "ap", "d", "M")){
                    String trans = phaseTwo.get(alpha, X);
                    if(trans.equals("x"))continue;

                    Set<String> neigh;
                    if(alpha.equals("ap")){
                        neigh = graph.getIngoingTypedStream(u, "a").map(e -> e.getSource().getId()).collect(Collectors.toSet());
                    }else{
                        neigh = graph.getOutgoingTypedStream(u, alpha).map(e -> e.getSink().getId()).collect(Collectors.toSet());
                    }

                    for(String w : neigh){
                        if(graph.getEdge(w, v, trans) == null){
                            if(trans.equals("M")){
                                graph.addMemoryAlias(w, v);
                                graph.addMemoryAlias(v, w);
                                workList.add(Triple.of(v, trans, w));
                            }else{
                                graph.addTransition(trans, w, v);
                            }
                            workList.add(Triple.of(w, trans, v));
                        }
                    }

                }

            }

        }
    }

    private void valueAnalysis(){

        //v_1
        Queue<Pair<String, String>> queue = new ArrayDeque<>();

        for(GEdge edge : graph.edgeStream().filter(e -> e.getId().equals("v_1")).collect(Collectors.toSet())){
            queue.add(Pair.of(edge.getSink().getId(), edge.getSource().getId()));
        }

        while (!queue.isEmpty()){
            Pair<String, String> curr = queue.poll();
            String sink = curr.getFirst();
            String source = curr.getSecond();

            for(GEdge e : graph.getOutgoingTypedStream(sink, "v_1").collect(Collectors.toSet())){
                String nSink = e.getSink().getId();

                if(graph.getEdge(source, nSink, "v_1") == null){
                    graph.addTransition("v_1", source, nSink);
                    queue.add(Pair.of(nSink, source));
                }
            }
        }

        for(GEdge edge : graph.edgeStream().filter(e -> e.getId().equals("M")).collect(Collectors.toSet())){

            String source = edge.getSource().getId();

            for(GEdge edgeV : graph.getIngoingTyped(source, "v_1")){
                graph.addTransition("vp_1", edgeV.getSource().getId(), edge.getSink().getId());
            }


        }

        Queue<Triple<String, String, String>> workList = new ArrayDeque<>();

        for(GEdge edge : graph.edgeStream().filter(e -> e.getId().equals("a")).collect(Collectors.toSet())){

            String u = edge.getSource().getId();
            String v = edge.getSink().getId();

            for(String alpha : Set.of("v_1", "vp_1", "M")){

                for(String w : graph.getOutgoingTypedStream(v, alpha).map(e -> e.getSink().getId()).collect(Collectors.toSet())){
                    if(graph.getEdge(u, w, "v_2") == null){
                        graph.addTransition("v_2", u, w);
                        workList.add(Triple.of(u, "v_2", w));
                    }
                }


            }

        }

        while(!workList.isEmpty()){
            Triple<String, String, String> curr = workList.poll();
            String v = curr.getThird();

            for(String alpha : Set.of("v_1", "vp_1", "M", "v_2")){

                for(String w : graph.getOutgoingTypedStream(v, alpha).map(e -> e.getSink().getId()).collect(Collectors.toSet())){
                    if(graph.getEdge(w, v, "v_2") == null){
                        graph.addTransition("v_2", w, v);
                    }
                }

            }

        }



    }

    public Map<String, Set<String>> getAliases() throws InterruptedException {

        Map<String, Set<String>> aliases = new HashMap<>();

        if(isNeeded()) {
            memoryAlias();
            valueAnalysis();

            for (GEdge e : graph.edgeStream().filter(
                    e -> Set.of("M", "v_1", "vp_1", "v_2").contains(e.getId())
            ).collect(Collectors.toSet())) {

                String u = e.getSource().getLabel();
                String v = e.getSink().getLabel();


                for (String x : List.of(u, v)) {
                    if (!aliases.containsKey(x)) {
                        aliases.put(x, new HashSet<>());
                    }

                    aliases.get(x).add(u);
                    aliases.get(x).add(v);
                }

            }
        }

        return aliases;
    }

}
