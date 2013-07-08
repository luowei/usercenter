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



import java.util.Collections;
import java.util.List;

import com.hwlcn.ldap.asn1.ASN1StreamReader;
import com.hwlcn.ldap.asn1.ASN1StreamReaderSequence;
import com.hwlcn.ldap.util.NotMutable;
import com.hwlcn.ldap.util.ThreadSafety;
import com.hwlcn.ldap.util.ThreadSafetyLevel;



/**
 * This class provides a data structure for holding information about the result
 * of processing a search request.  This includes the elements of the
 * {@link com.hwlcn.ldap.ldap.sdk.LDAPResult} object, but also contains additional information specific
 * to the search operation.  This includes:
 * <UL>
 *   <LI>The number of {@link SearchResultEntry} objects returned from the
 *       server.  This will be available regardless of whether the entries are
 *       included in this search result object or were returned through a
 *       {@link SearchResultListener}.</LI>
 *   <LI>The number of {@link SearchResultReference} objects returned from the
 *       server.  This will be available regardless of whether the entries are
 *       included in this search result object or were returned through a
 *       {@link SearchResultListener}.</LI>
 *   <LI>A list of the {@link SearchResultEntry} objects returned from the
 *       server.  This will be {@code null} if a {@link SearchResultListener}
 *       was used to return the entries.</LI>
 *   <LI>A list of the {@link SearchResultReference} objects returned from the
 *       server.  This will be {@code null} if a {@link SearchResultListener}
 *       was used to return the entries.</LI>
 * </UL>
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class SearchResult
       extends LDAPResult
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 1938208530894131198L;



  // The number of matching entries returned for this search.
  private int numEntries;

  // The number of search result references returned for this search.
  private int numReferences;

  // A list that may be used to hold the search result entries returned for
  // this search.
  private List<SearchResultEntry> searchEntries;

  // A list that may be used to hold the search result references returned for
  // this search.
  private List<SearchResultReference> searchReferences;



  /**
   * Creates a new search result object with the provided information.  This
   * version of the constructor should be used if the search result entries and
   * references were returned to the client via the {@code SearchResultListener}
   * interface.
   *
   * @param  messageID          The message ID for the LDAP message that is
   *                            associated with this LDAP result.
   * @param  resultCode         The result code from the search result done
   *                            response.
   * @param  diagnosticMessage  The diagnostic message from the search result
   *                            done response, if available.
   * @param  matchedDN          The matched DN from the search result done
   *                            response, if available.
   * @param  referralURLs       The set of referral URLs from the search result
   *                            done response, if available.
   * @param  numEntries         The number of search result entries returned
   *                            for this search.
   * @param  numReferences      The number of search result references returned
   *                            for this search.
   * @param  responseControls   The set of controls from the search result done
   *                            response, if available.
   */
  public SearchResult(final int messageID, final ResultCode resultCode,
                      final String diagnosticMessage, final String matchedDN,
                      final String[] referralURLs, final int numEntries,
                      final int numReferences, final Control[] responseControls)
  {
    super(messageID, resultCode, diagnosticMessage, matchedDN, referralURLs,
          responseControls);

    this.numEntries    = numEntries;
    this.numReferences = numReferences;

    searchEntries    = null;
    searchReferences = null;
  }



  /**
   * Creates a new search result object with the provided information.  This
   * version of the constructor should be used if the search result entries and
   * references were collected in lists rather than returned to the requester
   * through the {@code SearchResultListener} interface.
   *
   * @param  messageID          The message ID for the LDAP message that is
   *                            associated with this LDAP result.
   * @param  resultCode         The result code from the search result done
   *                            response.
   * @param  diagnosticMessage  The diagnostic message from the search result
   *                            done response, if available.
   * @param  matchedDN          The matched DN from the search result done
   *                            response, if available.
   * @param  referralURLs       The set of referral URLs from the search result
   *                            done response, if available.
   * @param  searchEntries      A list containing the set of search result
   *                            entries returned by the server.  It may only be
   *                            {@code null} if the search result entries were
   *                            returned through the
   *                            {@code SearchResultListener} interface.
   * @param  searchReferences   A list containing the set of search result
   *                            references returned by the server.  It may only
   *                            be {@code null} if the search result entries
   *                            were returned through the
   *                            {@code SearchResultListener} interface.
   * @param  numEntries         The number of search result entries returned
   *                            for this search.
   * @param  numReferences      The number of search result references returned
   *                            for this search.
   * @param  responseControls   The set of controls from the search result done
   *                            response, if available.
   */
  public SearchResult(final int messageID, final ResultCode resultCode,
                      final String diagnosticMessage, final String matchedDN,
                      final String[] referralURLs,
                      final List<SearchResultEntry> searchEntries,
                      final List<SearchResultReference> searchReferences,
                      final int numEntries, final int numReferences,
                      final Control[] responseControls)
  {
    super(messageID, resultCode, diagnosticMessage, matchedDN, referralURLs,
          responseControls);

    this.numEntries       = numEntries;
    this.numReferences    = numReferences;
    this.searchEntries    = searchEntries;
    this.searchReferences = searchReferences;
  }



  /**
   * Creates a new search result object with the provided message ID and with
   * the protocol op and controls read from the given ASN.1 stream reader.
   *
   * @param  messageID        The LDAP message ID for the LDAP message that is
   *                          associated with this LDAP result.
   * @param  messageSequence  The ASN.1 stream reader sequence used in the
   *                          course of reading the LDAP message elements.
   * @param  reader           The ASN.1 stream reader from which to read the
   *                          protocol op and controls.
   *
   * @return  The decoded search result object.
   *
   * @throws  LDAPException  If a problem occurs while reading or decoding data
   *                         from the ASN.1 stream reader.
   */
  static SearchResult readSearchResultFrom(final int messageID,
                           final ASN1StreamReaderSequence messageSequence,
                           final ASN1StreamReader reader)
         throws LDAPException
  {
    final LDAPResult r =
         LDAPResult.readLDAPResultFrom(messageID, messageSequence, reader);

    return new SearchResult(messageID, r.getResultCode(),
         r.getDiagnosticMessage(), r.getMatchedDN(), r.getReferralURLs(),
         -1, -1, r.getResponseControls());
  }



  /**
   * Retrieves the number of matching entries returned for the search operation.
   *
   * @return  The number of matching entries returned for the search operation.
   */
  public int getEntryCount()
  {
    return numEntries;
  }



  /**
   * Retrieves the number of search references returned for the search
   * operation.  This may be zero even if search references were received if the
   * connection used when processing the search was configured to automatically
   * follow referrals.
   *
   * @return  The number of search references returned for the search operation.
   */
  public int getReferenceCount()
  {
    return numReferences;
  }



  /**
   * Retrieves a list containing the matching entries returned from the search
   * operation.  This will only be available if a {@code SearchResultListener}
   * was not used during the search.
   *
   * @return  A list containing the matching entries returned from the search
   *          operation, or {@code null} if a {@code SearchResultListener} was
   *          used during the search.
   */
  public List<SearchResultEntry> getSearchEntries()
  {
    if (searchEntries == null)
    {
      return null;
    }

    return Collections.unmodifiableList(searchEntries);
  }



  /**
   * Retrieves the search result entry with the specified DN from the set of
   * entries returned.  This will only be available if a
   * {@code SearchResultListener} was not used during the search.
   *
   * @param  dn  The DN of the search result entry to retrieve.  It must not
   *             be {@code null}.
   *
   * @return  The search result entry with the provided DN, or {@code null} if
   *          the specified entry was not returned, or if a
   *          {@code SearchResultListener} was used for the search.
   *
   * @throws  LDAPException  If a problem is encountered while attempting to
   *                         parse the provided DN or a search entry DN.
   */
  public SearchResultEntry getSearchEntry(final String dn)
         throws LDAPException
  {
    if (searchEntries == null)
    {
      return null;
    }

    final DN parsedDN = new DN(dn);
    for (final SearchResultEntry e : searchEntries)
    {
      if (parsedDN.equals(e.getParsedDN()))
      {
        return e;
      }
    }

    return null;
  }



  /**
   * Retrieves a list containing the search references returned from the search
   * operation.  This will only be available if a {@code SearchResultListener}
   * was not used during the search, and may be empty even if search references
   * were received if the connection used when processing the search was
   * configured to automatically follow referrals.
   *
   * @return  A list containing the search references returned from the search
   *          operation, or {@code null} if a {@code SearchResultListener} was
   *          used during the search.
   */
  public List<SearchResultReference> getSearchReferences()
  {
    if (searchReferences == null)
    {
      return null;
    }

    return Collections.unmodifiableList(searchReferences);
  }



  /**
   * Provides information about the entries and references returned for the
   * search operation.  This must only be called when a search result is created
   * and the search result must not be altered at any point after that.
   *
   * @param  numEntries        The number of entries returned for the search
   *                           operation.
   * @param  searchEntries     A list containing the entries returned from the
   *                           search operation, or {@code null} if a
   *                           {@code SearchResultListener} was used during the
   *                           search.
   * @param  numReferences     The number of references returned for the search
   *                           operation.
   * @param  searchReferences  A list containing the search references returned
   *                           from the search operation, or {@code null} if a
   *                           {@code SearchResultListener} was used during the
   *                           search.
   */
  void setCounts(final int numEntries,
                 final List<SearchResultEntry> searchEntries,
                 final int numReferences,
                 final List<SearchResultReference> searchReferences)
  {
    this.numEntries    = numEntries;
    this.numReferences = numReferences;

    if (searchEntries == null)
    {
      this.searchEntries = null;
    }
    else
    {
      this.searchEntries = Collections.unmodifiableList(searchEntries);
    }

    if (searchReferences == null)
    {
      this.searchReferences = null;
    }
    else
    {
      this.searchReferences = Collections.unmodifiableList(searchReferences);
    }
  }



  /**
   * Appends a string representation of this LDAP result to the provided buffer.
   *
   * @param  buffer  The buffer to which to append a string representation of
   *                 this LDAP result.
   */
  @Override()
  public void toString(final StringBuilder buffer)
  {
    buffer.append("SearchResult(resultCode=");
    buffer.append(getResultCode());

    final int messageID = getMessageID();
    if (messageID >= 0)
    {
      buffer.append(", messageID=");
      buffer.append(messageID);
    }

    final String diagnosticMessage = getDiagnosticMessage();
    if (diagnosticMessage != null)
    {
      buffer.append(", diagnosticMessage='");
      buffer.append(diagnosticMessage);
      buffer.append('\'');
    }

    final String matchedDN = getMatchedDN();
    if (matchedDN != null)
    {
      buffer.append(", matchedDN='");
      buffer.append(matchedDN);
      buffer.append('\'');
    }

    final String[] referralURLs = getReferralURLs();
    if (referralURLs.length > 0)
    {
      buffer.append(", referralURLs={");
      for (int i=0; i < referralURLs.length; i++)
      {
        if (i > 0)
        {
          buffer.append(", ");
        }

        buffer.append('\'');
        buffer.append(referralURLs[i]);
        buffer.append('\'');
      }
      buffer.append('}');
    }

    buffer.append(", entriesReturned=");
    buffer.append(numEntries);
    buffer.append(", referencesReturned=");
    buffer.append(numReferences);

    final Control[] responseControls = getResponseControls();
    if (responseControls.length > 0)
    {
      buffer.append(", responseControls={");
      for (int i=0; i < responseControls.length; i++)
      {
        if (i > 0)
        {
          buffer.append(", ");
        }

        buffer.append(responseControls[i]);
      }
      buffer.append('}');
    }

    buffer.append(')');
  }
}
