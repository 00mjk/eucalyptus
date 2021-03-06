/*
 * This code was generated by AWS Flow Framework Annotation Processor.
 * Refer to Amazon Simple Workflow Service documentation at http://aws.amazon.com/documentation/swf 
 *
 * Any changes made directly to this file will be lost when 
 * the code is regenerated.
 */
 package com.eucalyptus.portal.workflow;

import com.amazonaws.services.simpleworkflow.flow.DataConverter;
import com.amazonaws.services.simpleworkflow.flow.StartWorkflowOptions;
import com.amazonaws.services.simpleworkflow.flow.WorkflowClientBase;
import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.amazonaws.services.simpleworkflow.flow.generic.GenericWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution;
import com.amazonaws.services.simpleworkflow.model.WorkflowType;

class AwsUsageHourlyAggregateWorkflowClientImpl extends WorkflowClientBase implements AwsUsageHourlyAggregateWorkflowClient {

    public AwsUsageHourlyAggregateWorkflowClientImpl(WorkflowExecution workflowExecution, WorkflowType workflowType,  
            StartWorkflowOptions options, DataConverter dataConverter, GenericWorkflowClient genericClient) {
        super(workflowExecution, workflowType, options, dataConverter, genericClient);
    }
    
    @Override
    public final Promise<Void> aggregateHourly() {
        return aggregateHourly((StartWorkflowOptions)null);
    }

    @Override
    public final Promise<Void> aggregateHourly(Promise<?>... waitFor) {
        return aggregateHourly((StartWorkflowOptions)null, waitFor);
    }
    
    @Override
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public final Promise<Void> aggregateHourly(StartWorkflowOptions optionsOverride, Promise<?>... waitFor) {
        return (Promise) startWorkflowExecution(new Object[0], optionsOverride, Void.class, waitFor);
    }
    	

}