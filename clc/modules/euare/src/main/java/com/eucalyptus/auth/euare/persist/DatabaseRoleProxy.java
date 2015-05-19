/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.auth.euare.persist;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Debugging;
import com.eucalyptus.auth.PolicyParseException;
import com.eucalyptus.auth.euare.checker.InvalidValueException;
import com.eucalyptus.auth.euare.checker.ValueChecker;
import com.eucalyptus.auth.euare.checker.ValueCheckerFactory;
import com.eucalyptus.auth.euare.persist.entities.InstanceProfileEntity;
import com.eucalyptus.auth.euare.persist.entities.PolicyEntity;
import com.eucalyptus.auth.euare.persist.entities.RoleEntity;
import com.eucalyptus.auth.euare.principal.EuareAccount;
import com.eucalyptus.auth.euare.principal.EuareRole;
import com.eucalyptus.auth.policy.PolicyParser;
import com.eucalyptus.auth.policy.PolicyPolicy;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.euare.principal.EuareInstanceProfile;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.PolicyVersions;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.Tx;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;

/**
 * Role implementation backed by RoleEntity
 */
public class DatabaseRoleProxy implements EuareRole {

  private static final long serialVersionUID = 1L;

  private static Logger LOG = Logger.getLogger( DatabaseRoleProxy.class );

  private static final ValueChecker POLICY_NAME_CHECKER = ValueCheckerFactory.createPolicyNameChecker( );

  private RoleEntity delegate;
  private transient Supplier<String> accountNumberSupplier =
      DatabaseAuthUtils.getAccountNumberSupplier( this );

  public DatabaseRoleProxy( RoleEntity delegate ) {
    this.delegate = delegate;
  }

  @Override
  public String getDisplayName() {
    return Accounts.getRoleFullName( this );
  }

