/*
 * Copyright 2007-2013 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2008-2013 UnboundID Corp.
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



import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.hwlcn.ldap.asn1.ASN1OctetString;
import com.hwlcn.ldap.ldap.sdk.schema.Schema;
import com.hwlcn.core.annotation.NotMutable;
import com.hwlcn.core.annotation.ThreadSafety;
import com.hwlcn.ldap.util.ThreadSafetyLevel;

import static com.hwlcn.ldap.ldap.sdk.LDAPMessages.*;
import static com.hwlcn.ldap.util.Validator.*;


@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class DN
       implements Comparable<DN>, Comparator<DN>, Serializable
{

  private static final RDN[] NO_RDNS = new RDN[0];



  public static final DN NULL_DN = new DN();


  private static final long serialVersionUID = -5272968942085729346L;

  private final RDN[] rdns;

  private final Schema schema;

  private final String dnString;

  private volatile String normalizedString;



  public DN(final RDN... rdns)
  {
    ensureNotNull(rdns);

    this.rdns = rdns;
    if (rdns.length == 0)
    {
      dnString         = "";
      normalizedString = "";
      schema           = null;
    }
    else
    {
      Schema s = null;
      final StringBuilder buffer = new StringBuilder();
      for (final RDN rdn : rdns)
      {
        if (buffer.length() > 0)
        {
          buffer.append(',');
        }
        rdn.toString(buffer, false);

        if (s == null)
        {
          s = rdn.getSchema();
        }
      }

      dnString = buffer.toString();
      schema   = s;
    }
  }




  public DN(final List<RDN> rdns)
  {
    ensureNotNull(rdns);

    if (rdns.isEmpty())
    {
      this.rdns        = NO_RDNS;
      dnString         = "";
      normalizedString = "";
      schema           = null;
    }
    else
    {
      this.rdns = rdns.toArray(new RDN[rdns.size()]);

      Schema s = null;
      final StringBuilder buffer = new StringBuilder();
      for (final RDN rdn : this.rdns)
      {
        if (buffer.length() > 0)
        {
          buffer.append(',');
        }
        rdn.toString(buffer, false);

        if (s == null)
        {
          s = rdn.getSchema();
        }
      }

      dnString = buffer.toString();
      schema   = s;
    }
  }


  public DN(final RDN rdn, final DN parentDN)
  {
    ensureNotNull(rdn, parentDN);

    rdns = new RDN[parentDN.rdns.length + 1];
    rdns[0] = rdn;
    System.arraycopy(parentDN.rdns, 0, rdns, 1, parentDN.rdns.length);

    Schema s = null;
    final StringBuilder buffer = new StringBuilder();
    for (final RDN r : rdns)
    {
      if (buffer.length() > 0)
      {
        buffer.append(',');
      }
      r.toString(buffer, false);

      if (s == null)
      {
        s = r.getSchema();
      }
    }

    dnString = buffer.toString();
    schema   = s;
  }




  public DN(final String dnString)
         throws LDAPException
  {
    this(dnString, null);
  }



  public DN(final String dnString, final Schema schema)
         throws LDAPException
  {
    ensureNotNull(dnString);

    this.dnString = dnString;
    this.schema   = schema;

    final ArrayList<RDN> rdnList = new ArrayList<RDN>(5);

    final int length = dnString.length();
    if (length == 0)
    {
      rdns             = NO_RDNS;
      normalizedString = "";
      return;
    }

    int pos = 0;
    boolean expectMore = false;
rdnLoop:
    while (pos < length)
    {

      while ((pos < length) && (dnString.charAt(pos) == ' '))
      {
        pos++;
      }

      if (pos >= length)
      {

        if (rdnList.isEmpty())
        {
          break;
        }
        else
        {
          throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
                                  ERR_DN_ENDS_WITH_COMMA.get());
        }
      }


      int rdnEndPos;
      int rdnStartPos = pos;
      int attrStartPos = pos;
      while (pos < length)
      {
        final char c = dnString.charAt(pos);
        if ((c == ' ') || (c == '='))
        {
          break;
        }
        else if ((c == ',') || (c == ';'))
        {
          throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
                                  ERR_DN_UNEXPECTED_COMMA.get(pos));
        }

        pos++;
      }

      String attrName = dnString.substring(attrStartPos, pos);
      if (attrName.length() == 0)
      {
        throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
                                ERR_DN_NO_ATTR_IN_RDN.get());
      }



      while ((pos < length) && (dnString.charAt(pos) == ' '))
      {
        pos++;
      }

      if ((pos >= length) || (dnString.charAt(pos) != '='))
      {

        throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
                                ERR_DN_NO_EQUAL_SIGN.get(attrName));
      }


      pos++;
      while ((pos < length) && (dnString.charAt(pos) == ' '))
      {
        pos++;
      }



      if (pos >= length)
      {
        throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
                                ERR_DN_NO_VALUE_FOR_ATTR.get(attrName));
      }



      ASN1OctetString value;
      if (dnString.charAt(pos) == '#')
      {

        final byte[] valueArray = RDN.readHexString(dnString, ++pos);
        value = new ASN1OctetString(valueArray);
        pos += (valueArray.length * 2);
        rdnEndPos = pos;
      }
      else
      {

        final StringBuilder buffer = new StringBuilder();
        pos = RDN.readValueString(dnString, pos, buffer);
        value = new ASN1OctetString(buffer.toString());
        rdnEndPos = pos;
      }



      while ((pos < length) && (dnString.charAt(pos) == ' '))
      {
        pos++;
      }

      if (pos >= length)
      {

        rdnList.add(new RDN(attrName, value, schema,
             getTrimmedRDN(dnString, rdnStartPos,rdnEndPos)));
        expectMore = false;
        break;
      }

      switch (dnString.charAt(pos))
      {
        case '+':

          pos++;
          break;

        case ',':
        case ';':

          rdnList.add(new RDN(attrName, value, schema,
               getTrimmedRDN(dnString, rdnStartPos,rdnEndPos)));
          pos++;
          expectMore = true;
          continue rdnLoop;

        default:

          throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
                                  ERR_DN_UNEXPECTED_CHAR.get(
                                       dnString.charAt(pos), pos));
      }

      if (pos >= length)
      {
        throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
                                ERR_DN_ENDS_WITH_PLUS.get());
      }



      final ArrayList<String> nameList = new ArrayList<String>(5);
      final ArrayList<ASN1OctetString> valueList =
           new ArrayList<ASN1OctetString>(5);
      nameList.add(attrName);
      valueList.add(value);

      while (pos < length)
      {

        while ((pos < length) && (dnString.charAt(pos) == ' '))
        {
          pos++;
        }

        if (pos >= length)
        {
          throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
                                  ERR_DN_ENDS_WITH_PLUS.get());
        }


        attrStartPos = pos;
        while (pos < length)
        {
          final char c = dnString.charAt(pos);
          if ((c == ' ') || (c == '='))
          {
            break;
          }
          else if ((c == ',') || (c == ';'))
          {
            throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
                                    ERR_DN_UNEXPECTED_COMMA.get(pos));
          }

          pos++;
        }

        attrName = dnString.substring(attrStartPos, pos);
        if (attrName.length() == 0)
        {
          throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
                                  ERR_DN_NO_ATTR_IN_RDN.get());
        }


        // Skip over any spaces before the equal sign.
        while ((pos < length) && (dnString.charAt(pos) == ' '))
        {
          pos++;
        }

        if ((pos >= length) || (dnString.charAt(pos) != '='))
        {
          // We didn't find an equal sign.
          throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
                                  ERR_DN_NO_EQUAL_SIGN.get(attrName));
        }

        // Skip over the equal sign, and then any spaces leading up to the
        // attribute value.
        pos++;
        while ((pos < length) && (dnString.charAt(pos) == ' '))
        {
          pos++;
        }


        // If we're at the end of the string, then it's not a valid DN.
        if (pos >= length)
        {
          throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
                                  ERR_DN_NO_VALUE_FOR_ATTR.get(attrName));
        }


        // Read the value for this RDN component.
        if (dnString.charAt(pos) == '#')
        {
          // It is a hex-encoded value, so we'll read until we find the end of
          // the string or the first non-hex character, which must be a space, a
          // comma, or a plus sign.
          final byte[] valueArray = RDN.readHexString(dnString, ++pos);
          value = new ASN1OctetString(valueArray);
          pos += (valueArray.length * 2);
          rdnEndPos = pos;
        }
        else
        {
          // It is a string value, which potentially includes escaped
          // characters.
          final StringBuilder buffer = new StringBuilder();
          pos = RDN.readValueString(dnString, pos, buffer);
          value = new ASN1OctetString(buffer.toString());
          rdnEndPos = pos;
        }


        // Skip over any spaces until we find a comma, a plus sign, or the end
        // of the value.
        while ((pos < length) && (dnString.charAt(pos) == ' '))
        {
          pos++;
        }

        nameList.add(attrName);
        valueList.add(value);

        if (pos >= length)
        {
          // We've hit the end of the RDN and the end of the DN.
          final String[] names = nameList.toArray(new String[nameList.size()]);
          final ASN1OctetString[] values =
               valueList.toArray(new ASN1OctetString[valueList.size()]);
          rdnList.add(new RDN(names, values, schema,
               getTrimmedRDN(dnString, rdnStartPos,rdnEndPos)));
          expectMore = false;
          break rdnLoop;
        }

        switch (dnString.charAt(pos))
        {
          case '+':
            // There are still more RDN components to be read, so we're not done
            // yet.
            pos++;

            if (pos >= length)
            {
              throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
                                      ERR_DN_ENDS_WITH_PLUS.get());
            }
            break;

          case ',':
          case ';':
            // We've hit the end of the RDN, but there is still more of the DN
            // to be read.
            final String[] names =
                 nameList.toArray(new String[nameList.size()]);
            final ASN1OctetString[] values =
                 valueList.toArray(new ASN1OctetString[valueList.size()]);
            rdnList.add(new RDN(names, values, schema,
                 getTrimmedRDN(dnString, rdnStartPos,rdnEndPos)));
            pos++;
            expectMore = true;
            continue rdnLoop;

          default:
            // It's an illegal character.  This should never happen.
            throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
                                    ERR_DN_UNEXPECTED_CHAR.get(
                                         dnString.charAt(pos), pos));
        }
      }
    }

    // If we are expecting more information to be provided, then it means that
    // the string ended with a comma or semicolon.
    if (expectMore)
    {
      throw new LDAPException(ResultCode.INVALID_DN_SYNTAX,
                              ERR_DN_ENDS_WITH_COMMA.get());
    }

    // At this point, we should have all of the RDNs to use to create this DN.
    rdns = new RDN[rdnList.size()];
    rdnList.toArray(rdns);
  }



  /**
   * Retrieves a trimmed version of the string representation of the RDN in the
   * specified portion of the provided DN string.  Only non-escaped trailing
   * spaces will be removed.
   *
   * @param  dnString  The string representation of the DN from which to extract
   *                   the string representation of the RDN.
   * @param  start     The position of the first character in the RDN.
   * @param  end       The position marking the end of the RDN.
   *
   * @return  A properly-trimmed string representation of the RDN.
   */
  private static String getTrimmedRDN(final String dnString, final int start,
                                      final int end)
  {
    final String rdnString = dnString.substring(start, end);
    if (! rdnString.endsWith(" "))
    {
      return rdnString;
    }

    final StringBuilder buffer = new StringBuilder(rdnString);
    while ((buffer.charAt(buffer.length() - 1) == ' ') &&
           (buffer.charAt(buffer.length() - 2) != '\\'))
    {
      buffer.setLength(buffer.length() - 1);
    }

    return buffer.toString();
  }



  /**
   * Indicates whether the provided string represents a valid DN.
   *
   * @param  s  The string for which to make the determination.  It must not be
   *            {@code null}.
   *
   * @return  {@code true} if the provided string represents a valid DN, or
   *          {@code false} if not.
   */
  public static boolean isValidDN(final String s)
  {
    try
    {
      new DN(s);
      return true;
    }
    catch (LDAPException le)
    {
      return false;
    }
  }




  /**
   * Retrieves the leftmost (i.e., furthest from the naming context) RDN
   * component for this DN.
   *
   * @return  The leftmost RDN component for this DN, or {@code null} if this DN
   *          does not have any RDNs (i.e., it is the null DN).
   */
  public RDN getRDN()
  {
    if (rdns.length == 0)
    {
      return null;
    }
    else
    {
      return rdns[0];
    }
  }



  /**
   * Retrieves the string representation of the leftmost (i.e., furthest from
   * the naming context) RDN component for this DN.
   *
   * @return  The string representation of the leftmost RDN component for this
   *          DN, or {@code null} if this DN does not have any RDNs (i.e., it is
   *          the null DN).
   */
  public String getRDNString()
  {
    if (rdns.length == 0)
    {
      return null;
    }
    else
    {
      return rdns[0].toString();
    }
  }



  /**
   * Retrieves the string representation of the leftmost (i.e., furthest from
   * the naming context) RDN component for the DN with the provided string
   * representation.
   *
   * @param  s  The string representation of the DN to process.  It must not be
   *            {@code null}.
   *
   * @return  The string representation of the leftmost RDN component for this
   *          DN, or {@code null} if this DN does not have any RDNs (i.e., it is
   *          the null DN).
   *
   * @throws  LDAPException  If the provided string cannot be parsed as a DN.
   */
  public static String getRDNString(final String s)
         throws LDAPException
  {
    return new DN(s).getRDNString();
  }



  /**
   * Retrieves the set of RDNs that comprise this DN.
   *
   * @return  The set of RDNs that comprise this DN.
   */
  public RDN[] getRDNs()
  {
    return rdns;
  }



  /**
   * Retrieves the set of RDNs that comprise the DN with the provided string
   * representation.
   *
   * @param  s  The string representation of the DN for which to retrieve the
   *            RDNs.  It must not be {@code null}.
   *
   * @return  The set of RDNs that comprise the DN with the provided string
   *          representation.
   *
   * @throws  LDAPException  If the provided string cannot be parsed as a DN.
   */
  public static RDN[] getRDNs(final String s)
         throws LDAPException
  {
    return new DN(s).getRDNs();
  }



  /**
   * Retrieves the set of string representations of the RDNs that comprise this
   * DN.
   *
   * @return  The set of string representations of the RDNs that comprise this
   *          DN.
   */
  public String[] getRDNStrings()
  {
    final String[] rdnStrings = new String[rdns.length];
    for (int i=0; i < rdns.length; i++)
    {
      rdnStrings[i] = rdns[i].toString();
    }
    return rdnStrings;
  }



  /**
   * Retrieves the set of string representations of the RDNs that comprise this
   * DN.
   *
   * @param  s  The string representation of the DN for which to retrieve the
   *            RDN strings.  It must not be {@code null}.
   *
   * @return  The set of string representations of the RDNs that comprise this
   *          DN.
   *
   * @throws  LDAPException  If the provided string cannot be parsed as a DN.
   */
  public static String[] getRDNStrings(final String s)
         throws LDAPException
  {
    return new DN(s).getRDNStrings();
  }



  /**
   * Indicates whether this DN represents the null DN, which does not have any
   * RDN components.
   *
   * @return  {@code true} if this DN represents the null DN, or {@code false}
   *          if not.
   */
  public boolean isNullDN()
  {
    return (rdns.length == 0);
  }



  /**
   * Retrieves the DN that is the parent for this DN.  Note that neither the
   * null DN nor DNs consisting of a single RDN component will be considered to
   * have parent DNs.
   *
   * @return  The DN that is the parent for this DN, or {@code null} if there
   *          is no parent.
   */
  public DN getParent()
  {
    switch (rdns.length)
    {
      case 0:
      case 1:
        return null;

      case 2:
        return new DN(rdns[1]);

      case 3:
        return new DN(rdns[1], rdns[2]);

      case 4:
        return new DN(rdns[1], rdns[2], rdns[3]);

      case 5:
        return new DN(rdns[1], rdns[2], rdns[3], rdns[4]);

      default:
        final RDN[] parentRDNs = new RDN[rdns.length - 1];
        System.arraycopy(rdns, 1, parentRDNs, 0, parentRDNs.length);
        return new DN(parentRDNs);
    }
  }



  /**
   * Retrieves the DN that is the parent for the DN with the provided string
   * representation.  Note that neither the null DN nor DNs consisting of a
   * single RDN component will be considered to have parent DNs.
   *
   * @param  s  The string representation of the DN for which to retrieve the
   *            parent.  It must not be {@code null}.
   *
   * @return  The DN that is the parent for this DN, or {@code null} if there
   *          is no parent.
   *
   * @throws  LDAPException  If the provided string cannot be parsed as a DN.
   */
  public static DN getParent(final String s)
         throws LDAPException
  {
    return new DN(s).getParent();
  }



  /**
   * Retrieves the string representation of the DN that is the parent for this
   * DN.  Note that neither the null DN nor DNs consisting of a single RDN
   * component will be considered to have parent DNs.
   *
   * @return  The DN that is the parent for this DN, or {@code null} if there
   *          is no parent.
   */
  public String getParentString()
  {
    final DN parentDN = getParent();
    if (parentDN == null)
    {
      return null;
    }
    else
    {
      return parentDN.toString();
    }
  }



  /**
   * Retrieves the string representation of the DN that is the parent for the
   * DN with the provided string representation.  Note that neither the null DN
   * nor DNs consisting of a single RDN component will be considered to have
   * parent DNs.
   *
   * @param  s  The string representation of the DN for which to retrieve the
   *            parent.  It must not be {@code null}.
   *
   * @return  The DN that is the parent for this DN, or {@code null} if there
   *          is no parent.
   *
   * @throws  LDAPException  If the provided string cannot be parsed as a DN.
   */
  public static String getParentString(final String s)
         throws LDAPException
  {
    return new DN(s).getParentString();
  }



  /**
   * Indicates whether this DN is an ancestor of the provided DN.  It will be
   * considered an ancestor of the provided DN if the array of RDN components
   * for the provided DN ends with the elements that comprise the array of RDN
   * components for this DN (i.e., if the provided DN is subordinate to, or
   * optionally equal to, this DN).  The null DN will be considered an ancestor
   * for all other DNs (with the exception of the null DN if {@code allowEquals}
   * is {@code false}).
   *
   * @param  dn           The DN for which to make the determination.
   * @param  allowEquals  Indicates whether a DN should be considered an
   *                      ancestor of itself.
   *
   * @return  {@code true} if this DN may be considered an ancestor of the
   *          provided DN, or {@code false} if not.
   */
  public boolean isAncestorOf(final DN dn, final boolean allowEquals)
  {
    int thisPos = rdns.length - 1;
    int thatPos = dn.rdns.length - 1;

    if (thisPos < 0)
    {
      // This DN must be the null DN, which is an ancestor for all other DNs
      // (and equal to the null DN, which we may still classify as being an
      // ancestor).
      return (allowEquals || (thatPos >= 0));
    }

    if ((thisPos > thatPos) || ((thisPos == thatPos) && (! allowEquals)))
    {
      // This DN has more RDN components than the provided DN, so it can't
      // possibly be an ancestor, or has the same number of components and equal
      // DNs shouldn't be considered ancestors.
      return false;
    }

    while (thisPos >= 0)
    {
      if (! rdns[thisPos--].equals(dn.rdns[thatPos--]))
      {
        return false;
      }
    }

    // If we've gotten here, then we can consider this DN to be an ancestor of
    // the provided DN.
    return true;
  }



  /**
   * Indicates whether this DN is an ancestor of the DN with the provided string
   * representation.  It will be considered an ancestor of the provided DN if
   * the array of RDN components for the provided DN ends with the elements that
   * comprise the array of RDN components for this DN (i.e., if the provided DN
   * is subordinate to, or optionally equal to, this DN).  The null DN will be
   * considered an ancestor for all other DNs (with the exception of the null DN
   * if {@code allowEquals} is {@code false}).
   *
   * @param  s            The string representation of the DN for which to make
   *                      the determination.
   * @param  allowEquals  Indicates whether a DN should be considered an
   *                      ancestor of itself.
   *
   * @return  {@code true} if this DN may be considered an ancestor of the
   *          provided DN, or {@code false} if not.
   *
   * @throws  LDAPException  If the provided string cannot be parsed as a DN.
   */
  public boolean isAncestorOf(final String s, final boolean allowEquals)
         throws LDAPException
  {
    return isAncestorOf(new DN(s), allowEquals);
  }



  /**
   * Indicates whether the DN represented by the first string is an ancestor of
   * the DN represented by the second string.  The first DN will be considered
   * an ancestor of the second DN if the array of RDN components for the first
   * DN ends with the elements that comprise the array of RDN components for the
   * second DN (i.e., if the first DN is subordinate to, or optionally equal to,
   * the second DN).  The null DN will be considered an ancestor for all other
   * DNs (with the exception of the null DN if {@code allowEquals} is
   * {@code false}).
   *
   * @param  s1           The string representation of the first DN for which to
   *                      make the determination.
   * @param  s2           The string representation of the second DN for which
   *                      to make the determination.
   * @param  allowEquals  Indicates whether a DN should be considered an
   *                      ancestor of itself.
   *
   * @return  {@code true} if the first DN may be considered an ancestor of the
   *          second DN, or {@code false} if not.
   *
   * @throws  LDAPException  If either of the provided strings cannot be parsed
   *                         as a DN.
   */
  public static boolean isAncestorOf(final String s1, final String s2,
                                     final boolean allowEquals)
         throws LDAPException
  {
    return new DN(s1).isAncestorOf(new DN(s2), allowEquals);
  }



  /**
   * Indicates whether this DN is a descendant of the provided DN.  It will be
   * considered a descendant of the provided DN if the array of RDN components
   * for this DN ends with the elements that comprise the RDN components for the
   * provided DN (i.e., if this DN is subordinate to, or optionally equal to,
   * the provided DN).  The null DN will not be considered a descendant for any
   * other DNs (with the exception of the null DN if {@code allowEquals} is
   * {@code true}).
   *
   * @param  dn           The DN for which to make the determination.
   * @param  allowEquals  Indicates whether a DN should be considered a
   *                      descendant of itself.
   *
   * @return  {@code true} if this DN may be considered a descendant of the
   *          provided DN, or {@code false} if not.
   */
  public boolean isDescendantOf(final DN dn, final boolean allowEquals)
  {
    int thisPos = rdns.length - 1;
    int thatPos = dn.rdns.length - 1;

    if (thatPos < 0)
    {
      // The provided DN must be the null DN, which will be considered an
      // ancestor for all other DNs (and equal to the null DN), making this DN
      // considered a descendant for that DN.
      return (allowEquals || (thisPos >= 0));
    }

    if ((thisPos < thatPos) || ((thisPos == thatPos) && (! allowEquals)))
    {
      // This DN has fewer DN components than the provided DN, so it can't
      // possibly be a descendant, or it has the same number of components and
      // equal DNs shouldn't be considered descendants.
      return false;
    }

    while (thatPos >= 0)
    {
      if (! rdns[thisPos--].equals(dn.rdns[thatPos--]))
      {
        return false;
      }
    }

    // If we've gotten here, then we can consider this DN to be a descendant of
    // the provided DN.
    return true;
  }



  /**
   * Indicates whether this DN is a descendant of the DN with the provided
   * string representation.  It will be considered a descendant of the provided
   * DN if the array of RDN components for this DN ends with the elements that
   * comprise the RDN components for the provided DN (i.e., if this DN is
   * subordinate to, or optionally equal to, the provided DN).  The null DN will
   * not be considered a descendant for any other DNs (with the exception of the
   * null DN if {@code allowEquals} is {@code true}).
   *
   * @param  s            The string representation of the DN for which to make
   *                      the determination.
   * @param  allowEquals  Indicates whether a DN should be considered a
   *                      descendant of itself.
   *
   * @return  {@code true} if this DN may be considered a descendant of the
   *          provided DN, or {@code false} if not.
   *
   * @throws  LDAPException  If the provided string cannot be parsed as a DN.
   */
  public boolean isDescendantOf(final String s, final boolean allowEquals)
         throws LDAPException
  {
    return isDescendantOf(new DN(s), allowEquals);
  }



  /**
   * Indicates whether the DN represented by the first string is a descendant of
   * the DN represented by the second string.  The first DN will be considered a
   * descendant of the second DN if the array of RDN components for the first DN
   * ends with the elements that comprise the RDN components for the second DN
   * (i.e., if the first DN is subordinate to, or optionally equal to, the
   * second DN).  The null DN will not be considered a descendant for any other
   * DNs (with the exception of the null DN if {@code allowEquals} is
   * {@code true}).
   *
   * @param  s1           The string representation of the first DN for which to
   *                      make the determination.
   * @param  s2           The string representation of the second DN for which
   *                      to make the determination.
   * @param  allowEquals  Indicates whether a DN should be considered an
   *                      ancestor of itself.
   *
   * @return  {@code true} if this DN may be considered a descendant of the
   *          provided DN, or {@code false} if not.
   *
   * @throws  LDAPException  If either of the provided strings cannot be parsed
   *                         as a DN.
   */
  public static boolean isDescendantOf(final String s1, final String s2,
                                       final boolean allowEquals)
         throws LDAPException
  {
    return new DN(s1).isDescendantOf(new DN(s2), allowEquals);
  }



  /**
   * Indicates whether this DN falls within the range of the provided search
   * base DN and scope.
   *
   * @param  baseDN  The base DN for which to make the determination.  It must
   *                 not be {@code null}.
   * @param  scope   The scope for which to make the determination.  It must not
   *                 be {@code null}.
   *
   * @return  {@code true} if this DN is within the range of the provided base
   *          and scope, or {@code false} if not.
   *
   * @throws  LDAPException  If a problem occurs while making the determination.
   */
  public boolean matchesBaseAndScope(final String baseDN,
                                     final SearchScope scope)
         throws LDAPException
  {
    return matchesBaseAndScope(new DN(baseDN), scope);
  }



  /**
   * Indicates whether this DN falls within the range of the provided search
   * base DN and scope.
   *
   * @param  baseDN  The base DN for which to make the determination.  It must
   *                 not be {@code null}.
   * @param  scope   The scope for which to make the determination.  It must not
   *                 be {@code null}.
   *
   * @return  {@code true} if this DN is within the range of the provided base
   *          and scope, or {@code false} if not.
   *
   * @throws  LDAPException  If a problem occurs while making the determination.
   */
  public boolean matchesBaseAndScope(final DN baseDN, final SearchScope scope)
         throws LDAPException
  {
    ensureNotNull(baseDN, scope);

    switch (scope.intValue())
    {
      case SearchScope.BASE_INT_VALUE:
        return equals(baseDN);

      case SearchScope.ONE_INT_VALUE:
        return baseDN.equals(getParent());

      case SearchScope.SUB_INT_VALUE:
        return isDescendantOf(baseDN, true);

      case SearchScope.SUBORDINATE_SUBTREE_INT_VALUE:
        return isDescendantOf(baseDN, false);

      default:
        throw new LDAPException(ResultCode.PARAM_ERROR,
             ERR_DN_MATCHES_UNSUPPORTED_SCOPE.get(dnString,
                  String.valueOf(scope)));
    }
  }




  /**
   * Generates a hash code for this DN.
   *
   * @return  The generated hash code for this DN.
   */
  @Override() public int hashCode()
  {
    return toNormalizedString().hashCode();
  }



  /**
   * Indicates whether the provided object is equal to this DN.  In order for
   * the provided object to be considered equal, it must be a non-null DN with
   * the same set of RDN components.
   *
   * @param  o  The object for which to make the determination.
   *
   * @return  {@code true} if the provided object is considered equal to this
   *          DN, or {@code false} if not.
   */
  @Override()
  public boolean equals(final Object o)
  {
    if (o == null)
    {
      return false;
    }

    if (this == o)
    {
      return true;
    }

    if (! (o instanceof DN))
    {
      return false;
    }

    final DN dn = (DN) o;
    return (toNormalizedString().equals(dn.toNormalizedString()));
  }



  /**
   * Indicates whether the DN with the provided string representation is equal
   * to this DN.
   *
   * @param  s  The string representation of the DN to compare with this DN.
   *
   * @return  {@code true} if the DN with the provided string representation is
   *          equal to this DN, or {@code false} if not.
   *
   * @throws  LDAPException  If the provided string cannot be parsed as a DN.
   */
  public boolean equals(final String s)
         throws LDAPException
  {
    if (s == null)
    {
      return false;
    }

    return equals(new DN(s));
  }



  /**
   * Indicates whether the two provided strings represent the same DN.
   *
   * @param  s1  The string representation of the first DN for which to make the
   *             determination.  It must not be {@code null}.
   * @param  s2  The string representation of the second DN for which to make
   *             the determination.  It must not be {@code null}.
   *
   * @return  {@code true} if the provided strings represent the same DN, or
   *          {@code false} if not.
   *
   * @throws  LDAPException  If either of the provided strings cannot be parsed
   *                         as a DN.
   */
  public static boolean equals(final String s1, final String s2)
         throws LDAPException
  {
    return new DN(s1).equals(new DN(s2));
  }



  /**
   * Retrieves a string representation of this DN.
   *
   * @return  A string representation of this DN.
   */
  @Override()
  public String toString()
  {
    return dnString;
  }



  /**
   * Retrieves a string representation of this DN with minimal encoding for
   * special characters.  Only those characters specified in RFC 4514 section
   * 2.4 will be escaped.  No escaping will be used for non-ASCII characters or
   * non-printable ASCII characters.
   *
   * @return  A string representation of this DN with minimal encoding for
   *          special characters.
   */
  public String toMinimallyEncodedString()
  {
    final StringBuilder buffer = new StringBuilder();
    toString(buffer, true);
    return buffer.toString();
  }



  /**
   * Appends a string representation of this DN to the provided buffer.
   *
   * @param  buffer  The buffer to which to append the string representation of
   *                 this DN.
   */
  public void toString(final StringBuilder buffer)
  {
    toString(buffer, false);
  }



  /**
   * Appends a string representation of this DN to the provided buffer.
   *
   * @param  buffer            The buffer to which the string representation is
   *                           to be appended.
   * @param  minimizeEncoding  Indicates whether to restrict the encoding of
   *                           special characters to the bare minimum required
   *                           by LDAP (as per RFC 4514 section 2.4).  If this
   *                           is {@code true}, then only leading and trailing
   *                           spaces, double quotes, plus signs, commas,
   *                           semicolons, greater-than, less-than, and
   *                           backslash characters will be encoded.
   */
  public void toString(final StringBuilder buffer,
                       final boolean minimizeEncoding)
  {
    for (int i=0; i < rdns.length; i++)
    {
      if (i > 0)
      {
        buffer.append(',');
      }

      rdns[i].toString(buffer, minimizeEncoding);
    }
  }



  /**
   * Retrieves a normalized string representation of this DN.
   *
   * @return  A normalized string representation of this DN.
   */
  public String toNormalizedString()
  {
    if (normalizedString == null)
    {
      final StringBuilder buffer = new StringBuilder();
      toNormalizedString(buffer);
      normalizedString = buffer.toString();
    }

    return normalizedString;
  }



  /**
   * Appends a normalized string representation of this DN to the provided
   * buffer.
   *
   * @param  buffer  The buffer to which to append the normalized string
   *                 representation of this DN.
   */
  public void toNormalizedString(final StringBuilder buffer)
  {
    for (int i=0; i < rdns.length; i++)
    {
      if (i > 0)
      {
        buffer.append(',');
      }

      buffer.append(rdns[i].toNormalizedString());
    }
  }



  /**
   * Retrieves a normalized representation of the DN with the provided string
   * representation.
   *
   * @param  s  The string representation of the DN to normalize.  It must not
   *            be {@code null}.
   *
   * @return  The normalized representation of the DN with the provided string
   *          representation.
   *
   * @throws  LDAPException  If the provided string cannot be parsed as a DN.
   */
  public static String normalize(final String s)
         throws LDAPException
  {
    return normalize(s, null);
  }



  /**
   * Retrieves a normalized representation of the DN with the provided string
   * representation.
   *
   * @param  s       The string representation of the DN to normalize.  It must
   *                 not be {@code null}.
   * @param  schema  The schema to use to generate the normalized string
   *                 representation of the DN.  It may be {@code null} if no
   *                 schema is available.
   *
   * @return  The normalized representation of the DN with the provided string
   *          representation.
   *
   * @throws  LDAPException  If the provided string cannot be parsed as a DN.
   */
  public static String normalize(final String s, final Schema schema)
         throws LDAPException
  {
    return new DN(s, schema).toNormalizedString();
  }



  /**
   * Compares the provided DN to this DN to determine their relative order in
   * a sorted list.
   *
   * @param  dn  The DN to compare against this DN.  It must not be
   *             {@code null}.
   *
   * @return  A negative integer if this DN should come before the provided DN
   *          in a sorted list, a positive integer if this DN should come after
   *          the provided DN in a sorted list, or zero if the provided DN can
   *          be considered equal to this DN.
   */
  public int compareTo(final DN dn)
  {
    return compare(this, dn);
  }



  /**
   * Compares the provided DN values to determine their relative order in a
   * sorted list.
   *
   * @param  dn1  The first DN to be compared.  It must not be {@code null}.
   * @param  dn2  The second DN to be compared.  It must not be {@code null}.
   *
   * @return  A negative integer if the first DN should come before the second
   *          DN in a sorted list, a positive integer if the first DN should
   *          come after the second DN in a sorted list, or zero if the two DN
   *          values can be considered equal.
   */
  public int compare(final DN dn1, final DN dn2)
  {
    ensureNotNull(dn1, dn2);

    // We want the comparison to be in reverse order, so that DNs will be sorted
    // hierarchically.
    int pos1 = dn1.rdns.length - 1;
    int pos2 = dn2.rdns.length - 1;
    if (pos1 < 0)
    {
      if (pos2 < 0)
      {
        // Both DNs are the null DN, so they are equal.
        return 0;
      }
      else
      {
        // The first DN is the null DN and the second isn't, so the first DN
        // comes first.
        return -1;
      }
    }
    else if (pos2 < 0)
    {
      // The second DN is the null DN, which always comes first.
      return 1;
    }


    while ((pos1 >= 0) && (pos2 >= 0))
    {
      final int compValue = dn1.rdns[pos1].compareTo(dn2.rdns[pos2]);
      if (compValue != 0)
      {
        return compValue;
      }

      pos1--;
      pos2--;
    }


    // If we've gotten here, then one of the DNs is equal to or a descendant of
    // the other.
    if (pos1 < 0)
    {
      if (pos2 < 0)
      {
        // They're both the same length, so they should be considered equal.
        return 0;
      }
      else
      {
        // The first is shorter than the second, so it should come first.
        return -1;
      }
    }
    else
    {
      // The second RDN is shorter than the first, so it should come first.
      return 1;
    }
  }



  /**
   * Compares the DNs with the provided string representations to determine
   * their relative order in a sorted list.
   *
   * @param  s1  The string representation for the first DN to be compared.  It
   *             must not be {@code null}.
   * @param  s2  The string representation for the second DN to be compared.  It
   *             must not be {@code null}.
   *
   * @return  A negative integer if the first DN should come before the second
   *          DN in a sorted list, a positive integer if the first DN should
   *          come after the second DN in a sorted list, or zero if the two DN
   *          values can be considered equal.
   *
   * @throws  LDAPException  If either of the provided strings cannot be parsed
   *                         as a DN.
   */
  public static int compare(final String s1, final String s2)
         throws LDAPException
  {
    return compare(s1, s2, null);
  }



  /**
   * Compares the DNs with the provided string representations to determine
   * their relative order in a sorted list.
   *
   * @param  s1      The string representation for the first DN to be compared.
   *                 It must not be {@code null}.
   * @param  s2      The string representation for the second DN to be compared.
   *                 It must not be {@code null}.
   * @param  schema  The schema to use to generate the normalized string
   *                 representations of the DNs.  It may be {@code null} if no
   *                 schema is available.
   *
   * @return  A negative integer if the first DN should come before the second
   *          DN in a sorted list, a positive integer if the first DN should
   *          come after the second DN in a sorted list, or zero if the two DN
   *          values can be considered equal.
   *
   * @throws  LDAPException  If either of the provided strings cannot be parsed
   *                         as a DN.
   */
  public static int compare(final String s1, final String s2,
                            final Schema schema)
         throws LDAPException
  {
    return new DN(s1, schema).compareTo(new DN(s2, schema));
  }
}
