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

/** A CharSequence which can efficiently subdivide and append itself. */
class PoolString implements CharSequence, java.io.Serializable {
	private final CharSequence base;
	private final int startIndex, endIndex;

	public static PoolString of(String base) {
		return of(base, 0, base.length());
	}

	public static PoolString of(String base, int startIndex, int endIndex) {
		if (startIndex == endIndex) {
			return empty;
		} else {
			return new PoolString(base, startIndex, endIndex);
		}
	}

	private static final PoolString empty = new PoolString("", 0, 0);

	private PoolString(CharSequence base, int startIndex, int endIndex) {
		//Preconditions.checkArgument(base instanceof StringBuilder || base instanceof String);
		Preconditions.checkArgument(0 <= startIndex);
		Preconditions.checkArgument(startIndex <= endIndex);
		Preconditions.checkArgument(endIndex <= base.length());
		this.base = base;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
	}

	@Override
	public int length() {
		return endIndex - startIndex;
	}

	@Override
	public char charAt(int index) {
		return base.charAt(startIndex + index);
	}

	@Override
	public PoolString subSequence(int start, int end) {
		if (start == end) {
			return empty();
		}
		return new PoolString(base, startIndex + start, startIndex + end);
	}

	@Override
	public String toString() {
		return base.subSequence(startIndex, endIndex).toString();
	}

	public PoolString concat(PoolString other) {
		if (this == empty) {
			return other;
		} else if (other == empty) {
			return this;
		} else if (base == other.base && endIndex == other.startIndex) {
			return new PoolString(base, startIndex, other.endIndex);
		} else {
			StringBuilder builder;
			int start, end;
			if (base instanceof StringBuilder) {
				builder = (StringBuilder) base;
				start = startIndex;
				end = endIndex + other.length();
			} else {
				builder = new StringBuilder(length() + other.length());
				builder.append(this);
				start = 0;
				end = length() + other.length();
			}
			builder.append(other);
			return new PoolString(builder, start, end);
		}
	}

	public PoolString concat(String other) {
		if (base instanceof String && (endIndex + other.length() <= base.length())) {
			String base = (String) this.base;
			for (int i = 0; i < other.length(); ++i) {
				if (base.charAt(i + endIndex) != other.charAt(i)) {
					return concat(of(other));
				}
			}
			return new PoolString(base, startIndex, endIndex + other.length());
		}
		return concat(of(other));
	}

	public static PoolString concat(CharSequence... poolStringsOrStrings) {
		if (poolStringsOrStrings.length == 0) {
			return empty;
		}
		PoolString total = asPool(poolStringsOrStrings[0]);
		for (int i = 1; i < poolStringsOrStrings.length; ++i) {
			CharSequence next = poolStringsOrStrings[i];
			if (next instanceof String) {
				total = total.concat((String) next);
			} else {
				total = total.concat((PoolString) next);
			}
		}
		return total;
	}

	private static PoolString asPool(CharSequence sequence) {
		return sequence instanceof PoolString ? ((PoolString) sequence) : PoolString.of((String) sequence);
	}

	public boolean sameAs(CharSequence other) {
		if (length() != other.length()) {
			return false;
		}
		for (int i = 0; i < length(); ++i) {
			if (charAt(i) != other.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	public boolean startsWith(String prefix) {
		if (length() < prefix.length()) {
			return false;
		}
		for (int i = 0; i < prefix.length(); ++i) {
			if (charAt(i) != prefix.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	public boolean endsWith(String suffix) {
		if (length() < suffix.length()) {
			return false;
		}
		int offset = length() - suffix.length();
		for (int i = 0; i < suffix.length(); ++i) {
			if (charAt(i + offset) != suffix.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	public int indexOf(String lookingFor) {
		int result;
		if (base instanceof String) {
			result = ((String) base).indexOf(lookingFor, startIndex);
		} else {
			result = ((StringBuilder) base).indexOf(lookingFor, startIndex);
		}
		if (result == -1 || result >= endIndex) {
			return -1;
		} else {
			return result - startIndex;
		}
	}

	public int indexOf(char lookingFor) {
		int result;
		if (base instanceof String) {
			result = ((String) base).indexOf(lookingFor, startIndex);
		} else {
			result = ((StringBuilder) base).indexOf(String.valueOf(lookingFor), startIndex);
		}
		if (result == -1 || result >= endIndex) {
			return -1;
		} else {
			return result - startIndex;
		}
	}

	/**
	 * Returns a PoolString which represents everything from
	 * the start of this string until `lookingFor` is found.
	 * If the string is never found, returns this.
	 */
	public PoolString until(String lookingFor) {
		int idx = indexOf(lookingFor);
		if (idx == -1) {
			return this;
		} else {
			return subSequence(0, idx);
		}
	}

	/** 
	 * Asserts that the other string was generated
	 * from a call to {@link #until(String)}, and
	 * then returns a new PoolString representing
	 * everything after that.
	 */
	public PoolString after(PoolString other) {
		if (other.isEmpty()) {
			return this;
		}
		Preconditions.checkArgument(other.base == base);
		Preconditions.checkArgument(other.startIndex == startIndex);
		if (other.endIndex == endIndex) {
			return empty;
		}
		Preconditions.checkArgument(other.endIndex < endIndex);
		return new PoolString(base, other.endIndex, endIndex);
	}

	/**
	 * Returns the line number of the start of this string.
	 * Throws an exception if this isn't based on a string
	 * any longer, because non-contiguous StringPools have
	 * been concatenated. 
	 */
	public int baseLineNumberStart() {
		return baseLineNumberOfOffset(startIndex);
	}

	/**
	 * Returns the line number of the end of this string.
	 * Throws an exception if this isn't based on a string
	 * any longer, because non-contiguous StringPools have
	 * been concatenated. 
	 */
	public int baseLineNumberEnd() {
		return baseLineNumberOfOffset(endIndex);
	}

	private int baseLineNumberOfOffset(int idx) {
		assertStringBased();
		int lineNumber = 1;
		for (int i = 0; i < base.length(); ++i) {
			if (base.charAt(i) == '\n') {
				++lineNumber;
			}
		}
		return lineNumber;
	}

	private void assertStringBased() {
		Preconditions.checkArgument(base instanceof String, "When you call concat on non-contiguous parts, you lose the conneciton to the original String.");
	}

	/** Returns the empty PoolString. */
	public static PoolString empty() {
		return empty;
	}

	public boolean isEmpty() {
		return length() == 0;
	}
}
