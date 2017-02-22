/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.rcp;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.EditorUtil;
import org.csstudio.display.builder.editor.tree.WidgetTree;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.javafx.swt.JFX_SWT_Wrapper;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

/** Outline view
 *
 *  <p>Displays tree of widgets
 *  @author Kay Kasemir
 */
public class OutlinePage extends Page implements IContentOutlinePage
{
    private class CopyAction extends Action
    {
        public CopyAction()
        {
            super(Messages.Copy);
            setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
        }

        @Override
        public void run()
        {
            editor.copyToClipboard();
        }
    }

    private class DeleteAction extends Action
    {
        public DeleteAction()
        {
            super(Messages.Delete);
            setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
        }

        @Override
        public void run()
        {
            editor.cutToClipboard();
        }
    }
    private final DisplayEditor editor;
    private final WidgetTree tree;

    private Control canvas;

    public OutlinePage(final DisplayEditor editor)
    {
        this.editor = editor;
        tree = new WidgetTree(editor.getWidgetSelectionHandler());
    }

    public void setModel(final DisplayModel model)
    {
        tree.setModel(model);
    }

    @Override
    public void createControl(final Composite parent)
    {
        final JFX_SWT_Wrapper wrapper = new JFX_SWT_Wrapper(parent, () ->
        {
            // StackPane w/ tree as single child to 'fill' the available space.
            final StackPane root = new StackPane(tree.create());
            return new Scene(root, 200.0, 400.0);
        });
        canvas = wrapper.getFXCanvas();
        EditorUtil.setSceneStyle(wrapper.getScene());

        createContextMenu();
    }

    private void createContextMenu()
    {
        final MenuManager manager = new MenuManager();
        manager.add(new CopyAction());
        manager.add(new DeleteAction());
        final Menu menu = manager.createContextMenu(canvas);
        canvas.setMenu(menu);
    }

    @Override
    public Control getControl()
    {
        return canvas;
    }

    @Override
    public void setFocus()
    {
        canvas.setFocus();
    }

    // Pass selection from WidgetTree on to SWT/RCP?
    // Seems unnecessary, the WidgetTree and DisplayEditor
    // use their own WidgetSelectionHandler, not RCP.

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener)
    {
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener)
    {
    }

    @Override
    public ISelection getSelection()
    {
        return null;
    }

    @Override
    public void setSelection(ISelection selection)
    {
    }
}
