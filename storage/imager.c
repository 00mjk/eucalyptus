#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <string.h>
#include <unistd.h> // getopt
#include <fcntl.h> // open
#include <ctype.h> // tolower
#include <errno.h> // errno

#include "euca_auth.h"
#include "eucalyptus.h"
#include "misc.h"
#include "imager.h"
#include "cmd.h"
#include "map.h"
#include "cache.h"

extern char ** environ;

#define MAX_REQS 32
#define MAX_PARAMS 32

static imager_request reqs [MAX_REQS];
static char * euca_home = NULL;
static map * artifacts_map;
static boolean debug = FALSE;

static void set_debug (boolean yes)
{
    // so euca libs will log to stdout
    if (yes==TRUE) {
        logfile (NULL, EUCADEBUG);
    } else {
        logfile (NULL, EUCAERROR);
    }
}

static void usage (const char * msg) 
{ 
    if (msg!=NULL)
        fprintf (stderr, "error: %s\n\n", msg);
        
    fprintf (stderr, "Usage: euca_imager [command param=value param2=value ...] [command2 ...]\n"); 

	if (msg==NULL) 
		fprintf (stderr, "Try 'euca_imager help' for list of commands\n");

    exit (1); 
}

void err (const char *format, ...)
{
    va_list ap;

    va_start(ap, format);
    fprintf (stderr, "error: ");
    vfprintf(stderr, format, ap);
    fprintf (stderr, "\n");
    fflush(stderr);
    va_end(ap);

    exit (1);
}

// finds a command by name among known_cmds[] defined in cmd.h
static imager_command * find_struct (char * name) 
{
	for (int i=0; i<(sizeof(known_cmds)/sizeof(imager_command)); i++) {
		if (strcmp(name, known_cmds[i].name)==0) {
			return &known_cmds[i];
		}
	}
	return NULL;
}

// either prints help messages or runs command-specific validator
static imager_command * validate_cmd (int index, char *this_cmd, imager_param *params, char *next_cmd)
{
	if (this_cmd==NULL) 
		return NULL;

	char * cmd = this_cmd;
	char help = 0;

	// see if it is a help request
	if (strcmp(cmd, "help")==0) {
		help = 1;
		if (next_cmd==NULL) { // not specific
			fprintf (stderr, "supported commands:\n");
			for (int i=0; i<(sizeof(known_cmds)/sizeof(imager_command)); i++) {
				fprintf (stderr, "\t%s\n", known_cmds[i].name);
			}
			exit (0);
		} else { // help for a specific command
			cmd = next_cmd;
		}
	}
	
	// find the function pointers for the command
	imager_command *cmd_struct = find_struct (cmd);
	if (cmd_struct==NULL)
		err ("command '%s' not found", cmd);

	// print command-specific help
	if (help) {
		char ** p = cmd_struct->parameters();
		if (p==NULL || *p==NULL) {
			fprintf (stderr, "command '%s' has no parameters\n", cmd_struct->name);
		} else {
			fprintf (stderr, "parameters for '%s' (* - mandatory):\n", cmd_struct->name);
			while (*p && *(p+1)) {
				fprintf (stderr, "%18s - %s\n", *p, *(p+1));
				p+=2;
			}
		}
		exit (0);
	}

	// fill out the request struct and pass to the validator
	imager_request * req = & reqs [index];
	req->cmd = cmd_struct;
	req->params = params;
	req->internal = NULL;
	if (cmd_struct->validate (req))
		err ("incorrect parameters for command '%s'", cmd_struct->name);

	return cmd_struct;
}

static void set_global_parameter (char * key, char * val)
{
	if (strcmp (key, "debug")==0) {
        debug = parse_boolean (val);
        set_debug (debug);
    } else if (strcmp (key, "work")==0) {
		set_work_dir (val);
	} else if (strcmp (key, "work_size")==0) {
		set_work_limit (atoll(val));
	} else if (strcmp (key, "cache")==0) {
		set_cache_dir (val);
	} else if (strcmp (key, "cache_size")==0) {
		set_cache_limit (atoll(val));
	} else {
		err ("unknown global parameter '%s'", key);
	}
    logprintfl (EUCAINFO, "GLOBAL: %s=%s\n", key, val);
}

