package com.eucalyptus.util.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.log4j.Logger;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.Callback.Checked;
import com.eucalyptus.util.concurrent.MoreExecutors;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class Futures {

  public static <R> CheckedListenableFuture<R> newAsyncMessageFuture( ) {
    return new AsyncResponseFuture<R>( );
  }

  public static Runnable addListenerHandler( CheckedListenableFuture<?> future, Callback<?> listener ) {
    Runnable r;
    future.addListener( r = new BasicCallbackProcessor( future, listener ), MoreExecutors.sameThreadExecutor( ) );
    return r;
  }

  static class BasicCallbackProcessor<R extends BaseMessage> implements Runnable {
    private final Callback<R> callback;
    private final Future<R>   future;
    private Logger            LOG;
    
    private BasicCallbackProcessor( Future<R> future, Callback<R> callback ) {
      this.callback = callback;
      this.future = future;
      this.LOG = Logger.getLogger( this.callback.getClass( ) );
    }
    
    @Override
    public void run( ) {
      R reply = null;
      Throwable failure = null;
      try {
        reply = this.future.get( );
      } catch ( Throwable t ) {
        LOG.error( t, t );
        failure = t;
      }
      if ( reply != null ) {
        try {
          EventRecord.caller( this.callback.getClass( ), EventType.CALLBACK, "fire(" + reply.getClass( ).getSimpleName( ) + ")" ).trace( );
          this.callback.fire( reply );
        } catch ( Throwable t ) {
          LOG.error( t, t );
        }
      } else if ( failure != null ) {
        this.doFail( failure );
      } else {
        LOG.warn( "Ignoring application of callback " + this.getClass( ).getSimpleName( ) + " since reply is: " + reply );
        Exceptions.eat( "Callback marked as done has null valued response: " + reply );
      }
    }
    
    private final void doFail( Throwable failure ) {
      if ( this.callback instanceof Callback.Checked ) {
        try {
          if ( ( failure instanceof ExecutionException ) && failure.getCause( ) != null ) {
            failure = failure.getCause( );
          }
          EventRecord.caller( this.callback.getClass( ), EventType.CALLBACK, "fireException(" + failure.getClass( ).getSimpleName( ) + ")" ).trace( );
          ( ( Checked ) this.callback ).fireException( failure );
        } catch ( Throwable t ) {
          LOG.error( t, t );
        }
      }
    }
    
  }

}
