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
package com.eucalyptus.auth.euare;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.persist.DatabaseAuthUtils;
import com.eucalyptus.auth.api.IdentityProvider;
import com.eucalyptus.auth.euare.common.identity.Account;
import com.eucalyptus.auth.euare.common.identity.DecodeSecurityTokenResponseType;
import com.eucalyptus.auth.euare.common.identity.DecodeSecurityTokenResult;
import com.eucalyptus.auth.euare.common.identity.DecodeSecurityTokenType;
import com.eucalyptus.auth.euare.common.identity.DescribeAccountsResponseType;
import com.eucalyptus.auth.euare.common.identity.DescribeAccountsType;
import com.eucalyptus.auth.euare.common.identity.DescribeInstanceProfileResponseType;
import com.eucalyptus.auth.euare.common.identity.DescribeInstanceProfileResult;
import com.eucalyptus.auth.euare.common.identity.DescribeInstanceProfileType;
import com.eucalyptus.auth.euare.common.identity.DescribePrincipalResponseType;
import com.eucalyptus.auth.euare.common.identity.DescribePrincipalType;
import com.eucalyptus.auth.euare.common.identity.DescribeRoleResponseType;
import com.eucalyptus.auth.euare.common.identity.DescribeRoleResult;
import com.eucalyptus.auth.euare.common.identity.DescribeRoleType;
import com.eucalyptus.auth.euare.common.identity.Identity;
import com.eucalyptus.auth.euare.common.identity.IdentityMessage;
import com.eucalyptus.auth.euare.common.identity.Policy;
import com.eucalyptus.auth.euare.common.identity.Principal;
import com.eucalyptus.auth.euare.common.identity.SecurityToken;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.AccountIdentifiersImpl;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.InstanceProfile;
import com.eucalyptus.auth.principal.PolicyScope;
import com.eucalyptus.auth.principal.PolicyVersion;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.SecurityTokenContent;
import com.eucalyptus.auth.principal.SecurityTokenContentImpl;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.EphemeralConfiguration;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.util.NonNullFunction;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.eucalyptus.util.async.AsyncRequests;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 *
 */
public class RemoteIdentityProvider implements IdentityProvider {

  private final Set<String> endpoints;

  public RemoteIdentityProvider( final Set<String> endpoints ) {
    this.endpoints = endpoints;
  }

  @Override
  public UserPrincipal lookupPrincipalByUserId( final String userId, final String nonce ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setUserId( userId );
    request.setNonce( nonce );
    return resultFor( request );
  }

  @Override
  public UserPrincipal lookupPrincipalByRoleId( final String roleId, final String nonce ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setRoleId( roleId );
    request.setNonce( nonce );
    return resultFor( request );
  }

  @Override
  public UserPrincipal lookupPrincipalByAccessKeyId( final String keyId, final String nonce ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setAccessKeyId( keyId );
    request.setNonce( nonce );
    return resultFor( request );
  }

  @Override
  public UserPrincipal lookupPrincipalByCertificateId( final String certificateId ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setCertificateId( certificateId );
    return resultFor( request );
  }

  @Override
  public UserPrincipal lookupPrincipalByCanonicalId( final String canonicalId ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setCanonicalId( canonicalId );
    return resultFor( request );
  }

  @Override
  public UserPrincipal lookupPrincipalByAccountNumber( final String accountNumber ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setAccountId( accountNumber );
    return resultFor( request );
  }

  @Override
  public UserPrincipal lookupPrincipalByAccountNumberAndUsername(
      final String accountNumber,
      final String name
  ) throws AuthException {
    final DescribePrincipalType request = new DescribePrincipalType( );
    request.setAccountId( accountNumber );
    request.setUsername( name );
    return resultFor( request );
  }

  @Override
  public AccountIdentifiers lookupAccountIdentifiersByAlias( final String alias ) throws AuthException {
    final DescribeAccountsType request = new DescribeAccountsType( );
    request.setAlias( alias );
    return resultFor( request );
  }

  @Override
  public AccountIdentifiers lookupAccountIdentifiersByCanonicalId( final String canonicalId ) throws AuthException {
    final DescribeAccountsType request = new DescribeAccountsType( );
    request.setCanonicalId( canonicalId );
    return resultFor( request );
  }

