# Copyright (c) 2011, Eucalyptus Systems, Inc.
# All rights reserved.
#
# Redistribution and use of this software in source and binary forms, with or
# without modification, are permitted provided that the following conditions
# are met:
#
#   Redistributions of source code must retain the above
#   copyright notice, this list of conditions and the
#   following disclaimer.
#
#   Redistributions in binary form must reproduce the above
#   copyright notice, this list of conditions and the
#   following disclaimer in the documentation and/or other
#   materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
#
# Author: Mitch Garnaat mgarnaat@eucalyptus.com

from boto.roboto.awsqueryrequest import AWSQueryRequest
from boto.roboto.param import Param
from eucadmin.command import Command
from eucadmin.cmdstrings import get_cmdstring
import eucadmin
import os
import time
import boto.utils

GetCertURL = 'https://localhost:8443/getX509?account=%s&user=%s&code=%s'

EucaP12File = '%s/var/lib/eucalyptus/keys/euca.p12'
CloudPKFile = '%s/var/lib/eucalyptus/keys/cloud-pk.pem'

class GetCredentials(AWSQueryRequest):
    
    ServiceClass = eucadmin.EucAdmin
    Description = 'Get credentials zip file.'
    Params = [Param(name='euca_home',
                    short_name='e', long_name='euca-home',
                    ptype='string', optional=True,
                    doc='Eucalyptus install dir, default is $EUCALYPTUS'),
              Param(name='account',
                    short_name='a', long_name='account',
                    ptype='string', optional=True, default='eucalyptus',
                    doc='The account whose credentials will be used'),
              Param(name='user',
                    short_name='u', long_name='user',
                    ptype='string', optional=True, default='admin',
                    doc='The Eucalyptus account that will be retrieved')]
    Args = [Param(name='zipfile', long_name='zipfile',
                  ptype='string', optional=False,
                  doc='The path to the resulting zip file with credentials')]
                    
    def check_zipfile(self):
        if os.path.exists(self.zipfile):
            msg = 'file %s already exists, ' % self.zipfile
            msg += 'please remove and try again'
            raise IOError(msg)

    def check_cloudpk_file(self):
        if os.path.exists(self.cloudpk_file):
            stats = os.stat(self.cloudpk_file)
            if stats.st_size > 0:
                return True
        return False

    def gen_cloudpk_file(self):
        cmd_string = get_cmdstring('openssl')
        cmd = Command(cmd_string % (self.eucap12_file, self.cloudpk_file))
                      
    def query_mysql(self, query, num_retries=2):
        result = None
        i = 0
        while i < num_retries:
            cmd = Command(query % (self.account, self.user, self.db_pass))
            result = cmd.stdout.strip()
            if result:
                break
            time.sleep(1)
            i += 1
        if not result:
            msg = 'The MySQL server is not responding.\n'
            msg += 'Please make sure MySQL is up and running.'
            raise ValueError(msg)
        return result

    def get_credentials(self):
        data = boto.utils.retry_url(GetCertURL % (self.account,
                                                  self.user,
                                                  self.token))
        fp = open(self.zipfile, 'wb')
        fp.write(data)
        fp.close()

    def get_dbpass(self):
        cmd_string = get_cmdstring('dbpass')
        cmd = Command(cmd_string % self.euca_home)
        self.db_pass = cmd.stdout.strip()

    def cli_formatter(self, data):
        pass

    def setup_query(self):
        self.token = None
        self.db_pass = None
        if 'euca_home' in self.request_params:
            self.euca_home = self.request_params['euca_home']
        else:
            if 'EUCALYPTUS' in os.environ:
                self.euca_home = os.environ['EUCALYPTUS']
            else:
                raise ValueError('Unable to find EUCALYPTUS home')
        self.account = self.request_params['account']
        self.user = self.request_params['user']
        self.zipfile = self.request_params['zipfile']
        self.eucap12_file = EucaP12File % self.euca_home
        self.cloudpk_file = CloudPKFile % self.euca_home
        if not self.check_cloudpk_file:
            self.gen_cloudpk_file()
        self.get_dbpass()

    def get_accesskey_secretkey(self, **args):
        self.args.update(args)
        self.process_args()
        self.setup_query()
        query = get_cmdstring('mysql_get_accesskey_secretkey')
        return self.query_mysql(query)

    def main(self, **args):
        self.args.update(args)
        self.process_args()
        self.setup_query()
        self.check_zipfile()
        # check local service?
        self.token = self.query_mysql(get_cmdstring('mysql_get_token'))
        self.get_credentials()
        
    def main_cli(self):
        self.do_cli()
        
        
