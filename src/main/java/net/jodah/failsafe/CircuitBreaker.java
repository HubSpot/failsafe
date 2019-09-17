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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import net.jodah.failsafe.function.CheckedRunnable;
import net.jodah.failsafe.internal.CircuitBreakerInternals;
import net.jodah.failsafe.internal.CircuitState;
import net.jodah.failsafe.internal.ClosedState;
import net.jodah.failsafe.internal.HalfOpenState;
import net.jodah.failsafe.internal.OpenState;
import net.jodah.failsafe.internal.util.Assert;
import net.jodah.failsafe.util.Ratio;

/**
 * A circuit breaker that temporarily halts execution when configurable thresholds are exceeded.
 * <p>
 * A circuit breaker has three states: <i>closed</i>, <i>open</i>, and <i>half-open</i>. When a circuit breaker is in
 * the <i>closed</i> (initial) state, executions are allowed. If a {@link #withFailureThreshold(int) configurable
 * number} of failures occur, the circuit breaker transitions to the <i>open</i> state. In the <i>open</i> state a
 * circuit breaker will fail executions withMigration {@link CircuitBreakerOpenException}. After a {@link #withDelay(Duration)
 * configurable delay}, the circuit breaker will transition to a <i>half-open</i> state. In the
 * <i>half-open</i> state a {@link #withSuccessThreshold(int) configurable number} of trial executions will be allowed,
 * after which the circuit breaker will transition back to <i>closed</i> or <i>open</i> depending on how many were
 * successful.
 * </p>
 * <p>
 * Note: CircuitBreaker extends {@link DelayablePolicy} and {@link FailurePolicy} which offer additional configuration.
 * </p>
 *
 * @param <R> result type
 * @author Jonathan Halterman
 * @see CircuitBreakerOpenException
 */
@SuppressWarnings("WeakerAccess")
public class CircuitBreaker<R> extends DelayablePolicy<CircuitBreaker<R>, R> {
  /** Writes guarded by "this" */
  private final AtomicReference<CircuitState> state = new AtomicReference<>();
  private final AtomicInteger currentExecutions = new AtomicInteger();
  private Duration delay = Duration.ofMinutes(1);
  private Duration timeout;
  private Ratio failureThreshold;
  private Ratio successThreshold;
  CheckedRunnable onOpen;
  CheckedRunnable onHalfOpen;
  CheckedRunnable onClose;

  /**
   * Creates a Circuit that opens after a single failure, closes after a single success, and has a 1 minute delay by
   * default.
   */
  public CircuitBreaker() {
    failureConditions = new ArrayList<>();
    state.set(new ClosedState(this, internals));
  }

  /**
   * The state of the circuit.
   */
  public enum State {
    /** The circuit is closed and fully functional, allowing executions to occur. */
    CLOSED,
    /** The circuit is opened and not allowing executions to occur. */
    OPEN,
    /** The circuit is temporarily allowing executions to occur. */
    HALF_OPEN
  }

  /**
   * Returns whether the circuit allows execution, possibly triggering a state transition.
   */
  public boolean allowsExecution() {
    return state.get().allowsExecution();
  }

  /**
   * Closes the circuit.
   */
  public void close() {
    transitionTo(State.CLOSED, onClose, null);
  }

  /**
   * Returns the delay before allowing another execution on the circuit. Defaults to 1 minute.
   *
   * @see #withDelay(Duration)
   */
  public Duration getDelay() {
    return delay;
  }

  /**
   * Returns the number of failures recorded in the current state when the state is CLOSED or HALF_OPEN. When the state
   * is OPEN, returns the failures recorded during the previous CLOSED state. The max number of failures is based on the
   * configured {@link #getFailureThreshold() failure threshold}.
   */
  public long getFailureCount() {
    return state.get().getFailureCount();
  }

  /**
   * Returns the ratio of failures to successes in the current state when the state is CLOSED or HALF_OPEN. When the
   * state is OPEN, returns the ratio recorded during the previous CLOSED state. The ratio is based on the configured
   * {@link #getFailureThreshold() failure threshold}.
   */
  public Ratio getFailureRatio() {
    return state.get().getFailureRatio();
  }

  /**
   * Returns the number of successes recorded in the current state when the state is CLOSED or HALF_OPEN. When the state
   * is OPEN, returns the successes recorded during the previous CLOSED state.The max number of successes is based on
   * the configured {@link #getSuccessThreshold() success threshold}.
   */
  public int getSuccessCount() {
    return state.get().getSuccessCount();
  }

  /**
   * Returns the ratio of successes to failures in the current state when the state is CLOSED or HALF_OPEN. When the
   * state is OPEN, returns the ratio recorded during the previous CLOSED state. The ratio is based on the configured
   * {@link #getSuccessThreshold() success threshold}.
   */
  public Ratio getSuccessRatio() {
    return state.get().getSuccessRatio();
  }

