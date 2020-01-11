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
import com.diffplug.common.base.Preconditions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bouncycastle.util.Arrays;
import pl.tlinkowski.annotation.basic.NullOr;

/** Represents a serialized value and its bytes.  Equality is determined solely by equal bytes. */
class Serialized<T extends Serializable> {
	private final @NullOr byte[] bytes;
	private final @NullOr T value;

	private Serialized(byte[] bytes, T value) {
		this.bytes = bytes;
		this.value = value;
	}

	public @NullOr T value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (bytes == null) {
			return false;
		} else if (other instanceof Serialized) {
			Serialized<?> o = (Serialized<?>) other;
			return Arrays.areEqual(bytes, o.bytes);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(bytes);
	}

	/** Writes out the serialized value to the given file. */
	public void writeTo(File cacheOutput) throws IOException {
		Path path = cacheOutput.toPath();
		Files.createDirectories(path.getParent());
		Files.write(path, bytes);
	}

	/** Reads the value from the file.  No problem if the file doesn't exist or doesn't have the expected value, it will just return a null value. */
	public static <T extends Serializable> Serialized<T> fromFile(File file, Class<T> clazz) throws IOException, ClassNotFoundException {
		if (!(file.exists() && file.isFile())) {
			return new Serialized<T>(null, null);
		}
		try {
			byte[] bytes = Files.readAllBytes(file.toPath());
			try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
				@SuppressWarnings("unchecked")
				T object = (T) input.readObject();
				Preconditions.checkArgument(clazz.isInstance(object), "Expected %s, was %s", clazz, object.getClass());
				return new Serialized<T>(bytes, object);
			}
		} catch (Exception e) {
			Errors.log().accept(e);
			return new Serialized<T>(null, null);
		}
	}

	public static <T extends Serializable> Serialized<T> fromValue(T value) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(byteStream)) {
			output.writeObject(value);
		}
		return new Serialized<>(byteStream.toByteArray(), value);
	}
}
