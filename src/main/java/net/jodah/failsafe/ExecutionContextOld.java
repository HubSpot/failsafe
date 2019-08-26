/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance withMigration the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package net.jodah.failsafe;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Contextual execution information.
 *
 * @author Jonathan Halterman
 */
// For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
@Deprecated
public class ExecutionContextOld {
  final Duration startTime;
  /** Number of execution attempts */
  volatile int executions;

  ExecutionContextOld(Duration startTime) {
    this.startTime = startTime;
  }

  ExecutionContextOld(ExecutionContextOld context) {
    this.startTime = context.startTime;
    this.executions = context.executions;
  }

  /**
   * Returns the elapsed time since initial execution began.
   */
  public Duration getElapsedTime() {
    return Duration.of(System.nanoTime() - startTime.toNanos(), ChronoUnit.NANOS);
  }

  /**
   * Gets the number of executions so far.
   */
  public int getExecutions() {
    return executions;
  }

  /**
   * Returns the time that the initial execution started.
   */
  public Duration getStartTime() {
    return startTime;
  }

  ExecutionContextOld copy() {
    return new ExecutionContextOld(this);
  }
}