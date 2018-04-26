/*
 *
 *    Copyright 2016 jshook
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * /
 */

package io.engineblock.activityapi.rates;

import com.codahale.metrics.Gauge;
import io.engineblock.activityapi.core.Startable;
import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.metrics.ActivityMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>This rate limiter uses nanoseconds as the unit of timing. This
 * works well because it is the native precision of the system timer
 * interface via {@link System#nanoTime()}. It is also low-error
 * in terms of rounding between floating point rates and nanoseconds,
 * at least in the round numbers that users tend to use. Further,
 * the current scheduling state is maintained as an atomic view of
 * accumulated nanoseconds granted to callers -- referred to here as the
 * ticks accumulator. This further simplifies the implementation by
 * allowing direct comparison of scheduled times with the current
 * state of the high-resolution system timer.
 *
 * <p>
 * Each time {@link #acquire()} or {@link #acquire(long)} is called,
 * a discrete scheduled time is calculated from the current state of
 * the ticks accumulator. If the calculated time is in the future,
 * then the method blocks (in the calling thread) using
 * {@link Thread#sleep(long, int)}. Finally, the method is unblocked,
 * and the nanosecond scheduling gap is returned to the caller.
 *
 * <p>
 * The ticks accumulator can be set to enforce strict isochronous timing
 * from one call to the next, or it can be allowed to dispatch a burst
 * of events as long as the average rate does not exceed the target rate.
 * In practice neither of these approaches is ideal. By default, the
 * scheduling buffer that may result from slow start of callers is
 * gradually removed, thus shifting from an initially bursty rate limiter
 * to a strictly isochronous one. This allows for calling threads to settle
 * in. A desirable feature of this rate limiter will be to add options to
 * limit based on strict limit or average limit.
 *
 * <p>
 * Note that the ticks accumulator can not rate limit a single event.
 * Acquiring a grant at some nanosecond size simply consumes nanoseconds
 * from the schedule, with the start time of the allotted time span
 * being conceptually aligned with the start time of the requested event.
 * In other words, previous allocations of the timeline determine the start
 * time of a new caller, not the caller itself.
 */
public class StrictRateLimiter implements Startable, RateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(StrictRateLimiter.class);
    private final Gauge<Long> delayGauge;
    private final RateSpec rateSpec;
    protected AtomicLong ticksTimeline;
    private long opTicks = 0L; // Number of nanos representing one grant at target rate
    private double rate = Double.NaN; // The "ops/s" rate as set by the user
    private long startTimeNanos;
    private AtomicLong lastSeenNanoTime;
    private AtomicLong totalAccumualatedDelay = new AtomicLong(0L);
    private AtomicLong gapClosings = new AtomicLong(0L);

    private volatile boolean started;

    // each blocking call will correct to strict schedule by gap * 1/2^n
    private int burstDampeningShift = 5;
    private double strictness;
    private State state = State.Idle;


    private boolean reportCoDelay = false;
    private long burstTicks;


    /**
     * Create a rate limiter.
     *
     * @param def The activity definition for this rate limiter
     */
    public StrictRateLimiter(ActivityDef def, String label, RateSpec rateSpec) {
        this.rateSpec = rateSpec;
        this.reportCoDelay = rateSpec.reportCoDelay;
        this.setRate(rateSpec.opsPerSec);
        this.setStrictness(rateSpec.strictness);
        this.delayGauge = ActivityMetrics.gauge(def, "cco-delay-" + label, new RateLimiters.DelayGauge(this));
    }

    public StrictRateLimiter(ActivityDef def, String label, RateSpec spec, AverageRateLimiter prior) {
        this(def, label, spec);
        this.totalAccumualatedDelay.set(prior.getTotalSchedulingDelay());
    }

    protected long getNanoClockTime() {
        return System.nanoTime();
    }

    /**
     * See {@link StrictRateLimiter} for interface docs.
     * effective calling overhead of acquire() is ~20ns
     *
     * @param nanos nanoseconds of time allotted to this event
     * @return nanoseconds that have already elapsed since this event's ideal time
     */
    @Override
    public long acquire(long nanos) {
        long opScheduleTime = ticksTimeline.getAndAdd(nanos);
        long timelinePosition = lastSeenNanoTime.get();

        if (opScheduleTime < timelinePosition) {
            return reportCoDelay ? (timelinePosition - opScheduleTime) + totalAccumualatedDelay.get() : 0L;
        }

        timelinePosition = getNanoClockTime();
        lastSeenNanoTime.set(timelinePosition);
        long scheduleDelay = timelinePosition - opScheduleTime;

        if (scheduleDelay > 0L) {
            // If slower than allowed rate,
            // then fast-forward ticks timeline to close gap by some proportion
            // thus simulating ready callers at some rate

//            long gap = (timelinePosition - opScheduleTime) - nanos;

            //            if (gap > nanos) {
//                ticksTimeline.addAndGet()
//            }
//            if (gap > 0) {

//            if (gap > nanos) {
//                gap >>>= burstDampeningShift;
//                if (gap > 0) {
//                    //logger.debug("closing gap by " + gap);
//                    ticksTimeline.addAndGet(gap);
//                    gapClosings.incrementAndGet();
//                    return(totalAccumualatedDelay.addAndGet(gap));
//                }
//            }

            return reportCoDelay ? scheduleDelay + totalAccumualatedDelay.get() : 0L;
        } else {
            scheduleDelay *= -1;
//            logger.debug("schedule delay: " + scheduleDelay);
            try {
                Thread.sleep(scheduleDelay / 1000000, (int) (scheduleDelay % 1000000L));
            } catch (InterruptedException ignoringSpuriousInterrupts) {
            }
            return 0L;
        }
    }

//            if ((timeSlicePosition%10)==0) {
//                // If slower than allowed rate,
//                // then fast-forward ticks timeline to
//                // close gap by some proportion.
//                long gap = (timelinePosition - timeSlicePosition) - nanos;
//                if (gap > 0) {
//                    gap >>>= burstDampeningShift;
//                    if (gap > 0) {
//                        logger.debug("closing gap by " + gap);
//                        ticksTimeline.addAndGet(gap);
//                    }
//                }
//            }
//        }
//
//        if (delayForNanos>0L) {
//            try {
////                logger.debug("sleeping " + timeSliceDelay);
//                Thread.sleep(delayForNanos / 1000000, (int) (delayForNanos % 1000000L));
//            } catch (InterruptedException ignoringSpuriousInterrupts) {
//                // This is only a safety for spurious interrupts. It should not be hit often.
//            }
//
//            // indicate that no cumulative delay is affecting this caller, only execution delay from above
//            return 0;
//        }
//        return timelinePosition - opScheduleTime;
//    }

    @Override
    public long acquire() {
        return acquire(opTicks);
    }


    @Override
    public long getTotalSchedulingDelay() {
        return reportCoDelay ? getRateSchedulingDelay() + totalAccumualatedDelay.get() : 0L;
    }

    @Override
    public long getRateSchedulingDelay() {
        return reportCoDelay ? (getNanoClockTime() - this.ticksTimeline.get()) : 0L;
    }


    public synchronized void start() {
        if (!started) {
            this.ticksTimeline = new AtomicLong(getNanoClockTime());
            this.lastSeenNanoTime = new AtomicLong(getNanoClockTime());
            this.totalAccumualatedDelay = new AtomicLong(0L);
            this.started = true;
            resetReferences();
        }
    }

    public long getOpNanos() {
        return opTicks;
    }

    public synchronized double setOpNanos(long opTicks) {
        if (opTicks <= 0) {
            throw new RuntimeException("The number of nanos per op must be greater than 0.");
        }
        this.opTicks = opTicks;
        this.rate = 1000000000d / opTicks;

        switch (state) {
            case Started:
                accumulateDelay();
                sync();
            case Idle:
        }

        return getRate();
    }

    @Override
    public double getRate() {
        return rate;
    }

    @Override
    public synchronized void setRate(double rate) {
        if (rate > 1000000000.0D) {
            throw new RuntimeException("The rate must not be greater than 1000000000. Timing precision is in nanos.");
        }
        if (rate <= 0.0D) {
            throw new RuntimeException("The rate must be greater than 0.0");
        }
        this.rate = rate;
        opTicks = (long) (1000000000d / rate);
        burstTicks = (strictness > 1.0) ? (long) (1000000000d / (rate * strictness)) : opTicks;
        logger.info("OpTicksNs for one cycle is " + opTicks + "ns");

        setOpNanos(opTicks);
    }

    private void accumulateDelay() {
        totalAccumualatedDelay.addAndGet(getTotalSchedulingDelay());
    }

    private void resetReferences() {
        long newSetTime = getNanoClockTime();
        this.ticksTimeline.set(newSetTime);
        startTimeNanos = newSetTime;
    }

    protected synchronized void sync() {
        long nanos = getNanoClockTime();
        startTimeNanos = nanos;
        lastSeenNanoTime.set(nanos);
        ticksTimeline.set(nanos);
    }

    public String toString() {
        return "spec=" + rateSpec.toString() +
                ", rateDelay=" + this.getRateSchedulingDelay() +
                ", totalDelay=" + this.getTotalSchedulingDelay() +
                "\n (used/seen)=(" + ticksTimeline.get() + "/" + lastSeenNanoTime.get() +
                ", clock=" + getNanoClockTime() +
                ", actual=" + System.nanoTime() +
                ", closing=" + gapClosings.get();
    }

    /**
     * Set a ratio of scheduling gap which will be closed automatically
     * if it is not used. If this value is 1.0, then scheduling nanos that
     * were not used in real time will be forfeited by the caller. This is
     * use-it-or-lose-it scheduling. If this value is set to 0.0, then the
     * unused time by slow callers will remain on the schedule to be absorbed
     * by bursting or periods of higher-than-rate usage, up to the point at
     * which the average rate is met.
     * <p>
     * This value will be converted to the nearest 1/2N equivalent shift
     * value for fast processing internally. This means that 0.0, 0.5, 0.25, 0.125,
     * and so forth are all valid, but in-between values will be converted to the
     * nearest matching offset. 1.0 is Also valid.
     * </p>
     *
     * @param strictness A value between 0.0 and 1.0 that sets strictness.
     * @return The nano
     */
    public int setStrictness(double strictness) {
        this.strictness = strictness;
        if (strictness > 1.0D) {
            this.burstSlice = this.
            throw new RuntimeException("gap fill ratio must be between 0.0D and 1.0D");
        }
        if (strictness == 1.0D) {
            this.burstDampeningShift = 0;
        } else {
            long longsize = (long) (strictness * (double) Long.MAX_VALUE);
            this.burstDampeningShift = Math.min(Long.numberOfLeadingZeros(longsize), 63);
        }

        return this.burstDampeningShift;
    }

    public double getStrictness() {
        return this.strictness;
    }

    @Override
    public synchronized void update(RateSpec rateSpec) {

        if (getRate() != rateSpec.opsPerSec) {
            setRate(rateSpec.opsPerSec);
        }
        if (getStrictness() != rateSpec.strictness) {
            setStrictness(rateSpec.strictness);
        }
    }

    @Override
    public RateSpec getRateSpec() {
        return this.rateSpec;
    }

    protected long setTicksTime(long ticks) {
        long was = ticksTimeline.get();
        this.ticksTimeline.set(ticks);
        return was;
    }

    private enum State {
        Idle,
        Started
    }

}
