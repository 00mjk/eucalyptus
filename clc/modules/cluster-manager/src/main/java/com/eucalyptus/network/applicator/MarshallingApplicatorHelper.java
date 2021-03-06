/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.network.applicator;

import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.log4j.Logger;
import com.eucalyptus.cluster.common.broadcast.NetworkInfo;
import com.eucalyptus.util.TypedKey;

/**
 *
 */
class MarshallingApplicatorHelper {

  private static final Logger logger = Logger.getLogger( MarshallingApplicatorHelper.class );

  private static final TypedKey<String> MARSHALLED_INFO_KEY = TypedKey.create( "MarshalledNetworkInfo" );

  static void clearMarshalledNetworkInfoCache( final ApplicatorContext context ) {
    context.removeAttribute( MARSHALLED_INFO_KEY );
  }

  static String getMarshalledNetworkInfo( final ApplicatorContext context ) throws ApplicatorException {
    String networkInfo = context.getAttribute( MARSHALLED_INFO_KEY );
    if ( networkInfo == null ) try {
      final NetworkInfo info = context.getNetworkInfo( );
      final JAXBContext jc = JAXBContext.newInstance( NetworkInfo.class.getPackage( ).getName( ) );
      final StringWriter writer = new StringWriter( 8192 );
      jc.createMarshaller().marshal( info, writer );

      networkInfo = writer.toString( );
      if ( logger.isTraceEnabled( ) ) {
        logger.trace( "Broadcasting network information:\n${networkInfo}" );
      }
      context.setAttribute( MARSHALLED_INFO_KEY, networkInfo );
    } catch ( final JAXBException e ) {
      throw new ApplicatorException( "Error marshalling network information", e );
    }
    return networkInfo;
  }

}
