package com.eucalyptus.auth;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.api.PolicyEngine;
import com.eucalyptus.auth.policy.RequestContext;
import com.eucalyptus.auth.policy.RequestContext.ContextAdaptor;
import com.eucalyptus.auth.policy.PolicyEngineImpl;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Condition;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.X509CertHelper;
import com.eucalyptus.images.Image;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.RunInstancesType;

public class AuthTest {

  private static Logger LOG = Logger.getLogger( AuthTest.class );
                                               
  public static final String MARK = "[YE] ";
  
  public static void test( ) {
    try {
      test2( );
    } catch ( Exception e ) {
      Debugging.logError( LOG, e, MARK + "Test failed" );
    }
  }
  
  public static void test1( ) throws Exception {
    LOG.debug( MARK + "Add account1" );
    Accounts.addAccount( "account1" );
    Users.addAccountAdmin( "account1", "foobar" );
    
    Map<String, String> info = Maps.newHashMap( );
    info.put( "Full name", "User 11" );
    info.put( "Email", "user11@foobar.com" );
    User user = Users.addUser( "user11", "/", true, true, info, true, true, true, "account1" );
    user.setInfo( "Department", "sales" );
    
    user.addSecretKey( "testkey" );
    String keyId = user.lookupSecretKeyId( "testkey" );
    LOG.debug( MARK + "testkey id = " + keyId );
    
    LOG.debug( MARK + "The user who has 'testkey' key is " + Users.lookupUserByAccessKeyId( keyId ) );
    
    LOG.debug( MARK + "The first active key: " + user.getFirstActiveSecretKeyId( ) );
    
    user.addX509Certificate( X509CertHelper.createCertificate( "testcert" ) );
    
    user.deactivateSecretKey( keyId );
    
    Group group = Groups.addGroup( "group1", "/", "account1" );
    group.addMember( user );
    
    for ( User u : group.getUsers( ) ) {
      LOG.debug( MARK + "group1 user: " + u.getName( ) );
    }
    
    for ( Group g : user.getGroups( ) ) {
      LOG.debug( MARK + "user11 group: " + g.getName( ) );
    }
    LOG.debug( MARK + "user11 info: " + user.getInfoMap ( ) );
    LOG.debug( MARK + "user11 account: " + user.getAccount( ).getName( ) );
    for ( String id : user.getActiveX509CertificateIds( ) ) {
      LOG.debug( MARK + "user11 active cert: " + id + "=" + user.getX509Certificate( id ) );
    }
    for ( String id : user.getInactiveX509CertificateIds( ) ) {
      LOG.debug( MARK + "user11 inactive cert: " + id + "=" + user.getX509Certificate( id ) );
    }
    for ( String id : user.getActiveSecretKeyIds( ) ) {
      LOG.debug( MARK + "user11 active key: " + id + "=" + user.getSecretKey( id ) );
    }
    for ( String id : user.getInactiveSecretKeyIds( ) ) {
      LOG.debug( MARK + "user11 inactive key: " + id + "=" + user.getSecretKey( id ) );
    }
    
    printUsers( "account1" );
    printGroups( "account1" );

    user = Users.addUser( "user12", "/", true, true, info, true, true, true, "account1" );
    group.addMember( user );
    
    Users.deleteUser( "user11", "account1", false, true );
    
    printUsers( "account1" );
    printGroups( "account1" );
    
    Groups.deleteGroup( "group1", "account1", true );
    
    printUsers( "account1" );
    printGroups( "account1" );
    
    LOG.debug( MARK + "Add account2" );
    Accounts.addAccount( "account2" );
    Users.addAccountAdmin( "account2", "foobar" );
    
    info = Maps.newHashMap( );
    info.put( "Full name", "User 12" );
    info.put( "Email", "user12@foobar.com" );
    user = Users.addUser( "user12", "/", true, true, info, true, true, true, "account2" );
    
    group = Groups.addGroup( "group1", "/", "account2" );
    group.addMember( user );
    
    printUsers( "account2" );
    printGroups( "account2" );
    
    printAccounts( );
    
    String policy =
      "{" +
        "'Version':'2010-11-14'," +
        "'Statement':[{" +
          "'Sid':'1'," +
          "'Effect':'Allow'," +
          "'Action':'ec2:RunInstances'," +
          "'Resource':'*'," +
          "'Condition':{" +
            "'DateEquals':{" +
              "'aws:currenttime':'2010-11-14'" +
            "}" +
          "}" +
        "}]" +
      "}";
    
    Policies.attachGroupPolicy( policy, "group1", "account2" );
    
    List<? extends Authorization> auths = Policies.lookupAuthorizations( "ec2:image", user.getUserId( ) );
    printAuths( auths );
  }
  
