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


import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class BlowdryerPluginTest extends GradleHarness {
	private void githubTagExtra(String path, String tag, String... extra) throws IOException {
		write(path,
				"plugins {",
				"  id 'com.diffplug.blowdryer'",
				"}",
				"blowdryer {",
				"  github('diffplug/blowdryer', 'tag', '" + tag + "');",
				"}",
				"import com.diffplug.blowdryer.Blowdryer",
				"",
				Arrays.stream(extra).collect(Collectors.joining("\n")));
	}

	@Test
	public void githubTag() throws IOException {
		githubTagExtra("build.gradle", "test/2/a",
				"assert Blowdryer.file('sample').text == 'a'",
				"assert Blowdryer.prop('sample', 'name') == 'test'",
				"assert Blowdryer.prop('sample', 'ver_spotless') == '1.2.0'");
		gradleRunner().build();

		githubTagExtra("build.gradle", "test/2/b",
				"assert Blowdryer.file('sample').text == 'b'",
				"assert Blowdryer.prop('sample', 'name') == 'testB'",
				"assert Blowdryer.prop('sample', 'group') == 'com.diffplug.gradleB'");
		gradleRunner().build();

		// double-check that failures do fail
		githubTagExtra("build.gradle", "test/2/b",
				"assert Blowdryer.file('sample').text == 'a'");
		gradleRunner().buildAndFail();
	}

	@Test
	public void devLocal() throws IOException {
		write("../blowdryer-script/src/main/resources/sample", "c");
		write("../blowdryer-script/src/main/resources/sample.properties",
				"name=test",
				"group=com.diffplug.gradle");
		write("build.gradle",
				"plugins {",
				"  id 'com.diffplug.blowdryer'",
				"}",
				"blowdryer {",
				"  devLocal('../blowdryer-script');",
				"}",
				"import com.diffplug.blowdryer.Blowdryer",
				"",
				"assert Blowdryer.file('sample').text == 'c\\n'",
				"assert Blowdryer.prop('sample', 'name') == 'test'",
				"assert Blowdryer.prop('sample', 'group') == 'com.diffplug.gradle'");
		gradleRunner().build();
	}

	@Test
	public void multiproject() throws IOException {
		write("settings.gradle",
				"include 'subproject'");
		githubTagExtra("build.gradle", "test/2/a",
				"assert Blowdryer.file('sample').text == 'a'",
				"assert Blowdryer.prop('sample', 'name') == 'test'",
				"assert Blowdryer.prop('sample', 'group') == 'com.diffplug.gradle'");
		write("subproject/build.gradle",
				"import com.diffplug.blowdryer.Blowdryer",
				"",
				"assert Blowdryer.file('sample').text == 'a'",
				"assert Blowdryer.prop('sample', 'name') == 'test'",
				"assert Blowdryer.prop('sample', 'group') == 'com.diffplug.gradle'");
		gradleRunner().build();

		// double-check that failures do fail
		write("subproject/build.gradle",
				"import com.diffplug.blowdryer.Blowdryer",
				"",
				"assert Blowdryer.file('sample').text == 'b'");
		gradleRunner().buildAndFail();
	}

	@Test
	public void missingResourceThrowsError() throws IOException {
		githubTagExtra("build.gradle", "test/2/a",
				"Blowdryer.file('notPresent')");
		Assertions.assertThat(gradleRunner().buildAndFail().getOutput().replace("\r\n", "\n")).contains(
				"https://raw.githubusercontent.com/diffplug/blowdryer/test/2/a/src/main/resources/notPresent\n" +
						"  received http code 404\n" +
						"  404: Not Found");
	}

	@Test
	public void applyOnNonRootThrowsError() throws IOException {
		write("settings.gradle",
				"include 'subproject'");
		githubTagExtra("subproject/build.gradle", "test/2/a");
		Assertions.assertThat(gradleRunner().buildAndFail().getOutput().replace("\r\n", "\n")).contains(
				"An exception occurred applying plugin request [id: 'com.diffplug.blowdryer']\n" +
						"> Failed to apply plugin [id 'com.diffplug.blowdryer']\n" +
						"   > You must apply com.diffplug.blowdryer only on the root project, not :subproject");
	}
}
