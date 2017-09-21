package org.deeplearning4j.optimize.listeners;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.api.activations.Activations;
import org.deeplearning4j.optimize.api.TrainingListener;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * This TrainingListener implementation provides a way to "sleep" during specific Neural Network training phases.
 * Suitable for debugging/testing purposes.
 *
 * PLEASE NOTE: All timers treat time values as milliseconds.
 * PLEASE NOTE: Do not use it in production environment.
 *
 * @author raver119@gmail.com
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Slf4j
public class SleepyTrainingListener implements TrainingListener {
    public enum SleepMode {
        /**
         * In this mode parkNanos() call will be used, to make process really idle
         */
        PARK,

        /**
         * In this mode Thread.sleep() call will be used, to make sleep traceable via profiler
         */
        SLEEP,

        /**
         * Busy-lock will be used, to guarantee 100% thread use
         */
        BUSY,
    }


    public enum TimeMode {
        /**
         * In this mode, listener will be trying to match specified time for a given invocation method.
         * I.e. if iteration sleep is set to 500, and real iteration was 35 ms, thread will be sleeping for 465ms, to match target time of 500ms
         *
         */
        ADDITIVE,

        /**
         * In this mode, listener will just call
         */
        SIMPLE
    }

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    protected final transient ThreadLocal<AtomicLong> lastEE = new ThreadLocal<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    protected final transient ThreadLocal<AtomicLong> lastES = new ThreadLocal<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    protected final transient ThreadLocal<AtomicLong> lastFF = new ThreadLocal<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    protected final transient ThreadLocal<AtomicLong> lastBP = new ThreadLocal<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    protected final transient ThreadLocal<AtomicLong> lastIteration = new ThreadLocal<>();

    @Builder.Default
    protected long timerEE = 0L;
    @Builder.Default
    protected long timerES = 0L;
    @Builder.Default
    protected long timerFF = 0L;
    @Builder.Default
    protected long timerBP = 0L;
    @Builder.Default
    protected long timerIteration = 0L;

    @Builder.Default
    protected SleepMode sleepMode = SleepMode.PARK;

    @Builder.Default
    protected TimeMode timeMode = TimeMode.SIMPLE;

    protected void sleep(long sleepTimeMs) {
        if (sleepTimeMs < 1)
            return;

        switch (sleepMode) {
            case PARK:
                LockSupport.parkNanos(sleepTimeMs * 1000000);
                break;
            case BUSY: {
                long target = System.currentTimeMillis() + sleepTimeMs;
                while (System.currentTimeMillis() < target);;
            }
                break;
            case SLEEP:
                try {
                    Thread.sleep(sleepTimeMs);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                throw new IllegalStateException("Unknown SleepMode value passed in: " + sleepMode);
        }
    }

    protected void sleep(AtomicLong lastTime, long sleepTime) {
        if (sleepTime == 0)
            return;

        // if that's SIMPLE mode - just sleep specific time, and go
        if (timeMode == TimeMode.SIMPLE) {
            sleep(sleepTime);
            return;
        }

        // we're skipping first iteration here, just sleeping fixed amount of time
        if (lastTime == null) {
            sleep(sleepTime);
            return;
        }


        // getting delta between real cycle time and desired one.
        long currentTime = System.currentTimeMillis();
        long delta = sleepTime - (currentTime - lastTime.get());

        sleep(delta);
    }

    @Override
    public void onEpochStart(Model model) {
        sleep(lastES.get(), timerES);

        if (lastES.get() == null)
            lastES.set(new AtomicLong(System.currentTimeMillis()));
        else
            lastES.get().set(System.currentTimeMillis());
    }

    @Override
    public void onEpochEnd(Model model) {
        sleep(lastEE.get(), timerEE);

        if (lastEE.get() == null)
            lastEE.set(new AtomicLong(System.currentTimeMillis()));
        else
            lastEE.get().set(System.currentTimeMillis());
    }

    @Override
    public void onForwardPass(Model model, List<Activations> activations) {
        sleep(lastFF.get(), timerFF);

        if (lastFF.get() == null)
            lastFF.set(new AtomicLong(System.currentTimeMillis()));
        else
            lastFF.get().set(System.currentTimeMillis());
    }

    @Override
    public void onForwardPass(Model model, Map<String, Activations> activations) {
        sleep(lastFF.get(), timerFF);

        if (lastFF.get() == null)
            lastFF.set(new AtomicLong(System.currentTimeMillis()));
        else
            lastFF.get().set(System.currentTimeMillis());
    }

    @Override
    public void iterationDone(Model model, int iteration, int epoch) {
        sleep(lastIteration.get(), timerIteration);

        if (lastIteration.get() == null)
            lastIteration.set(new AtomicLong(System.currentTimeMillis()));
        else
            lastIteration.get().set(System.currentTimeMillis());
    }

    @Override
    public void onBackwardPass(Model model) {
        sleep(lastBP.get(), timerBP);

        if (lastBP.get() == null)
            lastBP.set(new AtomicLong(System.currentTimeMillis()));
        else
            lastBP.get().set(System.currentTimeMillis());
    }

    @Override
    public void onGradientCalculation(Model model) {
        //
    }
}
