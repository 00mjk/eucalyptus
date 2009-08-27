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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.handlers.soap;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.eucalyptus.ws.EucalyptusRemoteFault;
import com.eucalyptus.ws.MappingHttpMessage;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.binding.Binding;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.google.common.collect.Lists;

import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;

@ChannelPipelineCoverage( "all" )
public class SoapHandler extends MessageStackHandler {
  private static Logger     LOG                              = Logger.getLogger( SoapHandler.class );
  private final SOAPFactory soapFactory                      = OMAbstractFactory.getSOAP11Factory( );

  @Override
  public void incomingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      final MappingHttpMessage message = ( MappingHttpMessage ) event.getMessage( );
      final SOAPEnvelope env = message.getSoapEnvelope( );
      if ( !env.hasFault( ) ) {
        message.setOmMessage( env.getBody( ).getFirstElement( ) );
      } else {
        final SOAPHeader header = env.getHeader( );
        final List<SOAPHeaderBlock> headers = Lists.newArrayList( header.examineAllHeaderBlocks( ) );
        // :: try to get the fault info from the soap header -- hello there? :://
        String action = "ProblemAction";
        String relatesTo = "RelatesTo";
        for ( final SOAPHeaderBlock headerBlock : headers ) {
          if ( action.equals( headerBlock.getLocalName( ) ) ) {
            action = headerBlock.getText( );
          } else if ( relatesTo.equals( headerBlock.getLocalName( ) ) ) {
            relatesTo = headerBlock.getText( );
          }
        }
        // :: process the real fault :://
        final SOAPFault fault = env.getBody( ).getFault( );
        String faultReason = "";
        final Iterator children = fault.getChildElements( );
        while ( children.hasNext( ) ) {
          final OMElement child = ( OMElement ) children.next( );
          faultReason += child.getText( );
        }
        final String faultCode = fault.getCode( ).getText( );
        faultReason = faultReason.replaceAll( faultCode, "" );
        throw new EucalyptusRemoteFault( action, relatesTo, faultCode, faultReason );
      }
    }
  }

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      final MappingHttpMessage httpMessage = ( MappingHttpMessage ) event.getMessage( );
      if( httpMessage.getMessage( ) instanceof EucalyptusErrorMessageType ) {
        EucalyptusErrorMessageType errMsg = (EucalyptusErrorMessageType) httpMessage.getMessage( );
        httpMessage.setSoapEnvelope( Binding.createFault( errMsg.getSource( ), errMsg.getMessage( ), errMsg.getStatusMessage( ) ) );
      } else {
        // :: assert sourceElem != null :://
        httpMessage.setSoapEnvelope( this.soapFactory.getDefaultEnvelope( ) );
        httpMessage.getSoapEnvelope( ).getBody( ).addChild( httpMessage.getOmMessage( ) );
      }
    }
  }

}
