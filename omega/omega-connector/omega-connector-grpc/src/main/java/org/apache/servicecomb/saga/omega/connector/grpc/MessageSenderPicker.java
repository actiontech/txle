/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.connector.grpc;

import com.google.common.base.Supplier;
import java.util.Collection;
import java.util.Map;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;

/**
 * The strategy of picking a specific {@link MessageSender} from a {@link Collection} of {@link
 * MessageSender}s
 */
public interface MessageSenderPicker {

  /**
   * Pick one from the Collection. Return default sender if none is picked.
   *
   * @param messageSenders Candidates map, the Key Set of which is the collection of candidate
   * senders.
   * @param defaultSender Default sender provider
   * @return The specified one.
   */
  MessageSender pick(Map<MessageSender, Long> messageSenders,
      Supplier<MessageSender> defaultSender);
}
