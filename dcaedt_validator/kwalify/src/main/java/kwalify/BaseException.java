/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * @(#)BaseException.java $Rev: 3 $ $Release: 0.5.1 $
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

public abstract class BaseException extends KwalifyRuntimeException  {

    private final String yPath;
    private final transient Object value;
    private final transient Rule rule;
    private int lineNum = -1;

    BaseException(String message, String ypath, Object value, Rule rule) {
        super(message);
        this.yPath = ypath;
        this.value = value;
        this.rule  = rule;
    }

    public String getPath() { return "".equals(yPath) ? "/" : yPath; }

    public Object getValue() { return value; }

    public Rule getRule() { return rule; }

    public int getLineNumber() { return lineNum; }

    public void setLineNumber(int lineNum) { this.lineNum = lineNum; }
}
