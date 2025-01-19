package com.furkanbegen.creditmodule.model;

import lombok.Getter;

@Getter
public enum InstallmentOption {
  SIX(6),
  NINE(9),
  TWELVE(12),
  TWENTY_FOUR(24);

  private final int value;

  InstallmentOption(int value) {
    this.value = value;
  }

  public static InstallmentOption fromValue(int value) {
    for (InstallmentOption option : values()) {
      if (option.getValue() == value) {
        return option;
      }
    }
    throw new IllegalArgumentException("Invalid installment option: " + value);
  }
}
