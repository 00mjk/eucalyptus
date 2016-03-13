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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

#ifndef _INCLUDE_EUCA_TO_MIDO_H_
#define _INCLUDE_EUCA_TO_MIDO_H_

//!
//! @file net/euca-to-mido.h
//! Need definition
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <midonet-api.h>
#include <eucanetd_config.h>

#include "euca_gni.h"
/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  TYPEDEFS                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                ENUMERATIONS                                |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXPORTED PROTOTYPES                            |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                           STATIC INLINE PROTOTYPES                         |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

int get_next_router_id(mido_config * mido, int *nextid);
int set_router_id(mido_config * mido, int id);
int clear_router_id(mido_config * mido, int id);

int cidr_split(char *cidr, char *outnet, char *outslashnet, char *outgw, char *outplustwo);
int is_mido_vpc_plustwo(mido_config *mido, char *iptocheck);

enum vpc_route_entry_target_t parse_mido_route_entry_target(const char *target);

int initialize_mido(mido_config * mido, char *eucahome, int flushmode, int disable_l2_isolation, char *ext_eucanetdhostname, char *ext_rthosts, char *ext_pubnw,
                    char *ext_pubgwip, char *int_rtnetwork, char *int_rtslashnet);
int reinitialize_mido(mido_config *mido);
int clear_mido_gnitags(mido_config *mido);
int check_mido_tunnelzone();

int discover_mido_resources(mido_config * mido);

int add_mido_resource_router(mido_config *mido, midoname *router);
int add_mido_resource_ipaddrgroup(mido_config *mido, midoname *ipag);

int delete_mido_resource_chain(mido_config *mido, char *chainname);
int delete_mido_resource_ipaddrgroup(mido_config *mido, char *ipag);
int delete_mido_resource_ipaddrgroup_ip(mido_config *mido, midoname *ipag, midoname *ip);

int populate_mido_iphostmap(mido_config *mido);

int populate_mido_core(mido_config * mido, mido_core * midocore);
int create_mido_core(mido_config * mido, mido_core * midocore);
int delete_mido_core(mido_config * mido, mido_core * midocore);

int populate_mido_vpc(mido_config * mido, mido_core * midocore, mido_vpc * vpc);
int create_mido_vpc(mido_config * mido, mido_core * midocore, mido_vpc * vpc);
int delete_mido_vpc(mido_config * mido, mido_vpc * vpc);
int find_mido_vpc(mido_config * mido, char *vpcname, mido_vpc ** outvpc);

int populate_mido_vpc_subnet(mido_config * mido, mido_vpc * vpc, mido_vpc_subnet * vpcsubnet);
int create_mido_vpc_subnet(mido_config * mido, mido_vpc * vpc, mido_vpc_subnet * vpcsubnet, char *subnet, char *slashnet, char *gw, char *instanceDNSDomain,
                           u32 * instanceDNSServers, int max_instanceDNSServers);
int delete_mido_vpc_subnet(mido_config * mido, mido_vpc_subnet * subnet);
int find_mido_vpc_subnet(mido_vpc * vpc, char *subnetname, mido_vpc_subnet ** outvpcsubnet);
int find_mido_vpc_subnet_global(mido_config * mido, char *subnetname, mido_vpc_subnet ** outvpcsubnet);

int parse_mido_vpc_subnet_route_table(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet,
        char *subnetNetaddr, char *subnetSlashnet, gni_route_table *rtable, gni_vpc *gnivpc,
        gni_instance **gniinterfaces, int max_gniinterfaces, mido_parsed_route **proutes, int *max_proutes);
int create_mido_vpc_subnet_route_table(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet,
        char *subnetNetaddr, char *subnetSlashnet, gni_route_table *rtable, gni_vpc *gnivpc,
        gni_instance **gniinterfaces, int max_gniinterfaces);

int populate_mido_vpc_instance(mido_config * mido, mido_core * midocore, mido_vpc * vpc, mido_vpc_subnet * vpcsubnet, mido_vpc_instance * vpcinstance);
int create_mido_vpc_instance(mido_config * mido, mido_vpc_instance * vpcinstance, char *node);
int delete_mido_vpc_instance(mido_config *mido, mido_vpc_instance * vpcinstance);
int find_mido_vpc_instance(mido_vpc_subnet * vpcsubnet, char *instancename, mido_vpc_instance ** outvpcinstance);
int find_mido_vpc_instance_global(mido_config * mido, char *instancename, mido_vpc_instance ** outvpcinstance);

int populate_mido_vpc_natgateway(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet, mido_vpc_natgateway *vpcnatgateway);
int create_mido_vpc_natgateway(mido_config *mido, mido_vpc *vpc, mido_vpc_natgateway *vpcnatgateway);
int delete_mido_vpc_natgateway(mido_config *mido, mido_vpc_natgateway *vpcnatgateway);
int find_mido_vpc_natgateway(mido_vpc *vpc, char *natgname, mido_vpc_natgateway **outvpcnatgateway);

