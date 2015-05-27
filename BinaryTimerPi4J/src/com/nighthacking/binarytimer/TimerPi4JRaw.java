package com.nighthacking.binarytimer;

import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;
import java.io.IOException;

/**
 * @author Stephen Chin <steveonjava@gmail.com>
 */
public class TimerPi4JRaw {

  public static void main(String[] args) throws IOException {
    Gpio.wiringPiSetup();
    if (!GpioUtil.isExported(1)) {
      GpioUtil.export(1, GpioUtil.DIRECTION_OUT);
    } else {
      GpioUtil.setDirection(1, GpioUtil.DIRECTION_OUT);
    }
    Gpio.pinMode(1, Gpio.OUTPUT);
    final int ITERATIONS = 100000000;
    long start = System.nanoTime();
    for (int i = 0; i < ITERATIONS; i++) {
      Gpio.digitalWrite(1, 1);
      Gpio.digitalWrite(1, 0);
    }
    long duration = System.nanoTime() - start;
    System.out.println("Duration = " + duration);
    double period = (duration / 1000000d / ITERATIONS);
    System.out.println("Period (in ms) = " + period);
    System.out.println("Frequency (in kHz) = " + 1d / period);
    GpioUtil.unexport(1);
  }
}
