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

#ifndef _INCLUDE_EUCA_GNI_H_
#define _INCLUDE_EUCA_GNI_H_

//!
//! @file net/euca_gni.h
//! Defines the global network interface
//!

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <libxml/tree.h>
#include <libxml/parser.h>
#include <libxml/xpath.h>
#include <libxml/xpathInternals.h>

#include <eucalyptus.h>
#include <data.h>
#include <euca_string.h>
#include <euca_network.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define MAX_NETWORK_INFO_LEN                     10485760   //!< The maximum length of the network info string in GNI structure

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

enum { GNI_ITERATE_PRINT, GNI_ITERATE_FREE };

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                 STRUCTURES                                 |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//! GNI element name structure
typedef struct gni_name_t {
    char name[1024];                   //!< GNI element name string
} gni_name;

//! GNI IP Table Rule
typedef struct gni_rule_t {
    int protocol;
    int fromPort;
    int toPort;
    int icmpType;
    int icmpCode;
    int cidrSlashnet;
    u32 cidrNetaddr;
    char cidr[NETWORK_ADDR_LEN];
    char groupId[SECURITY_GROUP_ID_LEN];
    char groupOwnerId[16];
} gni_rule;

//! GNI Security Group Information structure
typedef struct gni_secgroup_t {
    char accountId[128];               //!< Security Group Account ID string
    char name[SECURITY_GROUP_ID_LEN];  //!< Security Group Name string (i.e. sg-xxxxxxxx)
    char chainname[32];                //!< Associated chain name TODO: Really needed?
    gni_name *grouprules;              //!< List of associated rules with the group
    int max_grouprules;                //!< Number of rules in the list
    gni_rule *ingress_rules;
    int max_ingress_rules;
    gni_rule *egress_rules;
    int max_egress_rules;
    gni_name *instance_names;          //!< List of instance names
    int max_instance_names;            //!< Number of instance names in the list
    gni_name *interface_names;         //!< List of interface names
    int max_interface_names;           //!< Number of interface names in the list
} gni_secgroup;

//! GNI Instance Information structure
typedef struct gni_instance_t {
    char name[INTERFACE_ID_LEN];       //!< Instance ID string
    char ifname[INTERFACE_ID_LEN];     //!< Interface ID string
    char attachmentId[ENI_ATTACHMENT_ID_LEN]; //!< Attachment ID string
    char accountId[128];               //!< Instance Account ID string
    u8 macAddress[ENET_BUF_SIZE];      //!< Associated MAC address
    u32 publicIp;                      //!< Assigned public IP address
    u32 privateIp;                     //!< Assigned private IP address
    char vpc[16];                      //!< VPC ID associated with this interface
    char subnet[16];                   //!< subnet ID associated with this interface
    char node[HOSTNAME_LEN];
    char nodehostname[HOSTNAME_LEN];
    boolean srcdstcheck;               //!< Source/Destination Check flag (only for interfaces)
    int deviceidx;                     //!< NIC device index (only for interfaces)
    gni_name instance_name;            //!< Instance name associated
    gni_name *secgroup_names;          //!< List of associated security group names
    int max_secgroup_names;            //!< Number of security group names in the list
    gni_name *interface_names;         //!< List of associated interface names (only for instances)
    int max_interface_names;           //!< Number of interface names in the list
} gni_instance;

//! GNI Subnet Information Structure
typedef struct gni_subnet_t {
    u32 subnet;                        //!< Subnet address
    u32 netmask;                       //!< Netmask address
    u32 gateway;                       //!< Gateway address
} gni_subnet;

//! GNI Managed Subnet Information Structure
typedef struct gni_managedsubnet_t {
    u32 subnet;                        //!< Subnet address
    u32 netmask;                       //!< Netmask address
    u16 minVlan;                       //!< Minimum usable VLAN
    u16 maxVlan;                       //!< Maximum usable VLAN
    u32 segmentSize;                   //!< How big should we make each segment?
} gni_managedsubnet;

