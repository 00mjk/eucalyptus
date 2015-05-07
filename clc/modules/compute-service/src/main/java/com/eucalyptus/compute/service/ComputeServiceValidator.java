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
package com.eucalyptus.compute.service;

import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import java.util.Map;
import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.compute.common.ComputeMessage;
import com.eucalyptus.compute.common.internal.account.ComputeAccounts;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.MessageValidation;

/**
 *
 */
public class ComputeServiceValidator {

  public ComputeMessage validate( final ComputeMessage request ) throws EucalyptusCloudException {
    // Authorization check
    final AuthContextSupplier user = Contexts.lookup().getAuthContext( );
    if ( !Permissions.perhapsAuthorized( PolicySpec.VENDOR_EC2, getIamActionByMessageType( request ), user ) ) {
      throw new ComputeServiceAuthorizationException( "UnauthorizedOperation", "You are not authorized to perform this operation." );
    }

    // Validation
    if ( request instanceof MessageValidation.ValidatableMessage ) {
      final Map<String, String> validationErrorsByField = ((MessageValidation.ValidatableMessage)request).validate( );
      if ( !validationErrorsByField.isEmpty() ) {
        final String error = validationErrorsByField.values().iterator().next();
        throw new ComputeServiceClientException( "InvalidParameterValue", error );
      }
    }

    // Account setup
    try {
      ComputeAccounts.ensureInitialized( user.get( ).getAccountNumber( ) );
    } catch ( AuthException e ) {
      throw new EucalyptusCloudException( e );
    }

    return request;
  }

}
