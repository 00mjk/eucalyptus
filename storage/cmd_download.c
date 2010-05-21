#include <stdio.h>
#include <stdlib.h>
#include <string.h> // strcmp
#include <unistd.h> // access
#include "eucalyptus.h" 
#include "misc.h"
#include "walrus.h"
#include "imager.h"
#include "cache.h"
#include "map.h"

#define EUCA_MAX_DIGEST_SIZE 100000 // digests are usually 3K, so 100K should be enough

#define NODE_CERT "var/lib/eucalyptus/keys/node-cert.pem"
#define NODE_PK   "var/lib/eucalyptus/keys/node-pk.pem"

static char def_cert [1024];
static char def_pk [1024];

#define _M     "* " // mandatory
#define _IN    "in"
#define _CACHE "cache"
#define _CERT  "cert"
#define _LOGIN "login"
#define _OUT   "out"
#define _PASS  "password"
#define _PK    "pk"
#define _TYPE  "type"
#define _WORK  "work"
#define _VPATH "vsphere-path"

static char * params [] = {
	_M _IN,     "URL from which to download the file (Web, Walrus, ...)",
	_CACHE,     "cache the object? (def=no)",
	_CERT,      "X509 certificate for Walrus (def=$EUCALYPTUS/" NODE_CERT ")",
	_LOGIN,     "login or name of the file containing it",
	_M _OUT,    "local name for the downloaded object",
	_PASS,      "password or name of the file containing it",
	_PK,        "X509 private key for Walrus (def=$EUCALYPTUS/" NODE_PK ")",
	_TYPE,      "{disk|kernel|partition|ramdisk} (def=unknown)",
	_VPATH,     "vSphere path to a file, including the datastore",
	_WORK,      "create a work copy? (default=yes)",
	NULL
};

typedef struct _download_params {
	char * in;
	boolean cache;
	char * cert;
	enum content_type {
		UNKNOWN=0,
		DISK,
		KERNEL,
		PARTITION,
		RAMDISK
	} content;
	char * login;
	char * out;
	char * password;
	char * pk;
	enum download_type {
		WEB=0,
		VSPHERE,
		WALRUS,
	} type;
	char * vpath;
	boolean work;
} download_params;

static enum content_type parse_content_type_enum (const char * s)
{
	char * lc = strduplc (s);
	enum content_type val;

	if (strcmp (lc, "unknown")==0) val = UNKNOWN;
	else if (strcmp (lc, "disk")==0) val = DISK;
	else if (strcmp (lc, "kernel")==0) val = KERNEL;
	else if (strcmp (lc, "part")==0 || strcmp (lc, "partition")==0) val = PARTITION;
	else if (strcmp (lc, "ramdisk")==0 || strcmp (lc, "initrd")==0) val = RAMDISK;
	else err ("failed to parse '%s' as content type", lc);
	free (lc);
	
	return val;
}

char ** download_parameters () 
{
	return params;
}

int download_validate (imager_request * req)
{
	print_req (req);

	download_params * state = calloc (sizeof (download_params), 1);
	if (state==NULL) 
		err ("out of memory");

	// default values
	state->cache = FALSE;
	state->work = TRUE;

	// record in 'state' all specified parameters
	for (imager_param * p = req->params; p!=NULL && p->key!=NULL; p++) {
		if (strcmp (p->key, _IN)==0)         {state->in       = p->val; }
		else if (strcmp (p->key, _CACHE)==0) {state->cache    = parse_boolean (p->val); }
		else if (strcmp (p->key, _CERT)==0)  {state->cert     = p->val; }
		else if (strcmp (p->key, _LOGIN)==0) {state->login    = parse_loginpassword (p->val); }
		else if (strcmp (p->key, _OUT)==0)   {state->out      = p->val; }
		else if (strcmp (p->key, _PASS)==0)  {state->password = parse_loginpassword (p->val); }
		else if (strcmp (p->key, _PK)==0)    {state->pk       = p->val; }
		else if (strcmp (p->key, _TYPE)==0)  {state->content  = parse_content_type_enum (p->val); }
		else if (strcmp (p->key, _VPATH)==0) {state->vpath    = p->val; }
		else if (strcmp (p->key, _WORK)==0)  {state->work     = parse_boolean (p->val); }
		else
			err ("invalid parameter '%s' for command 'download'", p->key);
	}

	// ensure mandatory params are present
	if (state->in==NULL) err ("missing mandatory parameter '" _IN "'");
	if (state->out==NULL) err ("missing mandatory parameter '" _OUT "'");

	// ensure paired parameters are both present
	if ((state->login!=NULL && state->password==NULL) ||
		(state->login==NULL && state->password!=NULL))
		err ("both login and password must be specified");
	if ((state->cert!=NULL && state->pk==NULL) ||
		(state->cert==NULL && state->pk!=NULL))
		err ("both cert and pk must be specified");

	// verify file readability
	if (state->cert) {
		verify_readability (state->cert);
		verify_readability (state->pk);
	}

	// figure out what kind of download this is
	if (strncmp ("http", state->in, 4)!=0)
		err ("only URLs beginning with 'http...' are supported");

	if (strstr (state->in, "services/Walrus")!=NULL) { // looks like a Walrus URL
		state->type = WALRUS;
		if (state->cert==NULL) { // try to find default credentials

			// try to find default Walrus creds
			snprintf (def_cert, sizeof (def_cert), "%s/" NODE_CERT, get_euca_home());
			snprintf (def_pk,   sizeof (def_pk),   "%s/" NODE_PK,   get_euca_home());
			if (access (def_cert, R_OK) || access (def_pk, R_OK)) {
				err ("could not find or open default Walrus credentials (set EUCALYPTUS or specify cert= and pk=)");
			} else {
				state->cert = def_cert;
				state->pk = def_pk;
			}
		}

	} else if (strstr (state->in, "?dcPath=")!=NULL) { // looks like a vSphere URL
		state->type = VSPHERE;
		if (state->vpath) {
			err ("with " _VPATH " option the URL must not contain a path");
		}

	} else {
		if (state->vpath) {
			if (strstr (state->in, "?")!=NULL ) { // TODO: do better job checking
				err ("with " _VPATH " option the URL must not contain a path");
			}
			state->type = VSPHERE;
		}
		state->type = WEB; // any old Web URL
	}

	if (state->type == VSPHERE) {
		// TODO: parse URL and path
	}

	if (state->type != WALRUS) { // TODO: finish vSphere and plain HTTP downloads
		err ("sorry, vSphere downloads aren't implemented yes");
	}

	req->internal = (void *) state; // save pointer to find it later

	return 0;
}

