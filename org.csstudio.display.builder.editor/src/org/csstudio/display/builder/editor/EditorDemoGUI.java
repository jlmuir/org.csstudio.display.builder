/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import static org.csstudio.display.builder.editor.Plugin.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.logging.Level;

import org.csstudio.display.builder.editor.actions.ActionDescription;
import org.csstudio.display.builder.editor.actions.LoadModelAction;
import org.csstudio.display.builder.editor.actions.SaveModelAction;
import org.csstudio.display.builder.editor.properties.PropertyPanel;
import org.csstudio.display.builder.editor.tree.WidgetTree;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.persist.ModelLoader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.csstudio.display.builder.util.ResourceUtil;

import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** All the editor components for standalone test
 *
 *  <pre>
 *  Toolbar
 *  ------------------------------------------------
 *  WidgetTree | Editor (w/ palette) | PropertyPanel
 *  ------------------------------------------------
 *  Status
 *  </pre>
 *
 *  @author Kay Kasemir
 *  @author Claudio Rosati
 */
@SuppressWarnings("nls")
public class EditorDemoGUI
{
    private volatile File file = null;

    private final JFXRepresentation toolkit;

    private DisplayEditor editor;

    private WidgetTree tree;

    private PropertyPanel property_panel;

    // Need mouse location to 'paste' widget,
    // but key handler does not receive it.
    // So track mouse in separate mouse listener

    /** Last known mouse location */
    private int mouse_x, mouse_y;

    /** Track current mouse location inside editor */
    private final EventHandler<MouseEvent> mouse_tracker = event ->
    {
        mouse_x = (int) event.getX();
        mouse_y = (int) event.getY();
    };

    final EventHandler<KeyEvent> key_handler = event ->
    {
        final KeyCode code = event.getCode();
        // Use Ctrl-C .. except on Mac, where it's Command-C ..
        final boolean meta = event.isShortcutDown();
        if (meta  &&  code == KeyCode.Z)
            editor.getUndoableActionManager().undoLast();
        else if (meta  &&  code == KeyCode.Y)
            editor.getUndoableActionManager().redoLast();
        else if (meta  &&  code == KeyCode.X)
            editor.cutToClipboard();
        else if (meta  &&  code == KeyCode.C)
            editor.copyToClipboard();
        else if (meta  &&  code == KeyCode.V)
        {   // Is mouse inside editor?
            if (! editor.getContextMenuNode()
                        .getLayoutBounds()
                        .contains(mouse_x, mouse_y))
            {   // Pasting somewhere in upper left corner
                final Random random = new Random();
                mouse_x = random.nextInt(100);
                mouse_y = random.nextInt(100);
            }
            editor.pasteFromClipboard(mouse_x, mouse_y);
        }
        else // Pass on, don't consume
            return;
        event.consume();
    };

    public EditorDemoGUI(final Stage stage)
    {
        toolkit = new JFXRepresentation(true);
        createElements(stage);
    }

    private void createElements(final Stage stage)
    {
        editor = new DisplayEditor(toolkit, 50);

        tree = new WidgetTree(editor);

        property_panel = new PropertyPanel(editor);

        // Left: Widget tree
        Label header = new Label("Widgets");
        header.setMaxWidth(Double.MAX_VALUE);
        header.getStyleClass().add("header");

        final Control tree_control = tree.create();
        VBox.setVgrow(tree_control, Priority.ALWAYS);
        final VBox tree_box = new VBox(header, tree_control);

        // Center: Editor
        final Node editor_scene = editor.create();
        extendToolbar(editor.getToolBar());

        // Right: Properties
        header = new Label("Properties");
        header.setMaxWidth(Double.MAX_VALUE);
        header.getStyleClass().add("header");
        final VBox properties_box = new VBox(header, property_panel);

        final SplitPane center_split = new SplitPane(tree_box, editor_scene, properties_box);
        center_split.setDividerPositions(0.2, 0.8);

        final BorderPane layout = new BorderPane();
        layout.setCenter(center_split);
        BorderPane.setAlignment(center_split, Pos.TOP_LEFT);

        editor_scene.addEventFilter(MouseEvent.MOUSE_MOVED, mouse_tracker);

        layout.addEventFilter(KeyEvent.KEY_PRESSED, key_handler);

        stage.setTitle("Editor");
        stage.setWidth(1200);
        stage.setHeight(600);
        final Scene scene = new Scene(layout, 1200, 600);
        stage.setScene(scene);
        EditorUtil.setSceneStyle(scene);

        // If ScenicView.jar is added to classpath, open it here
        //ScenicView.show(scene);

        stage.show();
    }

    private void extendToolbar(final ToolBar toolbar)
    {
        final Button debug = new Button("Debug");
        debug.setOnAction(event -> editor.debug());

        toolbar.getItems().add(0, createButton(new LoadModelAction(this)));
        toolbar.getItems().add(1, createButton(new SaveModelAction(this)));
        toolbar.getItems().add(2, new Separator());
        toolbar.getItems().add(new Separator());
        toolbar.getItems().add(debug);
    }

    private Button createButton(final ActionDescription action)
    {
        final Button button = new Button();
        try
        {
            button.setGraphic(new ImageView(new Image(ResourceUtil.openPlatformResource(action.getIconResourcePath()))));
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Cannot load action icon", ex);
        }
        button.setTooltip(new Tooltip(action.getToolTip()));
        button.setOnAction(event -> action.run(editor));
        return button;
    }

    /** @return Currently edited file */
    public File getFile()
    {
        return file;
    }

    /** Load model from file
     *  @param file File that contains the model
     */
    public void loadModel(final File file)
    {
        EditorUtil.getExecutor().execute(() ->
        {
            try
            {
                final DisplayModel model = ModelLoader.loadModel(new FileInputStream(file), file.getCanonicalPath());
                setModel(model);
                this.file = file;
            }
            catch (final Exception ex)
            {
                logger.log(Level.SEVERE, "Cannot start", ex);
            }
        });
    }

    /** Save model to file
     *  @param file File into which to save the model
     */
    public void saveModelAs(final File file)
    {
        EditorUtil.getExecutor().execute(() ->
        {
            logger.log(Level.FINE, "Save as {0}", file);
            try
            (
                final ModelWriter writer = new ModelWriter(new FileOutputStream(file));
            )
            {
                writer.writeModel(editor.getModel());
                this.file = file;
            }
            catch (Exception ex)
            {
                logger.log(Level.SEVERE, "Cannot save as " + file, ex);
            }
        });
    }

    private void setModel(final DisplayModel model)
    {
        // Representation needs to be created in UI thread
        toolkit.execute(() ->
        {
            editor.setModel(model);
            tree.setModel(model);
        });
    }

    public void dispose()
    {
        editor.dispose();
    }
}