//! GNI Node Information Structure
typedef struct gni_node_t {
    char name[HOSTNAME_LEN];           //!< The Node name
    gni_name *instance_names;          //!< A list of associated instance names
    int max_instance_names;            //!< Number of instance names in the list
} gni_node;

//! GNI Cluster Information Structure
typedef struct gni_cluster_t {
    char name[HOSTNAME_LEN];           //!< The Cluster name
    u32 enabledCCIp;                   //!< The enabled CC IP address
    char macPrefix[ENET_MACPREFIX_LEN]; //!< The MAC address prefix to use for instances
    gni_subnet private_subnet;         //!< Cluster Subnet Information
    u32 *private_ips;                  //!< List of private IPs associated with this cluster
    int max_private_ips;               //!< Number of private IPs in the list
    gni_node *nodes;                   //!< List of associated nodes information
    int max_nodes;                     //!< Number of nodes in the lsit
} gni_cluster;

typedef struct gni_network_acl_t {
} gni_network_acl;

typedef struct gni_route_entry_t {
    char destCidr[16];
    char target[32];
} gni_route_entry;

typedef struct gni_route_table_t {
    char name[16];
    char accountId[128];
    gni_route_entry *entries;
    int max_entries;
} gni_route_table;

typedef struct gni_internet_gateway_t {
    char name[16];
    char accountId[128];
} gni_internet_gateway;

typedef struct gni_nat_gateway_t {
    char name[32];
    char accountId[128];
    u8 macAddress[ENET_BUF_SIZE];
    u32 publicIp;
    u32 privateIp;
    char vpc[16];
    char subnet[16];
} gni_nat_gateway;

typedef struct gni_vpcsubnet_t {
    char name[16];
    char accountId[128];
    char cidr[24];
    char cluster_name[HOSTNAME_LEN];
    char networkAcl_name[16];
    char routeTable_name[16];
} gni_vpcsubnet;

typedef struct gni_vpc_t {
    char name[16];
    char accountId[128];
    char cidr[24];
    char dhcpOptionSet[16];
    gni_vpcsubnet *subnets;
    int max_subnets;
    gni_network_acl *networkAcls;
    int max_networkAcls;
    gni_route_table *routeTables;
    int max_routeTables;
    gni_nat_gateway *natGateways;
    int max_natGateways;
    gni_name *internetGatewayNames;
    int max_internetGatewayNames;
} gni_vpc;

typedef struct gni_hostname_t {
    struct in_addr ip_address;
    char hostname[HOSTNAME_SIZE];
} gni_hostname;

typedef struct gni_hostname_info_t {
    gni_hostname *hostnames;
    int max_hostnames;
} gni_hostname_info;

//! Global GNI Information Structure
typedef struct globalNetworkInfo_t {
    boolean init;                           //!< has the structure been initialized successfully?
    char networkInfo[MAX_NETWORK_INFO_LEN]; //!< XML content used to build this structure
    char version[32];                       //!< latest version ID of the document
    char appliedVersion[32];                //!< latest known applied version ID of the document
    char sMode[NETMODE_LEN];                //!< The network mode string passed in the GNI
    euca_netmode nmCode;                    //!< The network mode code (see euca_netmode_t)
    u32 enabledCLCIp;                       //!< IP address of the enabled CLC
    char EucanetdHost[HOSTNAME_LEN];
    char GatewayHosts[HOSTNAME_LEN*3*33];
    char PublicNetworkCidr[HOSTNAME_LEN];
    char PublicGatewayIP[HOSTNAME_LEN];
    char instanceDNSDomain[HOSTNAME_LEN];   //!< The DNS domain name to use for the instances
    u32 *instanceDNSServers;           //!< List of DNS servers
    int max_instanceDNSServers;        //!< Number of DNS servers in the list
#ifdef USE_IP_ROUTE_HANDLER
    u32 publicGateway;                 //!< Public network default gateway
#endif /* USE_IP_ROUTE_HANDLER */
    u32 *public_ips;                   //!< List of associated public IPs
    int max_public_ips;                //!< Number of associated public IPs in the list
    gni_subnet *subnets;               //!< List of global subnet information
    int max_subnets;                   //!< Number of global subnets in the list
    gni_managedsubnet *managedSubnet;  //!< List of managed subnet entry for MANAGED modes
    int max_managedSubnets;            //!< Number of global managed subnets in the list
    gni_cluster *clusters;             //!< List of clusters information
    int max_clusters;                  //!< Number of clusters in the list
    gni_instance *instances;           //!< List of instances information
    int max_instances;                 //!< Number of instances in the list
    gni_instance *interfaces;          //!< List of interfaces information
    int max_interfaces;                //!< Number of interfaces in the list
    gni_secgroup *secgroups;           //!< List of security group information
    int max_secgroups;                 //!< Number of security groups in the list
    gni_vpc *vpcs;                     //!< List of VPC information
    int max_vpcs;                      //!< Number of VPCs
    gni_internet_gateway *vpcIgws;     //!< List of VPC Internet Gateways
    int max_vpcIgws;                   //!< Number of VPC Internet Gateways
} globalNetworkInfo;

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

