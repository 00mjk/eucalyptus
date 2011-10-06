/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ComponentService;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.ServiceOperation;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.Topology;
import com.eucalyptus.empyrean.DescribeServicesResponseType;
import com.eucalyptus.empyrean.DescribeServicesType;
import com.eucalyptus.empyrean.DisableServiceResponseType;
import com.eucalyptus.empyrean.DisableServiceType;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.empyrean.EnableServiceResponseType;
import com.eucalyptus.empyrean.EnableServiceType;
import com.eucalyptus.empyrean.ModifyServiceResponseType;
import com.eucalyptus.empyrean.ModifyServiceType;
import com.eucalyptus.empyrean.ServiceId;
import com.eucalyptus.empyrean.ServiceStatusType;
import com.eucalyptus.empyrean.StartServiceResponseType;
import com.eucalyptus.empyrean.StartServiceType;
import com.eucalyptus.empyrean.StopServiceResponseType;
import com.eucalyptus.empyrean.StopServiceType;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

@ComponentService( Empyrean.class )
public class EmpyreanService {
  private static Logger LOG = Logger.getLogger( EmpyreanService.class );
  
  private enum TransitionName {
    START, STOP, ENABLE, DISABLE, RESTART
  }
  
  @ServiceOperation
  public enum ModifyService implements Function<ModifyServiceType, ModifyServiceResponseType> {
    INSTANCE;
    
    @Override
    public ModifyServiceResponseType apply( ModifyServiceType input ) {
      try {
        return EmpyreanService.modifyService( input );
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }
    
  }
  
  @ServiceOperation
  public enum StartService implements Function<StartServiceType, StartServiceResponseType> {
    INSTANCE;
    
    @Override
    public StartServiceResponseType apply( StartServiceType input ) {
      try {
        return EmpyreanService.startService( input );
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }
    
  }
  
  @ServiceOperation
  public enum StopService implements Function<StopServiceType, StopServiceResponseType> {
    INSTANCE;
    
    @Override
    public StopServiceResponseType apply( StopServiceType input ) {
      try {
        return EmpyreanService.stopService( input );
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }
    
  }
  
  @ServiceOperation
  public enum EnableService implements Function<EnableServiceType, EnableServiceResponseType> {
    INSTANCE;
    
    @Override
    public EnableServiceResponseType apply( EnableServiceType input ) {
      try {
        return EmpyreanService.enableService( input );
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }
    
  }
  
  @ServiceOperation
  public enum DisableService implements Function<DisableServiceType, DisableServiceResponseType> {
    INSTANCE;
    
    @Override
    public DisableServiceResponseType apply( DisableServiceType input ) {
      try {
        return EmpyreanService.disableService( input );
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }
    
  }
  
  public static ModifyServiceResponseType modifyService( ModifyServiceType request ) throws Exception {
    ModifyServiceResponseType reply = request.getReply( );
    TransitionName transition = TransitionName.valueOf( request.getState( ).toUpperCase( ) );
    for ( Component comp : Components.list( ) ) {
      ServiceConfiguration a;
      try {
        a = comp.lookupServiceConfiguration( request.getName( ) );
      } catch ( Exception ex1 ) {
        continue;
      }
      Component.State serviceState = a.lookupState( );
      reply.set_return( true );
      try {
        switch ( transition ) {
          case DISABLE:
            switch ( a.lookupState( ) ) {
              case ENABLED:
                Topology.getInstance( ).disable( a ).get( );
                break;
              default:
                return reply;
            }
            break;
          case ENABLE:
            switch ( a.lookupState( ) ) {
              case INITIALIZED:
              case PRIMORDIAL:
              case BROKEN:
              case LOADED:
              case STOPPED:
              case DISABLED:
              case NOTREADY:
                Topology.getInstance( ).enable( a ).get( );
                break;
              case ENABLED:
              default:
                return reply;
            }
            break;
          case STOP:
            switch ( a.lookupState( ) ) {
              case ENABLED:
                Topology.getInstance( ).disable( a ).get( );
              case INITIALIZED:
              case PRIMORDIAL:
              case BROKEN:
              case STOPPED:
              case LOADED:
              case DISABLED:
              case NOTREADY:
                Topology.getInstance( ).stop( a ).get( );
                break;
              default:
                return reply;
            }
            break;
          case START:
            switch ( a.lookupState( ) ) {
              case INITIALIZED:
              case PRIMORDIAL:
              case BROKEN:
              case STOPPED:
              case LOADED:
              case DISABLED:
              case NOTREADY:
                Topology.getInstance( ).start( a ).get( );
                break;
              case ENABLED:
              default:
                return reply;
            }
            break;
          case RESTART:
            switch ( a.lookupState( ) ) {
              case ENABLED:
                Topology.getInstance( ).disable( a ).get( );
              case DISABLED:
              case NOTREADY:
                Topology.getInstance( ).stop( a ).get( );
              case INITIALIZED:
              case PRIMORDIAL:
              case BROKEN:
              case LOADED:
              default:
                Topology.getInstance( ).start( a ).get( );
                break;
            }
            break;
        }
      } catch ( InterruptedException ex ) {
        Thread.currentThread( ).interrupt( );
        throw ex;
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        throw ex;
      }
    }
    return reply;
  }
  
