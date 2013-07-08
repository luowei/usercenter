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
package com.hwlcn.ldap.ldap.protocol;



import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.hwlcn.ldap.asn1.ASN1Buffer;
import com.hwlcn.ldap.asn1.ASN1BufferSequence;
import com.hwlcn.ldap.asn1.ASN1Element;
import com.hwlcn.ldap.asn1.ASN1Enumerated;
import com.hwlcn.ldap.asn1.ASN1OctetString;
import com.hwlcn.ldap.asn1.ASN1Sequence;
import com.hwlcn.ldap.asn1.ASN1StreamReader;
import com.hwlcn.ldap.asn1.ASN1StreamReaderSequence;
import com.hwlcn.ldap.ldap.sdk.Control;
import com.hwlcn.ldap.ldap.sdk.ExtendedResult;
import com.hwlcn.ldap.ldap.sdk.LDAPException;
import com.hwlcn.ldap.ldap.sdk.LDAPResult;
import com.hwlcn.ldap.ldap.sdk.ResultCode;
import com.hwlcn.ldap.util.NotMutable;
import com.hwlcn.ldap.util.InternalUseOnly;
import com.hwlcn.ldap.util.ThreadSafety;
import com.hwlcn.ldap.util.ThreadSafetyLevel;

import static com.hwlcn.ldap.ldap.protocol.ProtocolMessages.*;
import static com.hwlcn.ldap.util.Debug.*;
import static com.hwlcn.ldap.util.StaticUtils.*;
import static com.hwlcn.ldap.util.Validator.*;



/**
 * This class provides an implementation of a extended response protocol op.
 */
