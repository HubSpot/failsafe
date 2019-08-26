package net.jodah.failsafe.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.jodah.failsafe.internal.util.Assert;

// For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
@Deprecated
public class Schedulers {
  private Schedulers() {
  }

  /**
   * Returns a Scheduler adapted from the {@code executor}.
   *
   * @throws NullPointerException if {@code executor} is null
   */
  public static Scheduler of(final ScheduledExecutorService executor) {
    Assert.notNull(executor, "executor");
    return new Scheduler() {
      @Override
      public ScheduledFuture<?> schedule(Callable<?> callable, long delay, TimeUnit unit) {
        return executor.schedule(callable, delay, unit);
      }
    };
  }
}
