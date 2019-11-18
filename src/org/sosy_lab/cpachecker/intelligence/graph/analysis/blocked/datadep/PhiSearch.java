package org.sosy_lab.cpachecker.intelligence.graph.analysis.blocked.datadep;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.sosy_lab.common.ShutdownNotifier;

import java.util.*;

public class PhiSearch {

    private Map<String, Set<String>> phis;
    private ShutdownNotifier notifier;

    public PhiSearch(Map<String, Set<String>> pPhis, ShutdownNotifier pShutdownNotifier) {
        this.phis = pPhis;
        this.notifier = pShutdownNotifier;
    }

    public Set<String> resolve(String phi) throws InterruptedException {

        Map<String, Set<String>> tmp = new HashMap<>();
        Multimap<String, String> resolveTo = HashMultimap.create();

        ArrayDeque<String> resolveQ = new ArrayDeque<>();
        ArrayDeque<String> resolvableQ = new ArrayDeque<>();
        resolveQ.add(phi);

        while (!resolveQ.isEmpty()){
            String c = resolveQ.poll();

            if(notifier != null)
                notifier.shutdownIfNecessary();

            Set<String> preds = phis.remove(c);

            if(preds == null){
                preds = new HashSet<>();
            }

            tmp.put(c, preds);
            resolvableQ.addFirst(c);

            for(String pre : preds){
                if(pre.startsWith("phi_")){
                    resolveTo.put(pre, c);
                    if(phis.containsKey(pre)){
                        resolveQ.add(pre);
                    }
                }
            }

        }

        while (!resolvableQ.isEmpty()){
            String c = resolvableQ.pollFirst();

            if(notifier != null)
                notifier.shutdownIfNecessary();

            Set<String> curr = tmp.get(c);
            if(!curr.isEmpty()) phis.put(c, curr);

            for(String succ : resolveTo.get(c)){
                Set<String> next = tmp.get(succ);
                next.remove(c);
                next.addAll(curr);
            }

        }


        return phis.getOrDefault(phi, new HashSet<>());

    }




}
