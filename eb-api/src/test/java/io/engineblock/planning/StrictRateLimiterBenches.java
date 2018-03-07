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

package io.engineblock.planning;

import io.engineblock.activityimpl.ActivityDef;
import io.engineblock.rates.RateLimiter;
import io.engineblock.rates.RateLimiters;
import io.engineblock.rates.RateSpec;
import org.testng.annotations.Test;


/**
 * These tests run all the rate limiter micro benches with strict rate
 * limiting only, due to the strictness level being set to 1.0D.
 */
@Test(enabled=true)
public class StrictRateLimiterBenches extends BaseRateLimiterBenches {
    
    protected RateLimiter getRateLimiter(String paramSpec, double rate) {
        return RateLimiters.createOrUpdate(
                ActivityDef.parseActivityDef("alias=testing"),
                null,
                new RateSpec(rate,1.0D)
        );
    }


}