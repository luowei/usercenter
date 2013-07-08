/*
 * Copyright 2008-2013 UnboundID Corp.
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
package com.hwlcn.ldap.util;



import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

import static com.hwlcn.ldap.util.UtilityMessages.*;



/**
 * This class defines an HTTP value pattern component, which may be used provide
 * string values read from a specified remote file accessed via HTTP.
 */
final class HTTPValuePatternComponent
      extends ValuePatternComponent
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 8879412445617836376L;



  // The lines that make up the data file.
  private final String[] lines;

  // The random number generator that will be used to seed the thread-local
  // generators.
  private final Random seedRandom;

  // The random number generator that will be used by this component.
  private final ThreadLocal<Random> random;



  /**
   * Creates a new HTTP value pattern component with the provided information.
   *
   * @param  url   The HTTP URL to the file from which to read the data.
   * @param  seed  The value that will be used to seed the initial random number
   *               generator.
   *
   * @throws  java.io.IOException  If a problem occurs while reading data from the
   *                       specified HTTP URL.
   */
  HTTPValuePatternComponent(final String url, final long seed)
       throws IOException
  {
    // Create the random number generators that will be used.
    seedRandom = new Random(seed);
    random     = new ThreadLocal<Random>();


    final ArrayList<String> lineList = new ArrayList<String>(100);
    final URL parsedURL = new URL(url);
    final HttpURLConnection urlConnection =
         (HttpURLConnection) parsedURL.openConnection();
    final BufferedReader reader = new BufferedReader(new InputStreamReader(
         urlConnection.getInputStream()));

    try
    {
      while (true)
      {
        final String line = reader.readLine();
        if (line == null)
        {
          break;
        }

        lineList.add(line);
      }
    }
    finally
    {
      reader.close();
    }

    if (lineList.isEmpty())
    {
      throw new IOException(ERR_VALUE_PATTERN_COMPONENT_EMPTY_FILE.get());
    }

    lines = new String[lineList.size()];
    lineList.toArray(lines);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  void append(final StringBuilder buffer)
  {
    Random r = random.get();
    if (r == null)
    {
      r = new Random(seedRandom.nextLong());
      random.set(r);
    }

    buffer.append(lines[r.nextInt(lines.length)]);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  boolean supportsBackReference()
  {
    return true;
  }
}
