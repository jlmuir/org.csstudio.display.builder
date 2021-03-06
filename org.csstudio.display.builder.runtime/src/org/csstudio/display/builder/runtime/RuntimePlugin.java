/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime;

import java.util.logging.Logger;

/** Plugin information.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RuntimePlugin
{
    /** Plugin ID */
    public final static String ID = "org.csstudio.display.builder.runtime";

    /** Suggested logger for all runtime logging */
    public final static Logger logger = Logger.getLogger(RuntimePlugin.class.getName());
}
