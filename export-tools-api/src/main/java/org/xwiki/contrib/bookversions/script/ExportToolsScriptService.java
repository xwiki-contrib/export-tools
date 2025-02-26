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
package org.xwiki.contrib.bookversions.script;

import java.io.StringReader;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.api.Document;

/**
 * Export tools script service.
 *
 * @version $Id$
 * @since 1.0.1
 */
@Component
@Named(ExportToolsScriptService.ID)
@Singleton
public class ExportToolsScriptService implements ScriptService
{
    /**
     * The id of this script service.
     */
    public static final String ID = "exportTools";

    private static final ClassBlockMatcher MACRO_MATCHER = new ClassBlockMatcher(MacroBlock.class);

    @Inject
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private Logger logger;

    /**
     * Parse the document and get the String content of the export-pageTitle macro.
     *
     * @param document The Document to parse.
     * @return The String content of the export-pageTitle macro.
     */
    public String getExportPageTitleMacroContent(Document document)
    {
        return findDetailsMacros(document.getXDOM(), document.getSyntax().toIdString());
    }

    private XDOM parse(String text, String syntaxId)
    {
        ComponentManager componentManager = componentManagerProvider.get();
        XDOM result;
        try {
            Parser parser = componentManager.getInstance(Parser.class, syntaxId);
            result = parser.parse(new StringReader(text));
        } catch (Exception e) {
            result = null;
        }
        return result;
    }

    private XDOM getMacroXDOM(MacroBlock macroBlock, String syntaxId)
        throws ComponentLookupException
    {
        ComponentManager componentManager = componentManagerProvider.get();
        if (componentManager.hasComponent(Macro.class, macroBlock.getId())) {
            ContentDescriptor macroContentDescriptor =
                ((Macro<?>) componentManager.getInstance(Macro.class, macroBlock.getId()))
                    .getDescriptor()
                    .getContentDescriptor();

            if (macroContentDescriptor != null && macroContentDescriptor.getType().equals(Block.LIST_BLOCK_TYPE)
                && StringUtils.isNotBlank(macroBlock.getContent()))
            {
                return parse(macroBlock.getContent(), syntaxId);
            }
        } else if (StringUtils.isNotBlank(macroBlock.getContent())) {
            // Just assume that the macro content is wiki syntax if we don't know the macro.
            logger.debug("Calling parse on unknown macro [{}] with syntax [{}]", macroBlock.getId(), syntaxId);
            return parse(macroBlock.getContent(), syntaxId);
        }
        return null;
    }

    private String findDetailsMacros(XDOM xdom, String syntaxId)
    {
        List<MacroBlock> macros = xdom.getBlocks(MACRO_MATCHER, Block.Axes.DESCENDANT_OR_SELF);
        for (MacroBlock macroBlock : macros) {
            try {
                if ("export-pagetitle".equals(macroBlock.getId())) {
                    return macroBlock.getContent();
                } else {
                    XDOM macroXDOM = getMacroXDOM(macroBlock, syntaxId);
                    if (macroXDOM != null) {
                        String res = findDetailsMacros(macroXDOM, syntaxId);
                        if (res != null) {
                            return res;
                        }
                    }
                }
            } catch (ComponentLookupException e) {
                logger.error("Component lookup error trying to find the confluence_details macro", e);
            }
        }
        return null;
    }
}
