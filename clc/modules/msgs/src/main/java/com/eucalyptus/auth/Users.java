/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.auth;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.UserProvider;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.records.EventClass;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Tx;

/**
 * Facade for accessing the system configured credential provider.
 * 
 * @author decker
 * @see UserProvider
 */
public class Users {
  private static Logger LOG = Logger.getLogger( Users.class );
  private static UserProvider users;

  public static void setUserProvider( UserProvider provider ) {
    synchronized( Users.class ) {
      LOG.info( "Setting the user provider to: " + provider.getClass( ) );
      users = provider;
    }
  }
  
  public static UserProvider getUserProvider() {
     return users;
  }

  public static User addUser( String userName, String path, boolean skipRegistration, boolean enabled, Map<String, String> info,
                                     boolean createKey, boolean createCert, boolean createPassword, String accountName ) throws AuthException {
    return Users.getUserProvider( ).addUser( userName, path, skipRegistration, enabled, info, createKey, createCert, createPassword, accountName );
  }

  public static void deleteUser( String userName, String accountName, boolean forceDeleteAdmin, boolean recursive ) throws AuthException {
    Users.getUserProvider( ).deleteUser( userName, accountName, forceDeleteAdmin, recursive );
  }
  
  public static User lookupUserByAccessKeyId( String keyId ) throws AuthException {
    return Users.getUserProvider( ).lookupUserByAccessKeyId( keyId );
  }
  
  public static User lookupUserByCertificate( X509Certificate cert ) throws AuthException {
    return Users.getUserProvider( ).lookupUserByCertificate( cert );
  }
 
  public static User lookupUserByName( String userName, String accountName ) throws AuthException {
    return Users.getUserProvider( ).lookupUserByName( userName, accountName );
  }
  
  public static User lookupSystemAdmin( ) throws AuthException {
    return Users.getUserProvider( ).lookupSystemAdmin( );
  }
  
  public static User lookupAccountAdmin( String accountName ) throws AuthException {
    return Users.getUserProvider( ).lookupAccountAdmin( accountName );
  }
  
  public static User lookupUserById( String userId ) throws AuthException {
    return Users.getUserProvider( ).lookupUserById( userId );
  }
  
  public static boolean shareSameAccount( String userId1, String userId2 ) {
    return Users.getUserProvider( ).shareSameAccount( userId1, userId2 );
  }
 
  public static List<User> listAllUsers( ) throws AuthException {
    return Users.getUserProvider( ).listAllUsers( );
  }
  
  public static void addSystemAdmin( ) throws AuthException {
    Users.getUserProvider( ).addSystemAdmin( );
  }
  
  public static void addAccountAdmin( String accountName, String password ) throws AuthException {
    Users.getUserProvider( ).addAccountAdmin( accountName, password );
  }
  
}