  /**
   * Gets the ratio of successive failures that must occur when in a closed state in order to open the circuit else
   * {@code null} if none has been configured.
   *
   * @see #withFailureThreshold(int)
   * @see #withFailureThreshold(int, int)
   */
  public Ratio getFailureThreshold() {
    return failureThreshold;
  }

  /**
   * Gets the state of the circuit.
   */
  public State getState() {
    return state.get().getInternals();
  }

  /**
   * Gets the ratio of successive successful executions that must occur when in a half-open state in order to close the
   * circuit else {@code null} if none has been configured.
   *
   * @see #withSuccessThreshold(int)
   * @see #withSuccessThreshold(int, int)
   */
  public Ratio getSuccessThreshold() {
    return successThreshold;
  }

  /**
   * Returns timeout for executions else {@code null} if none has been configured.
   *
   * @deprecated Use {@link Timeout} instead
   * @see #withTimeout(Duration)
   */
  public Duration getTimeout() {
    return timeout;
  }

  /**
   * Half-opens the circuit.
   */
  public void halfOpen() {
    transitionTo(State.HALF_OPEN, onHalfOpen, null);
  }

  /**
   * Returns whether the circuit is closed.
   */
  public boolean isClosed() {
    return State.CLOSED.equals(getState());
  }

  /**
   * Returns whether the circuit is half open.
   */
  public boolean isHalfOpen() {
    return State.HALF_OPEN.equals(getState());
  }

  /**
   * Returns whether the circuit is open.
   */
  public boolean isOpen() {
    return State.OPEN.equals(getState());
  }

  /**
   * Calls the {@code runnable} when the circuit is closed.
   * <p>Note: Any exceptions that are thrown from within the {@code runnable} are ignored.</p>
   */
  /* Return type altered to support runtime compatability of signature bytecode withMigration 1.X failsafe */
  public void onClose(CheckedRunnable runnable) {
    onClose = runnable;
  }

  /**
   * Calls the {@code runnable} when the circuit is half-opened.
   * <p>Note: Any exceptions that are thrown within the {@code runnable} are ignored.</p>
   */
  /* Return type altered to support runtime compatability of signature bytecode withMigration 1.X failsafe */
  public void onHalfOpen(CheckedRunnable runnable) {
    onHalfOpen = runnable;
  }

  /**
   * Calls the {@code runnable} when the circuit is opened.
   * <p>Note: Any exceptions that are thrown within the {@code runnable} are ignored.</p>
   */
  /* Return type altered to support runtime compatability of signature bytecode withMigration 1.X failsafe */
  public void onOpen(CheckedRunnable runnable) {
    onOpen = runnable;
  }

  /**
   * Calls the {@code runnable} when the circuit is closed.
   * <p>Note: Any exceptions that are thrown from within the {@code runnable} are ignored.</p>
   */
  /* Alias for dealing withMigration runtime error on binary incompatibility change of the onClose signature by manually swapping off the affected method name during the migration */
  public void onCloseMigration(CheckedRunnable runnable) {
    onClose = runnable;
  }

  /**
   * Calls the {@code runnable} when the circuit is half-opened.
   * <p>Note: Any exceptions that are thrown within the {@code runnable} are ignored.</p>
   */
  /* Alias for dealing withMigration runtime error on binary incompatibility change of the onHalfOpen signature by manually swapping off the affected method name during the migration */
  public void onHalfOpenMigration(CheckedRunnable runnable) {
    onHalfOpen = runnable;
  }

  /**
   * Calls the {@code runnable} when the circuit is opened.
   * <p>Note: Any exceptions that are thrown within the {@code runnable} are ignored.</p>
   */
  /* Alias for dealing withMigration runtime error on binary incompatibility change of the onOpen signature by manually swapping off the affected method name during the migration */
  public void onOpenMigration(CheckedRunnable runnable) {
    onOpen = runnable;
  }

  /* Backwards compatability for 1.x -> 2.x migration. Use `handle` */
  @Deprecated
  public CircuitBreaker failOn(Class<? extends Throwable> failure) {
    return handle(failure);
  }

  /* Backwards compatability for 1.x -> 2.x migration. Use `handle` */
  @Deprecated
  public CircuitBreaker failOn(Class<? extends Throwable>... failures) {
    return handle(failures);
  }

  /* Backwards compatability for 1.x -> 2.x migration. Use `handle` */
  @Deprecated
  public CircuitBreaker failOn(List<Class<? extends Throwable>> failures) {
    return handle(failures);
  }

  /* Backwards compatability for 1.x -> 2.x migration. Use `handleIf` */
  @Deprecated
  public CircuitBreaker failOn(net.jodah.failsafe.function.Predicate<? extends Throwable> failurePredicate) {
    return handleIf(failurePredicate.toJavaUtil());
  }

