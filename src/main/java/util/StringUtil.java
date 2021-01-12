/*
    This file is part of Nephren.

    StringUtil.java
    Copyright (C) 2020, 2021  Relius Wang

    Nephren is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Nephren is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with Nephren.  If not, see <https://www.gnu.org/licenses/>.
 */

package util;

import org.jetbrains.annotations.NotNull;

public class StringUtil {
    private StringUtil() {
    }

    public static boolean startsWithIgnoreCase(@NotNull String str, @NotNull String prefix) {
        return (str.length() >= prefix.length() &&
                str.regionMatches(true, 0, prefix, 0, prefix.length()));
    }

}
