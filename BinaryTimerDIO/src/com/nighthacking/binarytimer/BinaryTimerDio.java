package com.nighthacking.binarytimer;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdk.dio.DeviceManager;
import jdk.dio.gpio.GPIOPin;

/**
 * @author Stephen Chin <steveonjava@gmail.com>
 */
public class BinaryTimerDio {

  public static void main(String[] args) throws IOException, InterruptedException {
    new BinaryTimerDio().run();
  }

  private final GPIOPin[] leds = new GPIOPin[17];
  private final GPIOPin[] buttons = new GPIOPin[4];
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  private ScheduledFuture<?> future;
  private int counter = 63 << 3; // light up all the greens initially [63s]
  private volatile boolean paused = true;
  private ScheduledFuture<?> shutdownFuture;

  public void run() throws IOException, InterruptedException {
    initPins();
    Runtime.getRuntime().addShutdownHook(new Thread(this::closePins));
    addListeners();
    updateLeds();
    runClock();
  }

  private void initPins() throws IOException {
    for (int i = 0; i < 17; i++) {
      leds[i] = DeviceManager.open(i + 1);
    }
    for (int i = 0; i < 4; i++) {
      buttons[i] = DeviceManager.open(i + 18);
    }
  }

  private void closePins() {
    for (int i = 0; i < 17; i++) {
      try {
        leds[i].close();
      } catch (IOException ex) {
        Logger.getLogger(BinaryTimerDio.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    for (int i = 0; i < 4; i++) {
      try {
        buttons[i].close();
      } catch (IOException ex) {
        Logger.getLogger(BinaryTimerDio.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  private void addListeners() throws IOException {
    buttons[0].setInputListener(new DioDebouncer(new PLHelper(event -> {
      if (!event.getValue() && paused) { // Blue Button: Add 1 (and then x2)
        prepareEdit();
        counter <<= 1;      // shift the bits (time x2)
        counter |= 0x8;     // turn on the second bit (+1)
        updateLeds();
      }
    })));
    buttons[1].setInputListener(new DioDebouncer(new PLHelper(event -> {
      if (!event.getValue() && paused) { // Gray Button: Add 0 (and then x2)
        prepareEdit();
        counter <<= 1;      // shift the bits (time x2)
        updateLeds();
      }
    })));
    buttons[2].setInputListener(new DioDebouncer(new PLHelper(event -> {
      if (!event.getValue()) { // Red Button: Divide by 2, Pause, Shutdown
        shutdownFuture = executor.schedule(executor::shutdown, 3, TimeUnit.SECONDS);
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
    })));
    buttons[3].setInputListener(new DioDebouncer(new PLHelper(event -> {
      if (!event.getValue()) { // Green Button: Run/Pause
        if (paused && counter >= 0) {
          counter &= 0x1FFFF; // clip to max leds
        }
        paused = !paused;
        updateLeds();
      }
    })));
  }

  private void prepareEdit() {
    if (counter < 0) {  // if we went negative, clear out that funk
      counter = 0;
    }
    counter &= ~0xC0000007; // turn off blue leds and high order bits
  }

  private void runClock() throws InterruptedException, IOException {
    future = executor.scheduleAtFixedRate(() -> {
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
      GPIOPin pin = leds[16 - i];
      boolean newValue = (counter >> i & 0x1) != 0;
      try {
        if (pin.getValue() != newValue) {
          pin.setValue(newValue);
        }
      } catch (IOException ex) {
        Logger.getLogger(BinaryTimerDio.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

  private void setAllLeds(boolean value) {
    for (int i = 0; i < 17; i++) {
      try {
        leds[16 - i].setValue(value);
      } catch (IOException ex) {
        Logger.getLogger(BinaryTimerDio.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
  }

}
