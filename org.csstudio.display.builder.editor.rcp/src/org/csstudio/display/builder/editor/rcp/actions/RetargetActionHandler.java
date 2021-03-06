/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.rcp.actions;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.eclipse.jface.action.Action;

/** Base for retarget-action handler
 *  @author Kay Kasemir
 */
public class RetargetActionHandler extends Action
{   // Used as handler for RetargetAction, so no need for label, icon
    protected final DisplayEditor editor;

    public RetargetActionHandler(final DisplayEditor editor, final String command_id)
    {
        this.editor = editor;
        setActionDefinitionId(command_id);
    }
}
