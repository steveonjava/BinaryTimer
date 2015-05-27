package com.nighthacking.binarytimer;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Stephen Chin <steveonjava@gmail.com>
 */
public class BinaryTimerPi4J {

  private static final Pin[] LED_PINS = {
    RaspiPin.GPIO_15,
    RaspiPin.GPIO_16,
    RaspiPin.GPIO_01,
    RaspiPin.GPIO_04,
    RaspiPin.GPIO_05,
    RaspiPin.GPIO_06,
    RaspiPin.GPIO_10,
    RaspiPin.GPIO_11,
    RaspiPin.GPIO_26,
    RaspiPin.GPIO_27,
    RaspiPin.GPIO_28,
    RaspiPin.GPIO_29,
    RaspiPin.GPIO_21,
    RaspiPin.GPIO_22,
    RaspiPin.GPIO_23,
    RaspiPin.GPIO_24,
    RaspiPin.GPIO_25
  };

  private static final Pin[] BUTTON_PINS = {
    RaspiPin.GPIO_14,
    RaspiPin.GPIO_13,
    RaspiPin.GPIO_12,
    RaspiPin.GPIO_03
  };

  public static void main(String[] args) {
    new BinaryTimerPi4J().run();
  }

  private GpioController gpio;
  private final GpioPinDigitalOutput[] leds = new GpioPinDigitalOutput[LED_PINS.length];
  private final GpioPinDigitalInput[] buttons = new GpioPinDigitalInput[BUTTON_PINS.length];
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  private int counter = 63 << 3; // light up all the greens initially [63s]
  private volatile boolean paused = true;
  private ScheduledFuture<?> shutdownFuture;

  public void run() {
    initPins();
    addListeners();
    updateLeds();
    runClock();
  }

  private void initPins() {
    gpio = GpioFactory.getInstance();
    for (int i = 0; i < 17; i++) {
      leds[i] = gpio.provisionDigitalOutputPin(LED_PINS[i]);
    }
    for (int i = 0; i < 4; i++) {
      buttons[i] = gpio.provisionDigitalInputPin(BUTTON_PINS[i], PinPullResistance.PULL_UP);
      buttons[i].setDebounce(20);
    }
  }
  
  private void shutdown() {
    executor.shutdown();
    gpio.shutdown();
  }

  private void addListeners() {
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

  private void runClock() {
    executor.scheduleAtFixedRate(() -> {
      if (!paused) {
        if (counter <= 0) {
          if ((System.currentTimeMillis() / 250) % 2 == 0) {
            setAllLeds(false);
          } else {
            updateLeds();
          }
        } else {
          updateLeds();
        }
        counter--;
      }
    }, 0, 125, TimeUnit.MILLISECONDS);
  }

  private void updateLeds() {
    for (int i = 0; i < 17; i++) {
      GpioPinDigitalOutput pin = leds[16 - i];
      boolean newValue = (counter >> i & 0x1) != 0;
      if (pin.getState().isHigh() != newValue) {
        pin.setState(newValue);
      }
    }
  }

  private void setAllLeds(boolean value) {
    for (int i = 0; i < 17; i++) {
      leds[16 - i].setState(value);
    }
  }

}
