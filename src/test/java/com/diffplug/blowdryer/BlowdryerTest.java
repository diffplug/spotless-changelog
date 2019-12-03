/*
 * Copyright 2019 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.blowdryer;


import org.assertj.core.api.Assertions;
import org.junit.Test;

public class BlowdryerTest {
	@Test
	public void filenameSafe() {
		filenameSafe("http://shortName.com/a+b-0-9~Z", "http-shortName.com-a+b-0-9-Z");
		filenameSafe("https://raw.githubusercontent.com/diffplug/durian-build/07f588e52eb0f31e596eab0228a5df7233a98a14/gradle/spotless/spotless.license.java",
				"https-raw.githubusercontent.com-diffplug--3vpUTw--14-gradle-spotless-spotless.license.java");
	}

	private void filenameSafe(String url, String safe) {
		Assertions.assertThat(Blowdryer.filenameSafe(url)).isEqualTo(safe);
	}
}
