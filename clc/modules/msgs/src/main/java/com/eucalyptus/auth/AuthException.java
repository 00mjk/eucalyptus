package com.eucalyptus.auth;

import com.eucalyptus.BaseException;

/**
 * Exception for auth operations.
 * 
 * @author wenye
 *
 */
public class AuthException extends BaseException {

  private static final long serialVersionUID = 1L;
  
  // Auth error types
  public static final String EMPTY_USER_NAME = "Empty user name";
  public static final String EMPTY_USER_ID = "Empty user ID";
  public static final String EMPTY_GROUP_NAME = "Empty group name";
  public static final String EMPTY_GROUP_ID = "Empty group ID";
  public static final String EMPTY_ACCOUNT_NAME = "Empty account name";
  public static final String USER_DELETE_CONFLICT = "User has resources attached and can not be deleted";
  public static final String GROUP_DELETE_CONFLICT = "Group has resources attached and can not be deleted";
  public static final String ACCOUNT_DELETE_CONFLICT = "Account still has groups and can not be deleted";
  public static final String DELETE_ACCOUNT_ADMIN = "Can not delete account admin";
  public static final String DELETE_SYSTEM_ADMIN = "Can not delete system admin account";
  public static final String USER_CREATE_FAILURE = "Can not create user";
  public static final String USER_DELETE_FAILURE = "Can not delete user";
  public static final String GROUP_CREATE_FAILURE = "Can not create group";
  public static final String GROUP_DELETE_FAILURE = "Can not delete group";
  public static final String ACCOUNT_CREATE_FAILURE = "Can not create account";
  public static final String ACCOUNT_DELETE_FAILURE = "Can not delete account";
  public static final String USER_ALREADY_EXISTS = "User already exists";
  public static final String GROUP_ALREADY_EXISTS = "Group already exists";
  public static final String ACCOUNT_ALREADY_EXISTS = "Account already exists";
  public static final String NO_SUCH_USER = "No such user";
  public static final String NO_SUCH_GROUP = "No such group";
  public static final String NO_SUCH_ACCOUNT = "No such account";
  public static final String USER_GROUP_DELETE = "Can not delete user group";
  public static final String NO_SUCH_CERTIFICATE = "No such certificate";
  
  public AuthException( ) {
    super( );
  }
  
  public AuthException( String message, Throwable cause ) {
    super( message, cause );
  }
  
  public AuthException( String message ) {
    super( message );
  }
  
  public AuthException( Throwable cause ) {
    super( cause );
  }
  
}
