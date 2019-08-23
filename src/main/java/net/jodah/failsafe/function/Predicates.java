package net.jodah.failsafe.function;

public class Predicates {
  public static <T, U> java.util.function.BiPredicate<T, U> convertToJavaBiPredicate(BiPredicate<T, U> failsafeBiPredicate) {
    return failsafeBiPredicate::test;
  }

  public static <T> java.util.function.Predicate<T> convertToPredicate(Predicate<T> failsafePredicate) {
    return failsafePredicate::test;
  }
}