int main (int argc, char * argv[])
{
    set_debug (debug);


	// initialize globals
	artifacts_map = map_create (10);

	// use $EUCALYPTUS env var if available
    char root [] = "";
	euca_home = getenv (EUCALYPTUS_ENV_VAR_NAME);
    if (!euca_home) {
        euca_home = root;
    }

	// TODO: make printing of argv[]
    char buf [4096] = "\"";
    for (int i=0; i<argc; i++) {
      strncat (buf, argv[i], sizeof (buf));
      strncat (buf, "\" \"", sizeof (buf));
    }
    logprintfl (EUCADEBUG, "argv[]: %s\n", buf);

	// parse command-line parameters
	char * cmd_name = NULL;
	imager_param * cmd_params = NULL;
	int nparams = 0;
	int ncmds = 0;
	while ( *(++argv) ) {
		char * eq = strstr(*argv, "="); // all params have '='s

		if (eq==NULL) { // it's a command
			// process previous command, if any
			if (validate_cmd (ncmds, cmd_name, cmd_params, *argv)!=NULL)
				ncmds++; // increment only if there was a previous command
			
			if (ncmds+1>MAX_REQS)
				err ("too many commands (max is %d)", MAX_REQS);

			cmd_name = * argv;
			cmd_params = NULL;
			nparams = 0;

		} else { // this is a parameter
			if (strlen (eq) == 1)
				usage ("parameters must have non-empty values");
			* eq = '\0'; // split key from value
			if (strlen (* argv) == 1)
				usage ("parameters must have non-empty names");
			char * key = * argv;
			char * val = eq + 1;
			if (key==NULL || val==NULL) 
				usage ("syntax error in parameters");
			if (key[0]=='-') key++; // skip '-' if any
			if (key[0]=='-') key++; // skip second '-' if any

			if (cmd_name==NULL) { // without a preceding command => global parameter
				set_global_parameter (key, val);
				continue;
			}

			if (cmd_params==NULL) {
				cmd_params = calloc (MAX_PARAMS+1, sizeof(imager_param)); // +1 for terminating NULL
			}
			if (nparams+1>MAX_PARAMS)
				err ("too many parameters (max is %d)", MAX_PARAMS);
			cmd_params[nparams].key = key;
			cmd_params[nparams].val = val;
			nparams++;
		}
	}
	if (validate_cmd (ncmds, cmd_name, cmd_params, *argv)!=NULL)
		ncmds++;

	logprintfl (EUCAINFO, "verified all parameters for %d command(s)\n", ncmds);

	// invoke the requirements checkers in the same order as on command line
	for (int i=0; i<ncmds; i++)
		if (reqs[i].cmd->requirements!=NULL)
			if (reqs[i].cmd->requirements (&reqs[i])!=OK)
				err ("failed while verifying requirements");

	// invoke the last command, which will trigger the commands it depends on
	int ret;
	for (int i=ncmds-1; i>=0; i--) {
		if (reqs[ncmds-1].cmd->execute!=NULL) {
			ret = reqs[ncmds-1].cmd->execute(&reqs[ncmds-1]);
			break;
		}
	}

	// invoke the cleaners for each command to tidy up disk space and memory allocations
	for (int i=0; i<ncmds; i++)
		if (reqs[i].cmd->cleanup!=NULL)
			reqs[i].cmd->cleanup (&reqs[i]);

	exit (ret);
}

// common functions used by commands

void print_req (imager_request * req) 
{
	logprintfl (EUCAINFO, "command: %s\n", req->cmd->name);
	for (imager_param * p = req->params; p!=NULL && p->key!=NULL; p++) {
		logprintfl (EUCAINFO, "\t%s=%s\n", p->key, p->val);
	}
}

// turn a string into a boolean (returned as a char)

char parse_boolean (const char * s)
{
	char * lc = strduplc (s);
	char val;

	if (strcmp (lc, "y")==0 ||
		strcmp (lc, "yes")==0 ||
		strcmp (lc, "t")==0 ||
		strcmp (lc, "true")==0) { val = 1; }
	else if (strcmp (lc, "n")==0 ||
			 strcmp (lc, "no")==0 ||
			 strcmp (lc, "f")==0 ||
			 strcmp (lc, "false")==0) {	val = 0; }
	else 
		err ("failed to parse value '%s' as boolean", lc);
	free (lc);
	
	return val;
}

// read in login or password from command line or from a file

char * parse_loginpassword (char * s)
{
	char * val = s;
	FILE * fp;
	if ((fp = fopen (s, "r"))!=NULL) {
		val = fp2str (fp);
		if (val==NULL) {
			err ("failed to read file '%s'", s);
		} else {
			logprintfl (EUCAINFO, "read in contents from '%s'\n", s);
		}
	}

	return val;
}

// make sure the path exists and is readable

int verify_readability (const char * path)
{
	if (fopen (path, "r")==NULL)
		err ("unable to read '%s'", path);
	return 0;
}

// return eucalyptus root

char * get_euca_home (void)
{
	return euca_home;
}

// return global artifacts map

map * get_artifacts_map (void)
{
	return artifacts_map;
}

// if path=A/B/C but only A exists, this will try to create B and C
int ensure_path_exists (const char * path, mode_t mode)
{
	int len = strlen (path);
	char * path_copy = strdup (path);
	int i;
	
	if (path_copy==NULL) 
		return errno;
	
	for (i=0; i<len; i++) {
		struct stat buf;
		char try_it = 0;
		
		if (path[i]=='/' && i>0) {
			path_copy[i] = '\0';
			try_it = 1;
		} else if (path[i]!='/' && i+1==len) { // last one
			try_it = 1;
		}
		
		if ( try_it ) {
			if ( stat (path_copy, &buf) == -1 ) {
				logprintfl (EUCAINFO, "trying to create path %s\n", path_copy);
				
				if ( mkdir (path_copy, mode) == -1) {
					logprintfl (EUCAERROR, "error: failed to create path %s: %s\n", path_copy, strerror (errno));
					
					if (path_copy) 
						free (path_copy);
					return errno;
				}
			}
			path_copy[i] = '/'; // restore the slash
		}
	}
	
	free (path_copy);
	return 0;
}

// if path=A/B/C but only A exists, this will try to create B, but not C
int ensure_dir_exists (const char * path, mode_t mode)
{
  int len = strlen (path);
  char * path_copy = strdup (path);
  int i, err = 0;
  
  if (path_copy==NULL) 
    return errno;
  
  for (i=len-1; i>0; i--) {
    if (path[i]=='/') {
      path_copy[i] = '\0';
      err = ensure_path_exists (path_copy, mode);
      break;
    }
  }
  
  free (path_copy);
  return err;
}