globalNetworkInfo *gni_init(void);
int gni_free(globalNetworkInfo * gni);
int gni_clear(globalNetworkInfo * gni);
int gni_print(globalNetworkInfo * gni);
int gni_iterate(globalNetworkInfo * gni, int mode);
int gni_populate(globalNetworkInfo * gni, gni_hostname_info *host_info, char *xmlpath);
int gni_populate_instances(globalNetworkInfo * gni, xmlXPathContextPtr ctxptr);
int gni_populate_interfaces(globalNetworkInfo * gni, xmlXPathContextPtr ctxptr);
int gni_populate_sgs(globalNetworkInfo * gni, xmlXPathContextPtr ctxptr);
int gni_populate_vpcs(globalNetworkInfo * gni, xmlXPathContextPtr ctxptr);
int gni_populate_gnidata(globalNetworkInfo * gni, xmlXPathContextPtr ctxptr);
int gni_populate_configuration(globalNetworkInfo * gni, gni_hostname_info *host_info, xmlXPathContextPtr ctxptr);

int gni_populate_instance_interface(gni_instance *instance, const char *xmlpath, xmlXPathContextPtr ctxptr);

int gni_is_self(const char *test_ip);

int gni_cluster_clear(gni_cluster *cluster);
int gni_node_clear(gni_node *node);
int gni_instance_clear(gni_instance *instance);
int gni_secgroup_clear(gni_secgroup *secgroup);
int gni_vpc_clear(gni_vpc *vpc);

int gni_find_self_node(globalNetworkInfo * gni, gni_node ** outnodeptr);
int gni_find_self_cluster(globalNetworkInfo * gni, gni_cluster ** outclusterptr);
int gni_find_secgroup(globalNetworkInfo * gni, const char *psGroupId, gni_secgroup ** pSecGroup);
int gni_find_instance(globalNetworkInfo * gni, const char *psInstanceId, gni_instance ** pInstance);
int gni_find_secondary_interfaces(globalNetworkInfo * gni, const char *psInstanceId, gni_instance * pAInstances[], int * size);

int gni_cloud_get_clusters(globalNetworkInfo * gni, char **cluster_names, int max_cluster_names, char ***out_cluster_names, int *out_max_cluster_names, gni_cluster ** out_clusters,
                           int *out_max_clusters);
int gni_cloud_get_secgroups(globalNetworkInfo * pGni, char **psSecGroupNames, int nbSecGroupNames, char ***psOutSecGroupNames, int *pOutNbSecGroupNames,
                           gni_secgroup ** pOutSecGroups, int *pOutNbSecGroups);
int gni_cluster_get_nodes(globalNetworkInfo * gni, gni_cluster * cluster, char **node_names, int max_node_names, char ***out_node_names, int *out_max_node_names,
                          gni_node ** out_nodes, int *out_max_nodes);
int gni_cluster_get_instances(globalNetworkInfo * pGni, gni_cluster * pCluster, char **psInstanceNames, int maxInstanceNames, char ***psOutInstanceNames, int *pOutNbInstanceNames,
                              gni_instance ** pOutInstances, int *pOutNbInstances);
