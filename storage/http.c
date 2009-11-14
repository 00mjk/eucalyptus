/*
Copyright (c) 2009  Eucalyptus Systems, Inc.	

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by 
the Free Software Foundation, only version 3 of the License.  
 
This file is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
for more details.  

You should have received a copy of the GNU General Public License along
with this program.  If not, see <http://www.gnu.org/licenses/>.
 
Please contact Eucalyptus Systems, Inc., 130 Castilian
Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/> 
if you need additional information or have any questions.

This file may incorporate work covered under the following copyright and
permission notice:

  Software License Agreement (BSD License)

  Copyright (c) 2008, Regents of the University of California
  

  Redistribution and use of this software in source and binary forms, with
  or without modification, are permitted provided that the following
  conditions are met:

    Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.

    Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
  IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
  OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
  THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
  LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
  SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
  IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
  BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
  THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
  OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
  WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
  ANY SUCH LICENSES OR RIGHTS.
*/
#include "config.h"
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h> /* close, stat */
#include <assert.h>
#include <string.h>
#include <strings.h>
#include <fcntl.h> /* open */
#include <sys/types.h> /* stat */
#include <sys/stat.h> /* stat */
#include <curl/curl.h>
#include <curl/easy.h>
#include "eucalyptus.h"
#include "misc.h"

#define TOTAL_RETRIES 3 /* download is retried in case of connection problems */
#define FIRST_TIMEOUT 4 /* in seconds, goes in powers of two afterwards */
#define STRSIZE 245 /* for short strings: files, hosts, URLs */

static size_t read_data (char *bufptr, size_t size, size_t nitems, void *userp);

struct request {
	FILE * fp; /* input file pointer to be used by curl READERs */
    long long total_read; /* bytes written during the operation */
    long long total_calls; /* write calls made during the operation */
};

static int curl_initialized = 0;

int http_put (const char * file_path, const char * url, const char * login, const char * password) 
{
    int code = ERROR;

    if (curl_initialized!=1) {
        curl_global_init(CURL_GLOBAL_SSL);
        curl_initialized = 1;
    }

    struct stat64 mystat;
    if (stat64 (file_path, &mystat)) {
        logprintfl (EUCAERROR, "http_put(): failed to stat %s\n", file_path);
		return code;
    }
    if (!S_ISREG(mystat.st_mode)) {
        logprintfl (EUCAERROR, "http_put(): %s is not a regular file\n", file_path);
		return code;
    }

	FILE * fp = fopen64 (file_path, "r");
	if (fp==NULL) {
		logprintfl (EUCAERROR, "http_put(): failed to open %s for reading\n", file_path);
		return code;
	}

	CURL * curl;
	CURLcode result;
	curl = curl_easy_init ();
	if (curl==NULL) {
		logprintfl (EUCAERROR, "http_put(): could not initialize libcurl\n");
		fclose (fp);
		return code;
	}

    logprintfl (EUCAINFO, "http_put(): uploading %s\n", file_path);
    logprintfl (EUCAINFO, "            to %s\n", url);

	char error_msg [CURL_ERROR_SIZE];
	curl_easy_setopt (curl, CURLOPT_ERRORBUFFER, error_msg);
	curl_easy_setopt (curl, CURLOPT_URL, url); 
    curl_easy_setopt (curl, CURLOPT_UPLOAD, 1L);
    curl_easy_setopt (curl, CURLOPT_INFILESIZE_LARGE, (curl_off_t)mystat.st_size);
    curl_easy_setopt (curl, CURLOPT_SSL_VERIFYPEER, 0L); // TODO: make this optional?

    if (login!=NULL && password!=NULL) {
        char userpwd [STRSIZE];
        snprintf (userpwd, STRSIZE, "%s:%s", login, password);
        curl_easy_setopt (curl, CURLOPT_USERPWD, userpwd);
    }

	struct request params;
    params.fp = fp;
    curl_easy_setopt (curl, CURLOPT_READDATA, &params);
    curl_easy_setopt (curl, CURLOPT_READFUNCTION, read_data);

    int retries = TOTAL_RETRIES;
    int timeout = FIRST_TIMEOUT;
    do {
        params.total_read = 0L;
        params.total_calls = 0L;
        result = curl_easy_perform (curl); /* do it */
        logprintfl (EUCADEBUG, "http_put(): uploaded %ld bytes in %ld sends\n", params.total_read, params.total_calls);

        if (result) { // curl error (connection or transfer failed)
            logprintfl (EUCAERROR,     "http_put(): %s (%d)\n", error_msg, result);

        } else {
            long httpcode;
            curl_easy_getinfo (curl, CURLINFO_RESPONSE_CODE, &httpcode);
            // TODO: pull out response message, too?
            
            switch (httpcode) {
            case 200L: // all good
                logprintfl (EUCAINFO, "http_put(): file updated sucessfully\n");
                code = OK;
                break;
            case 201L: // all good, created
                logprintfl (EUCAINFO, "http_put(): file created sucessfully\n");
                code = OK;
                break;
            case 408L: // timeout, retry
                logprintfl (EUCAWARN, "http_put(): server responded with HTTP code %ld (timeout)\n", httpcode);
                break;
            default: // some kind of error, will not retry
                logprintfl (EUCAERROR, "http_put(): server responded with HTTP code %ld\n", httpcode);
                retries = 0;
            }
        }

        if (code!=OK && retries > 0) {
            logprintfl (EUCAERROR, "            upload retry %d of %d will commence in %d seconds\n", TOTAL_RETRIES-retries+1, TOTAL_RETRIES, timeout);
            sleep (timeout);
            fseek (fp, 0L, SEEK_SET);
            timeout <<= 1;
        }

        retries--;
    } while (code!=OK && retries>0);
    fclose (fp);
    
	curl_easy_cleanup (curl);
    return code;
}

/* libcurl read handler */
static size_t read_data (char *buffer, size_t size, size_t nitems, void *params)
{
    assert (params != NULL);

    FILE * fp = ((struct request *)params)->fp;
    int items_read = 0;
    do {
        items_read += fread (buffer, size, nitems-items_read, fp);
    } while (items_read!=nitems && !feof(fp));
        
    ((struct request *)params)->total_read += items_read * size;
    ((struct request *)params)->total_calls++;

    return items_read;
}



