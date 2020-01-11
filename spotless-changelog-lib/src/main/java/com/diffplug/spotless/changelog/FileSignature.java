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


import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/** Computes the canon. */
final class FileSignature implements Serializable {
	private static final long serialVersionUID = 1L;

	private final String filename;
	@SuppressWarnings("unused")
	private final long filesize;
	@SuppressWarnings("unused")
	private final long lastModified;

	/** Signs the given file. */
	public static FileSignature sign(File file) throws IOException {
		return new FileSignature(file);
	}

	private FileSignature(File file) throws IOException {
		File canonical = file.getCanonicalFile();
		filename = canonical.getAbsolutePath();
		filesize = canonical.length();
		lastModified = canonical.lastModified();
	}

	public String canonicalPath() {
		return filename;
	}
}
