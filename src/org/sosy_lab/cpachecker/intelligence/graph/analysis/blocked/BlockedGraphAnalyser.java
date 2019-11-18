package org.sosy_lab.cpachecker.intelligence.graph.analysis.blocked;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.intelligence.ast.OptionKeys;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.GraphAnalyser;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.SCCUtil;
import org.sosy_lab.cpachecker.intelligence.graph.analysis.blocked.datadep.*;
import org.sosy_lab.cpachecker.intelligence.graph.model.GEdge;
import org.sosy_lab.cpachecker.intelligence.graph.model.GNode;
import org.sosy_lab.cpachecker.intelligence.graph.model.control.SVGraph;
import org.sosy_lab.cpachecker.intelligence.graph.model.navigator.IGraphNavigator;
import org.sosy_lab.cpachecker.intelligence.graph.model.navigator.SGraphNavigator;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.llvm_j.Use;

import java.util.*;
import java.util.stream.Collectors;

public class BlockedGraphAnalyser extends GraphAnalyser {

    private BlockGraph blockGraph = null;

    public BlockedGraphAnalyser(SVGraph pGraph, ShutdownNotifier pShutdownNotifier, LogManager pLogger) throws InterruptedException {
        super(pGraph, pShutdownNotifier, pLogger);
    }

    public BlockedGraphAnalyser(SVGraph pGraph) throws InterruptedException {
        super(pGraph);
    }

    private BlockGraph generateBlocks() throws InterruptedException {
        if(blockGraph != null)return blockGraph;

        blockGraph = new BlockGraph();

        blockGraph.addBlockNodeFrom(graph.getNode(startNode));

        IGraphNavigator navigator = new SGraphNavigator(graph);

        for(String node : navigator.nodes()){

            if(shutdownNotifier != null)
                shutdownNotifier.shutdownIfNecessary();

            String curr = graph.getNode(node).getLabel();
            if(curr.contains("CONDITION")){
                blockGraph.addBlockNodeFrom(graph.getNode(node));
            }
            Set<String> next = navigator.successor(node);
            if(next.size() > 1){
                for(String s: next)blockGraph.addBlockNodeFrom(graph.getNode(s));
            }
            Set<String> pre = navigator.predecessor(node);
            if(pre.size() > 1){
                blockGraph.addBlockNodeFrom(graph.getNode(node));
            }
        }

        ArrayDeque<String> q = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        q.add(startNode);
        seen.add(startNode);

        while (!q.isEmpty()){
            String curr = q.pollFirst();
            BlockGraph.BlockNode currNode = (BlockGraph.BlockNode) blockGraph.getNode(curr);
            currNode.getSequence().add(curr);

            ArrayDeque<String> q2 = new ArrayDeque<>();
            q2.addAll(navigator.successor(curr));

            while (!q2.isEmpty()){
                String currNext = q2.poll();
                if(blockGraph.getNode(currNext) != null){
                    blockGraph.addEdge(curr, currNext);
                    if(!seen.contains(currNext)){
                        seen.add(currNext);
                        q.add(currNext);
                    }
                }else{
                    currNode.getSequence().add(currNext);
                    q2.addAll(navigator.successor(currNext));
                }
            }

        }

        return blockGraph;
    }

    private void priorUse(BlockGraph blockGraph, BlockUse use, Queue<IUseDefEvent> events){
        for(GEdge e : blockGraph.getIngoing(use.getBlock())){
            if(!use.getVariable().contains("::") || use.getVariable().contains(e.getSource().getOption(OptionKeys.PARENT_FUNC))) {
                events.add(new BlockUse(
                        use.getUsePos(),
                        use.getVariable(),
                        e.getSource().getId()
                ));
            }
        }

    }

    private void localUseDef(BlockGraph blockGraph, Queue<IUseDefEvent> events, Map<String, Set<String>> aliases) throws InterruptedException {

        for(String bNodeId : blockGraph.nodes()){

            BlockGraph.BlockNode bNode = (BlockGraph.BlockNode) blockGraph.getNode(bNodeId);

            //Saves last definition in Block
            Map<String, String> lastDef = new HashMap<>();

            for(String sNodeId : bNode.getSequence()){
                if(shutdownNotifier != null)
                    shutdownNotifier.shutdownIfNecessary();

                GNode sNode = graph.getNode(sNodeId);

                if(sNode == null){
                    //Due to pruning an identifier can disappear
                    continue;
                }

                //Uses variables?
                if(sNode.containsOption(OptionKeys.VARS)){
                    for(String use: aliasGet(sNode.getOption(OptionKeys.VARS), aliases)){
                        if(lastDef.containsKey(use)){
                            events.add(new UseDef(sNodeId, use, lastDef.get(use)));
                        }else{
                            BlockUse bUse = new BlockUse(sNodeId, use, bNodeId);
                            priorUse(blockGraph, bUse, events);
                        }
                    }
                }

                //Defines variables?
                if(sNode.containsOption(OptionKeys.DECL_VARS)){

                    Set<String> decls = sNode.getOption(OptionKeys.DECL_VARS);

                    if(!sNode.getLabel().contains("DECL")){
                        decls = aliasGet(decls, aliases);
                    }

                    for(String decl : decls){
                        lastDef.put(decl, sNodeId);
                    }
                }


            }

            for(Map.Entry<String, String> e : lastDef.entrySet()){
                events.add(new BlockDef(e.getValue(), e.getKey(), bNodeId));
            }

        }

    }

