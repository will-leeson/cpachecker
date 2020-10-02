// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition;

import com.google.common.collect.Iterables;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.composition.control.ICompositionController;
import org.sosy_lab.cpachecker.core.algorithm.composition.control.RandomController;
import org.sosy_lab.cpachecker.core.algorithm.composition.control.ZMQController;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

@Options(prefix = "compositionAlgorithm.remote")
public class RemoteCompositionStrategy extends ADynamicCompositionStrategy {

  @Option(
      secure =  true,
      description = "ZMQ address"
  )
  private String socketAddress = null;

  public RemoteCompositionStrategy(
      Configuration pConfig,
      LogManager pLogger) throws InvalidConfigurationException {
    super(pConfig, pLogger);
    pConfig.inject(this);
  }

  @Override
  protected ICompositionController createController(
      Collection<String> controlActions, Collection<Integer> timeDistribution) {

    return new ZMQController(
        super.logger, socketAddress, new ArrayList<>(controlActions), new ArrayList<>(timeDistribution)
    );

  }

  @Override
  public @Nullable String getName() {
    return "Remote Composition";
  }

}
