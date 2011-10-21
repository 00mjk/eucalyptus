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

package com.eucalyptus.ws.server;

import java.net.URI;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentMessages;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.empyrean.ServiceTransitionType;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.records.Logs;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

@ChannelPipelineCoverage( "all" )
public enum SystemChecksHandler implements ChannelUpstreamHandler {
  SERVICE_STATE {
    @Override
    public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
      final MappingHttpRequest request = MappingHttpMessage.extractMessage( e );
      final BaseMessage msg = BaseMessage.extractMessage( e );
      if ( msg != null ) {
        try {
          Class<? extends ComponentId> compClass = ComponentMessages.lookup( msg );
          if ( Topology.isEnabledLocally( compClass ) ) {
            ctx.sendUpstream( e );
          } else {
            this.sendError( ctx, e, compClass, request.getServicePath( ) );
          }
        } catch ( NoSuchElementException ex ) {
          LOG.warn( "Failed to find reverse component mapping for message type: " + msg.getClass( ) );
          ctx.sendUpstream( e );
        } catch ( Exception ex ) {
          Logs.extreme( ).error( ex, ex );
          ctx.sendUpstream( e );
        }
      } else {
        ctx.sendUpstream( e );
      }
    }
    
  },
  MESSAGE_EPOCH {
    @Override
    public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
      final MappingHttpRequest request = MappingHttpMessage.extractMessage( e );
      final BaseMessage msg = BaseMessage.extractMessage( e );
      if ( msg != null ) {
        try {
          if ( msg instanceof ServiceTransitionType ) {
            Topology.touch( ( ServiceTransitionType ) msg );
            ctx.sendUpstream( e );
          } else if ( Topology.check( msg ) ) {
            ctx.sendUpstream( e );
          } else {
            Class<? extends ComponentId> compClass = ComponentMessages.lookup( msg );
            this.sendError( ctx, e, compClass, request.getServicePath( ) );
          }
        } catch ( Exception ex ) {
          Logs.extreme( ).error( ex, ex );
          ctx.sendUpstream( e );
        }
      }
    }
  };
  private static Logger LOG = Logger.getLogger( SystemChecksHandler.class );
  
  protected void sendError( ChannelHandlerContext ctx, ChannelEvent e, Class<? extends ComponentId> compClass, String originalPath ) {
    e.getFuture( ).cancel( );
    HttpResponse response = null;
    if ( !Topology.enabledServices( compClass ).isEmpty( ) ) {
      response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.TEMPORARY_REDIRECT );
      URI serviceUri = ServiceUris.remote( Topology.lookup( compClass ) );
      String redirectUri = serviceUri.toASCIIString( ) + originalPath.replace( serviceUri.getPath( ), "" );
      response.setHeader( HttpHeaders.Names.LOCATION, redirectUri );
    } else {
      response = new DefaultHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND );
    }
    ChannelFuture writeFuture = Channels.future( ctx.getChannel( ) );
    writeFuture.addListener( ChannelFutureListener.CLOSE );
    if ( ctx.getChannel( ).isConnected( ) ) {
      Channels.write( ctx, writeFuture, response );
    }
  }
}
