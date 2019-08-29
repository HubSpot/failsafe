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
package net.jodah.failsafe.examples;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Java8Example {
  @SuppressWarnings("unused")
  public static void main(String... args) {
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    RetryPolicy<Object> retryPolicy = new RetryPolicy<>();

    // Create a retryable functional interface
    Function<String, String> bar = value -> Failsafe.withMigration(retryPolicy).get(() -> value + "bar");

    // Create a retryable Stream operation
    Failsafe.withMigration(retryPolicy).get(() -> Stream.of("foo")
        .map(value -> Failsafe.withMigration(retryPolicy).get(() -> value + "bar"))
        .collect(Collectors.toList()));

    // Create a individual retryable Stream operation
    Stream.of("foo").map(value -> Failsafe.withMigration(retryPolicy).get(() -> value + "bar")).forEach(System.out::println);

    // Create a retryable CompletableFuture
    Failsafe.withMigration(retryPolicy).with(executor).getStageAsync(() -> CompletableFuture.supplyAsync(() -> "foo")
        .thenApplyAsync(value -> value + "bar")
        .thenAccept(System.out::println));

    // Create an individual retryable CompletableFuture stages
    CompletableFuture.supplyAsync(() -> Failsafe.withMigration(retryPolicy).get(() -> "foo"))
        .thenApplyAsync(value -> Failsafe.withMigration(retryPolicy).get(() -> value + "bar"))
        .thenAccept(System.out::println);
  }
}
