package net.jodah.failsafe.function;

import net.jodah.failsafe.AsyncExecutionOld;

/**
 * A Runnable that manually triggers asynchronous retries or completion via an asynchronous execution.
 *
 * @author Jonathan Halterman
 */
// For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
@Deprecated
@FunctionalInterface
public interface AsyncRunnableOld {
  void run(AsyncExecutionOld execution) throws Exception;
}