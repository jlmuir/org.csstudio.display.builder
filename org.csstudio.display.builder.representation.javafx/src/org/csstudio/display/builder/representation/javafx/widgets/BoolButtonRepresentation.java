/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.util.Optional;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.BoolButtonWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.diirt.vtype.VType;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.MouseEvent;

/** Creates JavaFX item for model widget
 *  @author Megan Grodowitz
 */
@SuppressWarnings("nls")
public class BoolButtonRepresentation extends RegionBaseRepresentation<ButtonBase, BoolButtonWidget>
{

    private final DirtyFlag dirty_representation = new DirtyFlag();
    private final DirtyFlag dirty_content = new DirtyFlag();
    protected volatile Boolean on_state = true;
    protected volatile int use_bit = 0;
    protected volatile Integer rt_value = 0;

    /** Optional modifier of the open display 'target */
    private Optional<OpenDisplayActionInfo.Target> target_modifier = Optional.empty();

    @Override
    public ButtonBase createJFXNode() throws Exception
    {
        //final List<ActionInfo> actions = model_widget.behaviorActions().getValue();
        final ButtonBase base;
        final Button button = new Button();
        //final ActionInfo the_action = actions.get(0);
        //button.setOnAction(event -> handleAction(the_action));
        button.setOnAction(event -> handlePress());
        base = button;

        // Model has width/height, but JFX widget has min, pref, max size.
        // updateChanges() will set the 'pref' size, so make min use that as well.
        base.setMinSize(ButtonBase.USE_PREF_SIZE, ButtonBase.USE_PREF_SIZE);

        // Monitor keys that modify the OpenDisplayActionInfo.Target.
        // Use filter to capture event that's otherwise already handled.
        base.addEventFilter(MouseEvent.MOUSE_PRESSED, this::checkModifiers);
        return base;
    }

    /** @param event Mouse event to check for target modifier keys */
    private void checkModifiers(final MouseEvent event)
    {
        if (event.isControlDown())
            target_modifier = Optional.of(OpenDisplayActionInfo.Target.TAB);
        else if (event.isShiftDown())
            target_modifier = Optional.of(OpenDisplayActionInfo.Target.WINDOW);
        else
            target_modifier = Optional.empty();

        // At least on Linux, a Control-click or Shift-click
        // will not 'arm' the button, so the click is basically ignored.
        // Force the 'arm', so user can Control-click or Shift-click to
        // invoke the button
        if (target_modifier.isPresent())
        {
            logger.log(Level.FINE, "{0} modifier: {1}", new Object[] { model_widget, target_modifier.get() });
            jfx_node.arm();
        }
    }

    /** @param respond to button press */
    private void handlePress()
    {
        logger.log(Level.FINE, "{0} pressed", model_widget);
        int new_val = (rt_value ^ ((use_bit < 0) ? 1 : (1 << use_bit)) );
        toolkit.fireWrite(model_widget, new_val);
    }

    /** @param action Action that the user invoked */
    private void handleAction(ActionInfo action)
    {
        logger.log(Level.FINE, "{0} pressed", model_widget);
        if (action instanceof OpenDisplayActionInfo  &&  target_modifier.isPresent())
        {
            final OpenDisplayActionInfo orig = (OpenDisplayActionInfo) action;
            action = new OpenDisplayActionInfo(orig.getDescription(), orig.getFile(), orig.getMacros(), target_modifier.get());
        }
        toolkit.fireAction(model_widget, action);
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.positionWidth().addUntypedPropertyListener(this::representationChanged);
        model_widget.positionHeight().addUntypedPropertyListener(this::representationChanged);
        model_widget.displayText().addUntypedPropertyListener(this::representationChanged);
        model_widget.displayFont().addUntypedPropertyListener(this::representationChanged);

        bitChanged(model_widget.behaviorBit(), null, model_widget.behaviorBit().getValue());
        model_widget.behaviorBit().addPropertyListener(this::bitChanged);
        model_widget.runtimeValue().addPropertyListener(this::contentChanged);

        //representationChanged(null,null,null);
    }

    private void stateChanged()
    {
        on_state = (use_bit < 0) ? (rt_value != 0) : (((rt_value >> use_bit) & 1) == 1);

        dirty_content.mark();
        toolkit.scheduleUpdate(this);
    }

    private void bitChanged(final WidgetProperty<Integer> property, final Integer old_value, final Integer new_value)
    {
        use_bit = new_value;

        stateChanged();
    }

    private void contentChanged(final WidgetProperty<VType> property, final VType old_value, final VType new_value)
    {
        rt_value = VTypeUtil.getValueNumber(new_value).intValue();
        stateChanged();
    }



    private void representationChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_representation.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_content.checkAndClear())
        {
            model_widget.displayText().setValue(on_state.toString());
            jfx_node.setText(model_widget.displayText().getValue());
        }
        if (dirty_representation.checkAndClear())
        {
            jfx_node.setText(model_widget.displayText().getValue());
            jfx_node.setPrefSize(model_widget.positionWidth().getValue(),
                                 model_widget.positionHeight().getValue());
            jfx_node.setFont(JFXUtil.convert(model_widget.displayFont().getValue()));
        }
    }
}
