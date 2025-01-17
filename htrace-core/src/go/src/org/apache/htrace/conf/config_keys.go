/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package conf

import (
	"fmt"
	"os"
)

//
// Configuration keys for HTrace.
//

// The platform-specific path separator.  Usually slash.
var PATH_SEP string = fmt.Sprintf("%c", os.PathSeparator)

// The platform-specific path list separator.  Usually colon.
var PATH_LIST_SEP string = fmt.Sprintf("%c", os.PathListSeparator)

// The name of the XML configuration file to look for.
const CONFIG_FILE_NAME = "htraced.xml"

// The web address to start the REST server on.
const HTRACE_WEB_ADDRESS = "web.address"

// The default port for the Htrace web address.
const HTRACE_WEB_ADDRESS_DEFAULT_PORT = 9095

// The directories to put the data store into.  Separated by PATH_LIST_SEP.
const HTRACE_DATA_STORE_DIRECTORIES = "data.store.directories"

// Boolean key which indicates whether we should clear data on startup.
const HTRACE_DATA_STORE_CLEAR = "data.store.clear"

// How many writes to buffer before applying backpressure to span senders.
const HTRACE_DATA_STORE_SPAN_BUFFER_SIZE = "data.store.span.buffer.size"

// Default values for HTrace configuration keys.
var DEFAULTS = map[string]string{
	HTRACE_WEB_ADDRESS: fmt.Sprintf("0.0.0.0:%d", HTRACE_WEB_ADDRESS_DEFAULT_PORT),
	HTRACE_DATA_STORE_DIRECTORIES: PATH_SEP + "tmp" + PATH_SEP + "htrace1" +
		PATH_LIST_SEP + PATH_SEP + "tmp" + PATH_SEP + "htrace2",
	HTRACE_DATA_STORE_CLEAR:            "false",
	HTRACE_DATA_STORE_SPAN_BUFFER_SIZE: "100",
}
