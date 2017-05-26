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

package io.engineblock.activityimpl.tracker;

import io.engineblock.activityapi.cycletracking.CycleSegment;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class CoreTrackerTest {

    @Test
    public void testCore4() {
        CoreTracker ct4 = new CoreTracker(0,3,4,1);
        ct4.markResult(0,0);
        ct4.markResult(1,1);
        ct4.markResult(2,2);
        ct4.markResult(3,3);
//        assertThat(ct4.getMaxContiguousMarked()).isEqualTo(3);
    }

    @Test
    public void testRotationSingleExtent() {
        CoreTracker ct4 = new CoreTracker(0,11,4,2);
        ct4.markResult(0,0);
        ct4.markResult(1,1);
        ct4.markResult(2,2);
        ct4.markResult(3,3);
        CycleSegment segment1 = ct4.getSegment(4); // without this, will block here due to lack of segment allowance
        ct4.markResult(4,4);
        ct4.markResult(5,5);
        ct4.markResult(6,6);
        ct4.markResult(7,7);
        CycleSegment segment2 = ct4.getSegment(4);
//        CycleSegment segment2 = ct4.getSegment(2);
//        CycleSegment segment3 = ct4.getSegment(2);
        ct4.flush();

    }

    @Test
    public void testCompletionBlocking() {
        CoreTracker ct = new CoreTracker(0,100,10,3);
        List<CycleSegment> readSegments = new ArrayList<CycleSegment>();
        AtomicLong readCount = new AtomicLong(0L);

        Runnable reader = new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                for (int i = 0; i <= 99; i++) {
                    CycleSegment segment = ct.getSegment(1);
                    readCount.incrementAndGet();
                    readSegments.add(segment);
                }
            }
        };

        Runnable writer = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i <= 99; i++) {
                    ct.markResult(i,i);
                }
            }
        };
        Thread readerThread = new Thread(reader);
        readerThread.start();
        Thread writerThread = new Thread(writer);
        writerThread.start();

        try {
            readerThread.join();
            writerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(readSegments).hasSize(100);
        System.out.println("finished writer and reader");
    }

    @Test
    public void testCompletionBlockingBulk() {
        long min=0;
        long max=1000000;
        CoreTracker ct = new CoreTracker(0,max,100000,4);
        List<CycleSegment> readSegments = new ArrayList<CycleSegment>();
        AtomicLong readCount = new AtomicLong(0L);

        Runnable reader = new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                for (long i = min; i < max; i++) {
                    CycleSegment segment = ct.getSegment(1);
                    readCount.incrementAndGet();
                    readSegments.add(segment);
                }
            }
        };

        Runnable writer = new Runnable() {
            @Override
            public void run() {
                for (long i = min; i < max; i++) {
                    ct.markResult(i,(int) i);
                }
            }
        };
        Thread readerThread = new Thread(reader);
        readerThread.start();
        Thread writerThread = new Thread(writer);
        writerThread.start();

        try {
            readerThread.join();
            writerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(readSegments).hasSize(1000000);

        System.out.println("finished writer and reader");
    }


}