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

package io.engineblock.markers.filebuffer;

import io.engineblock.activityapi.cycletracking.CycleResultSource;
import io.engineblock.activityimpl.ActivityDef;

import java.util.concurrent.atomic.AtomicLong;

public class FileBufferResultSource implements CycleResultSource {

    private ActivityDef activityDef;

    public FileBufferResultSource(ActivityDef activityDef) {
        this.activityDef = activityDef;
    }

    @Override
    public boolean isCycleCompleted(long cycle) {
        return false;
    }

    @Override
    public long getMaxContiguousMarked() {
        return 0;
    }

    @Override
    public AtomicLong getMinCycle() {
        return null;
    }

    @Override
    public AtomicLong getMaxCycle() {
        return null;
    }

    @Override
    public long getPendingCycle() {
        return 0;
    }

    @Override
    public long getCycleInterval(long stride) {
        return 0;
    }
}
