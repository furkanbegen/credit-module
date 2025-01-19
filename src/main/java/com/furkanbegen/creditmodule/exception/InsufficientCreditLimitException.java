package com.furkanbegen.creditmodule.exception;

public class InsufficientCreditLimitException extends RuntimeException {
  public InsufficientCreditLimitException(String message) {
    super(message);
  }
}
