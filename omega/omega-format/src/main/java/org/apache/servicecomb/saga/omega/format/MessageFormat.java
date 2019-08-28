/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.format;

import org.apache.servicecomb.saga.omega.transaction.MessageDeserializer;
import org.apache.servicecomb.saga.omega.transaction.MessageSerializer;

public interface MessageFormat extends MessageSerializer, MessageDeserializer {
}
