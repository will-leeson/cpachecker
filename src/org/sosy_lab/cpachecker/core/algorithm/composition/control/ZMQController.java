// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition.control;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.composition.control.ZMQMessages.ControlFeedback;
import org.sosy_lab.cpachecker.core.algorithm.composition.control.ZMQMessages.ControlResponse;
import org.sosy_lab.cpachecker.core.algorithm.composition.control.ZMQMessages.CoveredLine;
import org.sosy_lab.cpachecker.core.algorithm.composition.control.ZMQMessages.VerifierAvailableActions;
import org.sosy_lab.cpachecker.core.algorithm.composition.control.ZMQMessages.VerifierCommand;
import org.sosy_lab.cpachecker.core.algorithm.composition.control.ZMQMessages.VerifierNextStep;
import org.sosy_lab.cpachecker.core.algorithm.composition.control.ZMQMessages.VerifierResponse;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Socket;

public class ZMQController implements ICompositionController {

  private LogManager logger;

  private String socketAddress;

  private List<String> availableActions;
  private List<Integer> availableTimeLimits;


  private int currentAction;
  private int currentTimeLimit;
  private boolean nextActionArrived = false;
  private boolean hasUpdate = false;

  private double currentProgress;
  private int currentTimeConsumption;
  private List<CFAEdge> currentCoverage;


  private ZContext zmqContext;
  private Socket socket;


  public ZMQController(
      LogManager pLogger,
      String pSocketAddress,
      List<String> pAvailableActions, List<Integer> pAvailableTimeLimits) {
    logger = pLogger;
    socketAddress = pSocketAddress;
    availableActions = pAvailableActions;
    availableTimeLimits = pAvailableTimeLimits;

  }

  private void setupZMQ(){
    if(socket != null)return;


    logger.log(Level.INFO, "Create socket at address '"+socketAddress+"'. Await next command from remote.");

    zmqContext = new ZContext();

    socket = zmqContext.createSocket(SocketType.REP);
    socket.connect(socketAddress);

    }


  @Override
  public void reportAlgorithmProgress(double progress, int timeConsumption) {
      currentProgress = progress;
      currentTimeConsumption = timeConsumption;
      hasUpdate = true;
  }

  @Override
  public void reportCoverage(List<CFAEdge> coveredEdges) {
      currentCoverage = coveredEdges;
      hasUpdate = true;
  }

  @Override
  public boolean hasNextControlAction() {

    //TODO How can I decide whether the controller stopped if the controller has not decided yet?
    // We assume that there is always a next configuration!

    return currentAction != -1;
  }

  @Override
  public AlgorithmControl nextControlAction() {

      nextActionArrived = false;

      while(!nextActionArrived)
        awaitCommand(30000);

      if(currentAction == -1)
        zmqContext.close();

     return new AlgorithmControl(
         this.availableActions.get(currentAction),
         this.availableTimeLimits.get(currentTimeLimit)
     );

  }

  private Set<CoveredLine> coveredLines(){
    Set<CoveredLine> lines = new HashSet<>();

    if(this.currentCoverage == null)
      return lines;

    //We overapproximate here
    //If a covered edge covers a statement partially, we say it is covered fully
    for(CFAEdge edge : this.currentCoverage){

      FileLocation location = edge.getFileLocation();

      CoveredLine line = CoveredLine.newBuilder()
                            .setLineNumber(location.getStartingLineInOrigin())
                            .setCharOffset(location.getNodeOffset())
                            .setLineLength(location.getNodeLength())
                            .build();

      lines.add(line);

    }

    return lines;
  }


  private void handleStatusRequest(Command pCommand){

    if(!hasUpdate && !nextActionArrived){
      ControlResponse response = ControlResponse.newBuilder()
                                    .setActionId(currentAction)
                                    .setTimelimitId(currentTimeLimit)
                                    .setStatusCode(101)
                                    .build();

      pCommand.getVerifierResponseBuilder().setResponse(response);
      pCommand.reply();
      return;
    }


    ControlFeedback.Builder controlFeedbackBuilder = ControlFeedback.newBuilder();

    controlFeedbackBuilder.setActionId(currentAction);
    controlFeedbackBuilder.setTimelimitId(currentTimeLimit);
    controlFeedbackBuilder.setProgress((float) currentProgress);
    controlFeedbackBuilder.setTimeconsumption(currentTimeConsumption);

    controlFeedbackBuilder.addAllCoveredLines(coveredLines());

    ControlFeedback feedback = controlFeedbackBuilder.build();

    pCommand.getVerifierResponseBuilder().setStatus(feedback);
    pCommand.reply();

    hasUpdate = false;

  }

