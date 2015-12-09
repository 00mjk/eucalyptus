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
package com.eucalyptus.objectstorage.policy;

import static com.eucalyptus.auth.policy.PolicySpec.S3_DELETEOBJECTVERSION;
import static com.eucalyptus.auth.policy.PolicySpec.S3_GETOBJECTVERSION;
import static com.eucalyptus.auth.policy.PolicySpec.S3_GETOBJECTVERSIONACL;
import static com.eucalyptus.auth.policy.PolicySpec.S3_PUTOBJECTVERSIONACL;
import static com.eucalyptus.auth.policy.PolicySpec.VENDOR_S3;
import static com.eucalyptus.auth.policy.PolicySpec.qualifiedName;
import java.util.Set;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.StringConditionOp;
import com.eucalyptus.auth.policy.key.PolicyKey;
import com.google.common.collect.ImmutableSet;
import net.sf.json.JSONException;

/**
 *
 */
@PolicyKey( VersionIdKey.KEY_NAME )
public class VersionIdKey implements ObjectStorageKey {
  static final String KEY_NAME = "s3:versionid";

  private static final Set<String> actions = ImmutableSet.<String>builder( )
      .add( qualifiedName( VENDOR_S3, S3_GETOBJECTVERSION ) )
      .add( qualifiedName( VENDOR_S3, S3_GETOBJECTVERSIONACL ) )
      .add( qualifiedName( VENDOR_S3, S3_PUTOBJECTVERSIONACL ) )
      .add( qualifiedName( VENDOR_S3, S3_DELETEOBJECTVERSION ) )
      .build( );

  @Override
  public String value( ) throws AuthException {
    return ObjectStoragePolicyContext.getVersionId( );
  }

  @Override
  public void validateConditionType( final Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( !StringConditionOp.class.isAssignableFrom( conditionClass ) ) {
      throw new JSONException( KEY_NAME + " is not allowed in condition " + conditionClass.getName( ) + ". String conditions are required." );
    }
  }

  @Override
  public void validateValueType( final String value ) {
  }

  @Override
  public boolean canApply( final String action ) {
    return actions.contains( action );
  }
}