  /* Backwards compatability for 1.x -> 2.x migration. Use `handleIf` */
  @Deprecated
  public CircuitBreaker failOn(Predicate<? extends Throwable> failurePredicate) {
    return handleIf(failurePredicate);
  }

  /* Backwards compatability for 1.x -> 2.x migration. Use `handleIf` */
  @Deprecated
  public CircuitBreaker failWhen(R result) {
    return handleIf(resultPredicateFor(result));
  }

  /* Backwards compatability for 1.x -> 2.x migration. Use `handleIf` */
  @Deprecated
  public <T extends Throwable> CircuitBreaker failIf(net.jodah.failsafe.function.Predicate<T> failurePredicate) {
    return handleIf(failurePredicate.toJavaUtil());
  }

  /* Backwards compatability for 1.x -> 2.x migration. Use `handleIf` */
  @Deprecated
  public <T extends Throwable> CircuitBreaker failIf(Predicate<T> failurePredicate) {
    return handleIf(failurePredicate);
  }

  /* Backwards compatability for 1.x -> 2.x migration. Use `handleIf` */
  @Deprecated
  public <T extends Throwable> CircuitBreaker failIf(net.jodah.failsafe.function.BiPredicate<R, T> resultPredicate) {
    return handleIf(resultPredicate.toJavaUtil());
  }

  /* Backwards compatability for 1.x -> 2.x migration. Use `handleIf` */
  @Deprecated
  public <T extends Throwable> CircuitBreaker failIf(BiPredicate<R, T> resultPredicate) {
    return handleIf(resultPredicate);
  }


  /**
   * Opens the circuit.
   */
  public void open() {
    transitionTo(State.OPEN, onOpen, null);
  }

  /**
   * Records an execution that is about to take place by incrementing the internal executions count. Required for
   * standalone CircuitBreaker usage.
   */
  public void preExecute() {
    currentExecutions.incrementAndGet();
  }

  /**
   * Records an execution failure.
   */
  public void recordFailure() {
    recordExecutionFailure(null);
  }

  /**
   * Records an execution {@code failure} as a success or failure based on the failure configuration as determined by
   * {@link #isFailure(R, Throwable)}.
   *
   * @see #isFailure(R, Throwable)
   */
  public void recordFailure(Throwable failure) {
    recordResult(null, failure);
  }

  /**
   * Records an execution {@code result} as a success or failure based on the failure configuration as determined by
   * {@link #isFailure(R, Throwable)}.
   *
   * @see #isFailure(R, Throwable)
   */
  public void recordResult(R result) {
    recordResult(result, null);
  }

  /**
   * Records an execution success.
   */
  public void recordSuccess() {
    try {
      state.get().recordSuccess();
    } finally {
      currentExecutions.decrementAndGet();
    }
  }

  @Override
  public String toString() {
    return getState().toString();
  }

  /**
   * Sets the {@code delay} to wait in open state before transitioning to half-open.
   *
   * @throws NullPointerException if {@code delay} is null
   * @throws IllegalArgumentException if {@code delay} < 0
   */
  public CircuitBreaker<R> withDelay(Duration delay) {
    Assert.notNull(delay, "delay");
    Assert.isTrue(delay.toNanos() >= 0, "delay must not be negative");
    this.delay = delay;
    return this;
  }

  /* Backwards compatability for 1.x -> 2.x migration */
  @Deprecated
  public CircuitBreaker withDelay(long delay, TimeUnit timeUnit) {
    return withDelay(Duration.of(delay, TimeUnitToChronoUnit.toChronoUnit(timeUnit)));
  }

  /**
   * Sets the number of successive failures that must occur when in a closed state in order to open the circuit.
   * <p>
   * If a {@link #withSuccessThreshold(int) success threshold} is not configured, the {@code failureThreshold} will also
   * be used when the circuit breaker is in a half-open state to determine whether to transition back to open or
   * closed.
   * </p>
   *
   * @throws IllegalArgumentException if {@code failureThresh} < 1
   */
  public CircuitBreaker<R> withFailureThreshold(int failureThreshold) {
    Assert.isTrue(failureThreshold >= 1, "failureThreshold must be greater than or equal to 1");
    return withFailureThreshold(failureThreshold, failureThreshold);
  }