  public static StartServiceResponseType startService( StartServiceType request ) throws Exception {
    StartServiceResponseType reply = request.getReply( );
    for ( ServiceId serviceInfo : request.getServices( ) ) {
      try {
        Component comp = Components.lookup( serviceInfo.getType( ) );
        ServiceConfiguration service = TypeMappers.transform( serviceInfo, ServiceConfiguration.class );
        if ( service.isVmLocal( ) ) {
          try {
            Topology.start( service ).get( );
            reply.getServices( ).add( serviceInfo );
          } catch ( IllegalStateException ex ) {
            LOG.error( ex, ex );
            throw ex;
          } catch ( ExecutionException ex ) {
            LOG.error( ex, ex );
            throw Exceptions.toCatchable( ex.getCause( ) );
          } catch ( InterruptedException ex ) {
            LOG.error( ex, ex );
            Thread.currentThread( ).interrupt( );
            throw ex;
          }
        }
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        throw ex;
      }
    }
    return reply;
  }
  
  public static StopServiceResponseType stopService( StopServiceType request ) throws Exception {
    StopServiceResponseType reply = request.getReply( );
    for ( ServiceId serviceInfo : request.getServices( ) ) {
      try {
        Component comp = Components.lookup( serviceInfo.getType( ) );
        ServiceConfiguration service = TypeMappers.transform( serviceInfo, ServiceConfiguration.class );
        if ( service.isVmLocal( ) ) {
          try {
            Topology.stop( service ).get( );
            reply.getServices( ).add( serviceInfo );
          } catch ( IllegalStateException ex ) {
            LOG.error( ex, ex );
            throw ex;
          } catch ( ExecutionException ex ) {
            LOG.error( ex, ex );
            throw Exceptions.toCatchable( ex.getCause( ) );
          } catch ( InterruptedException ex ) {
            LOG.error( ex, ex );
            Thread.currentThread( ).interrupt( );
            throw ex;
          }
        }
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        throw ex;
      }
    }
    return reply;
  }
  
  public static EnableServiceResponseType enableService( EnableServiceType request ) throws Exception {
    EnableServiceResponseType reply = request.getReply( );
    for ( ServiceId serviceInfo : request.getServices( ) ) {
      try {
        Component comp = Components.lookup( serviceInfo.getType( ) );
        ServiceConfiguration service = TypeMappers.transform( serviceInfo, ServiceConfiguration.class );
        if ( service.isVmLocal( ) ) {
          try {
            Topology.getInstance( ).enable( service ).get( );
            reply.getServices( ).add( serviceInfo );
          } catch ( ServiceRegistrationException ex ) {
            LOG.error( ex, ex );
            throw ex;
          } catch ( IllegalStateException ex ) {
            LOG.error( ex, ex );
            throw ex;
          } catch ( ExecutionException ex ) {
            LOG.error( ex, ex );
            throw Exceptions.toCatchable( ex.getCause( ) );
          } catch ( InterruptedException ex ) {
            LOG.error( ex, ex );
            Thread.currentThread( ).interrupt( );
            throw ex;
          }
        }
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        throw ex;
      }
    }
    return reply;
  }
  
  public static DisableServiceResponseType disableService( DisableServiceType request ) throws Exception {
    DisableServiceResponseType reply = request.getReply( );
    for ( ServiceId serviceInfo : request.getServices( ) ) {
      try {
        Component comp = Components.lookup( serviceInfo.getType( ) );
        ServiceConfiguration service = TypeMappers.transform( serviceInfo, ServiceConfiguration.class );
        if ( service.isVmLocal( ) ) {
          try {
            Topology.getInstance( ).disable( service ).get( );
            reply.getServices( ).add( serviceInfo );
          } catch ( IllegalStateException ex ) {
            LOG.error( ex, ex );
            throw ex;
          } catch ( ExecutionException ex ) {
            LOG.error( ex, ex );
            throw Exceptions.toCatchable( ex.getCause( ) );
          } catch ( InterruptedException ex ) {
            LOG.error( ex, ex );
            Thread.currentThread( ).interrupt( );
            throw ex;
          }
        }
      } catch ( NoSuchElementException ex ) {
        LOG.error( ex, ex );
        throw ex;
      }
    }
    return reply;
  }
  
