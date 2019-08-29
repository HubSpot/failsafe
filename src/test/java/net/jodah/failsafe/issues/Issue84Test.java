package net.jodah.failsafe.issues;

import net.jodah.failsafe.*;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.concurrent.*;

import static org.testng.Assert.assertFalse;

@Test
public class Issue84Test {
  public void shouldHandleCircuitBreakerOpenException() throws Throwable {
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    CircuitBreaker<Boolean> circuitBreaker = new CircuitBreaker<Boolean>().withDelay(Duration.ofMinutes(10)).handleResult(false);
    circuitBreaker.open();

    // Synchronous
    Asserts.assertThrows(() -> Failsafe.withMigration(circuitBreaker).get(() -> true), CircuitBreakerOpenException.class);

    // Synchronous withMigration fallback
    assertFalse(Failsafe.withMigration(Fallback.of(false), circuitBreaker).get(() -> true));

    // Asynchronous
    Future<Boolean> future1 = Failsafe.withMigration(circuitBreaker).with(executor).getAsync(() -> true);
    Asserts.assertThrows(future1::get, ExecutionException.class, CircuitBreakerOpenException.class);

    // Asynchronous withMigration fallback
    Future<Boolean> future2 = Failsafe.withMigration(Fallback.of(false), circuitBreaker).with(executor).getAsync(() -> true);
    assertFalse(future2.get());

    // Future
    Future<Boolean> future3 = Failsafe.withMigration(circuitBreaker).with(executor).getStageAsync(() -> CompletableFuture.completedFuture(false));
    Asserts.assertThrows(future3::get, ExecutionException.class, CircuitBreakerOpenException.class);

    // Future withMigration fallback
    Future<Boolean> future4 = Failsafe.withMigration(Fallback.of(false), circuitBreaker)
        .getStageAsync(() -> CompletableFuture.completedFuture(false));
    assertFalse(future4.get());
  }
}
