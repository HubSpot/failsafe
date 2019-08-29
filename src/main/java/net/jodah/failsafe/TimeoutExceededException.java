/*
 * Copyright 2019 the original author or authors.
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

/**
 * Thrown when an execution exceeds a configured {@link Timeout}.
 *
 * @author Jonathan Halterman
 */
public class TimeoutExceededException extends FailsafeException {
  private static final long serialVersionUID = 1L;

  private final Timeout timeout;

  public TimeoutExceededException(Timeout timeout) {
    this.timeout = timeout;
  }

  /** Retruns the {@link Timeout} that caused the exception. */
  public Timeout getTimeout() {
    return timeout;
  }
}
