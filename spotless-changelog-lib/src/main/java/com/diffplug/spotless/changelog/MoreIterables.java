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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

final class MoreIterables {
	// prevent direct instantiation
	private MoreIterables() {}

	/** Returns a shallow copy of input elements, throwing on null elements. */
	static <T> List<T> toNullHostileList(Iterable<T> input) {
		List<T> shallowCopy = (input instanceof Collection)
				? new ArrayList<>(((Collection<?>) input).size())
				: new ArrayList<>();
		for (T e : input) {
			shallowCopy.add(Objects.requireNonNull(e));
		}
		return shallowCopy;
	}

	/** Sorts "raw" and removes duplicates, throwing on null elements. */
	static <T extends Comparable<T>> List<T> toSortedSet(Iterable<T> raw) {
		List<T> toBeSorted = toNullHostileList(raw);
		// sort it
		Collections.sort(toBeSorted);
		// remove any duplicates (normally there won't be any)
		if (toBeSorted.size() > 1) {
			Iterator<T> iter = toBeSorted.iterator();
			T last = iter.next();
			while (iter.hasNext()) {
				T next = iter.next();
				if (next.compareTo(last) == 0) {
					iter.remove();
				} else {
					last = next;
				}
			}
		}
		return toBeSorted;
	}
}
