/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.propsheet;

import org.csstudio.display.builder.util.undo.UndoableAction;
import org.csstudio.display.builder.util.undo.UndoableActionManager;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.eclipse.swt.graphics.FontData;

/** Undo-able command to change plot fonts
 *  @author Kay Kasemir
 */
public class ChangeTitleFontCommand extends UndoableAction
{
    final private Model model;
    final private FontData old_font, new_font;

    /** Register and perform the command
     *  @param model Model to configure
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param new_color New value
     */
    public ChangeTitleFontCommand(final Model model,
            final UndoableActionManager operations_manager,
            final FontData new_font)
    {
        super(Messages.TitleFontTT);
        this.model = model;
        this.old_font = model.getTitleFont();
        this.new_font = new_font;
        operations_manager.execute(this);
    }

    /** {@inheritDoc} */
    @Override
    public void run()
    {
        model.setTitleFont(new_font);
    }

    /** {@inheritDoc} */
    @Override
    public void undo()
    {
        model.setTitleFont(old_font);
    }
}
