/*
 * Copyright 2017 DiffPlug
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


import com.diffplug.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.AbstractFileAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class ResourceHarness {
	/**
	 * On OS X, the temp folder is a symlink,
	 * and some of gradle's stuff breaks symlinks.
	 * By only accessing it through the {@link #rootFolder()}
	 * and {@link #newFile()} apis, we can guarantee there
	 * will be no symlink problems.
	 */
	@Rule
	public TemporaryFolder folderDontUseDirectly = new TemporaryFolder();

	/** Returns the root folder (canonicalized to fix OS X issue) */
	protected File rootFolder() throws IOException {
		return folderDontUseDirectly.getRoot().getCanonicalFile();
	}

	/** Returns a File (in a temporary folder) which has the given contents. */
	protected File file(String subpath) throws IOException {
		return new File(rootFolder(), subpath);
	}

	/** Returns a File (in a temporary folder) which has the given contents. */
	protected File write(String subpath, byte[] content) throws IOException {
		File file = file(subpath);
		file.getParentFile().mkdirs();
		Files.write(file.toPath(), content);
		return file;
	}

	/** Writes the given content to the given path. */
	protected File write(String path, String... lines) throws IOException {
		File file = file(path);
		file.getParentFile().mkdirs();
		Files.write(file.toPath(), Arrays.asList(lines), StandardCharsets.UTF_8);
		return file;
	}

	protected AbstractFileAssert<?> assertFile(String path) throws IOException {
		return Assertions.assertThat(file(path));
	}

	protected ListAssert<String> assertFolderContent(String path) throws IOException {
		List<String> children = new ArrayList<>();
		for (File child : file(path).listFiles()) {
			children.add(child.getName());
		}
		Collections.sort(children);
		return Assertions.assertThat(children);
	}

	/** Returns the contents of the given file from the src/test/resources directory. */
	protected static byte[] readTestResource(String filename) throws IOException {
		URL url = ResourceHarness.class.getResource("/" + filename);
		if (url == null) {
			throw new IllegalArgumentException("No such resource " + filename);
		}
		return Resources.toByteArray(url);
	}
}