    @Override
    public void applyDD(Map<String, Set<String>> aliases) throws InterruptedException {

        if(aliases == null)
            aliases = new HashMap<>();

        BlockGraph bGraph = generateBlocks();

        Queue<IUseDefEvent> events = new ArrayDeque<>();

        localUseDef(bGraph, events, aliases);

        Queue<UseDef> useDefQueue = new ArrayDeque<>();
        Queue<BlockUse> useQueue = new ArrayDeque<>();

        int phiIndex = 0;
        Map<String, Set<String>> phis = new HashMap<>();
        Table<String, String, String> blockDefs = HashBasedTable.create();


        do {

            if(shutdownNotifier != null)
                shutdownNotifier.shutdownIfNecessary();

            while (!events.isEmpty()){

                IUseDefEvent event = events.poll();

                if(shutdownNotifier != null)
                    shutdownNotifier.shutdownIfNecessary();

                if(event instanceof BlockUse){
                    useQueue.add((BlockUse)event);
                } else if (event instanceof BlockDef){
                    BlockDef def = (BlockDef)event;
                    blockDefs.put(def.getBlock(), def.getVariable(), def.getDefPos());
                } else if (event instanceof UseDef){
                    useDefQueue.add((UseDef)event);
                }

            }

            if(useQueue.isEmpty())break;

            BlockUse use = useQueue.poll();

            if(blockDefs.contains(use.getBlock(), use.getVariable())){

                if(use.getUsePos().startsWith("phi_")){
                    Set<String> phiDefs = phis.get(use.getUsePos());
                    phiDefs.add(blockDefs.get(use.getBlock(), use.getVariable()));
                }else {
                    useDefQueue.add(new UseDef(use.getUsePos(), use.getVariable(), blockDefs.get(use.getBlock(), use.getVariable())));
                }
                continue;
            }

            Set<String> mPred = bGraph.getIngoingStream(use.getBlock()).map(e -> e.getSource().getId())
                                      .collect(Collectors.toSet());

            Set<String> pred = new HashSet<>();

            if(!use.getVariable().contains("::")){
                pred = mPred;
            }else{
                String var = use.getVariable();
                for(String p : mPred){
                    if(var.contains(bGraph.getNode(p).getOption(OptionKeys.PARENT_FUNC)+"::")){
                        pred.add(p);
                    }
                }
            }



            if(pred.isEmpty()){
                GNode n = graph.getNode(use.getUsePos());
                if(n != null)
                    System.out.println("Unknown use-def for ("+n.getLabel()+", "+use.getVariable()+")");
                continue;
            }

            if(mPred.size() == 1){
                useQueue.add(new BlockUse(use.getUsePos(), use.getVariable(), pred.iterator().next()));
                continue;
            }

            String id = "phi_"+(phiIndex++);
            phis.put(id, new HashSet<>());
            events.add(new BlockDef(id, use.getVariable(), use.getBlock()));
            useDefQueue.add(new UseDef(use.getUsePos(), use.getVariable(), id));


            for(String p : pred){
                events.add(new BlockUse(id, use.getVariable(), p));
            }

        } while (!useQueue.isEmpty());

        PhiSearch search = new PhiSearch(phis, shutdownNotifier);

        //Draw dependencies


        while (!useDefQueue.isEmpty()) {
            UseDef useDef = useDefQueue.poll();

            if(shutdownNotifier != null)
                shutdownNotifier.shutdownIfNecessary();

            if (useDef.getDefPos().startsWith("phi_")) {

                for (String s : search.resolve(useDef.getDefPos())) {
                    useDefQueue.add(new UseDef(useDef.getUsePos(), useDef.getVariable(), s));
                }
            } else {
                graph.addDDEdge(useDef.getDefPos(), useDef.getUsePos());
            }

        }

    }




}
