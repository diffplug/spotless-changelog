/*
 * Copyright 2020 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.spotless.changelog;


import org.assertj.core.api.Assertions;
import org.junit.Test;

public class PoolStringTest {
	@Test
	public void afterTest() {
		PoolString abcdef = PoolString.of("abcdef");

		PoolString untilA = abcdef.until("a");
		Assertions.assertThat(untilA).hasToString("");
		Assertions.assertThat(abcdef.after(untilA)).hasToString("abcdef");

		PoolString untilC = abcdef.until("c");
		Assertions.assertThat(untilC).hasToString("ab");
		Assertions.assertThat(abcdef.after(untilC)).hasToString("cdef");

		PoolString untilF = abcdef.until("f");
		Assertions.assertThat(untilF).hasToString("abcde");
		Assertions.assertThat(abcdef.after(untilF)).hasToString("f");

		PoolString untilZ = abcdef.until("z");
		Assertions.assertThat(untilZ).hasToString("abcdef");
		Assertions.assertThat(abcdef.after(untilZ)).hasToString("");
	}
}
