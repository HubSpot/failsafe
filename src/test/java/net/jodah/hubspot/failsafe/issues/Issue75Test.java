package net.jodah.hubspot.failsafe.issues;

import net.jodah.hubspot.failsafe.CircuitBreaker;
import net.jodah.hubspot.failsafe.Failsafe;
import net.jodah.hubspot.failsafe.Fallback;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Issue75Test {
  @Test
  public void testThatFailSafeIsBrokenWithFallback() throws Exception {
    CircuitBreaker<Integer> breaker = new CircuitBreaker<Integer>().withFailureThreshold(10, 100).withSuccessThreshold(2).withDelay(
        Duration.ofMillis(100));
    ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    int result = Failsafe.with(Fallback.of(e -> 999), breaker)
        .with(service)
        .getStageAsync(() -> CompletableFuture.completedFuture(223))
        .get();

    Assert.assertEquals(result, 223);
  }
}
