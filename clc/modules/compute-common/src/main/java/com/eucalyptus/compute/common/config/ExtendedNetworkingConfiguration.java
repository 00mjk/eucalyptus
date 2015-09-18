package com.eucalyptus.compute.common.config;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;

import java.util.regex.Pattern;

/**
 * Created by zhill on 8/13/15.
 */
@ConfigurableClass(root="cloud.network", description="Global cloud network configuration")
public class ExtendedNetworkingConfiguration {

  @ConfigurableField( description = "Comma delimited list of protocol numbers to support in EDGE mode for security group rules beyond the EC2-classic defaults (tcp,udp,icmp)",
      initial="",
      changeListener = ProtocolListPropertyChangeListener.class)
  public static String EC2_CLASSIC_ADDITIONAL_PROTOCOLS_ALLOWED = "";

  public static boolean isProtocolInExceptionList(Integer number) {
    return number != null && Iterables.contains(
        Splitter.on(',').trimResults()
            .split(EC2_CLASSIC_ADDITIONAL_PROTOCOLS_ALLOWED), String.valueOf(number));
  }

  /**
   * Enforces a format of a comma delimited list of integers between 0-255
   */
  public static class ProtocolListPropertyChangeListener implements PropertyChangeListener<String> {
    static Pattern commaListPattern = Pattern.compile(
        "\\s*(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\s*,\\s*)*([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])?\\s*");
    @Override
    public void fireChange( final ConfigurableProperty property,
                     final String newValue ) throws ConfigurablePropertyException {
      if ( !Strings.isNullOrEmpty(newValue) ) {
        if(!commaListPattern.matcher(newValue).matches()) {
          throw new ConfigurablePropertyException("Invalid format. Must conform to regex: " + commaListPattern.toString());
        }
      }
    }
  }
}
