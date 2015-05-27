package com.nighthacking.binarytimer;

import java.io.IOException;
import jdk.dio.DeviceManager;
import jdk.dio.gpio.GPIOPin;

/**
 * @author Stephen Chin <steveonjava@gmail.com>
 */
public class TimerDio {

  public static void main(String[] args) throws IOException {
    GPIOPin led = DeviceManager.open(1);
    final int ITERATIONS = 100000;
    long start = System.nanoTime();
    for (int i = 0; i < ITERATIONS; i++) {
      led.setValue(true);
      led.setValue(false);
    }
    long duration = System.nanoTime() - start;
    System.out.println("Duration = " + duration);
    double period = (duration / 1000000d / ITERATIONS);
    System.out.println("Period (in ms) = " + period);
    System.out.println("Frequency (in kHz) = " + 1d / period);
    led.close();
  }
}
