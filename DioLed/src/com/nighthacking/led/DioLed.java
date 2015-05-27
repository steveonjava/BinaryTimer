package com.nighthacking.led;

import java.io.IOException;
import jdk.dio.DeviceManager;
import jdk.dio.gpio.GPIOPin;

/**
 * @author Stephen Chin <steveonjava@gmail.com>
 */
public class DioLed {

  public static void main(String[] args) throws IOException, InterruptedException {
    try (GPIOPin led = DeviceManager.open(1);) {
      for (int i = 0; i < 10; i++) {
        led.setValue(i % 2 == 0);
        Thread.sleep(500);
      }
    }
  }
}
