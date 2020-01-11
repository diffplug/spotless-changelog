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


import com.diffplug.common.base.Errors;
import java.io.IOException;
import java.io.Serializable;

public class Misc {
	/** Copies the given value using serialization. */
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T copy(T input) {
		try {
			Serialized<T> serialized = Serialized.fromValue(input);
			Serialized<?> copy = Serialized.fromBytes(serialized.bytes(), input.getClass());
			return (T) copy.value();
		} catch (IOException | ClassNotFoundException e) {
			throw Errors.asRuntime(e);
		}
	}
}