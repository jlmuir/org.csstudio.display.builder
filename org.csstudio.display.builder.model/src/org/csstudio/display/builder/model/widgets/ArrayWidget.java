/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.behaviorPVName;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.displayBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimeValue;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.widgetMacros;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.macros.Macros;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.diirt.vtype.VType;

/**
 * An Array Widget contains copies of a child widget. Each copy is assigned the
 * value of one element of a PV.
 *
 * @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class ArrayWidget extends VisibleWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("array", WidgetCategory.STRUCTURE,
            Messages.ArrayWidget_Name,
            "platform:/plugin/org.csstudio.display.builder.model/icons/array.png",
            Messages.ArrayWidget_Description,
            Arrays.asList("org.csstudio.opibuilder.widgets.array"))
    {
        @Override
        public Widget createWidget()
        {
            return new ArrayWidget();
        }
    };

    private volatile WidgetProperty<Macros> macros;
    private volatile WidgetProperty<String> pv_name;
    private volatile ChildrenProperty children;
    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<VType> value;

    public ArrayWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 400, 300);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(macros = widgetMacros.createProperty(this, new Macros()));
        properties.add(pv_name = behaviorPVName.createProperty(this, ""));
        properties.add(children = new ChildrenProperty(this));
        properties.add(background = displayBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BACKGROUND)));
        properties.add(value = runtimeValue.createProperty(this, null));
    }
    
    /** @return Widget 'macros' */
    public WidgetProperty<Macros> widgetMacros()
    {
        return macros;
    }

    /** @return Behavior 'pv_name' */
    public WidgetProperty<String> behaviorPVName()
    {
        return pv_name;
    }

    /** @return Runtime 'children' */
    public ChildrenProperty runtimeChildren()
    {
        return children;
    }

    /** @return Display 'background_color' */
    public WidgetProperty<WidgetColor> displayBackgroundColor()
    {
        return background;
    }

    /** @return Runtime 'value' */
    public WidgetProperty<VType> runtimeValue()
    {
        return value;
    }

    /** Array widget extends parent macros
     *  @return {@link Macros}
     */
    @Override
    public Macros getEffectiveMacros()
    {
        final Macros base = super.getEffectiveMacros();
        final Macros my_macros = widgetMacros().getValue();
        return Macros.merge(base, my_macros);
    }

}
