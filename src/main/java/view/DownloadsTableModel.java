/*
    This file is part of Nephren.

    DownloadsTableModel.java
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

package view;

import util.HttpDownload;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class DownloadsTableModel extends AbstractTableModel implements Observer {
    private static final List<Map<String, Class<?>>> column =
            List.of(Map.of("網址", String.class),
                    Map.of("大小", String.class),
                    Map.of("進度", JProgressBar.class),
                    Map.of("狀態", String.class));
    private static final String zeroOver = "0 Bytes";
    private final List<HttpDownload> downloadList =
            Collections.synchronizedList(new ArrayList<>());

    public DownloadsTableModel() {
        super();
    }

    // Bytes 轉換成 KiB, MiB, GiB 等人類可讀單位
    public static String humanReadableByteCountBin(long bytes) {
        AtomicLong absB = bytes == Long.MIN_VALUE ? new AtomicLong(Long.MAX_VALUE) : new AtomicLong(Math.abs(bytes));
        if (absB.get() < 1024) {
            return bytes + " B";
        }
        long value = absB.get();
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB.get() > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }

    public void addDownload(HttpDownload download) {
        download.addObserver(this);
        downloadList.add(download);
        fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
    }

    public HttpDownload getDownload(int rowIndex) {
        return downloadList.get(rowIndex);
    }

    public void clearDownload(int rowIndex) {
        downloadList.remove(rowIndex);
        fireTableRowsDeleted(rowIndex, rowIndex);
    }

    public String getColumnName(int columnIndex) {
        return column.get(columnIndex).keySet().stream().findFirst().get();
    }

    public Class<?> getColumnClass(int columnIndex) {
        return column.get(columnIndex).values().stream().findFirst().get();
    }

    @Override
    public int getRowCount() {
        return downloadList.size();
    }

    @Override
    public int getColumnCount() {
        return column.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        HttpDownload download = downloadList.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> download.getUri();
            case 1 -> download.getSize() != -1L ?
                    humanReadableByteCountBin(download.getSize()) : zeroOver;
            case 2 -> download.getProgress();
            case 3 -> download.getStatus().name();
            default -> "";
        };
    }

    @Override
    public void update(Observable o, Object arg) {
        int index = downloadList.indexOf(o);
        fireTableRowsUpdated(index, index);
    }
}