  /**
   * Sets the ratio of successive failures that must occur when in a closed state in order to open the circuit. For
   * example: 5, 10 would open the circuit if 5 out of the last 10 executions result in a failure. The circuit will not
   * be opened until at least the given number of {@code executions} have taken place.
   * <p>
   * If a {@link #withSuccessThreshold(int) success threshold} is not configured, the {@code failureThreshold} will also
   * be used when the circuit breaker is in a half-open state to determine whether to transition back to open or
   * closed.
   * </p>
   *
   * @param failures The number of failures that must occur in order to open the circuit
   * @param executions The number of executions to measure the {@code failures} against
   * @throws IllegalArgumentException if {@code failures} < 1, {@code executions} < 1, or {@code failures} is > {@code
   * executions}
   */
  public synchronized CircuitBreaker<R> withFailureThreshold(int failures, int executions) {
    Assert.isTrue(failures >= 1, "failures must be greater than or equal to 1");
    Assert.isTrue(executions >= 1, "executions must be greater than or equal to 1");
    Assert.isTrue(executions >= failures, "executions must be greater than or equal to failures");
    this.failureThreshold = new Ratio(failures, executions);
    state.get().setFailureThreshold(failureThreshold);
    return this;
  }

  /**
   * Sets the number of successive successful executions that must occur when in a half-open state in order to close the
   * circuit, else the circuit is re-opened when a failure occurs.
   *
   * @throws IllegalArgumentException if {@code successThreshold} < 1
   */
  public CircuitBreaker<R> withSuccessThreshold(int successThreshold) {
    Assert.isTrue(successThreshold >= 1, "successThreshold must be greater than or equal to 1");
    return withSuccessThreshold(successThreshold, successThreshold);
  }

  /**
   * Sets the ratio of successive successful executions that must occur when in a half-open state in order to close the
   * circuit. For example: 5, 10 would close the circuit if 5 out of the last 10 executions were successful. The circuit
   * will not be closed until at least the given number of {@code executions} have taken place.
   *
   * @param successes The number of successful executions that must occur in order to open the circuit
   * @param executions The number of executions to measure the {@code successes} against
   * @throws IllegalArgumentException if {@code successes} < 1, {@code executions} < 1, or {@code successes} is > {@code
   * executions}
   */
  public synchronized CircuitBreaker<R> withSuccessThreshold(int successes, int executions) {
    Assert.isTrue(successes >= 1, "successes must be greater than or equal to 1");
    Assert.isTrue(executions >= 1, "executions must be greater than or equal to 1");
    Assert.isTrue(executions >= successes, "executions must be greater than or equal to successes");
    this.successThreshold = new Ratio(successes, executions);
    state.get().setSuccessThreshold(successThreshold);
    return this;
  }

  public CircuitBreaker withTimeout(Duration timeout) {
    Assert.notNull(timeout, "timeout");
    Assert.isTrue(timeout.toNanos() > 0, "timeout must be greater than 0");
    this.timeout = timeout;
    return this;
  }

  /**
   * Sets the {@code timeout} for executions. Executions that exceed this timeout are not interrupted, but are recorded
   * as failures once they naturally complete.
   *
   * @throws NullPointerException if {@code timeUnit} is null
   * @throws IllegalArgumentException if {@code timeout} <= 0
   */
  /* Backwards compatability for 1.x -> 2.x migration */
  @Deprecated
  public CircuitBreaker withTimeout(long timeout, TimeUnit timeUnit) {
    return withTimeout(Duration.of(timeout, TimeUnitToChronoUnit.toChronoUnit(timeUnit)));
  }

  void recordResult(R result, Throwable failure) {
    try {
      if (isFailure(result, failure))
        state.get().recordFailure(null);
      else
        state.get().recordSuccess();
    } finally {
      currentExecutions.decrementAndGet();
    }
  }

  /**
   * Transitions to the {@code newState} if not already in that state and calls any associated event listener.
   */
  private void transitionTo(State newState, CheckedRunnable listener, ExecutionContext context) {
    boolean transitioned = false;
    synchronized (this) {
      if (!getState().equals(newState)) {
        switch (newState) {
          case CLOSED:
            state.set(new ClosedState(this, internals));
            break;
          case OPEN:
            Duration computedDelay = computeDelay(context);
            state.set(new OpenState(this, state.get(), computedDelay != null ? computedDelay : delay));
            break;
          case HALF_OPEN:
            state.set(new HalfOpenState(this, internals));
            break;
        }
        transitioned = true;
      }
    }

    if (transitioned && listener != null) {
      try {
        listener.run();
      } catch (Exception ignore) {
      }
    }
  }

  /**
   * Records an execution failure.
   */
  void recordExecutionFailure(ExecutionContext context) {
    try {
      state.get().recordFailure(context);
    } finally {
      currentExecutions.decrementAndGet();
    }
  }

  // Internal delegate implementation
  final CircuitBreakerInternals internals = new CircuitBreakerInternals() {
    @Override
    public int getCurrentExecutions() {
      return currentExecutions.get();
    }

    @Override
    public void open(ExecutionContext context) {
      transitionTo(State.OPEN, onOpen, context);
    }
  };

  @Override
  public PolicyExecutor toExecutor(AbstractExecution execution) {
    return new CircuitBreakerExecutor(this, execution);
  }
}
