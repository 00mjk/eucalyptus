/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
package com.eucalyptus.component.id;

import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentId.FaultLogPrefix;
import com.eucalyptus.component.annotation.AwsServiceName;

/**
 *
 */
@ComponentId.Partition( Eucalyptus.class )
@ComponentId.PublicService
@AwsServiceName( "sts" )
@ComponentId.PolicyVendor( PolicySpec.VENDOR_STS )
@FaultLogPrefix( "cloud" )
public class Tokens extends ComponentId {
  public static Tokens INSTANCE = new Tokens( );

  @Override
  public boolean isPublicService( ) {
    return true;
  }

}