@InternalUseOnly()
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class ExtendedResponseProtocolOp
       implements ProtocolOp
{
  /**
   * The BER type for the response OID element.
   */
  public static final byte TYPE_RESPONSE_OID = (byte) 0x8A;



  /**
   * The BER type for the response value element.
   */
  public static final byte TYPE_RESPONSE_VALUE = (byte) 0x8B;



  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -7757619031268544913L;



  // The value for this extended response.
  private final ASN1OctetString responseValue;

  // The result code for this extended response.
  private final int resultCode;

  // The referral URLs for this extended response.
  private final List<String> referralURLs;

  // The diagnostic message for this extended response.
  private final String diagnosticMessage;

  // The matched DN for this extended response.
  private final String matchedDN;

  // The OID for this extended response.
  private final String responseOID;



  /**
   * Creates a new instance of this extended response protocol op with the
   * provided information.
   *
   * @param  resultCode         The result code for this response.
   * @param  matchedDN          The matched DN for this response, if available.
   * @param  diagnosticMessage  The diagnostic message for this response, if
   *                            any.
   * @param  referralURLs       The list of referral URLs for this response, if
   *                            any.
   * @param  responseOID        The response OID for this response, if any.
   * @param  responseValue      The value for this response, if any.
   */
  public ExtendedResponseProtocolOp(final int resultCode,
                                    final String matchedDN,
                                    final String diagnosticMessage,
                                    final List<String> referralURLs,
                                    final String responseOID,
                                    final ASN1OctetString responseValue)
  {
    this.resultCode        = resultCode;
    this.matchedDN         = matchedDN;
    this.diagnosticMessage = diagnosticMessage;
    this.responseOID       = responseOID;

    if (referralURLs == null)
    {
      this.referralURLs = Collections.emptyList();
    }
    else
    {
      this.referralURLs = Collections.unmodifiableList(referralURLs);
    }

    if (responseValue == null)
    {
      this.responseValue = null;
    }
    else
    {
      this.responseValue =
           new ASN1OctetString(TYPE_RESPONSE_VALUE, responseValue.getValue());
    }
  }



  /**
   * Creates a new extended response protocol op from the provided extended
   * result object.
   *
   * @param  result  The extended result object to use to create this protocol
   *                 op.
   */
  public ExtendedResponseProtocolOp(final LDAPResult result)
  {
    resultCode        = result.getResultCode().intValue();
    matchedDN         = result.getMatchedDN();
    diagnosticMessage = result.getDiagnosticMessage();
    referralURLs      = toList(result.getReferralURLs());

    if (result instanceof ExtendedResult)
    {
      final ExtendedResult r = (ExtendedResult) result;
      responseOID   = r.getOID();
      responseValue = r.getValue();
    }
    else
    {
      responseOID   = null;
      responseValue = null;
    }
  }



  /**
   * Creates a new extended response protocol op read from the provided ASN.1
   * stream reader.
   *
   * @param  reader  The ASN.1 stream reader from which to read the extended
   *                 response.
   *
   * @throws  com.hwlcn.ldap.ldap.sdk.LDAPException  If a problem occurs while reading or parsing the
   *                         extended response.
   */
  ExtendedResponseProtocolOp(final ASN1StreamReader reader)
       throws LDAPException
  {
    try
    {
      final ASN1StreamReaderSequence opSequence = reader.beginSequence();
      resultCode = reader.readEnumerated();

      String s = reader.readString();
      ensureNotNull(s);
      if (s.length() == 0)
      {
        matchedDN = null;
      }
      else
      {
        matchedDN = s;
      }

      s = reader.readString();
      ensureNotNull(s);
      if (s.length() == 0)
      {
        diagnosticMessage = null;
      }
      else
      {
        diagnosticMessage = s;
      }

      ASN1OctetString value = null;
      String oid = null;
      final ArrayList<String> refs = new ArrayList<String>(1);
      while (opSequence.hasMoreElements())
      {
        final byte type = (byte) reader.peek();
        if (type == GenericResponseProtocolOp.TYPE_REFERRALS)
        {
          final ASN1StreamReaderSequence refSequence = reader.beginSequence();
          while (refSequence.hasMoreElements())
          {
            refs.add(reader.readString());
          }
        }
        else if (type == TYPE_RESPONSE_OID)
        {
          oid = reader.readString();
        }
        else if (type == TYPE_RESPONSE_VALUE)
        {
          value = new ASN1OctetString(type, reader.readBytes());
        }
        else
        {
          throw new LDAPException(ResultCode.DECODING_ERROR,
               ERR_EXTENDED_RESPONSE_INVALID_ELEMENT.get(toHex(type)));
        }
      }

      referralURLs  = Collections.unmodifiableList(refs);
      responseOID   = oid;
      responseValue = value;
    }
    catch (LDAPException le)
    {
      debugException(le);
      throw le;
    }
    catch (Exception e)
    {
      debugException(e);
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_EXTENDED_RESPONSE_CANNOT_DECODE.get(getExceptionMessage(e)), e);
    }
  }



  /**
   * Retrieves the result code for this extended response.
   *
   * @return  The result code for this extended response.
   */
  public int getResultCode()
  {
    return resultCode;
  }



  /**
   * Retrieves the matched DN for this extended response, if any.
   *
   * @return  The matched DN for this extended response, or {@code null} if
   *          there is no matched DN.
   */
  public String getMatchedDN()
  {
    return matchedDN;
  }



  /**
   * Retrieves the diagnostic message for this extended response, if any.
   *
   * @return  The diagnostic message for this extended response, or {@code null}
   *          if there is no diagnostic message.
   */
  public String getDiagnosticMessage()
  {
    return diagnosticMessage;
  }



  /**
   * Retrieves the list of referral URLs for this extended response.
   *
   * @return  The list of referral URLs for this extended response, or an empty
   *          list if there are no referral URLs.
   */
  public List<String> getReferralURLs()
  {
    return referralURLs;
  }



  /**
   * Retrieves the OID for this extended response, if any.
   *
   * @return  The OID for this extended response, or {@code null} if there is no
   *          response OID.
   */
  public String getResponseOID()
  {
    return responseOID;
  }



  /**
   * Retrieves the value for this extended response, if any.
   *
   * @return  The value for this extended response, or {@code null} if there is
   *          no response value.
   */
  public ASN1OctetString getResponseValue()
  {
    return responseValue;
  }



  /**
   * {@inheritDoc}
   */
  public byte getProtocolOpType()
  {
    return LDAPMessage.PROTOCOL_OP_TYPE_EXTENDED_RESPONSE;
  }



  /**
   * {@inheritDoc}
   */
  public ASN1Element encodeProtocolOp()
  {
    final ArrayList<ASN1Element> elements = new ArrayList<ASN1Element>(6);
    elements.add(new ASN1Enumerated(getResultCode()));

    final String mdn = getMatchedDN();
    if (mdn == null)
    {
      elements.add(new ASN1OctetString());
    }
    else
    {
      elements.add(new ASN1OctetString(mdn));
    }

    final String dm = getDiagnosticMessage();
    if (dm == null)
    {
      elements.add(new ASN1OctetString());
    }
    else
    {
      elements.add(new ASN1OctetString(dm));
    }

    final List<String> refs = getReferralURLs();
    if (! refs.isEmpty())
    {
      final ArrayList<ASN1Element> refElements =
           new ArrayList<ASN1Element>(refs.size());
      for (final String r : refs)
      {
        refElements.add(new ASN1OctetString(r));
      }
      elements.add(new ASN1Sequence(GenericResponseProtocolOp.TYPE_REFERRALS,
           refElements));
    }

    if (responseOID != null)
    {
      elements.add(new ASN1OctetString(TYPE_RESPONSE_OID, responseOID));
    }

    if (responseValue != null)
    {
      elements.add(responseValue);
    }

    return new ASN1Sequence(LDAPMessage.PROTOCOL_OP_TYPE_EXTENDED_RESPONSE,
         elements);
  }



  /**
   * Decodes the provided ASN.1 element as an extended response protocol op.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded extended response protocol op.
   *
   * @throws  com.hwlcn.ldap.ldap.sdk.LDAPException  If the provided ASN.1 element cannot be decoded as
   *                         an extended response protocol op.
   */
  public static ExtendedResponseProtocolOp decodeProtocolOp(
                                                final ASN1Element element)
         throws LDAPException
  {
    try
    {
      final ASN1Element[] elements =
           ASN1Sequence.decodeAsSequence(element).elements();
      final int resultCode =
           ASN1Enumerated.decodeAsEnumerated(elements[0]).intValue();

      final String matchedDN;
      final String md =
           ASN1OctetString.decodeAsOctetString(elements[1]).stringValue();
      if (md.length() > 0)
      {
        matchedDN = md;
      }
      else
      {
        matchedDN = null;
      }

      final String diagnosticMessage;
      final String dm =
           ASN1OctetString.decodeAsOctetString(elements[2]).stringValue();
      if (dm.length() > 0)
      {
        diagnosticMessage = dm;
      }
      else
      {
        diagnosticMessage = null;
      }

      ASN1OctetString responseValue = null;
      List<String> referralURLs = null;
      String responseOID = null;
      if (elements.length > 3)
      {
        for (int i=3; i < elements.length; i++)
        {
          switch (elements[i].getType())
          {
            case GenericResponseProtocolOp.TYPE_REFERRALS:
              final ASN1Element[] refElements =
                   ASN1Sequence.decodeAsSequence(elements[3]).elements();
              referralURLs = new ArrayList<String>(refElements.length);
              for (final ASN1Element e : refElements)
              {
                referralURLs.add(
                     ASN1OctetString.decodeAsOctetString(e).stringValue());
              }
              break;

            case TYPE_RESPONSE_OID:
              responseOID = ASN1OctetString.decodeAsOctetString(elements[i]).
                   stringValue();
              break;

            case TYPE_RESPONSE_VALUE:
              responseValue = ASN1OctetString.decodeAsOctetString(elements[i]);
              break;

            default:
              throw new LDAPException(ResultCode.DECODING_ERROR,
                   ERR_EXTENDED_RESPONSE_INVALID_ELEMENT.get(
                        toHex(elements[i].getType())));
          }
        }
      }

      return new ExtendedResponseProtocolOp(resultCode, matchedDN,
           diagnosticMessage, referralURLs, responseOID, responseValue);
    }
    catch (final LDAPException le)
    {
      debugException(le);
      throw le;
    }
    catch (final Exception e)
    {
      debugException(e);
      throw new LDAPException(ResultCode.DECODING_ERROR,
           ERR_EXTENDED_RESPONSE_CANNOT_DECODE.get(getExceptionMessage(e)),
           e);
    }
  }



  /**
   * {@inheritDoc}
   */
  public void writeTo(final ASN1Buffer buffer)
  {
    final ASN1BufferSequence opSequence =
         buffer.beginSequence(LDAPMessage.PROTOCOL_OP_TYPE_EXTENDED_RESPONSE);
    buffer.addEnumerated(resultCode);
    buffer.addOctetString(matchedDN);
    buffer.addOctetString(diagnosticMessage);

    if (! referralURLs.isEmpty())
    {
      final ASN1BufferSequence refSequence =
           buffer.beginSequence(GenericResponseProtocolOp.TYPE_REFERRALS);
      for (final String s : referralURLs)
      {
        buffer.addOctetString(s);
      }
      refSequence.end();
    }

    if (responseOID != null)
    {
      buffer.addOctetString(TYPE_RESPONSE_OID, responseOID);
    }

    if (responseValue != null)
    {
      buffer.addOctetString(TYPE_RESPONSE_VALUE, responseValue.getValue());
    }

    opSequence.end();
  }



  /**
   * Creates a extended result from this protocol op.
   *
   * @param  controls  The set of controls to include in the extended result.
   *                   It may be empty or {@code null} if no controls should be
   *                   included.
   *
   * @return  The extended result that was created.
   */
  public ExtendedResult toExtendedResult(final Control... controls)
  {
    final String[] referralArray = new String[referralURLs.size()];
    referralURLs.toArray(referralArray);

    return new ExtendedResult(-1, ResultCode.valueOf(resultCode),
         diagnosticMessage, matchedDN, referralArray, responseOID,
         responseValue, controls);
  }



  /**
   * Retrieves a string representation of this protocol op.
   *
   * @return  A string representation of this protocol op.
   */
  @Override()
  public String toString()
  {
    final StringBuilder buffer = new StringBuilder();
    toString(buffer);
    return buffer.toString();
  }



  /**
   * {@inheritDoc}
   */
  public void toString(final StringBuilder buffer)
  {
    buffer.append("ExtendedResponseProtocolOp(resultCode=");
    buffer.append(resultCode);

    if (matchedDN != null)
    {
      buffer.append(", matchedDN='");
      buffer.append(matchedDN);
      buffer.append('\'');
    }

    if (diagnosticMessage != null)
    {
      buffer.append(", diagnosticMessage='");
      buffer.append(diagnosticMessage);
      buffer.append('\'');
    }

    if (! referralURLs.isEmpty())
    {
      buffer.append(", referralURLs={");

      final Iterator<String> iterator = referralURLs.iterator();
      while (iterator.hasNext())
      {
        buffer.append('\'');
        buffer.append(iterator.next());
        buffer.append('\'');
        if (iterator.hasNext())
        {
          buffer.append(',');
        }
      }

      buffer.append('}');
    }

    if (responseOID != null)
    {
      buffer.append(", responseOID='");
      buffer.append(responseOID);
      buffer.append('\'');
    }

    buffer.append(')');
  }
}