  @Override
  public OwnerFullName getOwner() {
    try {
      return AccountFullName.getInstance( getAccount().getAccountNumber() );
    } catch ( AuthException e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  @Override
  public String toString( ) {
    final StringBuilder sb = new StringBuilder( );
    try {
      DatabaseAuthUtils.invokeUnique( RoleEntity.class, "roleId", this.delegate.getRoleId(), new Tx<RoleEntity>( ) {
        @Override
        public void fire( RoleEntity t ) {
          sb.append( t.toString( ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to toString for " + this.delegate );
    }
    return sb.toString();
  }

  @Override
  public String getRoleId() {
    return this.delegate.getRoleId();
  }

  @Override
  public String getRoleArn( ) throws AuthException {
    return Accounts.getRoleArn( this );
  }

  @Override
  public String getName() {
    return this.delegate.getName();
  }

  @Override
  public String getPath() {
    return this.delegate.getPath();
  }

  @Override
  public String getSecret() {
    return this.delegate.getSecret();
  }

  @Override
  public PolicyVersion getPolicy() {
    try {
      return PolicyVersions.policyVersion( PolicyScope.Resource, getRoleArn( ) ).apply( getAssumeRolePolicy() );
    } catch ( Exception e ) {
      throw Exceptions.toUndeclared( e );
    }
  }

  @Override
  public Policy getAssumeRolePolicy() throws AuthException {
    final List<Policy> results = Lists.newArrayList();
    dbCallback( "getAssumeRolePolicy", new Callback<RoleEntity>() {
      @Override
      public void fire( final RoleEntity roleEntity ) {
        results.add( new DatabasePolicyProxy( roleEntity.getAssumeRolePolicy() ) );
      }
    } );
    return results.get( 0 );
  }

  @Override
  public Policy setAssumeRolePolicy( final String policy ) throws AuthException, PolicyParseException {
    final PolicyPolicy parsedPolicy = PolicyParser.getResourceInstance().parse( policy );
    final PolicyEntity policyEntity = PolicyEntity.create( "assume-role-policy-for-" + getRoleId(), parsedPolicy.getPolicyVersion( ), policy );
    try ( final TransactionResource db = Entities.transactionFor( RoleEntity.class ) ) {
      final RoleEntity roleEntity = getRoleEntity( );
      // Due to https://hibernate.onjira.com/browse/HHH-6484 we must explicitly delete the old policy
      final PolicyEntity oldAssumeRolePolicy = roleEntity.getAssumeRolePolicy();
      roleEntity.setAssumeRolePolicy( policyEntity );
      Entities.delete( oldAssumeRolePolicy );
      final PolicyEntity persistedPolicyEntity = Entities.persist( policyEntity );
      db.commit( );
      return new DatabasePolicyProxy( persistedPolicyEntity );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to set assume role policy for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to set assume role policy", e );
    }
  }

  @Override
  public Date getCreationTimestamp() {
    return delegate.getCreationTimestamp();
  }

  @Override
  public String getAccountNumber() throws AuthException {
    return DatabaseAuthUtils.extract( accountNumberSupplier );
  }

  public EuareAccount getAccount() throws AuthException {
    final List<EuareAccount> results = Lists.newArrayList();
    dbCallback( "getAccount", new Callback<RoleEntity>() {
      @Override
      public void fire( final RoleEntity roleEntity ) {
        results.add( new DatabaseAccountProxy( roleEntity.getAccount() ) );
      }
    } );
    return results.get( 0 );
  }

  @Override
  public List<Policy> getPolicies() throws AuthException {
    final List<Policy> results = Lists.newArrayList( );
    try ( final TransactionResource db = Entities.transactionFor( RoleEntity.class ) ) {
      final RoleEntity role = getRoleEntity( );
      for ( final PolicyEntity policyEntity : role.getPolicies( ) ) {
        results.add( new DatabasePolicyProxy( policyEntity ) );
      }
      return results;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get policies for " + this.delegate );
      throw new AuthException( "Failed to get policies", e );
    }
  }

  @Override
  public Policy addPolicy( String name, String policy ) throws AuthException, PolicyParseException {
    return storePolicy( name, policy, /*allowUpdate*/ false );
  }

  @Override
  public Policy putPolicy( String name, String policy ) throws AuthException, PolicyParseException {
    return storePolicy( name, policy, /*allowUpdate*/ true );
  }

  private Policy storePolicy( String name, String policy, boolean allowUpdate ) throws AuthException, PolicyParseException {
    check( POLICY_NAME_CHECKER, AuthException.INVALID_NAME, name );
    if ( DatabaseAuthUtils.policyNameinList( name, this.getPolicies( ) ) && !allowUpdate ) {
      Debugging.logError( LOG, null, "Policy name already used: " + name );
      throw new AuthException( AuthException.INVALID_NAME );
    }
    final PolicyPolicy policyPolicy = PolicyParser.getInstance().parse( policy );
    final PolicyEntity parsedPolicy = PolicyEntity.create( name, policyPolicy.getPolicyVersion( ), policy );
    try ( final TransactionResource db = Entities.transactionFor( RoleEntity.class ) ) {
      final RoleEntity roleEntity = getRoleEntity( );
      final PolicyEntity remove = DatabaseAuthUtils.removeNamedPolicy( roleEntity.getPolicies( ), name );
      if ( remove != null ) {
        Entities.delete( remove );
      }
      parsedPolicy.setRole( roleEntity );
      roleEntity.getPolicies( ).add( parsedPolicy );
      final PolicyEntity persistedPolicyEntity = Entities.persist( parsedPolicy );
      db.commit( );
      return new DatabasePolicyProxy( persistedPolicyEntity );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to attach policy for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to attach policy", e );
    }
  }

  @Override
  public void removePolicy( final String name ) throws AuthException {
    if ( Strings.isNullOrEmpty( name ) ) {
      throw new AuthException( AuthException.EMPTY_POLICY_NAME );
    }
    try ( final TransactionResource db = Entities.transactionFor( RoleEntity.class ) ) {
      final RoleEntity roleEntity = getRoleEntity( );
      final PolicyEntity policy = DatabaseAuthUtils.removeNamedPolicy( roleEntity.getPolicies(), name );
      if ( policy != null ) Entities.delete( policy );
      db.commit( );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to remove policy " + name + " in " + this.delegate );
      throw new AuthException( "Failed to remove policy", e );
    }
  }

  @Override
  public List<EuareInstanceProfile> getInstanceProfiles() throws AuthException {
    final List<EuareInstanceProfile> results = Lists.newArrayList( );
    try ( final TransactionResource db = Entities.transactionFor( InstanceProfileEntity.class ) ) {
      @SuppressWarnings( "unchecked" )
      List<InstanceProfileEntity> instanceProfiles = ( List<InstanceProfileEntity> ) Entities
          .createCriteria( InstanceProfileEntity.class )
          .createCriteria( "role" ).add( Restrictions.eq( "name", this.delegate.getName( ) ) )
          .setCacheable( true )
          .list( );
      for ( final InstanceProfileEntity instanceProfile : instanceProfiles ) {
        results.add( new DatabaseInstanceProfileProxy( instanceProfile  ) );
      }
      return results;
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, "Failed to get instance profiles for " + this.delegate.getName( ) );
      throw new AuthException( "Failed to get instance profiles", e );
    }
  }

  private void check( ValueChecker checker, String error, String value ) throws AuthException {
    try {
      checker.check( value );
    } catch ( InvalidValueException e ) {
      Debugging.logError( LOG, e, error + " " + value );
      throw new AuthException( error, e );
    }
  }

  private void dbCallback( final String description,
                           final Callback<RoleEntity> updateCallback ) throws AuthException {
    try {
      DatabaseAuthUtils.invokeUnique( RoleEntity.class, "roleId", getRoleId(), new Tx<RoleEntity>( ) {
        @Override
        public void fire( final RoleEntity roleEntity ) {
          updateCallback.fire( roleEntity );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to " + description + " for " + this.delegate );
      throw new AuthException( e );
    }
  }

  private RoleEntity getRoleEntity( ) throws Exception {
    return DatabaseAuthUtils.getUnique( RoleEntity.class, "roleId", getRoleId() );
  }

  private void readObject( ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject( );
    this.accountNumberSupplier = DatabaseAuthUtils.getAccountNumberSupplier( this );
  }
}
