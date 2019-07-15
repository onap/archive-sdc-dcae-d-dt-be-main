/*-
 * ============LICENSE_START=======================================================
 * SDC
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

/*
 * @(#)InvalidPathException.java	$Rev: 4 $ $Release: 0.5.1 $
 *
 * copyright(c) 2005 kuwata-lab all rights reserved.
 */

package kwalify;

/**
 * exception class thrown by YamlParser#setErrorsLineNumber() when path is wrong
 *
 * @revision    $Rev: 4 $
 * @release     $Release: 0.5.1 $
 */
public class InvalidPathException extends KwalifyRuntimeException {
    private static final long serialVersionUID = -4601461998104850880L;
    
    //private int _linenum;

    public InvalidPathException(String message) {
        super(message);
    }
}
