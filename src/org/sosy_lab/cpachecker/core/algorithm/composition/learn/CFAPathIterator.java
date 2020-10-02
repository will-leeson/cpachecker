// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition.learn;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;

public class CFAPathIterator implements Iterator<CFAEdge> {

  private Random randomInstance;
  private CFAEdge currentEdge;
  private Set<Integer> seenNodes = new HashSet<>();

  public CFAPathIterator(CFAEdge pStartEdge) {
    currentEdge = pStartEdge;
    randomInstance = new Random();
  }


  @Override
  public boolean hasNext() {
    return currentEdge != null;
  }

  private CFAEdge predecessor(){

    CFANode currentNode = currentEdge.getPredecessor();
    int numEntering = currentNode.getNumEnteringEdges();

    if(numEntering == 0)
      return null;

    if(currentNode.getNumEnteringEdges() == 1)
      return currentNode.getEnteringEdge(0);

    int choice = randomInstance.nextInt(numEntering);

    CFAEdge choiceEdge = currentNode.getEnteringEdge(choice);

    int incomingNodeId = choiceEdge.getPredecessor().getNodeNumber();

    //If we have seen this edge before, we will adjust the choice

    int startChoice = choice;
    boolean addEdge = true;

    while (seenNodes.contains(incomingNodeId)){
      choice = (choice + 1) % numEntering;
      if(choice == startChoice) {
        addEdge = false;
        break;
      }
      choiceEdge = currentNode.getEnteringEdge(choice);
      incomingNodeId = choiceEdge.getPredecessor().getNodeNumber();
    }

    if(!addEdge)return null;

    return choiceEdge;
  }

  @Override
  public CFAEdge next() {
    if(!hasNext()) throw new NoSuchElementException();

    CFAEdge returnEdge = currentEdge;
    currentEdge = predecessor();

    if(currentEdge != null)
      seenNodes.add(currentEdge.getPredecessor().getNodeNumber());

    return returnEdge;
  }
}
