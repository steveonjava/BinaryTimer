package com.nighthacking.binarytimer;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Stephen Chin <steveonjava@gmail.com>
 */
public class BinaryMillisecondTimerPi4JRaw {

  private static final int[] LED_PINS = {
    25, 24, 23, 22, 21, 29, 28, 27, 26, 11, 10, 6, 5, 4, 1, 16, 15
  };

  private static final Pin[] BUTTON_PINS = {
    RaspiPin.GPIO_14,
    RaspiPin.GPIO_13,
    RaspiPin.GPIO_12,
    RaspiPin.GPIO_03
  };

  public static void main(String[] args) throws IOException, InterruptedException {
    new BinaryMillisecondTimerPi4JRaw().run();
  }

  private GpioController gpio;
  private final GpioPinDigitalInput[] buttons = new GpioPinDigitalInput[BUTTON_PINS.length];
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  private int counter = 0x3FFF << 3; // light up all the LEDs initially [32.767s]
  private volatile boolean paused = true;
  private ScheduledFuture<?> shutdownFuture;

  public void run() throws IOException, InterruptedException {
    initPins();
    addListeners();
    updateLeds();
    runClock();
  }

  private void initPins() {
    gpio = GpioFactory.getInstance();
    for (int i = 0; i < 17; i++) {
      if (!GpioUtil.isExported(LED_PINS[i])) {
        GpioUtil.export(LED_PINS[i], GpioUtil.DIRECTION_OUT);
      } else {
        GpioUtil.setDirection(LED_PINS[i], GpioUtil.DIRECTION_OUT);
      }
      Gpio.pinMode(LED_PINS[i], Gpio.OUTPUT);
    }
    for (int i = 0; i < 4; i++) {
      buttons[i] = gpio.provisionDigitalInputPin(BUTTON_PINS[i], PinPullResistance.PULL_UP);
    }
  }
  
  private void shutdown() {
    executor.shutdown();
    for (int i = 0; i < 17; i++) {
      GpioUtil.unexport(LED_PINS[i]);
    }
    gpio.shutdown();
  }

  private void addListeners() throws IOException {
    buttons[0].addListener((GpioPinListenerDigital) event -> {
      if (event.getState().isLow() && paused) { // Blue Button: Add 1 (and then x2)
        prepareEdit();
        counter <<= 1;      // shift the bits (time x2)
        counter |= 0x8;     // turn on the second bit (+1)
        updateLeds();
      }
    });
    buttons[1].addListener((GpioPinListenerDigital) event -> {
      if (event.getState().isLow() && paused) { // Gray Button: Add 0 (and then x2)
        prepareEdit();
        counter <<= 1;      // shift the bits (time x2)
        updateLeds();
      }
    });
    buttons[2].addListener((GpioPinListenerDigital) event -> {
      if (event.getState().isLow()) { // Red Button: Divide by 2, Pause, Shutdown
        shutdownFuture = executor.schedule(this::shutdown, 3, TimeUnit.SECONDS);
        if (!paused) {
          paused = true;
        } else {
          prepareEdit();
          counter >>>= 1;     // shift the bits (time /2)
          counter &= ~0x4;    // clear the bit shifted off
        }
        updateLeds();
      } else {
        shutdownFuture.cancel(false);
      }
    });
    buttons[3].addListener((GpioPinListenerDigital) event -> {
      if (event.getState().isLow()) { // Green Button: Run/Pause
        if (paused && counter >= 0) {
          counter &= 0x1FFFF; // clip to max leds
        }
        paused = !paused;
        updateLeds();
      }
    });
  }

  private void prepareEdit() {
    if (counter < 0) {  // if we went negative, clear out that funk
      counter = 0;
    }
    counter &= ~0xC0000007; // turn off blue leds and high order bits
  }

  private void runClock() throws InterruptedException, IOException {
    executor.scheduleAtFixedRate(() -> {
      if (!paused) {
        if (counter <= 0) {
          if ((System.currentTimeMillis() / 250) % 2 == 0) {
            setAllLeds(0);
          } else {
            updateLeds();
          }
        } else {
          updateLeds();
        }
        counter--;
      }
    }, 0, 125000, TimeUnit.NANOSECONDS);
  }

  private void updateLeds() {
    for (int i = 0; i < 17; i++) {
      int newValue = counter >> i & 0x1;
      Gpio.digitalWrite(LED_PINS[i], newValue);
    }
  }

  private void setAllLeds(int value) {
    for (int i = 0; i < 17; i++) {
      Gpio.digitalWrite(LED_PINS[i], value);
    }
  }

}
