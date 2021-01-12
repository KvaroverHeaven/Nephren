/*
    This file is part of Nephren.

    URIParser.java
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

import java.net.URI;

public class URIParser {

    private URIParser() {
    }

    public static URI apply(@NotNull String uriString) {
        URI newUri = null;
        try {
            // 只有 https:// 和 http:// 才能允許下載
            if (StringUtil.startsWithIgnoreCase(uriString, "https://") ||
                    StringUtil.startsWithIgnoreCase(uriString, "http://")) {
                newUri = URI.create(uriString);
            }
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        return newUri;
    }
}
