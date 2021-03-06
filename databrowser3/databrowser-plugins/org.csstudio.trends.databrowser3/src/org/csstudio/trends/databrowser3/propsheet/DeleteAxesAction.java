/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.propsheet;

import org.csstudio.display.builder.util.undo.UndoableActionManager;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/** Action to delete value axes from model
 *  @author Kay Kasemir
 */
public class DeleteAxesAction extends Action
{
    final private UndoableActionManager operations_manager;
    final private Model model;
    final private TableViewer axes_table;

    public DeleteAxesAction(final UndoableActionManager operations_manager,
            final TableViewer axes_table, final Model model)
    {
        super(Messages.DeleteAxis,
                PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
        this.operations_manager = operations_manager;
        this.axes_table = axes_table;
        this.model = model;
    }

    @Override
    public void run()
    {
        // Get selected axis items
        final Object sel[] =
            ((IStructuredSelection)axes_table.getSelection()).toArray();
        if (sel.length <= 0)
            return;
        final AxisConfig axes[] = new AxisConfig[sel.length];
        for (int i=0; i<axes.length; ++i)
        {
            axes[i] = (AxisConfig)sel[i];
            // Check if axis is used by any model items.
            final ModelItem item = model.getFirstItemOnAxis(axes[i]);
            if (item != null)
            {
                MessageDialog.openWarning(axes_table.getTable().getShell(),
                        Messages.DeleteAxis,
                        NLS.bind(Messages.DeleteAxisWarningFmt,
                                axes[i].getName(), item.getName()));
                return;
            }
        }
        // Delete axes one by one.
        // While it would be almost trivial to remove many axes 'at once',
        // restoring them in the same order means keeping a copy of
        // the original axes array.
        // Doing it one by one, each DeleteAxisCommand only needs to remember
        // one axis position
        for (AxisConfig axis : axes)
            new DeleteAxisCommand(operations_manager, model, axis);
    }
}