int find_mido_vpc_chain(mido_config *mido, char *chainname, midoname **outchain);
int find_mido_vpc_ipaddrgroup(mido_config *mido, char *ipagname, midoname **outipag);
mido_resource_router *find_mido_router(mido_config *mido, char *rtname);
mido_resource_bridge *find_mido_bridge(mido_config *mido, char *brname);
mido_resource_chain *find_mido_chain(mido_config *mido, char *chainname);
mido_resource_ipaddrgroup *find_mido_ipaddrgroup(mido_config *mido, char *ipagname);
midoname *find_mido_ipaddrgroup_ip(mido_config *mido, mido_resource_ipaddrgroup *ipag, char *ip);
mido_resource_portgroup *find_mido_portgroup(mido_config *mido, char *pgname);
mido_resource_host *find_mido_host(mido_config *mido, char *name);
mido_resource_host *find_mido_host_byuuid(mido_config *mido, char *uuid);
mido_resource_host *search_mido_host_byip(mido_config *mido, char *ip);
//int find_mido_router_ports(mido_config *mido, midoname *device, midoname **outports, int *outports_max);
//int find_mido_bridge_ports(mido_config *mido, midoname *device, midoname **outports, int *outports_max);
midoname *find_mido_bridge_port_byinterface(mido_resource_bridge *br, char *name);
int find_mido_device_ports(midoname *ports, int max_ports, midoname *device, midoname ***outports, int *outports_max);
int find_mido_host_ports(midoname *ports, int max_ports, midoname *host, midoname ***outports, int *outports_max);
int find_mido_portgroup_ports(midoname *ports, int max_ports, midoname *portgroup, midoname ***outports, int *outports_max);
int parse_mido_vpc_subnet_cidr(mido_vpc_subnet *vpcsubnet, char **net, char **length);
int parse_mido_vpc_route_addr(midoname *route, char **srcnet, char **srclength, char **dstnet, char **dstlength);
int find_mido_vpc_subnet_routes(mido_config *mido, mido_vpc *vpc, mido_vpc_subnet *vpcsubnet, midoname ***croutes, int *croutes_max);

int populate_mido_vpc_secgroup(mido_config * mido, mido_vpc_secgroup * vpcsecgroup);
int create_mido_vpc_secgroup(mido_config * mido, mido_vpc_secgroup * vpcsecgroup);
//int delete_mido_vpc_secgroup(mido_vpc_secgroup * vpcsecgroup);
int delete_mido_vpc_secgroup(mido_config *mido, mido_vpc_secgroup *vpcsecgroup);
int find_mido_vpc_secgroup(mido_config * mido, char *secgroupname, mido_vpc_secgroup ** outvpcsecgroup);

int connect_mido_vpc_instance(mido_config *mido, mido_vpc_subnet *vpcsubnet, mido_vpc_instance *inst, midoname *vmhost, char *instanceDNSDomain);

int connect_mido_vpc_instance_elip(mido_config * mido, mido_core * midocore, mido_vpc * vpc, mido_vpc_subnet * vpcsubnet, mido_vpc_instance * inst);
int disconnect_mido_vpc_instance_elip(mido_config *mido, mido_vpc_instance * vpcinstance);

int clear_mido_config(mido_config *mido);

int free_mido_config_v(mido_config * mido, int mode);
int free_mido_config(mido_config * mido);
int free_mido_resources(mido_resources *midoresource);
int free_mido_core(mido_core * midocore);
int free_mido_vpc(mido_vpc * vpc);
int free_mido_vpc_subnet(mido_vpc_subnet * vpcsubnet);
int free_mido_vpc_instance(mido_vpc_instance * vpcinstance);
int free_mido_vpc_natgateway(mido_vpc_natgateway *vpcnatgateway);
int free_mido_vpc_secgroup(mido_vpc_secgroup * vpcsecgroup);

void print_mido_vpc(mido_vpc * vpc);
void print_mido_vpc_subnet(mido_vpc_subnet * vpcsubnet);
void print_mido_vpc_instance(mido_vpc_instance * vpcinstance);
void print_mido_vpc_secgroup(mido_vpc_secgroup * vpcsecgroup);

int do_midonet_maint(mido_config *mido);
int do_midonet_update(globalNetworkInfo * gni, mido_config * mido);
int do_midonet_update_pass1(globalNetworkInfo * gni, mido_config * mido);
int do_midonet_update_pass2(globalNetworkInfo * gni, mido_config * mido);
int do_midonet_update_pass3_vpcs(globalNetworkInfo * gni, mido_config * mido);
int do_midonet_update_pass3_sgs(globalNetworkInfo * gni, mido_config * mido);
int do_midonet_update_pass3_insts(globalNetworkInfo * gni, mido_config * mido);

int do_midonet_teardown(mido_config * mido);

int do_metaproxy_setup(mido_config * mido);
int do_metaproxy_teardown(mido_config * mido);
int do_metaproxy_maintain(mido_config * mido, int mode);

int create_mido_meta_core(mido_config * mido);
int create_mido_meta_vpc_namespace(mido_config * mido, mido_vpc * vpc);
int create_mido_meta_subnet_veth(mido_config * mido, mido_vpc * vpc, char *name, char *subnet, char *slashnet, char **tapiface);

int delete_mido_meta_core(mido_config * mido);
int delete_mido_meta_vpc_namespace(mido_config * mido, mido_vpc * vpc);
int delete_mido_meta_subnet_veth(mido_config * mido, char *name);

int read_mido_meta_vpc_namespace(mido_config * mido, mido_vpc * vpc);

#endif /* ! _INCLUDE_EUCA_TO_MIDO_H_ */