  private void handleAvailable(Command pCommand){

    List<String> timelimits = availableTimeLimits.stream().map(x -> Integer.toString(x)).collect(
        Collectors.toList());


    VerifierAvailableActions availableResponse = VerifierAvailableActions.newBuilder()
                                                        .addAllActionIds(availableActions)
                                                        .addAllTimelimitIds(timelimits)
                                                        .build();

    pCommand.getVerifierResponseBuilder().setActions(availableResponse);
    pCommand.reply();

  }

  private void handleControl(Command pCommand, VerifierNextStep pVerifierNextStep){

    int statusCode = 0;

    currentAction = pVerifierNextStep.getActionId();
    currentTimeLimit = pVerifierNextStep.getTimelimitId();

    if(currentAction >= this.availableActions.size() || currentTimeLimit >= this.availableTimeLimits.size())
        statusCode = 2;
    else
      nextActionArrived = true;


    ControlResponse response = ControlResponse.newBuilder()
        .setActionId(currentAction)
        .setTimelimitId(currentTimeLimit)
        .setStatusCode(statusCode)
        .build();

    pCommand.getVerifierResponseBuilder().setResponse(response);
    pCommand.reply();

  }


  private void handleShutdown(Command pCommand){

    ControlResponse response = ControlResponse.newBuilder()
        .setActionId(currentAction)
        .setTimelimitId(currentTimeLimit)
        .setStatusCode(1)
        .build();

    currentAction = -1;
    nextActionArrived = true;

    pCommand.getVerifierResponseBuilder().setResponse(response);
    pCommand.reply();

  }


  private void handleCommand(Command pCommand){

      VerifierCommand action = pCommand.getCommand();

      if(action.hasAvailable()){
          handleAvailable(pCommand);
          return;
      }

      if(action.hasStatus()){
          handleStatusRequest(pCommand);
          return;
      }

      if(action.hasControl()){
        handleControl(pCommand, action.getControl());
        return;
      }

      if(action.hasShutdown()){
        handleShutdown(pCommand);
        return;
      }


  }


  private void awaitCommand(int timeout){
    setupZMQ();
    socket.setSendTimeOut(timeout); // 30 second send timeout
    socket.setReceiveTimeOut(timeout); // 30 second receive timeout

    byte[] recv = socket.recv(0);

    if(recv == null)
      return;

    VerifierResponse response = null;

    try {
      VerifierCommand command = VerifierCommand.parseFrom(recv);

      handleCommand(
          new Command(command, socket)
      );
      return;

    } catch (InvalidProtocolBufferException pE) {

      pE.printStackTrace();

      response = VerifierResponse.newBuilder()
                                    .setResponse(
                                        ControlResponse.newBuilder()
                                            .setActionId(currentAction)
                                            .setTimelimitId(currentTimeLimit)
                                            .setStatusCode(404)
                                            .build()
                                    ).build();


    }

    if(response != null)
      socket.send(response.toByteArray());



  }


  private static class Command {

    private VerifierCommand command;
    private Socket socket;
    private VerifierResponse.Builder verifierResponseBuilder;

    public Command(VerifierCommand pCommand, Socket pSocket){
      this.command = pCommand;
      this.socket = pSocket;
      this.verifierResponseBuilder = VerifierResponse.newBuilder();
    }

    public VerifierCommand getCommand(){
      return command;
    }

    public VerifierResponse.Builder getVerifierResponseBuilder(){
      return verifierResponseBuilder;
    }

    public void reply(){
      VerifierResponse response = getVerifierResponseBuilder().build();
      socket.send(response.toByteArray());
    }

  }

}
