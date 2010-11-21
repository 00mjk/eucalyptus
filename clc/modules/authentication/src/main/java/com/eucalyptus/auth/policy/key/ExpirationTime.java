package com.eucalyptus.auth.policy.key;

import java.util.List;
import net.sf.json.JSONException;
import com.eucalyptus.auth.Contract;
import com.eucalyptus.auth.policy.PolicySpecConstants;
import com.eucalyptus.auth.policy.condition.ConditionOp;
import com.eucalyptus.auth.policy.condition.DateEquals;

public class ExpirationTime extends ContractKey {

  private static final String KEY = Keys.EC2_EXPIRATIONTIME;
  
  private static final String ACTION_RUNINSTANCES = PolicySpecConstants.VENDOR_EC2 + ":" + PolicySpecConstants.EC2_RUNINSTANCES;
  
  @Override
  public void validateConditionType( Class<? extends ConditionOp> conditionClass ) throws JSONException {
    if ( DateEquals.class != conditionClass ) {
      throw new JSONException( KEY + " is not allowed in condition " + conditionClass.getName( ) + ". DateEquals is required." );
    }
  }

  @Override
  public void validateValueType( String value ) throws JSONException {
    KeyUtils.validateDateValue( value, KEY );
  }

  @Override
  public Contract getContract( String[] values ) {
    return new SingleValueContract( this.getClass( ).getName( ), values[0] );
  }

  @Override
  public boolean canApply( String action, String resourceType ) {
    return ACTION_RUNINSTANCES.equals( action );
  }
  
}