  @Override
  public AccountIdentifiers lookupAccountIdentifiersByEmail( final String email ) throws AuthException {
    final DescribeAccountsType request = new DescribeAccountsType( );
    request.setEmail( email );
    return resultFor( request );
  }

  @Override
  public List<AccountIdentifiers> listAccountIdentifiersByAliasMatch( final String aliasExpression ) throws AuthException {
    final DescribeAccountsType request = new DescribeAccountsType( );
    request.setAliasLike( aliasExpression );
    return resultListFor( request );
  }

  @Override
  public InstanceProfile lookupInstanceProfileByName( final String accountNumber, final String name ) throws AuthException {
    final DescribeInstanceProfileType request = new DescribeInstanceProfileType( );
    request.setAccountId( accountNumber );
    request.setInstanceProfileName( name );
    try {
      final DescribeInstanceProfileResponseType response = send( request );
      final DescribeInstanceProfileResult result = response.getDescribeInstanceProfileResult();
      final EuareResourceName profileErn = (EuareResourceName) Ern.parse( result.getInstanceProfile( ).getInstanceProfileArn( ) );
      final Role role = TypeMappers.transform( result.getRole( ), Role.class );
      return new InstanceProfile( ) {
        @Override public String getAccountNumber( ) { return accountNumber; }
        @Override public String getInstanceProfileId( ) { return result.getInstanceProfile( ).getInstanceProfileId( ); }
        @Override public String getInstanceProfileArn( ) { return result.getInstanceProfile( ).getInstanceProfileArn(); }
        @Nullable
        @Override public Role getRole( ) { return role; }
        @Override public String getName( ) { return profileErn.getName( ); }
        @Override public String getPath( ) { return profileErn.getPath(); }
      };
    } catch ( Exception e ) {
      throw new AuthException( e );
    }
  }

  @Override
  public Role lookupRoleByName( final String accountNumber, final String name ) throws AuthException {
    final DescribeRoleType request = new DescribeRoleType( );
    request.setAccountId( accountNumber );
    request.setRoleName( name );
    try {
      final DescribeRoleResponseType response = send( request );
      final DescribeRoleResult result = response.getDescribeRoleResult();
      return TypeMappers.transform( result.getRole( ), Role.class );
    } catch ( Exception e ) {
      throw new AuthException( e );
    }
  }

  @Override
  public SecurityTokenContent decodeSecurityToken( final String accessKeyIdentifier,
                                                   final String securityToken ) throws AuthException {
    final DecodeSecurityTokenType request = new DecodeSecurityTokenType( );
    request.setAccessKeyId( accessKeyIdentifier );
    request.setSecurityToken( securityToken );
    try {
      final DecodeSecurityTokenResponseType response = send( request );
      final DecodeSecurityTokenResult result = response.getDecodeSecurityTokenResult();
      return TypeMappers.transform( result.getSecurityToken(), SecurityTokenContent.class );
    } catch ( Exception e ) {
      throw new AuthException( e );
    }
  }

  private <R extends IdentityMessage> R send( final IdentityMessage request ) throws Exception {
    final ServiceConfiguration config = new EphemeralConfiguration(
        ComponentIds.lookup( Identity.class ),
        "identity",
        "identity",
        URI.create( endpoints.iterator( ).next( ) ) ); //TODO:STEVE: endpoint handling
    return AsyncRequests.sendSync( config, request );
  }

  private AccountIdentifiers resultFor( final DescribeAccountsType request ) throws AuthException {
    try {
      final DescribeAccountsResponseType response = send( request );
      final List<Account> accounts = response.getDescribeAccountsResult( ).getAccounts( );
      if ( accounts.size( ) != 1 ) {
        throw new AuthException( "Account information not found" );
      }
      return TypeMappers.transform( Iterables.getOnlyElement( accounts ), AccountIdentifiers.class );
    } catch ( AuthException e ) {
      throw e;
    } catch ( Exception e ) {
      throw new AuthException( e );
    }
  }

  private List<AccountIdentifiers> resultListFor( final DescribeAccountsType request ) throws AuthException {
    try {
      final DescribeAccountsResponseType response = send( request );
      final List<Account> accounts = response.getDescribeAccountsResult( ).getAccounts( );
      return Lists.newArrayList( Iterables.transform(
          accounts,
          TypeMappers.lookup( Account.class, AccountIdentifiers.class ) ) );
    } catch ( AuthException e ) {
      throw e;
    } catch ( Exception e ) {
      throw new AuthException( e );
    }
  }

