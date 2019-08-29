package net.jodah.failsafe.function;

import net.jodah.failsafe.ExecutionContextOld;

/**
 * A runnable that provides execution context.
 *
 * @author Jonathan Halterman
 */
// For migration from 1.x to 2.x. This is used by SyncFailsafe/AsyncFailsafe and will be replaced by using FailsafeExecutor
@Deprecated
public interface ContextualRunnableOld {
  void run(ExecutionContextOld context) throws Exception;
}
