/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javax.xml.stream.XMLStreamWriter;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.persist.XMLUtil;
import org.csstudio.display.builder.model.properties.RuleInfo.ExprInfoString;
import org.csstudio.display.builder.model.properties.RuleInfo.ExprInfoValue;
import org.csstudio.display.builder.model.properties.RuleInfo.ExpressionInfo;
import org.w3c.dom.Element;

/** Widget property that describes rules
 *
 *  @author Megan Grodowitz
 */
@SuppressWarnings("nls")
public class RulesWidgetProperty extends WidgetProperty<List<RuleInfo>>
{
    private static final WidgetPropertyDescriptor<String> miscUnknownPropID =
            new WidgetPropertyDescriptor<String>(WidgetPropertyCategory.MISC,
                    "rule_unknown_propid", "RulesWidgetProperty:miscUnknownPropID", false)
    {
        @Override
        public WidgetProperty<String> createProperty(final Widget widget, final String value)
        {
            return new StringWidgetProperty(this, widget, value);
        }
    };


    public static WidgetProperty<?> propIDToNewProp(final Widget widget,
            final String prop_id, final String dbg_tag)
    {
        try
        {
            return widget.getProperty(prop_id).clone();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, widget + " cannot make new unknown property id " + prop_id);

            if ((dbg_tag != null) && (dbg_tag.length() > 0))
                return miscUnknownPropID.createProperty(null, dbg_tag);
            else
                return miscUnknownPropID.createProperty(null, prop_id + "?");
        }
    }

    /** Constructor
     *  @param descriptor Property descriptor
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     */
    public RulesWidgetProperty(
            final WidgetPropertyDescriptor<List<RuleInfo>> descriptor,
            final Widget widget,
            final List<RuleInfo> default_value)
    {
        super(descriptor, widget, default_value);
    }

    /** @param value Must be RuleInfo array(!), or empty List */
    @Override
    public void setValueFromObject(final Object value) throws Exception
    {
        if (value instanceof RuleInfo[])
            setValue(Arrays.asList((RuleInfo[]) value));
        else if ((value instanceof Collection) &&
                ((Collection<?>)value).isEmpty())
           setValue(Collections.emptyList());
        else
            throw new Exception("Need RuleInfo[], got " + value);
    }


    @Override
    public void writeToXML(final ModelWriter model_writer, final XMLStreamWriter writer) throws Exception
    {
        logger.log(Level.FINE, "Write " + value.size() + " rules to XML");
        for (final RuleInfo info : value)
        {
            // <rule name="name" prop_id="prop" out_exp="true">
            writer.writeStartElement(XMLTags.RULE);
            writer.writeAttribute(XMLTags.NAME, info.getName());
            writer.writeAttribute("prop_id", info.getPropID());
            writer.writeAttribute("out_exp", String.valueOf(info.getPropAsExprFlag()));

            for ( final ExpressionInfo<?> expr : info.getExpressions())
            {
                // <exp bool_exp="foo==1">
                writer.writeStartElement("exp");
                writer.writeAttribute("bool_exp", expr.getBoolExp());

                if (info.getPropAsExprFlag())
                {
                    // <expression>
                    writer.writeStartElement("expression");
                    if (!(expr.getPropVal() instanceof String))
                    {
                        logger.log(Level.SEVERE, "Mismatch of rules output expression flag with expression value type, expected String, got ", expr.getPropVal().getClass());
                        writer.writeCharacters("ERROR");
                    }
                    // some string of the value or expression
                    writer.writeCharacters((String)expr.getPropVal());
                }
                else
                {
                    // <value>
                    writer.writeStartElement("value");
                    if (!(expr.getPropVal() instanceof WidgetProperty<?>))
                    {
                        logger.log(Level.SEVERE, "Mismatch of rules output expression flag with expression value type, expected Widget Property, got ", expr.getPropVal().getClass());
                        writer.writeCharacters("ERROR");
                    }
                    // write the property
                    ((WidgetProperty<?>)expr.getPropVal()).writeToXML(model_writer, writer);
                }
                // </value> or </expression>
                writer.writeEndElement();
                // </exp>
                writer.writeEndElement();
            }
            for (final ScriptPV pv : info.getPVs())
            {
                //<pv trig="true">
                writer.writeStartElement(XMLTags.PV_NAME);
                if (! pv.isTrigger())
                    writer.writeAttribute(XMLTags.TRIGGER, Boolean.FALSE.toString());
                //some string of the pv name
                writer.writeCharacters(pv.getName());
                //</pv>
                writer.writeEndElement();
            }
            //</rule>
            writer.writeEndElement();
        }
    }

    @Override
    public void readFromXML(final ModelReader model_reader, final Element property_xml) throws Exception
    {
        final Iterable<Element> rule_xml = XMLUtil.getChildElements(property_xml, XMLTags.RULE);

        final List<RuleInfo> rules = new ArrayList<>();
        for (final Element xml : rule_xml)
        {
            String name, prop_id, out_exp_str;

            try
            {
                name = xml.getAttribute(XMLTags.NAME);
                if (name.isEmpty())
                    logger.log(Level.WARNING, "Missing rule 'name'");
            }
            catch (Exception ex)
            {
                name = "unknown";
                logger.log(Level.WARNING, "Failed to find rule name");
            }


            try
            {
                prop_id = xml.getAttribute("prop_id");
                if (prop_id.isEmpty())
                    logger.log(Level.WARNING, "Missing rule 'prop_id'");
            }
            catch (Exception e)
            {
                prop_id = "unknown";
                logger.log(Level.WARNING, "Failed to find rule prop_id");
            }


            boolean prop_as_expr = false;
            try
            {
                out_exp_str = xml.getAttribute("out_exp");
                prop_as_expr = false;
                if (out_exp_str.isEmpty())
                    logger.log(Level.WARNING, "Missing rule 'out_exp'");
                else
                    prop_as_expr = Boolean.parseBoolean(out_exp_str);
            }
            catch (Exception e)
            {
                logger.log(Level.WARNING, "Failed to find rule out_exp");
            }

            List<ExpressionInfo<?>> exprs;
            try
            {
                exprs = readExpressions(model_reader, prop_id, prop_as_expr, xml);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Failure to readExpressions for " + prop_id);
                exprs = new ArrayList<>();
                ex.printStackTrace();
            }

            final List<ScriptPV> pvs = readPVs(xml);
            rules.add(new RuleInfo(name, prop_id, prop_as_expr, exprs, pvs));
        }
        setValue(rules);
    }



    private List<ExpressionInfo<?>> readExpressions(final ModelReader model_reader,
            final String prop_id,
            final boolean prop_as_expr,
            final Element xml) throws Exception
    {
        final List<ExpressionInfo<?>> exprs = new ArrayList<>();
        final Iterable<Element> exprs_xml = XMLUtil.getChildElements(xml, "exp");
        final String tagstr = (prop_as_expr) ? "expression" : "value";

        for (final Element exp_xml : exprs_xml)
        {
            String bool_exp = exp_xml.getAttribute("bool_exp");
            if (bool_exp.isEmpty())
                logger.log(Level.WARNING, "Missing exp 'bool_exp'");

            final Element tag_xml = XMLUtil.getChildElement(exp_xml, tagstr);
            //legacy case where value is used for all value expression
            final Element val_xml = (tag_xml == null) ? XMLUtil.getChildElement(exp_xml, "value") : tag_xml;

            if (prop_as_expr)
            {
                final String val_str = (val_xml != null) ? XMLUtil.getString(val_xml) : "";
                exprs.add(new ExprInfoString(bool_exp, val_str));
            }
            else
            {
                final String val_str = (val_xml != null) ? XMLUtil.elementsToString(val_xml.getChildNodes(), false) : "";

                WidgetProperty<?> val_prop = propIDToNewProp(
                        this.getWidget(), prop_id, val_str);

                if ( val_prop.getName() != miscUnknownPropID.getName() )
                {
                    val_prop.readFromXML(model_reader, val_xml);
                }
                exprs.add(new ExprInfoValue<>(bool_exp, val_prop));
            }
        }
        return exprs;
    }

    private List<ScriptPV> readPVs(final Element xml)
    {
        final List<ScriptPV> pvs = new ArrayList<>();
        // Legacy used just 'pv'
        final Iterable<Element> pvs_xml;
        if (XMLUtil.getChildElement(xml, XMLTags.PV_NAME) != null)
            pvs_xml = XMLUtil.getChildElements(xml, XMLTags.PV_NAME);
        else
            pvs_xml = XMLUtil.getChildElements(xml, "pv");
        for (final Element pv_xml : pvs_xml)
        {   // Unless either the new or old attribute is _present_ and set to false,
            // default to triggering on this PV
            final boolean trigger =
                    XMLUtil.parseBoolean(pv_xml.getAttribute(XMLTags.TRIGGER), true) &&
                    XMLUtil.parseBoolean(pv_xml.getAttribute("trig"), true);
            final String name = XMLUtil.getString(pv_xml);
            pvs.add(new ScriptPV(name, trigger));
        }
        return pvs;
    }
}
