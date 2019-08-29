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

import net.jodah.failsafe.function.*;
import net.jodah.failsafe.internal.util.Assert;
import net.jodah.failsafe.util.concurrent.Scheduler;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Utilities for creating and applying functions.
 *
 * @author Jonathan Halterman
 */
final class Functions {
  private static final CompletableFuture<ExecutionResult> NULL_FUTURE = CompletableFuture.completedFuture(null);

  interface SettableSupplier<T> extends Supplier<T> {
    void set(T value);
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static abstract class AsyncCallableWrapper<T> implements Callable<T> {
    protected AsyncExecutionOld execution;

    void inject(AsyncExecutionOld execution) {
      this.execution = execution;
    }
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static abstract class ContextualCallableWrapper<T> implements Callable<T> {
    protected ExecutionContextOld context;

    void inject(ExecutionContextOld context) {
      this.context = context;
    }
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static <T> AsyncCallableWrapper<T> asyncOf(final AsyncCallable<T> callable) {
    Assert.notNull(callable, "callable");
    return new AsyncCallableWrapper<T>() {
      @Override
      public synchronized T call() throws Exception {
        try {
          execution.before();
          T result = callable.call(execution);
          return result;
        } catch (Throwable e) {
          execution.completeOrRetry(null, e);
          return null;
        }
      }
    };
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static <T> AsyncCallableWrapper<T> asyncOf(final AsyncRunnableOld runnable) {
    Assert.notNull(runnable, "runnable");
    return new AsyncCallableWrapper<T>() {
      @Override
      public synchronized T call() throws Exception {
        try {
          execution.before();
          runnable.run(execution);
        } catch (Throwable e) {
          execution.completeOrRetry(null, e);
        }

        return null;
      }
    };
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static <T> AsyncCallableWrapper<T> asyncOf(final Callable<T> callable) {
    Assert.notNull(callable, "callable");
    return new AsyncCallableWrapper<T>() {
      @Override
      public T call() throws Exception {
        try {
          execution.before();
          T result = callable.call();
          execution.completeOrRetry(result, null);
          return result;
        } catch (Throwable e) {
          execution.completeOrRetry(null, e);
          return null;
        }
      }
    };
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static <T> AsyncCallableWrapper<T> asyncOf(final CheckedRunnable runnable) {
    Assert.notNull(runnable, "runnable");
    return new AsyncCallableWrapper<T>() {
      @Override
      public T call() throws Exception {
        try {
          execution.before();
          runnable.run();
          execution.completeOrRetry(null, null);
        } catch (Throwable e) {
          execution.completeOrRetry(null, e);
        }

        return null;
      }
    };
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static <T> AsyncCallableWrapper<T> asyncOf(final ContextualCallable<T> callable) {
    Assert.notNull(callable, "callable");
    return new AsyncCallableWrapper<T>() {
      @Override
      public T call() throws Exception {
        try {
          execution.before();
          T result = callable.call(execution);
          execution.completeOrRetry(result, null);
          return result;
        } catch (Throwable e) {
          execution.completeOrRetry(null, e);
          return null;
        }
      }
    };
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static <T> AsyncCallableWrapper<T> asyncOf(final ContextualRunnableOld runnable) {
    Assert.notNull(runnable, "runnable");
    return new AsyncCallableWrapper<T>() {
      @Override
      public T call() throws Exception {
        try {
          execution.before();
          runnable.run(execution);
          execution.completeOrRetry(null, null);
        } catch (Throwable e) {
          execution.completeOrRetry(null, e);
        }

        return null;
      }
    };
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static <T> AsyncCallableWrapper<T> asyncOfFuture(
      final AsyncCallable<? extends java.util.concurrent.CompletionStage<T>> callable) {
    Assert.notNull(callable, "callable");
    return new AsyncCallableWrapper<T>() {
      Semaphore asyncFutureLock = new Semaphore(1);

      @Override
      public T call() throws Exception {
        try {
          execution.before();
          asyncFutureLock.acquire();
          callable.call(execution).whenComplete(new java.util.function.BiConsumer<T, Throwable>() {
            @Override
            public void accept(T innerResult, Throwable failure) {
              try {
                if (failure != null)
                  execution.completeOrRetry(innerResult,
                      failure instanceof java.util.concurrent.CompletionException ? failure.getCause() : failure);
              } finally {
                asyncFutureLock.release();
              }
            }
          });
        } catch (Throwable e) {
          try {
            execution.completeOrRetry(null, e);
          } finally {
            asyncFutureLock.release();
          }
        }

        return null;
      }
    };
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static <T> AsyncCallableWrapper<T> asyncOfFuture(final Callable<? extends java.util.concurrent.CompletionStage<T>> callable) {
    Assert.notNull(callable, "callable");
    return new AsyncCallableWrapper<T>() {
      @Override
      public T call() throws Exception {
        try {
          execution.before();
          callable.call().whenComplete(new java.util.function.BiConsumer<T, Throwable>() {
            @Override
            public void accept(T innerResult, Throwable failure) {
              // Unwrap CompletionException cause
              if (failure != null && failure instanceof java.util.concurrent.CompletionException)
                failure = failure.getCause();
              execution.completeOrRetry(innerResult, failure);
            }
          });
        } catch (Throwable e) {
          execution.completeOrRetry(null, e);
        }

        return null;
      }
    };
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static <T> AsyncCallableWrapper<T> asyncOfFuture(
      final ContextualCallable<? extends java.util.concurrent.CompletionStage<T>> callable) {
    Assert.notNull(callable, "callable");
    return new AsyncCallableWrapper<T>() {
      @Override
      public T call() throws Exception {
        try {
          execution.before();
          callable.call(execution).whenComplete(new java.util.function.BiConsumer<T, Throwable>() {
            @Override
            public void accept(T innerResult, Throwable failure) {
              // Unwrap CompletionException cause
              if (failure != null && failure instanceof java.util.concurrent.CompletionException)
                failure = failure.getCause();
              execution.completeOrRetry(innerResult, failure);
            }
          });
        } catch (Throwable e) {
          execution.completeOrRetry(null, e);
        }

        return null;
      }
    };
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static <T, U, R> CheckedBiFunction<T, U, R> fnOf(final Callable<R> callable) {
    return new CheckedBiFunction<T, U, R>() {
      @Override
      public R apply(T t, U u) throws Exception {
        return callable.call();
      }
    };
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static <T, U, R> CheckedBiFunction<T, U, R> fnOf(final CheckedBiConsumer<T, U> consumer) {
    return new CheckedBiFunction<T, U, R>() {
      @Override
      public R apply(T t, U u) throws Exception {
        consumer.accept(t, u);
        return null;
      }
    };
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static <T, U, R> CheckedBiFunction<T, U, R> fnOf(final CheckedConsumer<U> consumer) {
    return new CheckedBiFunction<T, U, R>() {
      @Override
      public R apply(T t, U u) throws Exception {
        consumer.accept(u);
        return null;
      }
    };
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static <T, U, R> CheckedBiFunction<T, U, R> fnOf(final CheckedFunction<U, R> function) {
    return new CheckedBiFunction<T, U, R>() {
      @Override
      public R apply(T t, U u) throws Exception {
        return function.apply(u);
      }
    };
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static <T, U, R> CheckedBiFunction<T, U, R> fnOf(final CheckedRunnable runnable) {
    return new CheckedBiFunction<T, U, R>() {
      @Override
      public R apply(T t, U u) throws Exception {
        runnable.run();
        return null;
      }
    };
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static <T, U, R> CheckedBiFunction<T, U, R> fnOf(final R result) {
    return new CheckedBiFunction<T, U, R>() {
      @Override
      public R apply(T t, U u) throws Exception {
        return result;
      }
    };
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static <T> Callable<T> callableOf(final CheckedRunnable runnable) {
    Assert.notNull(runnable, "runnable");
    return new Callable<T>() {
      @Override
      public T call() throws Exception {
        runnable.run();
        return null;
      }
    };
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static <T> Callable<T> callableOf(final ContextualCallable<T> callable) {
    Assert.notNull(callable, "callable");
    return new ContextualCallableWrapper<T>() {
      @Override
      public T call() throws Exception {
        T result = callable.call(context);
        return result;
      }
    };
  }

  // For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
  @Deprecated
  static <T> Callable<T> callableOf(final ContextualRunnableOld runnable) {
    Assert.notNull(runnable, "runnable");
    return new ContextualCallableWrapper<T>() {
      @Override
      public T call() throws Exception {
        runnable.run(context);
        return null;
      }
    };
  }



  /**
   * Returns a Supplier that pre-executes the {@code execution}, applies the {@code supplier}, records the result and
   * returns the result. This implementation also handles Thread interrupts.
   */
  static <T> Supplier<ExecutionResult> get(CheckedSupplier<T> supplier, AbstractExecution execution) {
    return () -> {
      ExecutionResult result;
      Throwable throwable = null;
      try {
        execution.preExecute();
        result = ExecutionResult.success(supplier.get());
      } catch (Throwable t) {
        throwable = t;
        result = ExecutionResult.failure(t);
      } finally {
        // Guard against race withMigration Timeout interruption
        synchronized (execution) {
          execution.canInterrupt = false;
          if (execution.interrupted)
            // Clear interrupt flag if interruption was intended
            Thread.interrupted();
          else if (throwable instanceof InterruptedException)
            // Set interrupt flag if interrupt occurred but was not intentional
            Thread.currentThread().interrupt();
        }
      }

      execution.record(result);
      return result;
    };
  }

  /**
   * Returns a Supplier that pre-executes the {@code execution}, applies the {@code supplier}, records the result and
   * returns a promise containing the result.
   */
  static <T> Supplier<CompletableFuture<ExecutionResult>> getPromise(ContextualSupplier<T> supplier,
    AbstractExecution execution) {
    Assert.notNull(supplier, "supplier");
    return () -> {
      ExecutionResult result;
      try {
        execution.preExecute();
        result = ExecutionResult.success(supplier.get(execution));
      } catch (Throwable t) {
        result = ExecutionResult.failure(t);
      }
      execution.record(result);
      return CompletableFuture.completedFuture(result);
    };
  }

  /**
   * Returns a Supplier that asynchronously applies the {@code supplier} and returns a promise containing the result.
   */
  @SuppressWarnings("unchecked")
  static Supplier<CompletableFuture<ExecutionResult>> getPromiseAsync(
    Supplier<CompletableFuture<ExecutionResult>> supplier, Scheduler scheduler, FailsafeFuture<Object> future) {
    return () -> {
      CompletableFuture<ExecutionResult> promise = new CompletableFuture<>();
      Callable<Object> callable = () -> supplier.get().whenComplete((result, error) -> {
        if (error != null)
          promise.completeExceptionally(error);
        else
          promise.complete(result);
      });

      try {
        future.inject((Future) scheduler.schedule(callable, 0, TimeUnit.NANOSECONDS));
      } catch (Throwable t) {
        promise.completeExceptionally(t);
      }
      return promise;
    };
  }

  /**
   * Returns a Supplier that pre-executes the {@code execution}, applies the {@code supplier}, attempts to complete the
   * {@code execution} if a failure occurs, and returns a promise containing the result. Locks to ensure the resulting
   * supplier cannot be applied multiple times concurrently.
   */
  static <T> Supplier<CompletableFuture<ExecutionResult>> getPromiseExecution(AsyncSupplier<T> supplier,
    AsyncExecution execution) {
    Assert.notNull(supplier, "supplier");
    return new Supplier<CompletableFuture<ExecutionResult>>() {
      @Override
      public synchronized CompletableFuture<ExecutionResult> get() {
        try {
          execution.preExecute();
          supplier.get(execution);
        } catch (Throwable e) {
          execution.completeOrHandle(null, e);
        }
        return NULL_FUTURE;
      }
    };
  }

  /**
   * Returns a Supplier that pre-executes the {@code execution}, applies the {@code supplier}, records the result and
   * returns a promise containing the result.
   */
  static <T> Supplier<CompletableFuture<ExecutionResult>> getPromiseOfStage(
    ContextualSupplier<? extends CompletionStage<? extends T>> supplier, AbstractExecution execution) {
    Assert.notNull(supplier, "supplier");
    return () -> {
      CompletableFuture<ExecutionResult> promise = new CompletableFuture<>();
      try {
        execution.preExecute();
        supplier.get(execution).whenComplete((result, failure) -> {
          if (failure instanceof CompletionException)
            failure = failure.getCause();
          ExecutionResult r = failure == null ? ExecutionResult.success(result) : ExecutionResult.failure(failure);
          execution.record(r);
          promise.complete(r);
        });
      } catch (Throwable t) {
        ExecutionResult result = ExecutionResult.failure(t);
        execution.record(result);
        promise.complete(result);
      }
      return promise;
    };
  }

  /**
   * Returns a Supplier that pre-executes the {@code execution}, applies the {@code supplier}, attempts to complete the
   * {@code execution} if a failure occurs, and returns a promise containing the result. Locks to ensure the resulting
   * supplier cannot be applied multiple times concurrently.
   */
  static <T> Supplier<CompletableFuture<ExecutionResult>> getPromiseOfStageExecution(
    AsyncSupplier<? extends CompletionStage<? extends T>> supplier, AsyncExecution execution) {
    Assert.notNull(supplier, "supplier");
    return new Supplier<CompletableFuture<ExecutionResult>>() {
      Semaphore asyncFutureLock = new Semaphore(1);

      @Override
      public CompletableFuture<ExecutionResult> get() {
        try {
          execution.preExecute();
          asyncFutureLock.acquire();
          supplier.get(execution).whenComplete((innerResult, failure) -> {
            try {
              if (failure != null)
                execution.completeOrHandle(innerResult,
                  failure instanceof CompletionException ? failure.getCause() : failure);
            } finally {
              asyncFutureLock.release();
            }
          });
        } catch (Throwable e) {
          try {
            execution.completeOrHandle(null, e);
          } finally {
            asyncFutureLock.release();
          }
        }

        return NULL_FUTURE;
      }
    };
  }

  static <T> AsyncSupplier<T> toAsyncSupplier(AsyncRunnable runnable) {
    Assert.notNull(runnable, "runnable");
    return execution -> {
      runnable.run(execution);
      return null;
    };
  }

  /**
   * Returns a SettableSupplier that supplies the set value once then uses the {@code supplier} for subsequent calls.
   */
  static <T> SettableSupplier<CompletableFuture<T>> toSettableSupplier(Supplier<CompletableFuture<T>> supplier) {
    return new SettableSupplier<CompletableFuture<T>>() {
      volatile boolean called;
      volatile CompletableFuture<T> value;

      @Override
      public CompletableFuture<T> get() {
        if (!called && value != null) {
          called = true;
          return value;
        } else
          return supplier.get();
      }

      @Override
      public void set(CompletableFuture<T> value) {
        called = false;
        this.value = value;
      }
    };
  }

  static <T> CheckedSupplier<T> toSupplier(CheckedRunnable runnable) {
    Assert.notNull(runnable, "runnable");
    return () -> {
      runnable.run();
      return null;
    };
  }

  static <T> CheckedSupplier<T> toSupplier(ContextualRunnable runnable, ExecutionContext context) {
    Assert.notNull(runnable, "runnable");
    return () -> {
      runnable.run(context);
      return null;
    };
  }

  static <T> CheckedSupplier<T> toSupplier(ContextualSupplier<T> supplier, ExecutionContext context) {
    Assert.notNull(supplier, "supplier");
    return () -> supplier.get(context);
  }

  static <T> ContextualSupplier<T> toCtxSupplier(CheckedRunnable runnable) {
    Assert.notNull(runnable, "runnable");
    return ctx -> {
      runnable.run();
      return null;
    };
  }

  static <T> ContextualSupplier<T> toCtxSupplier(ContextualRunnable runnable) {
    Assert.notNull(runnable, "runnable");
    return ctx -> {
      runnable.run(ctx);
      return null;
    };
  }

  static <T> ContextualSupplier<T> toCtxSupplier(CheckedSupplier<T> supplier) {
    Assert.notNull(supplier, "supplier");
    return ctx -> supplier.get();
  }

  static <T, R> CheckedFunction<T, R> toFn(CheckedConsumer<T> consumer) {
    return t -> {
      consumer.accept(t);
      return null;
    };
  }

  static <T, R> CheckedFunction<T, R> toFn(CheckedRunnable runnable) {
    return t -> {
      runnable.run();
      return null;
    };
  }

  static <T, R> CheckedFunction<T, R> toFn(CheckedSupplier<R> supplier) {
    return t -> supplier.get();
  }

  static <T, R> CheckedFunction<T, R> toFn(R result) {
    return t -> result;
  }
}
