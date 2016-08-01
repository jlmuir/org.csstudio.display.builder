/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.macros.MacroValueProvider;
import org.csstudio.display.builder.model.properties.RuleInfo.ExpressionInfo;

/** Transform rules into scripts
 *
 *
 *  <p>Rules produce scripts attached to widgets
 *  rules execute in response to changes in triggering
 *  PVs
 *
 *  @author Megan Grodowitz
 */
@SuppressWarnings("nls")
public class RuleToScript
{
    public static Map<String,String> pvNameOptions(String istr)
    {
        Map<String,String> pvm = new HashMap<String,String>();

        pvm.put("pv" + istr, "PVUtil.getDouble(pvs["+istr+"])"  );
        pvm.put("pvReal" + istr, "PVUtil.getDouble(pvs["+istr+"])"  );
        pvm.put("pvInt" + istr, "PVUtil.getLong(pvs["+istr+"])"  );
        pvm.put("pvLong" + istr, "PVUtil.getLong(pvs["+istr+"])"  );
        pvm.put("pvStr" + istr, "PVUtil.getString(pvs["+istr+"])"  );
        //pvm.put("pvLabels" + istr, "PVUtil.getLabels(pvs["+istr+"])"  );

        return pvm;
    }

    public static Map<String,String> pvNameOptions(int pvCount)
    {
        Map<String,String> pvm = new HashMap<String,String>();

        for (int idx = 0; idx < pvCount; idx++)
        {
            String istr = String.valueOf(idx);
            pvm.putAll(pvNameOptions(istr));
        }

        return pvm;
    }

    private enum PropFormat {
        NUMERIC, BOOLEAN, STRING, COLOR
    }

    private static String formatPropVal(WidgetProperty<?> prop, int exprIDX, PropFormat pform)
    {
        String ret = null;

        switch(pform)
        {
        case NUMERIC:
            ret = String.valueOf(prop.getValue());
            break;
        case BOOLEAN:
            ret = (Boolean) prop.getValue() ? "True" : "False";
            break;
        case STRING:
            ret = "\"" + prop.getValue() + "\"";
            break;
        case COLOR:
            if (exprIDX >= 0) {
                ret = "colorVal" + String.valueOf(exprIDX);
            }
            else {
                ret = "colorCurrent";
            }
            break;
        }

        return ret;
    }


