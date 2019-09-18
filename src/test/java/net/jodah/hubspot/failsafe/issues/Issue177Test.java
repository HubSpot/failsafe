package net.jodah.hubspot.failsafe.issues;

import net.jodah.hubspot.failsafe.Failsafe;
import net.jodah.hubspot.failsafe.Fallback;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNull;

@Test
public class Issue177Test {
  public void shouldSupportNullFallback() {
    Fallback<Boolean> fallback = Fallback.of((Boolean) null).handleResult(false);
    assertNull(Failsafe.with(fallback).get(() -> false));
  }
}
