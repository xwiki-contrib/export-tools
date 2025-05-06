/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.exporttools.macros;

/**
 * Parameters for export table layout marco.
 *
 * @version $Id: $
 * @since 1.3.0
 */
public class ExportTablelayoutMacroParameters
{
    private boolean repeatTableHeaders;

    private String widths;

    private Orientation orientation;

    /**
     * Check if the table headers should be repeated on each page during PDF export.
     *
     * @return true if the table headers should be repeated on each page; false otherwise
     */
    public boolean isRepeatTableHeaders()
    {
        return repeatTableHeaders;
    }

    /**
     * Sets whether the table headers should be repeated on each page during PDF export.
     *
     * @param repeatTableHeaders define if the table headers should be repeated
     *                           (true to repeat headers, false otherwise)
     */
    public void setRepeatTableHeaders(boolean repeatTableHeaders)
    {
        this.repeatTableHeaders = repeatTableHeaders;
    }

    /**
     * Retrieves the widths configuration for table columns.
     *
     * @return the string representation of the widths of each column for the table.
     */
    public String getWidths()
    {
        return widths;
    }

    /**
     * Sets the widths for the table layout.
     *
     * @param widths the string representation of the widths of each column for the table.
     */
    public void setWidths(String widths)
    {
        this.widths = widths;
    }

    /**
     * Retrieves the orientation setting.
     *
     * @return the page orientation.
     */
    public Orientation getOrientation()
    {
        return orientation;
    }

    /**
     * Sets the orientation.
     *
     * @param orientation the page orientation.
     */
    public void setOrientation(Orientation orientation)
    {
        this.orientation = orientation;
    }
}
