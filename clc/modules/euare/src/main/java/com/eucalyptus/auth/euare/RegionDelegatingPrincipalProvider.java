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

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Nonnull;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.euare.common.identity.Identity;
import com.eucalyptus.auth.euare.persist.DatabasePrincipalProvider;
import com.eucalyptus.auth.api.PrincipalProvider;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurationManager;
import com.eucalyptus.auth.euare.identity.region.RegionConfigurations;
import com.eucalyptus.auth.euare.identity.region.RegionInfo;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.InstanceProfile;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.SecurityTokenContent;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Either;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.NonNullFunction;
import com.eucalyptus.util.async.AsyncExceptions;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 *
 */
public class RegionDelegatingPrincipalProvider implements PrincipalProvider {

  private final PrincipalProvider localProvider = new DatabasePrincipalProvider( );
  private final RegionConfigurationManager regionConfigurationManager = new RegionConfigurationManager( );

  @Override
  public UserPrincipal lookupPrincipalByUserId( final String userId, final String nonce ) throws AuthException {
    return regionDispatchByIdentifier( userId, new NonNullFunction<PrincipalProvider, UserPrincipal>() {
      @Nonnull
      @Override
      public UserPrincipal apply( final PrincipalProvider principalProvider ) {
        try {
          return principalProvider.lookupPrincipalByUserId( userId, nonce );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public UserPrincipal lookupPrincipalByRoleId( final String roleId, final String nonce ) throws AuthException {
    return regionDispatchByIdentifier( roleId, new NonNullFunction<PrincipalProvider, UserPrincipal>() {
      @Nonnull
      @Override
      public UserPrincipal apply( final PrincipalProvider principalProvider ) {
        try {
          return principalProvider.lookupPrincipalByRoleId( roleId, nonce );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public UserPrincipal lookupPrincipalByAccessKeyId( final String keyId, final String nonce ) throws AuthException {
    return regionDispatchByIdentifier( keyId, new NonNullFunction<PrincipalProvider, UserPrincipal>() {
      @Nonnull
      @Override
      public UserPrincipal apply( final PrincipalProvider principalProvider ) {
        try {
          return principalProvider.lookupPrincipalByAccessKeyId( keyId, nonce );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public UserPrincipal lookupPrincipalByCertificateId( final String certificateId ) throws AuthException {
    return regionDispatchAndReduce( new NonNullFunction<PrincipalProvider, Either<AuthException, UserPrincipal>>() {
      @Nonnull
      @Override
      public Either<AuthException, UserPrincipal> apply( final PrincipalProvider principalProvider ) {
        try {
          return Either.right( principalProvider.lookupPrincipalByCertificateId( certificateId ) );
        } catch ( AuthException e ) {
          return Either.left( e );
        }
      }
    } );
  }

  @Override
  public UserPrincipal lookupPrincipalByCanonicalId( final String canonicalId ) throws AuthException {
    return regionDispatchAndReduce( new NonNullFunction<PrincipalProvider, Either<AuthException, UserPrincipal>>() {
      @Nonnull
      @Override
      public Either<AuthException, UserPrincipal> apply( final PrincipalProvider principalProvider ) {
        try {
          return Either.right( principalProvider.lookupPrincipalByCanonicalId( canonicalId ) );
        } catch ( AuthException e ) {
          return Either.left( e );
        }
      }
    } );
  }

  @Override
  public UserPrincipal lookupPrincipalByAccountNumber( final String accountNumber ) throws AuthException {
    return regionDispatchByAccountNumber( accountNumber, new NonNullFunction<PrincipalProvider, UserPrincipal>() {
      @Nonnull
      @Override
      public UserPrincipal apply( final PrincipalProvider principalProvider ) {
        try {
          return principalProvider.lookupPrincipalByAccountNumber( accountNumber );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public UserPrincipal lookupPrincipalByAccountNumberAndUsername(
      final String accountNumber,
      final String name
  ) throws AuthException {
    return regionDispatchByAccountNumber( accountNumber, new NonNullFunction<PrincipalProvider, UserPrincipal>() {
      @Nonnull
      @Override
      public UserPrincipal apply( final PrincipalProvider principalProvider ) {
        try {
          return principalProvider.lookupPrincipalByAccountNumberAndUsername( accountNumber, name );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public UserPrincipal lookupCachedPrincipalByUserId( final String userId, final String nonce ) throws AuthException {
    return lookupPrincipalByUserId( userId, nonce );
  }

  @Override
  public UserPrincipal lookupCachedPrincipalByRoleId( final String roleId, final String nonce ) throws AuthException {
    return lookupPrincipalByRoleId( roleId, nonce );
  }

  @Override
  public UserPrincipal lookupCachedPrincipalByAccessKeyId( final String keyId, final String nonce ) throws AuthException {
    return lookupPrincipalByAccessKeyId( keyId, nonce );
  }

  @Override
  public UserPrincipal lookupCachedPrincipalByCertificateId( final String certificateId ) throws AuthException {
    return lookupPrincipalByCertificateId( certificateId );
  }

  @Override
  public UserPrincipal lookupCachedPrincipalByAccountNumber( final String accountNumber ) throws AuthException {
    return lookupPrincipalByAccountNumber( accountNumber );
  }

  @Override
  public AccountIdentifiers lookupAccountIdentifiersByAlias( final String alias ) throws AuthException {
    return regionDispatchAndReduce( new NonNullFunction<PrincipalProvider, Either<AuthException, AccountIdentifiers>>() {
      @Nonnull
      @Override
      public Either<AuthException, AccountIdentifiers> apply( final PrincipalProvider principalProvider ) {
        try {
          return Either.right( principalProvider.lookupAccountIdentifiersByAlias( alias ) );
        } catch ( AuthException e ) {
          return Either.left( e );
        }
      }
    } );
  }

  @Override
  public AccountIdentifiers lookupAccountIdentifiersByCanonicalId( final String canonicalId ) throws AuthException {
    return regionDispatchAndReduce( new NonNullFunction<PrincipalProvider, Either<AuthException,AccountIdentifiers>>( ) {
      @Nonnull
      @Override
      public Either<AuthException,AccountIdentifiers> apply( final PrincipalProvider principalProvider ) {
        try {
          return Either.right( principalProvider.lookupAccountIdentifiersByCanonicalId( canonicalId ) );
        } catch ( AuthException e ) {
          return Either.left( e );
        }
      }
    } );
  }

  @Override
  public AccountIdentifiers lookupAccountIdentifiersByEmail( final String email ) throws AuthException {
    return regionDispatchAndReduce( new NonNullFunction<PrincipalProvider, Either<AuthException,AccountIdentifiers>>( ) {
      @Nonnull
      @Override
      public Either<AuthException,AccountIdentifiers> apply( final PrincipalProvider principalProvider ) {
        try {
          return Either.right( principalProvider.lookupAccountIdentifiersByEmail( email ) );
        } catch ( AuthException e ) {
          return Either.left( e );
        }
      }
    } );
  }

  @Override
  public List<AccountIdentifiers> listAccountIdentifiersByAliasMatch( final String aliasExpression ) throws AuthException {
    return regionDispatchAndReduce( new NonNullFunction<PrincipalProvider, Either<AuthException,List<AccountIdentifiers>>>( ) {
      @Nonnull
      @Override
      public Either<AuthException,List<AccountIdentifiers>> apply( final PrincipalProvider principalProvider ) {
        try {
          return Either.right( principalProvider.listAccountIdentifiersByAliasMatch( aliasExpression ) );
        } catch ( AuthException e ) {
          return Either.left( e );
        }
      }
    }, Lists.<AccountIdentifiers>newArrayList( ), CollectionUtils.<AccountIdentifiers,List<AccountIdentifiers>>addAll( ) );
  }

  @Override
  public InstanceProfile lookupInstanceProfileByName( final String accountNumber, final String name ) throws AuthException {
    return regionDispatchByAccountNumber( accountNumber, new NonNullFunction<PrincipalProvider, InstanceProfile>() {
      @Nonnull
      @Override
      public InstanceProfile apply( final PrincipalProvider principalProvider ) {
        try {
          return principalProvider.lookupInstanceProfileByName( accountNumber, name );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public Role lookupRoleByName( final String accountNumber, final String name ) throws AuthException {
    return regionDispatchByAccountNumber( accountNumber, new NonNullFunction<PrincipalProvider, Role>() {
      @Nonnull
      @Override
      public Role apply( final PrincipalProvider principalProvider ) {
        try {
          return principalProvider.lookupRoleByName( accountNumber, name );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public SecurityTokenContent decodeSecurityToken( final String accessKeyIdentifier,
                                                   final String securityToken ) throws AuthException {
    return regionDispatchByIdentifier( accessKeyIdentifier, new NonNullFunction<PrincipalProvider, SecurityTokenContent>() {
      @Nonnull
      @Override
      public SecurityTokenContent apply( final PrincipalProvider principalProvider ) {
        try {
          return principalProvider.decodeSecurityToken( accessKeyIdentifier, securityToken );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public void reserveGlobalName( final String namespace,
                                 final String name,
                                 final Integer duration,
                                 final String clientToken ) throws AuthException {
    final int numberOfRegions = Iterables.size( regionConfigurationManager.getRegionInfos( ) );
    final Integer successes = numberOfRegions <= 1 ?
        1 : // skip if only a local region
        regionDispatchAndReduce( new NonNullFunction<PrincipalProvider, Either<AuthException,String>>( ) {
          @Nonnull
          @Override
          public Either<AuthException,String> apply( final PrincipalProvider identityProvider ) {
            try {
              identityProvider.reserveGlobalName( namespace, name, duration, clientToken );
              return Either.right( "" );
            } catch ( AuthException e ) {
              if ( AsyncExceptions.isWebServiceErrorCode( e, AuthException.CONFLICT ) ||
                  AuthException.CONFLICT.equals( e.getMessage( ) ) ) {
                return Either.left( e );
              } else {
                throw Exceptions.toUndeclared( e );
              }
            }
          }
        }, 0, CollectionUtils.<String>count( Predicates.notNull( ) ) );
    if ( successes < numberOfRegions ) {
      throw new AuthException( AuthException.CONFLICT );
    }
  }

  @Override
  public X509Certificate getCertificateByAccountNumber( final String accountNumber ) throws AuthException {
    return regionDispatchByAccountNumber( accountNumber, new NonNullFunction<PrincipalProvider, X509Certificate>() {
      @Nonnull
      @Override
      public X509Certificate apply( final PrincipalProvider principalProvider ) {
        try {
          return principalProvider.getCertificateByAccountNumber( accountNumber );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  @Override
  public X509Certificate signCertificate(
      final String accountNumber,
      final RSAPublicKey publicKey,
      final String principal,
      final int expiryInDays
  ) throws AuthException {
    return regionDispatchByAccountNumber( accountNumber, new NonNullFunction<PrincipalProvider, X509Certificate>() {
      @Nonnull
      @Override
      public X509Certificate apply( final PrincipalProvider principalProvider ) {
        try {
          return principalProvider.signCertificate( accountNumber, publicKey, principal, expiryInDays );
        } catch ( AuthException e ) {
          throw Exceptions.toUndeclared( e );
        }
      }
    } );
  }

  private <R> R regionDispatchByIdentifier(
      final String identifier,
      final NonNullFunction<PrincipalProvider, R> invoker ) throws AuthException {
    return regionDispatch( regionConfigurationManager.getRegionByIdentifier( identifier ), invoker );
  }

  private <R> R regionDispatchByAccountNumber(
      final String accountNumber,
      final NonNullFunction<PrincipalProvider, R> invoker ) throws AuthException {
    return regionDispatch( regionConfigurationManager.getRegionByAccountNumber( accountNumber ), invoker );
  }

  private <R> R regionDispatch(
      final Optional<RegionInfo> regionInfo,
      final NonNullFunction<PrincipalProvider, R> invoker
  ) throws AuthException {
    try {
      if ( regionInfo.isPresent( ) &&
          !RegionConfigurations.getRegionName( ).asSet( ).contains( regionInfo.get( ).getName( ) ) ) {
        final Optional<Set<String>> endpoints = regionInfo.transform( RegionInfo.serviceEndpoints( "identity" ) );
        if ( endpoints.isPresent( ) && !endpoints.get( ).isEmpty( ) ) {
          final PrincipalProvider remoteProvider = new RemotePrincipalProvider( endpoints.get( ) );
          return invoker.apply( remoteProvider );
        }
        return invoker.apply( localProvider );
      } else {
        return invoker.apply( localProvider );
      }
    } catch ( final RuntimeException e ) {
      Exceptions.findAndRethrow( e, AuthException.class );
      throw e;
    }
  }

  private <R> R regionDispatchAndReduce(
      final NonNullFunction<PrincipalProvider, Either<AuthException,R>> invoker
  ) throws AuthException {
    return regionDispatchAndReduce( invoker, null, CollectionUtils.<R>unique( ) );
  }

  private <R,I> I regionDispatchAndReduce(
      final NonNullFunction<PrincipalProvider, Either<AuthException,R>> invoker,
      final I initial,
      final Function<I,Function<R,I>> reducer
  ) throws AuthException {
    try {
      final Iterable<RegionInfo> regionInfos = regionConfigurationManager.getRegionInfos( );
      final List<Either<AuthException,R>> regionResults = Lists.newArrayList( );
      regionResults.add( invoker.apply( localProvider ) );
      if ( !Iterables.isEmpty( regionInfos ) ) {
        final List<Future<Either<AuthException,R>>> regionResultFutures = Lists.newArrayList( );
        withRegions:
        for ( final RegionInfo regionInfo : regionInfos ) {
          if ( !RegionConfigurations.getRegionName( ).asSet( ).contains( regionInfo.getName( ) ) ) {
            for ( final RegionInfo.RegionService service : regionInfo.getServices( ) ) {
              if ( "identity".equals( service.getType( ) ) ) {
                final PrincipalProvider remoteProvider = new RemotePrincipalProvider( service.getEndpoints( ) );
                regionResultFutures.add( Threads.enqueue(
                    Identity.class,
                    RegionDelegatingPrincipalProvider.class,
                    FUtils.cpartial( invoker, remoteProvider ) ) );
                continue withRegions;
              }
            }
          }
        }
        for ( final Future<Either<AuthException,R>> future : regionResultFutures ) {
          try {
            regionResults.add( future.get( ) );
          } catch ( final InterruptedException e ) {
            throw new AuthException( "Interrupted" );
          } catch ( final ExecutionException e ) {
            throw new RuntimeException( e ); // Any AuthException caught and unwrapped below
          }
        }
      }
      final Iterable<R> successResults = Optional.presentInstances( Iterables.transform(
          regionResults,
          Either.<AuthException,R>rightOption( ) ) );
      if ( Iterables.size( successResults ) > 0 ) {
        return CollectionUtils.reduce( successResults, initial, reducer );
      }
      throw Iterables.get(
          Optional.presentInstances( Iterables.transform( regionResults, Either.<AuthException,R>leftOption( ) ) ),
          0 );
    } catch ( final RuntimeException e ) {
      Exceptions.findAndRethrow( e, AuthException.class );
      throw e;
    }

  }
}