  private UserPrincipal resultFor( final DescribePrincipalType request ) throws AuthException {
    try {
      final DescribePrincipalResponseType response = send( request );
      final Principal principal = response.getDescribePrincipalResult( ).getPrincipal( );
      if ( principal == null ) {
        throw new AuthException( "Invalid identity" );
      }
      final UserPrincipal[] userPrincipalHolder = new UserPrincipal[1];
      final Supplier<UserPrincipal> userPrincipalSupplier = new Supplier<UserPrincipal>() {
        @Override
        public UserPrincipal get() {
          return userPrincipalHolder[0];
        }
      };
      final EuareResourceName ern = (EuareResourceName) EuareResourceName.parse( principal.getArn( ) );
      final ImmutableList<AccessKey> accessKeys = ImmutableList.copyOf(
          Iterables.transform( principal.getAccessKeys( ), accessKeyTransform( userPrincipalSupplier ) ) );
      final ImmutableList<Certificate> certificates = ImmutableList.copyOf(
          Iterables.transform( principal.getCertificates( ), certificateTransform( userPrincipalSupplier ) ) );
      final ImmutableList<PolicyVersion> policies = ImmutableList.copyOf( Iterables.transform(
          principal.getPolicies(),
          TypeMappers.lookup( Policy.class, PolicyVersion.class ) ) );
      return userPrincipalHolder[0] = new UserPrincipal( ) {
        @Nonnull
        @Override
        public String getName( ) {
          return ern.getName();
        }

        @Nonnull
        @Override
        public String getPath() {
          return ern.getPath( );
        }

        @Nonnull
        @Override
        public String getUserId() {
          return principal.getUserId( );
        }

        @Nonnull
        @Override
        public String getAuthenticatedId() {
          return Objects.firstNonNull( principal.getRoleId( ), principal.getUserId( ) );
        }

        @Nonnull
        @Override
        public String getAccountAlias() {
          return principal.getAccountAlias( );
        }

        @Nonnull
        @Override
        public String getAccountNumber() {
          return ern.getNamespace( );
        }

        @Nonnull
        @Override
        public String getCanonicalId() {
          return principal.getCanonicalId( );
        }

        @Override
        public boolean isEnabled() {
          return principal.getEnabled( );
        }

        @Override
        public boolean isAccountAdmin() {
          return principal.getRoleId( ) == null && DatabaseAuthUtils.isAccountAdmin( getName( ) );
        }

        @Override
        public boolean isSystemAdmin() {
          return principal.getRoleId( ) == null && Accounts.isSystemAccount( getAccountAlias( ) );
        }

        @Override
        public boolean isSystemUser() {
          return Accounts.isSystemAccount( getAccountAlias( ) );
        }

        @Nullable
        @Override
        public String getPassword() {
          return principal.getPasswordHash( );
        }

        @Nullable
        @Override
        public Long getPasswordExpires() {
          return principal.getPasswordExpiry( );
        }

        @Nonnull
        @Override
        public List<AccessKey> getKeys( ) {
          return accessKeys;
        }

        @Nonnull
        @Override
        public List<Certificate> getCertificates( ) {
          return certificates;
        }

        @Nonnull
        @Override
        public List<PolicyVersion> getPrincipalPolicies( ) {
          return policies;
        }

        @Override
        public String getToken() {
          return null;
        }
      };
    } catch ( Exception e ) {
      throw new AuthException( e );
    }
  }

  private static NonNullFunction<com.eucalyptus.auth.euare.common.identity.AccessKey,AccessKey> accessKeyTransform(
    final Supplier<UserPrincipal> userPrincipalSupplier
  ) {
    return new NonNullFunction<com.eucalyptus.auth.euare.common.identity.AccessKey,AccessKey>( ) {
      @Nonnull
      @Override
      public AccessKey apply( final com.eucalyptus.auth.euare.common.identity.AccessKey accessKey ) {
        return new AccessKey( ) {
              @Override public Boolean isActive( ) { return true; }
              @Override public void setActive( final Boolean active ) { }
              @Override public String getAccessKey( ) { return accessKey.getAccessKeyId( ); }
              @Override public String getSecretKey( ) { return accessKey.getSecretAccessKey( ); }
              @Override public Date getCreateDate( ) { return null; }
              @Override public UserPrincipal getPrincipal( )  { return userPrincipalSupplier.get( ); }
            };
      }
    };
  }

