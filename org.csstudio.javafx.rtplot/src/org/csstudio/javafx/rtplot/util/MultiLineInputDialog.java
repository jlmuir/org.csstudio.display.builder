/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.util;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

/** Dialog for entering multi-line text
 *  @author Kay Kasemir
 */
public class MultiLineInputDialog extends Dialog<String>
{
    private final TextArea text;

    /** @param initial_text Initial text */
    public MultiLineInputDialog(final String initial_text)
    {
        text = new TextArea(initial_text);

        getDialogPane().setContent(new BorderPane(text));
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        setResizable(true);

        setResultConverter(button ->
        {
            return button == ButtonType.OK ? text.getText() : null;
        });
    }
}