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

package io.engineblock.rates;

import io.engineblock.activityimpl.ActivityDef;

import java.util.concurrent.atomic.AtomicLong;

public class TestableAverageRateLimiter extends AverageRateLimiter {

    private AtomicLong clock;

    public TestableAverageRateLimiter(AtomicLong clock, double maxOpsPerSecond, double strictness, ActivityDef def) {
        super(def,maxOpsPerSecond,strictness);
        this.clock = clock;
    }

    public long setClock(long newValue) {
        long oldValue = clock.get();
        clock.set(newValue);
        return oldValue;
    }

    public long getClock() {
        return clock.get();
    }

    @Override
    protected long getNanoClockTime() {
        return clock.get();
    }
}