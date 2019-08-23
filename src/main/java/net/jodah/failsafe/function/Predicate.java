package net.jodah.failsafe.function;

// Shim to migrate from 1.x to 2.x
@Deprecated
public interface Predicate<T> {
  boolean test(T t);
  void dontAllowLambdaShorthand();
}