    private static int countMatches(String s, char c)
    {
        int counter = 0;
        for( int i=0; i<s.length(); i++ ) {
            if( s.charAt(i) == c ) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * Substitute the string "True" for all instances of string "true" to update old javascript rules into Python
     */
    protected static String TrueFortrue(final String instr)
    {
        //return instr.replaceAll("(\\W|^)(true)", "$1True");
        Matcher m = Pattern.compile("(.*?)((true)+)").matcher(instr);
        StringBuffer sb = new StringBuffer();

        boolean inquotes=false;
        while(m.find()) {
            if ((countMatches(m.group(1), '\"') % 2) == 1)
                inquotes = !inquotes;
            if (inquotes)
                m.appendReplacement(sb, m.group(1) + m.group(2));
            else if (m.group(1).matches(".*\\w"))
                m.appendReplacement(sb, m.group(1) + m.group(2));
            else if (m.group(2).matches("true(true)+"))
                m.appendReplacement(sb, m.group(1) + m.group(2));
            else
                m.appendReplacement(sb, m.group(1) + "True");
        }
        m.appendTail(sb);

        return sb.toString();
    }

    /**
     * Substitute Python logical operators 'and', 'or', and 'not' for old
     * javascript operators '&&', '||', '!'
     */
    protected static String replaceLogicalOperators(final String instr)
    {
        //matches '&&', '||', and '!', but not '!='
        Matcher m = Pattern.compile("((.*?) ?)(\\&\\&|\\|\\||!(?!=)) ?").matcher(instr);
        Pattern qp = Pattern.compile("(?<!\\\\)\\\""); //matches `"` but not `\"`
        StringBuffer sb = new StringBuffer();

        boolean inquotes = false;
        while (m.find())
        {
            final Matcher qm = qp.matcher(m.group(2));
            int quotes = 0;
            while (qm.find())
                quotes++;
            if ((quotes & 1) != 0)
                inquotes = !inquotes;
            if (!inquotes)
            {
                String operator = m.group(3);
                if (operator.equals("&&"))
                    operator = "and";
                else if (operator.equals("||"))
                    operator = "or";
                else if (operator.equals("!"))
                    operator = "not";
                //quoteReplacement for group(2) to preserve escaping '\'
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(2)) + ' ' + operator + ' ');
            }
            else
                m.appendReplacement(sb, m.group(1) + m.group(3));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static String generatePy(final Widget attached_widget, final MacroValueProvider macros, final RuleInfo rule)
    {
        WidgetProperty<?> prop = attached_widget.getProperty(rule.getPropID());

        //TODO: Replace macros
        //example of replacing macros:
        //final String script_name = MacroHandler.replace(macros, script_info.getPath());

        PropFormat pform = PropFormat.STRING;

        if (prop.getDefaultValue() instanceof Number)
        {
            pform = PropFormat.NUMERIC;
        }
        else if (prop.getDefaultValue() instanceof Boolean)
        {
            pform = PropFormat.BOOLEAN;
        }
        else if (prop.getDefaultValue() instanceof WidgetColor)
        {
            pform = PropFormat.COLOR;
        }

        String script_str = "## Script for Rule: " + rule.getName() + "\n\n";

        script_str += "from org.csstudio.display.builder.runtime.script import PVUtil\n";
        if (pform == PropFormat.COLOR)
        {
            script_str += "from org.csstudio.display.builder.model.properties import WidgetColor\n";
        }

        script_str += "\n## Process variable extraction\n";

        Map<String,String> pvm = pvNameOptions(rule.getPVs().size());
        Map<String,String> output_pvm = new HashMap<String,String>();
        for (ExpressionInfo<?> expr : rule.getExpressions())
        {
            String[] toks = expr.getBoolExp().split("\\s");
            for (String tok : toks)
            {
                for (Map.Entry<String, String> entry : pvm.entrySet())
                {
                    final String varname = entry.getKey();
                    if (tok.contains(varname)) {
                        output_pvm.put(varname, entry.getValue());
                    }
                }
            }
        }
        for (Map.Entry<String, String> entry : output_pvm.entrySet())
        {
            script_str += entry.getKey() + " = " + entry.getValue() + "\n";
        }

        if (pform == PropFormat.COLOR)
        {
            script_str += "\n## Define Colors\n";
            WidgetColor col = (WidgetColor) prop.getValue();
            script_str += "colorCurrent = "
                    + "WidgetColor(" + String.valueOf(col.getRed())
                    + ", " + String.valueOf(col.getGreen())
                    + ", " + String.valueOf(col.getBlue()) + ")\n";

            if (!rule.getPropAsExprFlag())
            {
                int idx = 0;
                for (ExpressionInfo<?> expr : rule.getExpressions())
                {
                    col = ((WidgetProperty<WidgetColor>) expr.getPropVal()).getValue();
                    script_str += "colorVal" + String.valueOf(idx) + " = "
                            + "WidgetColor(" + String.valueOf(col.getRed())
                            + ", " + String.valueOf(col.getGreen())
                            + ", " + String.valueOf(col.getBlue()) + ")\n";
                    idx++;
                }
            }
        }

        script_str += "\n## Script Body\n";
        String indent = "    ";

        String setPropStr = "widget.setPropertyValue( \"" + rule.getPropID() + "\", ";
        int idx = 0;
        for (ExpressionInfo<?> expr : rule.getExpressions())
        {
            script_str += (idx == 0) ? "if" : "elif";
            script_str += " (" + replaceLogicalOperators(TrueFortrue(expr.getBoolExp())) + "):\n";
            script_str += indent + setPropStr;
            if (rule.getPropAsExprFlag())
            {
                script_str += replaceLogicalOperators(TrueFortrue(expr.getPropVal() + " )\n"));
            }
            else
            {
                script_str += formatPropVal((WidgetProperty<?>) expr.getPropVal(), idx, pform) + " )\n";
            }
            idx++;
        }

        if (idx > 0)
        {
            script_str += "else:\n";
            script_str += indent + setPropStr + formatPropVal(prop, -1, pform) + " )\n";
        }
        else {
            script_str += setPropStr + formatPropVal(prop, -1, pform) + " )\n";
        }

        return script_str;
    }
}
