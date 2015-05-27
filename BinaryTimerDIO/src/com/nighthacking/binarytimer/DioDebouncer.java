package com.nighthacking.binarytimer;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jdk.dio.gpio.PinEvent;
import jdk.dio.gpio.PinListener;

/**
 * Convenience class for DIO that handles button debouncing, so that only 1
 * event gets fired per press or release of a typical switch. Also hides the
 * startup artifacts for GPIO inputs configured with PULL_UP resistors.
 * 
 * @author Stephen Chin <steveonjava@gmail.com>
 */
public class DioDebouncer implements PinListener {
  private static final int STARTUP_INTERVAL = 200;
  private static final int DEBOUNCE_INTERVAL = 20;
  
  private final PinListener action;
  private int pin = -1;
  private PinEvent lastPE;
  private Future future;
  // Note: can't use the default thread pool since it is non-daemon
  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, target -> {
    Thread t = new Thread(target, "Debouncer for pin[" + pin + "]");
    t.setDaemon(true);
    return t;
  });
  
  public DioDebouncer(PinListener action) {
    this.action = action;
    // Get rid of startup wobble from DIO library
    future = executor.schedule(() -> {}, STARTUP_INTERVAL, TimeUnit.MILLISECONDS);
  }

  @Override
  public void valueChanged(PinEvent pe) {
    checkPin(pe);
    // Remove spurious events with the same value:
    if (lastPE != null && lastPE.getValue() == pe.getValue()) {return;}
    lastPE = pe;
    if (!future.isDone()) {return; /* Debounced! */}
    
    action.valueChanged(pe);
    future = executor.schedule(() -> {
      if (pe.getValue() != lastPE.getValue()) {
        // Value changed during the debounce interval:
        action.valueChanged(lastPE);
      }
    }, DEBOUNCE_INTERVAL, TimeUnit.MILLISECONDS);
  }

  // Make sure the same Debouncer is not used for multiple pins
  private void checkPin(PinEvent pe) {
    int id = pe.getDevice().getDescriptor().getID();
    if (pin == -1) {
      pin = id;
    } else if (pin != id) {
      throw new IllegalStateException("Debouncer pin[" + pin + "] != event pin[" + id + "]. Use a new debouncer per listener.");
    }
  }
}
