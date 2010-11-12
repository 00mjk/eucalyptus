package com.eucalyptus.auth.principal;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.domain.UserDomain;

/**
 * @author decker
 *
 */
public interface Group extends java.security.acl.Group, Cloneable, UserDomain {

  public String getName( );
  
  public void setName( String name ) throws AuthException;
  
  public String getPath( );
  
  public Boolean isUserGroup( );
  
  public Account getAccount( );
  
}
