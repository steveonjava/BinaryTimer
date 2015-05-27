package com.nighthacking.led;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.wiringpi.Gpio;

/**
 * @author Stephen Chin <steveonjava@gmail.com>
 */
public class Pi4JLed {

  public static void main(String[] args) {
    GpioController gpio = GpioFactory.getInstance();
    GpioPinDigitalOutput led = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01);
    for (int i = 0; i < 10; i++) {
      led.setState(i % 2 == 0);
      Gpio.delay(500);
    }
    gpio.shutdown();
  }
}
