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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.TableBlock;
import org.xwiki.rendering.block.TableCellBlock;
import org.xwiki.rendering.block.TableRowBlock;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import com.xpn.xwiki.XWikiContext;

/**
 * Table layout marco.
 *
 * @version $Id: $
 * @since 1.3.0
 */
@Component
@Named("export-tablelayout")
@Singleton
public class ExportTablelayoutMacro extends AbstractMacro<ExportTablelayoutMacroParameters>
{
    private static final Set<String> DEFAULT_CATEGORIES = Collections.singleton(DEFAULT_CATEGORY_FORMATTING);

    private static final String NAME = "Table layout";

    private static final String DESCRIPTION =
        "Change the rendering of table on export.";

    private static final String PERCENT = "%";

    private static final String STAR = "*";

    private static final String PX = "px";

    private static final String STYLE = "style";

    private static final String CSS_PERCENT_UNIT = "vb";

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private Logger logger;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public ExportTablelayoutMacro()
    {
        super(NAME, DESCRIPTION, ExportTablelayoutMacroParameters.class);
        setDefaultCategories(DEFAULT_CATEGORIES);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }

    @Override
    public List<Block> execute(ExportTablelayoutMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        XWikiContext xcontext = xwikiContextProvider.get();

        //if ("export".equals(xcontext.getAction())) {
        MacroBlock currentMacroBlock = context.getCurrentMacroBlock();
        TableBlock tableBlock = getTableBlock(currentMacroBlock.getNextSibling());
        applyParamOnTableBlock(tableBlock, parameters);
        //}
        return List.of();
    }

    private void applyParamOnTableBlock(TableBlock block, ExportTablelayoutMacroParameters parameters)
    {
        List<String> withParams = List.of();
        if (StringUtils.isNotEmpty(parameters.getWidths())) {
            withParams = parseWidthParam(parameters.getWidths());
            block.setParameter(STYLE, "table-layout:fixed;");
        }
        List<Block> rows = block.getBlocks(new ClassBlockMatcher(TableRowBlock.class), Block.Axes.DESCENDANT);
        for (Block row : rows) {
            List<Block> headerCells =
                row.getBlocks(new ClassBlockMatcher(TableCellBlock.class), Block.Axes.DESCENDANT);
            for (int i = 0; i < headerCells.size(); i++) {
                TableCellBlock cell = (TableCellBlock) headerCells.get(i);
                StringBuilder style = new StringBuilder();
                if (withParams.size() > i) {
                    String withParam = withParams.get(i);
                    style.append("width:").append(withParam).append(";");
                }
                cell.setParameter(STYLE, style.toString());
            }
        }
    }

    private TableBlock getTableBlock(Block blockParam)
    {
        Block block = blockParam;
        while (block != null) {
            if (block instanceof TableBlock) {
                return (TableBlock) block;
            }
            if (block.getChildren() != null && !block.getChildren().isEmpty()) {
                Block child = block.getChildren().get(0);
                TableBlock childResult = getTableBlock(child);
                if (childResult != null) {
                    return childResult;
                }
            }
            block = block.getNextSibling();
        }
        return null;
    }

    private List<String> parseWidthParam(String withParam)
    {
        String[] withs = withParam.split(",");
        List<String> result = new ArrayList<>(withs.length);
        Map<Integer, Integer> relativeParsedValues = new HashMap<>(withs.length);

        // Keep in mind that we also need to support the confluence value from scroll exporter
        // https://help.k15t.com/scroll-pdf-exporter/5.15/server/change-the-table-format

        // Note that mixing % and length unit is not good, it break calc function for CSS
        // (cf: https://github.com/w3c/csswg-drafts/issues/94). So we replace all percent by vb which bring some better
        // results

        for (int i = 0; i < withs.length; i++) {
            String w = withs[i];
            if (w.contains(PERCENT)) {
                result.add(w.strip().replace(PERCENT, CSS_PERCENT_UNIT));
            } else if (w.contains(STAR)) {
                // Handle the relative value. For example, if 60 pixels of space are available and the competing
                // relative lengths are 1*, 2*, and 3*, the 1* will be alloted 10 pixels, the 2* will be alloted 20
                // pixels, and the 3* will be alloted 30 pixels.
                String value = w.trim();
                if (STAR.equals(w.strip())) {
                    // The value "*" is equivalent to "1*".
                    value = "1*";
                }
                relativeParsedValues.put(i, Integer.parseInt(value.replace(STAR, "")));
                result.add(value);
            } else if (w.contains(PX)) {
                result.add(w.strip());
            } else if (w.strip().matches("\\d+")) {
                // Handle confluence value (value without unit implicitly mean px)
                result.add(w + PX);
            } else {
                result.add(w);
            }
        }
        List<String> notRelativeValues = result.stream().filter(i -> !i.endsWith(STAR)).collect(Collectors.toList());
        int sumRelativeValue = relativeParsedValues.values().stream().mapToInt(Integer::intValue).sum();

        // Calculate the relative values
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).endsWith(STAR)) {
                int percentValue = (relativeParsedValues.get(i) * 100) / sumRelativeValue;
                String newValue;
                if (notRelativeValues.isEmpty()) {
                    newValue = percentValue + CSS_PERCENT_UNIT;
                } else {
                    newValue = String.format("calc(%d%s - ((%s) * %d / %d))",
                        percentValue, CSS_PERCENT_UNIT, String.join(" + ", notRelativeValues),
                        relativeParsedValues.get(i), sumRelativeValue);
                }
                result.set(i, newValue);
            }
        }
        return result;
    }
}
