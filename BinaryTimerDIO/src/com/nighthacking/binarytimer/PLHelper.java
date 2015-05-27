/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nighthacking.binarytimer;

import java.io.IOException;
import jdk.dio.DeviceEventListener;
import jdk.dio.gpio.PinEvent;
import jdk.dio.gpio.PinListener;

/**
 * Helps with the common case where you are implementing a PinListener and
 * triggering other GPIOPins that may throw IOExceptions. I would argue that
 * IOExceptions are unrecoverable in this case, and a runtime exception is
 * more appropriate (which this class conveniently does).
 * 
 * @author Stephen Chin <steveonjava@gmail.com>
 */
public class PLHelper implements PinListener {
  private final PinListenerWithThrows listener;
  public static interface PinListenerWithThrows extends DeviceEventListener {
    public void valueChanged(PinEvent pe) throws IOException;
  }
  public PLHelper(PinListenerWithThrows listener) {
    this.listener = listener;
  }
  @Override
  public void valueChanged(PinEvent pe) {
    try {
      listener.valueChanged(pe);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
}