int gni_cluster_get_secgroup(globalNetworkInfo * pGni, gni_cluster * pCluster, char **psSecGroupNames, int nbSecGroupNames, char ***psOutSecGroupNames, int *pOutNbSecGroupNames,
                             gni_secgroup ** pOutSecGroups, int *pOutNbSecGroups);
int gni_node_get_instances(globalNetworkInfo * gni, gni_node * node, char **instance_names, int max_instance_names, char ***out_instance_names, int *out_max_instance_names,
                           gni_instance ** out_instances, int *out_max_instances);
int gni_node_get_secgroup(globalNetworkInfo * pGni, gni_node * pNode, char **psSecGroupNames, int nbSecGroupNames, char ***psOutSecGroupNames, int *pOutNbSecGroupNames,
                          gni_secgroup ** pOutSecGroups, int *pOutNbSecGroups);
int gni_instance_get_secgroups(globalNetworkInfo * gni, gni_instance * instance, char **secgroup_names, int max_secgroup_names, char ***out_secgroup_names,
                               int *out_max_secgroup_names, gni_secgroup ** out_secgroups, int *out_max_secgroups);
int gni_secgroup_get_instances(globalNetworkInfo * gni, gni_secgroup * secgroup, char **instance_names, int max_instance_names, char ***out_instance_names,
                               int *out_max_instance_names, gni_instance ** out_instances, int *out_max_instances);
int gni_secgroup_get_interfaces(globalNetworkInfo * gni, gni_secgroup * secgroup,
        char **interface_names, int max_interface_names, char ***out_interface_names,
        int *out_max_interface_names, gni_instance ** out_interfaces, int *out_max_interfaces);
int gni_secgroup_get_chainname(globalNetworkInfo * gni, gni_secgroup * secgroup, char **outchainname);
gni_route_table *gni_vpc_get_routeTable(gni_vpc *vpc, const char *tableName);
gni_vpcsubnet *gni_vpc_get_vpcsubnet(gni_vpc *vpc, const char *vpcsubnetName);
int gni_vpc_get_interfaces(globalNetworkInfo *gni, gni_vpc *vpc, gni_instance ***out_interfaces, int *max_out_interfaces);

int gni_validate(globalNetworkInfo * gni);
int gni_netmode_validate(const char *psMode);
int gni_subnet_validate(gni_subnet * subnet);
int gni_managed_subnet_validate(gni_managedsubnet * pSubnet);
int gni_cluster_validate(gni_cluster * cluster, boolean isManaged);
int gni_node_validate(gni_node * node);
int gni_instance_validate(gni_instance * instance);
int gni_interface_validate(gni_instance * interface);
int gni_secgroup_validate(gni_secgroup * secgroup);

int gni_serialize_iprange_list(char **inlist, int inmax, u32 ** outlist, int *outmax);
int evaluate_xpath_property(xmlXPathContextPtr ctxptr, char *expression, char ***results, int *max_results);
int evaluate_xpath_element(xmlXPathContextPtr ctxptr, char *expression, char ***results, int *max_results);

void gni_instance_interface_print(gni_instance *inst, int loglevel);
void gni_sg_print(gni_secgroup *sg, int loglevel);

gni_hostname_info *gni_init_hostname_info(void);
int gni_hostnames_print(gni_hostname_info *host_info);
int gni_hostnames_free(gni_hostname_info *host_info);
int gni_hostnames_get_hostname(gni_hostname_info *host_info, const char *ip_address, char **hostname);
int cmpipaddr(const void *p1, const void *p2);

int ruleconvert(char *rulebuf, char *outrule);
int ingress_gni_to_iptables_rule(char *scidr, gni_rule *iggnirule, char *outrule, int flags);

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

//! A macro equivalent to the gni_free() call and ensures the given pointer is set to NULL
#define GNI_FREE(_pGni) \
{                       \
    gni_free(_pGni);    \
    (_pGni) = NULL;     \
}

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                          STATIC INLINE IMPLEMENTATION                      |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#endif /* ! _INCLUDE_EUCA_GNI_H_ */