static char * download_walrus_digest (char * url)
{
	char * digest = NULL; 
	
	logprintfl (EUCAINFO, "dowloading digest from '%s'...\n", url);
	char * tmp_digest_path = alloc_tmp_file ("image-digest", EUCA_MAX_DIGEST_SIZE);
	if (walrus_object_by_url (url, tmp_digest_path, 0)==OK) {
		digest = file2strn (tmp_digest_path, EUCA_MAX_DIGEST_SIZE);
	}
	free_tmp_file (tmp_digest_path, EUCA_MAX_DIGEST_SIZE);
	
	return digest;
}

int download_requirements (imager_request * req)
{
	download_params * state = (download_params *) req->internal;
	long long size;
	char * digest;

	switch (state->type) {
	case WALRUS: 
	{
		char * walrus_digest = download_walrus_digest (state->in);
		if (walrus_digest==NULL) {
			logprintfl (EUCAERROR, "error: failed to download or process Walrus manifest '%s'\n", state->in);
			return ERROR;
		}
		size = str2longlong (walrus_digest, "<size>", "</size>");
		digest = str2str (walrus_digest, "<digest algorithm=\"SHA1\">", "</digest>");
		if (digest==NULL) {
			logprintfl (EUCAWARN, "warning: no digest found in Walrus manifest '%s'\n", state->in);
			digest = strdup ("N/A"); // because digest will get freed
		}
		free (walrus_digest);
		break;
	}
	case VSPHERE: // TODO
		break;
	case WEB: // TODO (do HEAD request?)
		break;
	default:
		err ("internal error (unexpected download type)");
	}

	char size_str [30];
	snprintf (size_str, sizeof (size_str), "%lld", size);
	char * attrs [] = {
		"id", state->out,
		"size", size_str,
		"digest", digest,
		NULL
	};

	artifacts_spec * output_spec = alloc_artifacts_spec (req, attrs);
	free (digest);
	if (output_spec==NULL) {
		logprintfl (EUCAERROR, "error: out of memory for artifacts\n");
		return ERROR;
	}

	output_spec->size = size;
	map_set (get_artifacts_map(), state->out, (void *)output_spec);

	return 0;
}

int download_execute (imager_request * req)
{
	download_params * state = (download_params *) req->internal;
	char * id = state->out;
	int ret = OK;

	// look up the artifacts from the preceding requirements() invocation
	artifacts_spec * spec = map_get (get_artifacts_map(), id);
	if (spec==NULL) {
		logprintfl (EUCAERROR, "error: execute() called before requirements() for 'download'\n");
		return ERROR;
	}

	output_file * o = preprocess_output_path (id, spec, state->work, state->cache, NULL);
	if (o==NULL) {
		free (spec);
		return ERROR;
	}

	if (strlen (o->path)) { // valid output file does not exist
		// (no input files expected in this stage)

		// do the download to create the output file
		logprintfl (EUCAINFO, "dowloading from '%s'...\n", state->in);
		switch (state->type) {
		case WALRUS:
			ret = walrus_image_by_manifest_url (state->in, o->path, 1);
			break;
		case VSPHERE:
			break;
		case WEB:
			break;
		default:
			err ("internal error (unexpected download type)");
		}
	}

	boolean success = TRUE;
	if (ret!=OK) {
		logprintfl (EUCAERROR, "error: failed to download '%s' to '%s'\n", state->in, state->out);
		success = FALSE;
	}
	postprocess_output_path (o, success);
	// TODO: free spec

	return ret;
}

int download_cleanup (imager_request * req)
{
	download_params * state = (download_params *) req->internal;

	logprintfl (EUCAINFO, "cleaning up for '%s'...\n", req->cmd->name);
	free (state);

	return 0;
}
