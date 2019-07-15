/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * @(#)KwalifyRuntimeException.java	$Rev: 3 $ $Release: 0.5.1 $
 * copyright(c) 2005 kuwata-lab all rights reserved.
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


package kwalify;

/**
 * base class of all runtime exception class in Kwalify
 *
 * @revision    $Rev: 3 $
 * @release     $Release: 0.5.1 $
 */
public abstract class KwalifyRuntimeException extends RuntimeException {
    public KwalifyRuntimeException(String message) {
        super(message);
    }
}
