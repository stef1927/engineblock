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

package io.engineblock.activityapi.ratelimits;

import io.engineblock.activityapi.sysperf.SysPerf;
import io.engineblock.activityapi.sysperf.SysPerfData;

import java.util.concurrent.locks.LockSupport;

public class TokenFiller implements Runnable {

    public final static double MIN_PER_SECOND = 10D;
    public final static double MAX_PER_SECOND = 1000D;
    private final SysPerfData PERFDATA = SysPerf.get().getPerfData(false);
    private final long interval = (long) 1E6;

    private final TokenPool tokenPool;
    private volatile boolean running = true;
    private RateSpec rateSpec;
    private Thread thread;

    /**
     * A token filler adds tokens to a {@link TokenPool} at some rate.
     * By default, this rate is at least every millisecond +- scheduling jitter
     * in the JVM.
     *
     * @param rateSpec A rate specifier
     */
    public TokenFiller(RateSpec rateSpec) {
        this.rateSpec = rateSpec;
        this.tokenPool= new TokenPool(rateSpec);
        this.tokenPool.refill(rateSpec.getNanosPerOp());
    }

    public TokenFiller apply(RateSpec rateSpec) {
        this.rateSpec = rateSpec;
        this.tokenPool.apply(rateSpec);
        return this;
    }

    private void stop() {
        this.running=false;
    }

    public TokenPool getTokenPool() {
        return tokenPool;
    }

    @Override
    public void run() {
        long lastRefillAt = System.nanoTime();
        while (running) {
            long nextRefillTime = lastRefillAt + interval;
            long thisRefillTime = System.nanoTime();
            while (thisRefillTime < nextRefillTime) {
//            while (thisRefillTime < lastRefillAt + interval) {
                long parkfor = Math.max(nextRefillTime - thisRefillTime, 0L);
                //System.out.println(ANSI_Blue + "parking for " + parkfor + "ns" + ANSI_Reset);
                LockSupport.parkNanos(parkfor);
                thisRefillTime = System.nanoTime();
            }
            long delta = thisRefillTime - lastRefillAt;
            lastRefillAt = thisRefillTime;

            //System.out.println(this);
            tokenPool.refill(delta);
        }
    }

    public TokenFiller start() {
        thread = new Thread(this);
        thread.setName(this.toString());
        thread.setDaemon(true);
        thread.start();
        //System.out.println("Starting token filler thread: " + this.toString());
        return this;
    }

    @Override
    public String toString() {
        return "TokenFiller spec=" + rateSpec + " interval=" + this.interval + "ns pool:" + tokenPool +" running=" + running;
    }

}
