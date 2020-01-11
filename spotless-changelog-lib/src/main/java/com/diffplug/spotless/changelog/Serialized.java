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


import com.diffplug.common.base.Preconditions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

/** Represents a serializable value and its serialized bytes.  Equality is determined solely by equal bytes. */
class Serialized<T extends Serializable> {
	private final byte[] bytes;
	private final T value;

	private Serialized(byte[] bytes, T value) {
		this.bytes = bytes;
		this.value = value;
	}

	public byte[] bytes() {
		return bytes;
	}

	public T value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (bytes == null) {
			return false;
		} else if (other instanceof Serialized) {
			Serialized<?> o = (Serialized<?>) other;
			return Arrays.equals(bytes, o.bytes);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(bytes);
	}

	/** Extracts a serialized directly from the given bytes. */
	public static <T extends Serializable> Serialized<T> fromBytes(byte[] bytes, Class<T> clazz) throws IOException, ClassNotFoundException {
		try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
			@SuppressWarnings("unchecked")
			T object = (T) input.readObject();
			Preconditions.checkArgument(clazz.isInstance(object), "Expected %s, was %s", clazz, object.getClass());
			return new Serialized<T>(bytes, object);
		}
	}

	/** Serializes the given value. */
	public static <T extends Serializable> Serialized<T> fromValue(T value) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(byteStream)) {
			output.writeObject(value);
		}
		return new Serialized<>(byteStream.toByteArray(), value);
	}
}
