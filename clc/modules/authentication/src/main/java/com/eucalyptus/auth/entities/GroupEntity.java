package com.eucalyptus.auth.entities;

import java.io.Serializable;
import java.security.Principal;
import java.util.Enumeration;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.codehaus.janino.Java.ThisReference;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.entities.AbstractPersistent;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * Database group entity.
 * 
 * @author wenye
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_group" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class GroupEntity extends AbstractPersistent implements Group, Serializable {

  @Transient
  private static final long serialVersionUID = 1L;

  @Transient
  private static Logger LOG = Logger.getLogger( GroupEntity.class );
  
  // Group name, not unique since different accounts can have the same group name
  @Column( name = "auth_group_name" )
  String name;
  
  // Group path (prefix to organize group name space, see AWS spec)
  @Column( name = "auth_group_path" )
  String path;
  
  // Indicates if this group is a special user group
  @Column( name = "auth_group_user_group" )
  Boolean userGroup;
  
  // Users in the group
  @ManyToMany( fetch = FetchType.LAZY )
  @JoinTable( name = "auth_group_has_users", joinColumns = { @JoinColumn( name = "auth_group_id" ) }, inverseJoinColumns = @JoinColumn( name = "auth_user_id" ) )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<UserEntity> users;

  // Policies for the group
  @OneToMany( cascade = { CascadeType.ALL }, mappedBy = "group" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<PolicyEntity> policies;
  
  // The owning account
  @ManyToOne
  @JoinColumn( name = "auth_group_owning_account" )
  AccountEntity account;
  
  public GroupEntity( ) {
    this.users = Lists.newArrayList( );
    this.policies = Lists.newArrayList( );
  }
  
  public GroupEntity( String name ) {
    this( );
    this.name = name;
  }
  
  public GroupEntity( Boolean userGroup ) {
    this( );
    this.userGroup = userGroup;
  }
  
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    
    GroupEntity that = ( GroupEntity ) o;    
    if ( !name.equals( that.name ) ) return false;
    
    return true;
  }

  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "Group(" );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "name=" ).append( this.getName( ) ).append( ", " );
    sb.append( "path=" ).append( this.getPath( ) ).append( ", " );
    sb.append( "userGroup=" ).append( this.isUserGroup( ) ).append( ", " );
    sb.append( "account=" ).append( this.getAccount( ).getName( ) ).append( ", " );
    sb.append( "users=[");
    for ( UserEntity u : this.getUsers( ) ) {
      sb.append( u.getName( ) ).append( ' ' );
    }
    sb.append( ']' );
    sb.append( "policies=[\n");
    for ( PolicyEntity p : this.getPolicies( ) ) {
      sb.append( p.getPolicyText( ) ).append( '\n' );
    }
    sb.append( ']' );
    sb.append( ")" );
    return sb.toString( );
  }
  
  @Override
  public boolean addMember( Principal user ) {
    if ( user != null && user instanceof UserEntity ) {
      return this.users.add( ( UserEntity ) user );
    } else {
      LOG.debug( "Invalid user type or empty user" );
      return false;
    }
  }

  @Override
  public boolean isMember( Principal user ) {
    if ( user != null && user instanceof UserEntity ) {
      return this.users.contains( ( UserEntity ) user );
    } else {
      LOG.debug( "Invalid user type or empty user" );
      return false;
    }
  }

  @Override
  public Enumeration<? extends Principal> members( ) {
    return Iterators.asEnumeration( this.users.iterator( ) );
  }

  @Override
  public boolean removeMember( Principal user ) {
    if ( user != null && user instanceof UserEntity ) {
      return this.users.remove( ( UserEntity ) user );
    } else {
      LOG.debug( "Invalid user type or empty user" );
      return false;
    }
  }

  @Override
  public String getName( ) {
    return this.name;
  }

  @Override
  public void setName( String name ) {
    this.name = name;
  }
  
  @Override
  public String getPath( ) {
    return this.path;
  }

  public void setPath( String path ) {
    this.path = path;
  }

  public Account getAccount( ) {
    return this.account;
  }
  
  public void setAccount( AccountEntity account ) {
    this.account = account;
  }
  
  public Boolean isUserGroup( ) {
    return this.userGroup;
  }
  
  public void setUserGroup( Boolean userGroup ) {
    this.userGroup = userGroup;
  }
  
  public List<PolicyEntity> getPolicies( ) {
    return this.policies;
  }
  
  public List<UserEntity> getUsers( ) {
    return this.users;
  }
  
}
