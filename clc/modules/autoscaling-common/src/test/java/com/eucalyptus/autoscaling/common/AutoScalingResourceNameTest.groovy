/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.autoscaling.common

import static org.junit.Assert.*
import org.junit.Test

import static com.eucalyptus.autoscaling.common.AutoScalingResourceName.*

/**
 * Unit tests for Auto Scaling ARNs 
 */
class AutoScalingResourceNameTest {

  /**
   * arn:aws:autoscaling::013765657871:launchConfiguration:6789b01a-a9c9-489f-9d24-ed39533fca61:launchConfigurationName/Test
   */
  @Test
  public void testLaunchConfigurationArn() {
    assertTrue( "Valid resource name", isResourceName().apply( "arn:aws:autoscaling::013765657871:launchConfiguration:6789b01a-a9c9-489f-9d24-ed39533fca61:launchConfigurationName/Test" ) )
    assertFalse( "Short name", isResourceName().apply( "Test" ) )
    AutoScalingResourceName name = parse( "arn:aws:autoscaling::013765657871:launchConfiguration:6789b01a-a9c9-489f-9d24-ed39533fca61:launchConfigurationName/Test" )
    assertEquals( "Name", "arn:aws:autoscaling::013765657871:launchConfiguration:6789b01a-a9c9-489f-9d24-ed39533fca61:launchConfigurationName/Test", name.resourceName )
    assertEquals( "Account number", "013765657871", name.namespace )
    assertEquals( "Service name", "autoscaling", name.service )
    assertEquals( "Type name", "launchConfiguration", name.type )
    assertEquals( "Uuid", "6789b01a-a9c9-489f-9d24-ed39533fca61", name.uuid )
    assertEquals( "Short name", "Test", name.getName( AutoScalingResourceName.Type.launchConfiguration ) )
  }

  /**
   * arn:aws:autoscaling::013765657871:autoScalingGroup:ee6258d4-a9a5-46e0-9896-1a7b3294c12e:autoScalingGroupName/Test
   */
  @Test
  public void testAutoScalingGroupArn() {
    assertTrue( "Valid resource name", isResourceName().apply( "arn:aws:autoscaling::013765657871:autoScalingGroup:ee6258d4-a9a5-46e0-9896-1a7b3294c12e:autoScalingGroupName/Test" ) )
    assertFalse( "Short name", isResourceName().apply( "Test" ) )
    AutoScalingResourceName name = parse( "arn:aws:autoscaling::013765657871:autoScalingGroup:ee6258d4-a9a5-46e0-9896-1a7b3294c12e:autoScalingGroupName/Test" )
    assertEquals( "Name", "arn:aws:autoscaling::013765657871:autoScalingGroup:ee6258d4-a9a5-46e0-9896-1a7b3294c12e:autoScalingGroupName/Test", name.resourceName )
    assertEquals( "Account number", "013765657871", name.namespace )
    assertEquals( "Service name", "autoscaling", name.service )
    assertEquals( "Type name", "autoScalingGroup", name.type )
    assertEquals( "Uuid", "ee6258d4-a9a5-46e0-9896-1a7b3294c12e", name.uuid )
    assertEquals( "Short name", "Test", name.getName( AutoScalingResourceName.Type.autoScalingGroup ) )
  }

  /**
   * arn:aws:autoscaling::013765657871:scalingPolicy:2886a285-6fdf-4018-8305-7aa037ed0d38:autoScalingGroupName/Test:policyName/TestUp
   */
  @Test
  public void testScalingPolicyArn() {
    assertTrue( "Valid resource name", isResourceName().apply( "arn:aws:autoscaling::013765657871:scalingPolicy:2886a285-6fdf-4018-8305-7aa037ed0d38:autoScalingGroupName/Test:policyName/TestUp" ) )
    assertFalse( "Short name", isResourceName().apply( "Test" ) )
    AutoScalingResourceName name = parse( "arn:aws:autoscaling::013765657871:scalingPolicy:2886a285-6fdf-4018-8305-7aa037ed0d38:autoScalingGroupName/Test:policyName/TestUp" )
    assertEquals( "Name", "arn:aws:autoscaling::013765657871:scalingPolicy:2886a285-6fdf-4018-8305-7aa037ed0d38:autoScalingGroupName/Test:policyName/TestUp", name.resourceName )
    assertEquals( "Account number", "013765657871", name.namespace )
    assertEquals( "Service name", "autoscaling", name.service )
    assertEquals( "Type name", "scalingPolicy", name.type )
    assertEquals( "Uuid", "2886a285-6fdf-4018-8305-7aa037ed0d38", name.uuid )
    assertEquals( "Scope name", "Test", name.getScope( AutoScalingResourceName.Type.scalingPolicy ) )
    assertEquals( "Short name", "TestUp", name.getName( AutoScalingResourceName.Type.scalingPolicy ) )
  }
}
