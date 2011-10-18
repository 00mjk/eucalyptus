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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws;

import java.net.InetAddress;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.ServiceBuilder;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.ServiceTransitions;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Internets;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

@Provides( Empyrean.class )
@RunDuring( Bootstrap.Stage.RemoteServicesInit )
public class ServiceBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( ServiceBootstrapper.class );
  
  static class ServiceBootstrapWorker implements Runnable {
    private final AtomicBoolean             running  = new AtomicBoolean( true );
    private final BlockingQueue<Runnable>   msgQueue = new LinkedBlockingQueue<Runnable>( );
    private final ExecutorService           executor = Executors.newFixedThreadPool( 20 );
    private static final ServiceBootstrapWorker worker   = new ServiceBootstrapWorker( );
    
    private ServiceBootstrapWorker( ) {
      for ( int i = 0; i < 20; i++ ) {
        this.executor.submit( this );
      }
    }
    
    public static void markFinished( ) {
      worker.running.set( false );
    }
    public static void submit( Runnable run ) {
      worker.msgQueue.add( run );
    }
    
    @Override
    public void run( ) {
      while ( !this.msgQueue.isEmpty( ) || this.running.get( ) ) {
        Runnable event;
        try {
          if ( ( event = this.msgQueue.poll( 2000, TimeUnit.MILLISECONDS ) ) != null ) {
            event.run( );
          }
        } catch ( InterruptedException e1 ) {
          Thread.currentThread( ).interrupt( );
          return;
        } catch ( final Throwable e ) {
          LOG.error( e, e );
        }
        LOG.debug( "Shutting down component registration request queue: " + Thread.currentThread( ).getName( ) );
      }
      
    }
  }
  
  enum ShouldLoad implements Predicate<ServiceConfiguration> {
    INSTANCE {
      
      @Override
      public boolean apply( final ServiceConfiguration config ) {
        boolean ret = config.getComponentId( ).isAlwaysLocal( ) || config.isVmLocal( ) || BootstrapArgs.isCloudController( );
        LOG.debug( "ServiceBootstrapper.shouldLoad(" + config.toString( ) + "):" + ret );
        return ret;
      }
    };
  }
  
  @Override
  public boolean load( ) {
    ServiceBootstrapper.execute( new Predicate<ServiceConfiguration>( ) {
      
      @Override
      public boolean apply( final ServiceConfiguration config ) {
        final Component comp = config.lookupComponent( );
        LOG.debug( "load(): " + config );
        try {
          comp.loadService( config ).get( );
          return true;
        } catch ( ServiceRegistrationException ex ) {
          config.error( ex );
          return false;
        } catch ( Exception ex ) {
          Exceptions.trace( "load(): Building service failed: " + Components.describe( comp ), ex );
          config.error( ex );
          return false;
        }
      }
    } );
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    ServiceBootstrapper.execute( new Predicate<ServiceConfiguration>( ) {
      
      @Override
      public boolean apply( final ServiceConfiguration config ) {
        final Component comp = config.lookupComponent( );
        ServiceBootstrapWorker.submit( new Runnable( ) {
          @Override
          public void run( ) {
            Bootstrap.awaitFinished( );
            try {
              ServiceTransitions.pathTo( config, Component.State.NOTREADY ).get( );
              try {
                ServiceTransitions.pathTo( config, Component.State.ENABLED ).get( );
              } catch ( IllegalStateException ex ) {
                LOG.error( ex, ex );
              } catch ( InterruptedException ex ) {
                LOG.error( ex, ex );
              } catch ( ExecutionException ex ) {
                LOG.error( ex, ex );
              }
            } catch ( Exception ex ) {
              LOG.error( ex, ex );
            }
          }
        } );
        return true;
      }
    } );
    ServiceBootstrapWorker.markFinished( );
    return true;
  }
  
  private static void execute( final Predicate<ServiceConfiguration> predicate ) throws NoSuchElementException {
    for ( final ComponentId compId : ComponentIds.list( ) ) {
      Component comp = Components.lookup( compId );
      if ( compId.isRegisterable( ) ) {
        ServiceBuilder<? extends ServiceConfiguration> builder = comp.getBuilder( );
        try {
          for ( ServiceConfiguration config : Iterables.filter( builder.list( ), ShouldLoad.INSTANCE ) ) {
            try {
              predicate.apply( config );
            } catch ( Exception ex ) {
              LOG.error( ex, ex );
            }
          }
        } catch ( ServiceRegistrationException ex ) {
          LOG.error( ex, ex );
        }
      } else if ( comp.hasLocalService( ) ) {
        final ServiceConfiguration config = comp.getLocalServiceConfiguration( );
        if ( config.isVmLocal( ) || ( BootstrapArgs.isCloudController( ) && config.isHostLocal( ) ) ) {
          predicate.apply( config );
        }
      }
    }
  }
  
  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#enable()
   */
  @Override
  public boolean enable( ) throws Exception {
    return true;
  }
  
  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#stop()
   */
  @Override
  public boolean stop( ) throws Exception {
    return true;
  }
  
  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#destroy()
   */
  @Override
  public void destroy( ) throws Exception {}
  
  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#disable()
   */
  @Override
  public boolean disable( ) throws Exception {
    return true;
  }
  
  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#check()
   */
  @Override
  public boolean check( ) throws Exception {
    return true;
  }
}
