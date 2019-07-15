/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * @(#)CommandOptionException.java	$Rev: 4 $ $Release: 0.5.1 $
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
 * exception class thrown if command-line option is wrong
 * 
 * @revision    $Rev: 4 $
 * @release     $Release: 0.5.1 $
 */
public class CommandOptionException extends KwalifyException {
    private static final long serialVersionUID = 6433387612335104714L;

    private String _error_symbol = null;
    private char _option;

    public CommandOptionException(String message, char option, String error_symbol) {
        super(message);
        _option = option;
        _error_symbol = error_symbol;
    }

    public String getErrorSymbol() { return _error_symbol; }
    public void setErrorSymbol(String error_symbol) { _error_symbol = error_symbol; }

    public char getOption() { return _option; }
    public void setOption(char option) { _option = option; }

}
