/*
 * Copyright 2009-2013 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2009-2013 UnboundID Corp.
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
package com.hwlcn.ldap.asn1;



import com.hwlcn.ldap.util.Mutable;
import com.hwlcn.ldap.util.ThreadSafety;
import com.hwlcn.ldap.util.ThreadSafetyLevel;

import static com.hwlcn.ldap.asn1.ASN1Messages.*;



/**
 * This class provides a data structure which is used in the course of reading
 * an ASN.1 sequence from an ASN.1 stream reader.  It provides access to the
 * BER type and the total number of bytes in the encoded representations of all
 * of the embedded values, and provides a method to determine when the end of
 * the sequence has been reached.
 */
@Mutable()
@ThreadSafety(level=ThreadSafetyLevel.NOT_THREADSAFE)
public final class ASN1StreamReaderSequence
{
  // The ASN.1 stream reader associated with this object.
  private final ASN1StreamReader reader;

  // The BER type for this ASN.1 sequence.
  private final byte type;

  // The number of bytes contained in the encoded representations of all of the
  // embedded values.
  private final int length;

  // The value for the total number of bytes read from the associated reader at
  // which the end of the sequence should be encountered.
  private final long endBytesRead;



  /**
   * Creates a new instance of this class with the provided information.
   *
   * @param  reader  The ASN.1 stream reader with which this object will be
   *                 associated.
   * @param  type    The BER type for this ASN.1 sequence.
   * @param  length  The number of bytes contained in the encoded
   *                 representations of all the embedded values.
   */
  ASN1StreamReaderSequence(final ASN1StreamReader reader, final byte type,
                           final int length)
  {
    this.reader = reader;
    this.type   = type;
    this.length = length;

    endBytesRead = reader.getTotalBytesRead() + length;
  }



  /**
   * Retrieves the BER type for this ASN.1 sequence.
   *
   * @return  The BER type for this ASN.1 sequence.
   */
  public byte getType()
  {
    return type;
  }



  /**
   * Retrieves the number of bytes contained in the encoded representations of
   * all the embedded values.
   *
   * @return  The number of bytes contained in the encoded representations of
   *          all the embedded values.
   */
  public int getLength()
  {
    return length;
  }



  /**
   * Indicates whether there are more elements in this sequence to be read from
   * the associated ASN.1 stream reader.
   *
   * @return  {@code true} if there are more elements in this sequence to be
   *          read from the associated ASN.1 stream reader or false if the end
   *          of the sequence has been reached.
   *
   * @throws  com.hwlcn.ldap.asn1.ASN1Exception  If the associated ASN.1 stream reader has already
   *                         read beyond the end of the sequence.
   */
  public boolean hasMoreElements()
         throws ASN1Exception
  {
    final long currentBytesRead = reader.getTotalBytesRead();
    if (currentBytesRead == endBytesRead)
    {
      return false;
    }
    else if (currentBytesRead < endBytesRead)
    {
      return true;
    }

    throw new ASN1Exception(ERR_STREAM_READER_SEQUENCE_READ_PAST_END.get(
         length, endBytesRead, currentBytesRead));
  }
}