  private static NonNullFunction<com.eucalyptus.auth.euare.common.identity.Certificate,Certificate> certificateTransform(
      final Supplier<UserPrincipal> userPrincipalSupplier
  ) {
    return new NonNullFunction<com.eucalyptus.auth.euare.common.identity.Certificate,Certificate>( ) {
      @Nonnull
      @Override
      public Certificate apply( final com.eucalyptus.auth.euare.common.identity.Certificate certificate ) {
        return new Certificate( ) {
          @Override public String getCertificateId( ) { return certificate.getCertificateBody( ); }
          @Override public Boolean isActive( ) { return true; }
          @Override public void setActive( final Boolean active ) { }
          @Override public Boolean isRevoked( ) { return false; }
          @Override public String getPem( ) { return certificate.getCertificateBody( ); }
          @Override public X509Certificate getX509Certificate( ) { return null; }
          @Override public Date getCreateDate( ) { return null; }
          @Override public UserPrincipal getPrincipal( ) { return userPrincipalSupplier.get( ); }
        };
      }
    };
  }

  @TypeMapper
  public enum AccountToAccountIdentifiersTransform implements Function<Account,AccountIdentifiers> {
    INSTANCE;

    @Nullable
    @Override
    public AccountIdentifiers apply( final Account account ) {
      return new AccountIdentifiersImpl(
        account.getAccountNumber( ),
        account.getAlias( ),
        account.getCanonicalId( )
      );
    }
  }

  @TypeMapper
  public enum RoleToRoleTransform implements Function<com.eucalyptus.auth.euare.common.identity.Role,Role> {
    INSTANCE;

    @Nullable
    @Override
    public Role apply( final com.eucalyptus.auth.euare.common.identity.Role role ) {
      final EuareResourceName roleErn = (EuareResourceName) Ern.parse( role.getRoleArn( ) );
      final PolicyVersion rolePolicy = TypeMappers.transform( role.getAssumeRolePolicy( ), PolicyVersion.class );
      return new Role( ) {
        @Override public String getAccountNumber( ) { return roleErn.getNamespace( ); }
        @Override public String getRoleId( ) { return role.getRoleId( ); }
        @Override public String getRoleArn( ) { return role.getRoleArn( ); }
        @Override public String getPath( ) { return roleErn.getPath( ); }
        @Override public String getName( ) { return roleErn.getName(); }
        @Override public String getSecret( ) { return role.getSecret(); }
        @Override public PolicyVersion getPolicy( ) { return rolePolicy; }
        @Override public String getDisplayName( ) { return Accounts.getRoleFullName( this ); }
        @Override public OwnerFullName getOwner( ) { return AccountFullName.getInstance( getAccountNumber() ); }
      };
    }
  }

  @TypeMapper
  public enum SecurityTokenToSecurityTokenContentTransform implements Function<SecurityToken,SecurityTokenContent> {
    INSTANCE;

    @Nullable
    @Override
    public SecurityTokenContent apply( final SecurityToken securityToken ) {
      return new SecurityTokenContentImpl(
          Optional.fromNullable( securityToken.getOriginatingAccessKeyId( ) ),
          Optional.fromNullable( securityToken.getOriginatingUserId( ) ),
          Optional.fromNullable( securityToken.getOriginatingRoleId( ) ),
          securityToken.getNonce( ),
          securityToken.getCreated( ),
          securityToken.getExpires( )
      );
    }
  }

  @TypeMapper
  public enum PolicyToPolicyVersionTransform implements Function<Policy,PolicyVersion> {
    INSTANCE;

    @Nullable
    @Override
    public PolicyVersion apply( final Policy policy ) {
      return new PolicyVersion( ) {
        @Override public String getPolicyVersionId( ) { return policy.getVersionId( ); }
        @Override public String getPolicyName( ) { return policy.getName( ); }
        @Override public PolicyScope getPolicyScope( ) { return PolicyScope.valueOf( policy.getScope( ) ); }
        @Override public String getPolicy( ) { return policy.getPolicy( ); }
        @Override public String getPolicyHash( ) { return policy.getHash( ); }
      };
    }
  }
}
