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
package com.hwlcn.ldap.ldap.sdk.persist;



import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import com.hwlcn.ldap.ldap.sdk.Attribute;
import com.hwlcn.ldap.ldap.sdk.Entry;
import com.hwlcn.ldap.util.NotMutable;
import com.hwlcn.ldap.util.ThreadSafety;
import com.hwlcn.ldap.util.ThreadSafetyLevel;

import static com.hwlcn.ldap.ldap.sdk.persist.PersistMessages.*;
import static com.hwlcn.ldap.util.Debug.*;
import static com.hwlcn.ldap.util.StaticUtils.*;
import static com.hwlcn.ldap.util.Validator.*;



/**
 * This class provides a data structure that holds information about an
 * annotated setter method.
 */
@NotMutable()
@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class SetterInfo
       implements Serializable
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -1743750276508505946L;



  // Indicates whether attempts to invoke the associated method should fail if
  // the LDAP attribute has a value that is not valid for the data type of the
  // method argument.
  private final boolean failOnInvalidValue;

  // Indicates whether attempts to invoke the associated method should fail if
  // the LDAP attribute has multiple values but the method argument can only
  // hold a single value.
  private final boolean failOnTooManyValues;

  // Indicates whether the associated method takes an argument that supports
  // multiple values.
  private final boolean supportsMultipleValues;

  // The class that contains the associated method.
  private final Class<?> containingClass;

  // The method with which this object is associated.
  private final Method method;

  // The encoder used for this method.
  private final ObjectEncoder encoder;

  // The name of the associated attribute type.
  private final String attributeName;



  /**
   * Creates a new setter info object from the provided method.
   *
   * @param  m  The method to use to create this object.
   * @param  c  The class which holds the method.
   *
   * @throws  LDAPPersistException  If a problem occurs while processing the
   *                                given method.
   */
  SetterInfo(final Method m, final Class<?> c)
       throws LDAPPersistException
  {
    ensureNotNull(m, c);

    method = m;
    m.setAccessible(true);

    final LDAPSetter  a = m.getAnnotation(LDAPSetter.class);
    if (a == null)
    {
      throw new LDAPPersistException(ERR_SETTER_INFO_METHOD_NOT_ANNOTATED.get(
           m.getName(), c.getName()));
    }

    final LDAPObject o = c.getAnnotation(LDAPObject.class);
    if (o == null)
    {
      throw new LDAPPersistException(ERR_SETTER_INFO_CLASS_NOT_ANNOTATED.get(
           c.getName()));
    }

    containingClass    = c;
    failOnInvalidValue = a.failOnInvalidValue();

    final Type[] params = m.getGenericParameterTypes();
    if (params.length != 1)
    {
      throw new LDAPPersistException(
           ERR_SETTER_INFO_METHOD_DOES_NOT_TAKE_ONE_ARGUMENT.get(m.getName(),
                c.getName()));
    }

    try
    {
      encoder = a.encoderClass().newInstance();
    }
    catch (Exception e)
    {
      debugException(e);
      throw new LDAPPersistException(ERR_SETTER_INFO_CANNOT_GET_ENCODER.get(
           a.encoderClass().getName(), m.getName(), c.getName(),
           getExceptionMessage(e)), e);
    }

    if (! encoder.supportsType(params[0]))
    {
      throw new LDAPPersistException(
           ERR_SETTER_INFO_ENCODER_UNSUPPORTED_TYPE.get(
                encoder.getClass().getName(), m.getName(), c.getName(),
                String.valueOf(params[0])));
    }

    supportsMultipleValues = encoder.supportsMultipleValues(m);
    if (supportsMultipleValues)
    {
      failOnTooManyValues = false;
    }
    else
    {
      failOnTooManyValues = a.failOnTooManyValues();
    }

    final String attrName = a.attribute();
    if ((attrName == null) || (attrName.length() == 0))
    {
      final String methodName = m.getName();
      if (methodName.startsWith("set") && (methodName.length() >= 4))
      {
        attributeName = toInitialLowerCase(methodName.substring(3));
      }
      else
      {
        throw new LDAPPersistException(ERR_SETTER_INFO_CANNOT_INFER_ATTR.get(
             methodName, c.getName()));
      }
    }
    else
    {
      attributeName = attrName;
    }
  }



  /**
   * Retrieves the method with which this object is associated.
   *
   * @return  The method with which this object is associated.
   */
  public Method getMethod()
  {
    return method;
  }



  /**
   * Retrieves the class that is marked with the {@link com.hwlcn.ldap.ldap.sdk.persist.LDAPObject} annotation
   * and contains the associated field.
   *
   * @return  The class that contains the associated field.
   */
  public Class<?> getContainingClass()
  {
    return containingClass;
  }



  /**
   * Indicates whether attempts to initialize an object should fail if the LDAP
   * attribute has a value that cannot be represented in the argument type for
   * the associated method.
   *
   * @return  {@code true} if an exception should be thrown if an LDAP attribute
   *          has a value that cannot be provided as an argument to the
   *          associated method, or {@code false} if the method should not be
   *          invoked.
   */
  public boolean failOnInvalidValue()
  {
    return failOnInvalidValue;
  }



  /**
   * Indicates whether attempts to initialize an object should fail if the
   * LDAP attribute has multiple values but the associated method argument can
   * only hold a single value.  Note that the value returned from this method
   * may be {@code false} even when the annotation has a value of {@code true}
   * if the associated method takes an argument that supports multiple values.
   *
   * @return  {@code true} if an exception should be thrown if an attribute has
   *          too many values to provide to the associated method, or
   *          {@code false} if the first value returned should be provided as an
   *          argument to the associated method.
   */
  public boolean failOnTooManyValues()
  {
    return failOnTooManyValues;
  }



  /**
   * Retrieves the encoder that should be used for the associated method.
   *
   * @return  The encoder that should be used for the associated method.
   */
  public ObjectEncoder getEncoder()
  {
    return encoder;
  }



  /**
   * Retrieves the name of the LDAP attribute used to hold values for the
   * associated method.
   *
   * @return  The name of the LDAP attribute used to hold values for the
   *          associated method.
   */
  public String getAttributeName()
  {
    return attributeName;
  }



  /**
   * Indicates whether the associated method takes an argument that can hold
   * multiple values.
   *
   * @return  {@code true} if the associated method takes an argument that can
   *          hold multiple values, or {@code false} if not.
   */
  public boolean supportsMultipleValues()
  {
    return supportsMultipleValues;
  }



  /**
   * Invokes the setter method on the provided object with the value from the
   * given attribute.
   *
   * @param  o               The object for which to invoke the setter method.
   * @param  e               The entry being decoded.
   * @param  failureReasons  A list to which information about any failures
   *                         may be appended.
   *
   * @return  {@code true} if the decode process was completely successful, or
   *          {@code false} if there were one or more failures.
   */
  boolean invokeSetter(final Object o, final Entry e,
                       final List<String> failureReasons)
  {
    boolean successful = true;

    final Attribute a = e.getAttribute(attributeName);
    if ((a == null) || (! a.hasValue()))
    {
      try
      {
        encoder.setNull(method, o);
      }
      catch (final LDAPPersistException lpe)
      {
        debugException(lpe);
        successful = false;
        failureReasons.add(lpe.getMessage());
      }

      return successful;
    }

    if (failOnTooManyValues && (a.size() > 1))
    {
      successful = false;
      failureReasons.add(ERR_SETTER_INFO_METHOD_NOT_MULTIVALUED.get(
           method.getName(), a.getName(), containingClass.getName()));
    }

    try
    {
      encoder.invokeSetter(method, o, a);
    }
    catch (LDAPPersistException lpe)
    {
      debugException(lpe);
      if (failOnInvalidValue)
      {
        successful = false;
        failureReasons.add(lpe.getMessage());
      }
    }

    return successful;
  }
}
