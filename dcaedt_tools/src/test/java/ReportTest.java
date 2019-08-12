/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * Copyright (C) 2019 Samsung. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

import static org.junit.Assert.assertEquals;

import java.security.Permission;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.junit.Before;
import org.junit.Test;

import utilities.Report;

public class ReportTest {

    private static final String TEST_CREATED_MESSAGE = "testCreateMessage";
    private static final String TEST_UPDATED_MESSAGE = "testUpdateMessage";
    private static final String TEST_NOT_UPDATED_MESSAGE = "testNotUpdateMessage";
    private static final String TEST_ERROR_MESSAGE = "testErrorMessage";

    private static final String CREATED_KEY = "Created:";
    private static final String UPDATED_KEY = "Updated:";
    private static final String NOT_UPDATED_KEY = "Not updated:";
    private static final String ERROR_KEY = "Error:";

    private Report testObject;

    @Before
    public void setup() {
        testObject = new Report();
    }

    @Test
    public void createReportTest() {
        testObject.addCreatedMessage(TEST_CREATED_MESSAGE);
        testObject.addUpdatedMessage(TEST_UPDATED_MESSAGE);
        testObject.addNotUpdatedMessage(TEST_NOT_UPDATED_MESSAGE);
        testObject.addErrorMessage(TEST_ERROR_MESSAGE);

        Map result = toHashMap(testObject.toString().split(System.lineSeparator()));

        assertEquals(TEST_CREATED_MESSAGE, result.get(CREATED_KEY));
        assertEquals(TEST_UPDATED_MESSAGE, result.get(UPDATED_KEY));
        assertEquals(TEST_NOT_UPDATED_MESSAGE, result.get(NOT_UPDATED_KEY));
        assertEquals(TEST_ERROR_MESSAGE, result.get(ERROR_KEY));
    }

    @Test
    public void statusCodeTest() {
        checkSystemExitStatus(0);
        checkSystemExitStatus(2);
    }

    private Map<String, String> toHashMap(String[] result) {
        Map<String, String> hashMap = new HashMap<>();
        fill(result, hashMap::put);
        return hashMap;
    }

    private void fill(String[] collection, BiConsumer<String, String> consumer) {
        for (int i = 0; i < collection.length; i += 2) {
            consumer.accept(collection[i], collection[i + 1]);
        }
    }

    private void checkSystemExitStatus(int statusCode) {

        class NoExitException extends RuntimeException {
        }

        SecurityManager securityManager = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission permission) {
            }

            @Override
            public void checkPermission(Permission permission, Object o) {
            }

            @Override
            public void checkExit(int status) {
                assertEquals(statusCode, status);
                super.checkExit(status);
                throw new NoExitException();
            }
        });

        try {
            testObject.setStatusCode(statusCode);
            testObject.reportAndExit();
        } catch (Exception ignore) {
        }

        System.setSecurityManager(securityManager);
    }
}
