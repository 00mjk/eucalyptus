// -*- mode: C; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil -*-
// vim: set softtabstop=4 shiftwidth=4 tabstop=4 expandtab:

/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

#include "data.h"
#include "blobstore.h"

/* This is two hours; note that using multiplication with longs in defines causes 32bit compile warnings */
#define INSTANCE_PREP_TIMEOUT_USEC 7200000000L // TODO: change the timeout?

#define MAX_ARTIFACT_DEPS 16
#define MAX_ARTIFACT_SIG 262144
#define MAX_SSHKEY_SIZE 262144

typedef struct _artifact {
    // artifact can be located either in a blobstore or on a file system:
    char id [EUCA_MAX_PATH]; // either ID or PATH to the artifact
    boolean id_is_path; // if set, id is a PATH

    char sig [MAX_ARTIFACT_SIG]; // unique signature for the artifact (IGNORED for a sentinel)
    boolean may_be_cached; // the underlying blob may reside in cache (it will not be modified by an instance)
    boolean is_in_cache; // indicates if the artifact is known to reside in cache (value only valid after artifact allocation)
    boolean must_be_file; // the bits for this artifact must reside in a regular file (rather than just on a block device)
    boolean must_be_hollow; // create the artifact with BLOBSTORE_FLAG_HOLLOW set (so its size won count toward the limit)
    int (* creator) (struct _artifact * a); // function that can create this artifact based on info in this struct (must be NULL for a sentinel)
    long long size_bytes; // size of the artifact, in bytes (OPTIONAL for some types)
    virtualBootRecord * vbr; // VBR associated with the artifact (OPTIONAL for some types)
    boolean do_make_bootable; // tells 'disk_creator' whether to make the disk bootable
    boolean do_tune_fs; // tells 'copy_creator' whether to tune the file system
    boolean is_partition; // this artifact is a partition for a disk to be constructed
    char sshkey [MAX_SSHKEY_SIZE]; // the key to inject into the artifact (OPTIONAL for all except keyed_disk_creator)
    blockblob * bb; // blockblob handle for the artifact, when it is open
    struct _artifact * deps [MAX_ARTIFACT_DEPS]; // array of pointers to artifacts that this artifact depends on
    int seq; // sequence number of the artifact
    int refs; // reference counter (1 or more if contained in deps[] of others)
    char instanceId [32]; // here purely for annotating logs
    void * internal; // OPTIONAL pointer to any other artifact-specific data 'creator' may need
} artifact;

int vbr_add_ascii (const char * spec_str, virtualMachine * vm_type);
int vbr_legacy (const char * instanceId, virtualMachine * vm, char *imageId, char *imageURL, char *kernelId, char *kernelURL, char *ramdiskId, char *ramdiskURL);
int vbr_parse (virtualMachine * vm, ncMetadata * meta);
artifact * vbr_alloc_tree (virtualMachine * vm, boolean do_make_bootable, boolean do_make_work_copy, const char * sshkey, const char * instanceId);
void art_set_instanceId (const char * instanceId);
int art_implement_tree (artifact * root, blobstore * work_bs, blobstore * cache_bs, const char * work_prefix, long long timeout);
artifact * art_alloc (const char * id, const char * sig, long long size_bytes, boolean may_be_cached, boolean must_be_file, boolean must_be_hollow, int (* creator) (artifact * a), virtualBootRecord * vbr);
int art_add_dep (artifact * a, artifact * dep);
void art_free (artifact * a);
boolean tree_uses_blobstore (artifact * a);
boolean tree_uses_cache (artifact * a);
