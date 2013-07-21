package com.hwlcn.ldap.util;




@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public enum ThreadSafetyLevel
{

  COMPLETELY_THREADSAFE,

  MOSTLY_THREADSAFE,

  MOSTLY_NOT_THREADSAFE,

  NOT_THREADSAFE,

  INTERFACE_THREADSAFE,

  INTERFACE_NOT_THREADSAFE,

  METHOD_THREADSAFE,

  METHOD_NOT_THREADSAFE
}
