package com.nighthacking.binarytimer;

import com.pi4j.wiringpi.Gpio;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Stephen Chin <steveonjava@gmail.com>
 */
public class TimerPi4JRawSysfs {

  public static void main(String[] args) throws IOException {
    exportPin("18");
    Gpio.wiringPiSetupSys();
    final int ITERATIONS = 1000000;
    long start = System.nanoTime();
    for (int i=0; i<ITERATIONS; i++) {
      Gpio.digitalWrite(18, 1);
      Gpio.digitalWrite(18, 0);
    }
    long duration = System.nanoTime() - start;
    System.out.println("Duration = " + duration);
    double period = (duration / 1000000d / ITERATIONS);
    System.out.println("Period (in ms) = " + period);
    System.out.println("Frequency (in kHz) = " + 1d / period);
    unexportPin("18");
  }

  private static void exportPin(String port) throws IOException {
    FileWriter exportFile = new FileWriter("/sys/class/gpio/export");
    exportFile.write(port);
    exportFile.flush();
    FileWriter directionFile = new FileWriter("/sys/class/gpio/gpio" + port + "/direction");
    directionFile.write("out");
    directionFile.flush();
  }

  private static void unexportPin(String port) throws IOException {
    FileWriter unexportFile = new FileWriter("/sys/class/gpio/unexport");
    unexportFile.write(port);
    unexportFile.flush();
  }
}