  public static void test2( ) throws Exception {
    Accounts.addAccount( "test" );
    Users.addAccountAdmin( "test", "foobar" );
    User tom = Users.addUser( "tom", "/", true, true, null, true, true, true, "test" );
    User jack = Users.addUser( "jack", "/", true, true, null, true, true, true, "test" );
    User chris = Users.addUser( "chris", "/", true, true, null, true, true, true, "test" );
    Group sales = Groups.addGroup( "sales", "/", "test" );
    sales.addMember( tom );
    Group marketing = Groups.addGroup( "marketing", "/", "test" );
    marketing.addMember( jack );
    
    String policy =
      "{" +
        "'Statement':[{" +
          "'Effect':'Allow'," +
          "'Action':'ec2:RunInstances'," +
          "'Resource':'arn:aws:ec2:::image/emi-1234*'," +
          "'Condition':{" +
            "'DateLessThanEquals':{" +
              "'aws:currenttime':'2010-12-31'" +
            "}" +
          "}" +
        "}," +
        "{" +
          "'Effect':'Deny'," +
          "'Action':'ec2:RunInstances'," +
          "'Resource':'arn:aws:ec2:::image/emi-12345678'," +
        "}]" +
      "}";
    
    Policies.attachGroupPolicy( policy, "sales", "test" );

    final User user = Users.lookupUserByName( "tom", "test" );
    RequestContext.setAdaptor( new ContextAdaptor( ) {

      @Override
      public User getRequestUser( ) {
        return user;
      }

      @Override
      public Class<? extends BaseMessage> getRequestMessageClass( ) {
        return RunInstancesType.class;
      }

      @Override
      public SocketAddress getRemoteAddress( ) {
        return new InetSocketAddress( "192.168.7.54", 80808 );
      }
      
    } );
    
    PolicyEngine engine = new PolicyEngineImpl( );
    engine.evaluateAuthorization( Image.class, "emi-12345678" );
  }
  
  private static void printAuths( List<? extends Authorization> auths ) throws AuthException {
    for ( Authorization a : auths ) {
      LOG.debug( MARK + a );
      for ( Condition c : a.getConditions( ) ) {
        LOG.debug( MARK + c );
      }
    }
  }
  
  private static void printAccounts( ) throws AuthException {
    LOG.debug( MARK + "---Accounts---" );
    for ( Account account : Accounts.listAllAccounts( ) ) {
      LOG.debug( MARK + account.toString( ) );
    }
  }
  
  private static void printUsers( String accountName ) throws AuthException {
    LOG.debug( MARK + "---Users for " + accountName + "---" );
    for ( User user : Accounts.listAllUsers( accountName ) ) {
      LOG.debug( MARK + user.toString( ) );
    }
  }
  
  private static void printGroups( String accountName ) throws AuthException {
    LOG.debug( MARK + "---Groups for " + accountName + "---" );
    for ( Group group : Accounts.listAllGroups( accountName ) ) {
      LOG.debug( MARK + group.toString( ) );
    }
  }
  
}
