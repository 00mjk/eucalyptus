package com.eucalyptus.component;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collection;
import com.eucalyptus.component.Component.State;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.fsm.HasStateMachine;
import com.eucalyptus.util.fsm.StateMachine;

public interface ServiceConfiguration extends Serializable, HasFullName<ServiceConfiguration>, HasStateMachine<ServiceConfiguration, Component.State, Component.Transition> {
  
  public abstract void setName( String name );
  
  public abstract void setPartition( String partition );
  
  public InetSocketAddress getSocketAddress( );
  
  public abstract String getHostName( );

  public abstract String getSourceHostName( );
  
  public abstract void setHostName( String hostName );

  public abstract void setSourceHostName( String aliasHostName );
  
  public abstract Integer getPort( );
  
  public abstract void setPort( Integer port );
  
  public abstract String getServicePath( );
  
  public abstract void setServicePath( String servicePath );
  
  public abstract URI getUri( );

  public abstract URI getUri( String prefix );
  
  public abstract Boolean isVmLocal( );
  
  public abstract Partition lookupPartition( );
  
  public abstract Component lookupComponent( );

  public abstract StateMachine<ServiceConfiguration, Component.State, Component.Transition> lookupStateMachine( );
  
  public abstract State lookupState( );

  public abstract ComponentId getComponentId( );
  
  public abstract Service lookupService( ) throws NoSuchServiceException;
  
  public abstract ServiceBuilder lookupBuilder( );

  public void fatal( Throwable t );
  
  public void error( Throwable t );
  
  public void urgent( Throwable t );
  
  public void warning( Throwable t );
  
  public void debug( Throwable t );
  
  public void info( Throwable t );

  public Collection<ServiceCheckRecord> lookupDetails( );

  public abstract Boolean isHostLocal( );

  /**
   * @return
   */
  InetAddress getInetAddress( );

}
