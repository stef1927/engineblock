/*
*   Copyright 2016 jshook
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
package io.engineblock.script;

import io.engineblock.core.ScenarioResult;
import org.testng.annotations.Test;


/**
 * These is here for experimentation on microbench scripts without requiring
 * them to be included in builds
 */
@Test
public class SpeedChecks {

    @Test(enabled = false)
    public void testSpeedSanity() {
        ScenarioResult scenarioResult = ScriptTests.runScenario("speedcheck");
    }

    @Test(enabled = false)
    public void testThreadSpeeds() {
        ScenarioResult scenarioResult = ScriptTests.runScenario("threadspeeds");
    }


}
