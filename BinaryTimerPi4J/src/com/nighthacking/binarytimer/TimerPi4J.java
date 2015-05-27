package com.nighthacking.binarytimer;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

/**
 * @author Stephen Chin <steveonjava@gmail.com>
 */
public class TimerPi4J {

  public static void main(String[] args) {
    final GpioController gpio = GpioFactory.getInstance();
    final GpioPinDigitalOutput led = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, PinState.LOW);
    final int ITERATIONS = 100000;
    long start = System.nanoTime();
    for (int i = 0; i < ITERATIONS; i++) {
      led.setState(true);
      led.setState(false);
    }
    long duration = System.nanoTime() - start;
    System.out.println("Duration = " + duration);
    double period = (duration / 1000000d / (ITERATIONS));
    System.out.println("Period (in ms) = " + period);
    System.out.println("Frequency (in kHz) = " + 1d / period);
    gpio.shutdown();
  }
}
