/*
    This file is part of Nephren.

    ProgressRender.java
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

package view;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ProgressRender extends JProgressBar implements TableCellRenderer {

    public ProgressRender(int min, int max) {
        super(min, max);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setValue((int) Math.round((Double) value));
        return this;
    }
}
