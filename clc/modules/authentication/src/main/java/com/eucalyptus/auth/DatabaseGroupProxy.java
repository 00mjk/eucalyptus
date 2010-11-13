package com.eucalyptus.auth;

import java.security.Principal;
import java.util.Enumeration;
import java.util.List;
import javax.persistence.EntityManager;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import com.eucalyptus.auth.entities.AccountEntity;
import com.eucalyptus.auth.entities.GroupEntity;
import com.eucalyptus.auth.entities.UserEntity;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.TransactionException;
import com.eucalyptus.util.Transactions;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public class DatabaseGroupProxy implements Group {
  
  private static final long serialVersionUID = 1L;

  private static Logger LOG = Logger.getLogger( DatabaseGroupProxy.class );
  
  private GroupEntity delegate;
  
  public DatabaseGroupProxy( GroupEntity delegate ) {
    this.delegate = delegate;
  }
  
  /**
   * Does not check if the user is already a member.
   */
  @Override
  public boolean addMember( Principal user ) {
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    EntityManager em = db.getEntityManager( );
    try {
      GroupEntity group = em.find( GroupEntity.class, this.delegate.getId( ) );
      UserEntity userEntity = DatabaseAuthProvider.getUniqueUser( db.getSession( ), user.getName( ), this.delegate.getAccount( ).getName( ) );
      group.addMember( userEntity );
      //userEntity.addGroup( group );
      db.commit( );
      return true;
    } catch ( Throwable e ) {
      Debugging.logError( LOG, e, "Failed to add user to group " + this.delegate );
      db.rollback( );
    }
    return false;
  }
  
  @Override
  public boolean isMember( Principal member ) {
    EntityWrapper<UserEntity> db = EntityWrapper.get( UserEntity.class );
    Session session = db.getSession( );
    try {
      Example accountExample = Example.create( new AccountEntity( this.delegate.getAccount( ).getName( ) ) ).enableLike( MatchMode.EXACT );
      Example groupExample = Example.create( new GroupEntity( this.delegate.getName( ) ) ).enableLike( MatchMode.EXACT );
      Example userExample = Example.create( new UserEntity( member.getName( ) ) ).enableLike( MatchMode.EXACT );
      @SuppressWarnings( "unchecked" )
      List<UserEntity> users = ( List<UserEntity> ) session
          .createCriteria( UserEntity.class ).setCacheable( true ).add( userExample )
          .createCriteria( "groups" ).setCacheable( true ).add( groupExample )
          .createCriteria( "account" ).setCacheable( true ).add( accountExample )
          .list( );
      db.commit( );
      return users.size( ) > 0;
    } catch ( Throwable e ) {
      Debugging.logError( LOG, e, "Failed to check membership for group " + this.delegate );
      db.rollback( );
    }
    return false;
  }
  
  @Override
  public Enumeration<? extends Principal> members( ) {
    return Iterators.asEnumeration( this.getUsers( ).iterator( ) );
  }
  
  @Override
  public boolean removeMember( Principal user ) {
    EntityWrapper<GroupEntity> db = EntityWrapper.get( GroupEntity.class );
    EntityManager em = db.getEntityManager( );
    try {
      GroupEntity group = em.find( GroupEntity.class, this.delegate.getId( ) );
      UserEntity userEntity = DatabaseAuthProvider.getUniqueUser( db.getSession( ), user.getName( ), this.delegate.getAccount( ).getName( ) );
      group.removeMember( userEntity );
      //userEntity.getGroups( ).remove( group );
      db.commit( );
      return true;
    } catch ( Throwable e ) {
      Debugging.logError( LOG, e, "Failed to remove user from group " + this.delegate );
      db.rollback( );
    }
    return false;
  }
  
  @Override
  public String getName( ) {
    return this.delegate.getName( );
  }
  
  @Override
  public String getPath( ) {
    return this.delegate.getPath( );
  }

  @Override
  public void setName( final String name ) {
    try {
      Transactions.one( GroupEntity.class, this.delegate.getId( ), new Tx<GroupEntity>( ) {
        public void fire( GroupEntity t ) throws Throwable {
          t.setName( name );
        }
      } );
    } catch ( TransactionException e ) {
      Debugging.logError( LOG, e, "Failed to setName for " + this.delegate );
    }
  }
  
  @Override
  public Account getAccount( ) {
    return new DatabaseAccountProxy( ( AccountEntity ) this.delegate.getAccount( ) );
  }

  @Override
  public Boolean isUserGroup( ) {
    return this.delegate.isUserGroup( );
  }
  
  @Override
  public String toString( ) {
    return this.toString( );
  }

  @Override
  public List<? extends User> getUsers( ) {
    List<DatabaseUserProxy> users = Lists.newArrayList( );
    for ( UserEntity u : this.delegate.getUsers( ) ) {
      users.add( new DatabaseUserProxy( u ) );
    }
    return users;
  }
  
}
