/*
    This file is part of Nephren.

    HashUtil.java
    Copyright (C) 2020, 2021, 2023  Relius Wang

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

import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    private HashUtil() {
    }

    public static String apply(ByteBuffer file, @NotNull String hashStr) throws DigestException {
        MessageDigest md;
        StringBuilder sb = new StringBuilder();
        byte[] hash;
        try {
            md = MessageDigest.getInstance(hashStr);
            hash = md.digest(file.array());
            // 將 bytes 轉成 16 進制字串
            for (byte b : hash) {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
        } catch (NoSuchAlgorithmException ex) {
            throw new DigestException("couldn't make digest of partial content");
        }
        return sb.toString();
    }
}
