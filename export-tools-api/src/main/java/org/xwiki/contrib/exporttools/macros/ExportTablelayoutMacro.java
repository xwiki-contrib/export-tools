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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
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
        MacroTransformationContext context)
    {
        XWikiContext xcontext = xwikiContextProvider.get();

        if ("export".equals(xcontext.getAction())) {
            MacroBlock currentMacroBlock = context.getCurrentMacroBlock();
            TableBlock tableBlock = getTableBlock(currentMacroBlock.getNextSibling());
            applyParamOnTableBlock(tableBlock, parameters);
        }
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
        List<Map.Entry<String, Integer>> widthParam = new ArrayList<>(withs.length);

        // Keep in mind that we also need to support the confluence value from scroll exporter
        // https://help.k15t.com/scroll-pdf-exporter/5.15/server/change-the-table-format

        for (String w : withs) {
            if (w.contains(PERCENT)) {
                widthParam.add(Map.entry(PERCENT, Integer.parseInt(w.replace(PERCENT, ""))));
            } else if (w.contains(STAR)) {
                // Handle the relative value. For example, if 60 pixels of space are available and the competing
                // relative lengths are 1*, 2*, and 3*, the 1* will be alloted 10 pixels, the 2* will be alloted 20
                // pixels, and the 3* will be alloted 30 pixels.
                String value = w.trim();
                if (STAR.equals(w.strip())) {
                    // The value "*" is equivalent to "1*".
                    value = "1*";
                }
                widthParam.add(Map.entry(STAR, Integer.parseInt(value.replace(STAR, ""))));
            } else if (w.strip().matches("\\d+")) {
                // Handle confluence value (value without unit implicitly mean px)
                widthParam.add(Map.entry(PX, Integer.parseInt(w.trim())));
            } else {
                logger.warn("Invalid value [{}], this will be ignored", w);
            }
        }
        if (widthParam.isEmpty()) {
            logger.warn("Can't decode any value of 'widths' parameter, the widths parameter will be ignored");
            return List.of();
        }

        String unit = widthParam.get(0).getKey();
        if (widthParam.stream().anyMatch(x -> !unit.equals(x.getKey()))) {
            logger.warn("Mixing unit for the 'widths' parameter will result to unexpected result");
        }
        int sumRelative = widthParam
            .stream()
            .filter(x -> STAR.equals(x.getKey()))
            .map(Map.Entry::getValue)
            .mapToInt(Integer::intValue)
            .sum();
        int sumPx = widthParam
            .stream()
            .filter(x -> PX.equals(x.getKey()))
            .map(Map.Entry::getValue)
            .mapToInt(Integer::intValue)
            .sum();

        List<String> result = new ArrayList<>(withs.length);
        for (Map.Entry<String, Integer> i : widthParam) {
            switch (i.getKey()) {
                case PERCENT:
                    result.add(i.getValue() + PERCENT);
                    break;
                case STAR:
                    result.add(((i.getValue() * 100) / sumRelative) + PERCENT);
                    break;
                case PX:
                    result.add(((i.getValue() * 100) / sumPx) + PERCENT);
                    break;
                default:
                    logger.error("Unknown unit [{}]", i.getKey());
                    break;
            }
        }
        return result;
    }
}