  static class Filters {
    static Predicate<ServiceConfiguration> partition( final String partition ) {
      return new Predicate<ServiceConfiguration>( ) {
        @Override
        public boolean apply( ServiceConfiguration input ) {
          return partition == null || partition.equals( input.getPartition( ) );
        }
      };
    }
    
    static Predicate<ServiceConfiguration> host( final String host ) {
      return new Predicate<ServiceConfiguration>( ) {
        @Override
        public boolean apply( ServiceConfiguration input ) {
          return host == null || host.equals( input.getHostName( ) );
        }
      };
    }
    
    static Predicate<ServiceConfiguration> state( final Component.State state ) {
      return new Predicate<ServiceConfiguration>( ) {
        @Override
        public boolean apply( ServiceConfiguration input ) {
          try {
            return input.lookupState( ).equals( state );
          } catch ( Exception ex ) {
            return false;
          }
        }
      };
    }
    
    static Predicate<Component> componentType( final ComponentId compId ) {
      return new Predicate<Component>( ) {
        @Override
        public boolean apply( Component input ) {
          return Empyrean.class.equals( compId.getClass( ) ) || input.getComponentId( ).equals( compId );
        }
      };
    }
    
    static Predicate<ServiceConfiguration> listAllOrInternal( final Boolean listAllArg, final Boolean listInternalArg ) {
      final boolean listAll = Boolean.TRUE.equals( listAllArg );
      final boolean listInternal = Boolean.TRUE.equals( listInternalArg );
      return new Predicate<ServiceConfiguration>( ) {
        @Override
        public boolean apply( ServiceConfiguration input ) {
          if ( listAll ) {
            return true;
          } else if ( input.getComponentId( ).isInternal( ) && listInternal && input.getPort( ) == -1
              ? true
                : Internets.testLocal( input.getHostName( ) ) ) {
            return true;
          } else if ( input.getComponentId( ).isUserService( ) ) {
            return true;
          } else if ( input.getComponentId( ).isAdminService( ) ) {
            return true;
          } else if ( input.getComponentId( ).isRegisterable( ) ) {
            return true;
          } else {
            return false;
          }
        }
      };
    }
  }
  
  public enum DescribeService implements Function<DescribeServicesType, DescribeServicesResponseType> {
    INSTANCE;
    
    @Override
    public DescribeServicesResponseType apply( DescribeServicesType input ) {
      try {
        return EmpyreanService.describeService( input );
      } catch ( Exception ex ) {
        throw Exceptions.toUndeclared( ex );
      }
    }
    
  }
  
  public static DescribeServicesResponseType describeService( final DescribeServicesType request ) {
    final DescribeServicesResponseType reply = request.getReply( );
    
    ComponentId compId = ( request.getByServiceType( ) != null )
      ? ComponentIds.lookup( request.getByServiceType( ).toLowerCase( ) )
      : Empyrean.INSTANCE;
    final boolean showEventStacks = Boolean.TRUE.equals( request.getShowEventStacks( ) );
    final boolean showEvents = Boolean.TRUE.equals( request.getShowEvents( ) ) || showEventStacks;
    
    Function<ServiceConfiguration, ServiceStatusType> transformToStatus = ServiceConfigurations.asServiceStatus( showEvents, showEventStacks );
    List<Predicate<ServiceConfiguration>> filters = new ArrayList<Predicate<ServiceConfiguration>>( ) {
      {
        if ( request.getByPartition( ) != null ) {
          Partitions.exists( request.getByPartition( ) );
          this.add( Filters.host( request.getByPartition( ) ) );
        }
        if ( request.getByState( ) != null ) {
          Component.State stateFilter = Component.State.valueOf( request.getByState( ).toUpperCase( ) );
          this.add( Filters.state( stateFilter ) );
        }
        this.add( Filters.host( request.getByHost( ) ) );
        this.add( Filters.listAllOrInternal( request.getListAll( ), request.getListInternal( ) ) );
      }
    };
    Predicate<Component> componentFilter = Filters.componentType( compId );
    Predicate<ServiceConfiguration> configPredicate = Predicates.and( filters );
    
    for ( Component comp : Components.list( ) ) {
      if ( componentFilter.apply( comp ) ) {
        for ( final ServiceConfiguration config : comp.lookupServiceConfigurations( ) ) {
          if ( configPredicate.apply( config ) ) {
            reply.getServiceStatuses( ).add( transformToStatus.apply( config ) );
          }
        }
      }
    }
    return reply;
  }
}
