/*
 * Copyright 2011-2013 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2011-2013 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package com.hwlcn.ldap.ldap.sdk;



import java.util.TimerTask;

import com.hwlcn.ldap.ldap.protocol.LDAPResponse;
import com.hwlcn.ldap.util.Debug;
import com.hwlcn.ldap.util.StaticUtils;

import static com.hwlcn.ldap.ldap.sdk.LDAPMessages.*;



/**
 * This class provides a timer task that can be used to ensure that operation
 * timeouts for asynchronous operations are properly respected.
 */
final class AsyncTimeoutTimerTask
      extends TimerTask
{
  // The async helper with which this task is associated.
  private final CommonAsyncHelper helper;



  /**
   * Creates a new timer task that will provide a timeout response for an
   * asynchronous operation if no other response has been received first.
   *
   * @param  helper  The async helper with which this task is associated.
   */
  AsyncTimeoutTimerTask(final CommonAsyncHelper helper)
  {
    this.helper = helper;
  }



  /**
   * Generates a timeout response for the associated operation.
   */
  @Override()
  public void run()
  {
    final long waitTimeNanos = System.nanoTime() - helper.getCreateTimeNanos();
    final long waitTimeMillis = waitTimeNanos / 1000000L;

    final LDAPConnection conn = helper.getConnection();
    final boolean abandon = conn.getConnectionOptions().abandonOnTimeout();

    final String message;
    if (abandon)
    {
      message = INFO_ASYNC_OPERATION_TIMEOUT_WITH_ABANDON.get(waitTimeMillis);
    }
    else
    {
      message =
           INFO_ASYNC_OPERATION_TIMEOUT_WITHOUT_ABANDON.get(waitTimeMillis);
    }

    final LDAPResponse response;
    switch (helper.getOperationType())
    {
      case ADD:
      case DELETE:
      case MODIFY:
      case MODIFY_DN:
        response = new LDAPResult(helper.getAsyncRequestID().getMessageID(),
             ResultCode.TIMEOUT, message, null, StaticUtils.NO_STRINGS,
             StaticUtils.NO_CONTROLS);
        break;
      case COMPARE:
        response = new CompareResult(helper.getAsyncRequestID().getMessageID(),
             ResultCode.TIMEOUT, message, null, StaticUtils.NO_STRINGS,
             StaticUtils.NO_CONTROLS);
        break;
      case SEARCH:
        final AsyncSearchHelper searchHelper = (AsyncSearchHelper) helper;
        response = new SearchResult(helper.getAsyncRequestID().getMessageID(),
             ResultCode.TIMEOUT, message, null, StaticUtils.NO_STRINGS,
             searchHelper.getNumEntries(), searchHelper.getNumReferences(),
             StaticUtils.NO_CONTROLS);
        break;
      default:
        // This should never happen.
        return;
    }

    try
    {
      helper.responseReceived(response);
      if (abandon)
      {
        conn.abandon(helper.getAsyncRequestID());
      }
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
    }
  }
}
