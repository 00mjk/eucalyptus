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

//!
//! @file net/euca_arp.c
//! Implements the API necessary to work with ARP
//!

#ifdef USE_EUCA_ARP
/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  INCLUDES                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#include <stdio.h>
#include <ctype.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <netdb.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <net/if_arp.h>
#include <netinet/if_ether.h>
#include <linux/if_ether.h>

#include <eucalyptus.h>
#include <misc.h>
#include <log.h>
#include <euca_string.h>
#include <atomic_file.h>

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  DEFINES                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

#define ARP_HLEN                                 28 //!< ARP Header Length (28 bytes)
#define VLAN_HLEN                                 6 //!< VLAN Header Length (4 bytes)

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                  TYPEDEFS                                  |
 |                                                                            |
\*----------------------------------------------------------------------------*/

typedef struct ethhdr eth_header;
typedef struct ether_arp arp_header;
typedef struct vlan_hdr_t vlan_header;

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

//! 802.1Q VLAN Header
struct vlan_hdr_t {
    u_short tpid;                      //!< Tag Protocol Identifier
    u_short tci;                       //!< Tag Control Information (3 bits PCP, 1 bit DEI, 12 bits VLAN Identifier)
    u_short h_proto;                   //!< Next protocol in line
} __attribute__ ((packed));

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                             EXTERNAL VARIABLES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/* Should preferably be handled in header file */

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              GLOBAL VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC VARIABLES                              |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static u_char gArpPkt[ETH_FRAME_LEN] = { 0 };   //!< The ARP packet to send
static u_char gEthZero[ETH_ALEN] = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };  //!< Ethernet Zero Address 00:00:00:00:00:00
static u_char gEthBroadcast[ETH_ALEN] = { 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF }; //!< Ethernet Broadcast Address FF:FF:FF:FF:FF:FF

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                              STATIC PROTOTYPES                             |
 |                                                                            |
\*----------------------------------------------------------------------------*/

static int send_gratuitous_arp(const char *psDevice, const char *psIp, const char *psMac, int vlan);
static void usage(void);

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                                   MACROS                                   |
 |                                                                            |
\*----------------------------------------------------------------------------*/

/*----------------------------------------------------------------------------*\
 |                                                                            |
 |                               IMPLEMENTATION                               |
 |                                                                            |
\*----------------------------------------------------------------------------*/

//!
//! Sends a gratuitous ARP on behalf of a given host
//!
//! @param[in] psInterface a constant string pointer to the interface name to send onto
//! @param[in] psIp a constant string pointer to the IP address of the host we are sending the gratuitous ARP for
//! @param[in] psMac a constant string pointer to the MAC address associated with the IP address
//! @param[in] vlan the VLAN identifier to use or -1 if no VLAN are to be used
//!
//! @return 0 on success or -1 on failure
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
static int send_gratuitous_arp(const char *psDevice, const char *psIp, const char *psMac, int vlan)
{
    int rc = 0;
    int len = 0;
    int sock = 0;
    u_int ip = 0;
    u_char *pMac = NULL;
    u_char aMac[ETH_ALEN] = { 0x00 };
    eth_header *pEth = ((eth_header *) gArpPkt);
    arp_header *pArp = ((arp_header *) (gArpPkt + ETH_HLEN));
    vlan_header *pVlan = NULL;
    struct sockaddr sa = { 0 };

    // Make sure we were provided with an interface, a MAC and an IP
    if (!psDevice || !psIp || !psMac) {
        LOGERROR("Fail to send gratuitous ARP to device %s for IP %s using MAC %s\n", SP(psDevice), SP(psIp), SP(psMac));
        return (1);
    }
    // Convert the MAC address to hexadecimal
    if ((pMac = euca_mac2hex(psMac, aMac)) == aMac) {
        // Convert the IP address
        ip = htonl(euca_dot2hex(psIp));

        // Set the ethernet source, destination address
        memcpy(pEth->h_source, aMac, ETH_ALEN);
        memcpy(pEth->h_dest, gEthBroadcast, ETH_ALEN);

        if ((vlan < 0) || (vlan > 0xFFF)) {
            // Go straight to ARP protocol
            pEth->h_proto = htons(ETH_P_ARP);
        } else {
            // Take a detour using 802.1Q
            pEth->h_proto = htons(ETH_P_8021Q);

            // Set the VLAN header
            pVlan = ((vlan_header *) & pEth->h_proto);
            pVlan->tci = htons(vlan & 0x0FFF);
            pVlan->h_proto = htons(ETH_P_ARP);

            // Re-adjust the ARP header
            pArp = ((arp_header *) (((u_char *) pVlan) + VLAN_HLEN));
        }

        // Fill the ARP header information for our host
        pArp->ea_hdr.ar_hrd = htons(ARPHRD_ETHER);
        pArp->ea_hdr.ar_pro = htons(ETH_P_IP);
        pArp->ea_hdr.ar_hln = ETHER_ADDR_LEN;
        pArp->ea_hdr.ar_pln = sizeof(in_addr_t);
        pArp->ea_hdr.ar_op = htons(ARPOP_REQUEST);
        memcpy(pArp->arp_sha, aMac, ETH_ALEN);
        memcpy(pArp->arp_spa, &ip, 4);
        memcpy(pArp->arp_tha, gEthZero, ETH_ALEN);
        memcpy(pArp->arp_tpa, &ip, 4);

        // Calculate the length
        len = ((u_char *) & pArp->arp_tpa[3]) - ((u_char *) pEth);

        // Open a socket to transmit this packet
        if ((sock = socket(PF_PACKET, SOCK_PACKET, htons(ETH_P_ARP))) < 0) {
            LOGERROR("Fail to send gratuitous ARP on device %s for IP %s using MAC %s and VLAN %d. socket = %d, %d - %s\n", psDevice, psIp, psMac, vlan, sock, errno,
                     strerror(errno));
            return (1);
        }
        // Send out on the given interface
        bzero(sa.sa_data, sizeof(sa.sa_data));
        strncpy(sa.sa_data, psDevice, (sizeof(sa.sa_data) - 1));

        if ((rc = sendto(sock, gArpPkt, len, (MSG_DONTROUTE | MSG_DONTWAIT), &sa, sizeof(sa))) < len) {
            LOGERROR("Fail to send gratuitous ARP on device %s for IP %s using MAC %s and VLAN %d. rc = %d, len = %d\n", psDevice, psIp, psMac, vlan, rc, len);
            close(sock);
            return (1);
        }

        LOGDEBUG("Sent gratuitous ARP on device %s for IP %s using MAC %s and VLAN %d.\n", psDevice, psIp, psMac, vlan);
        close(sock);
        return (0);
    }
    LOGERROR("Fail to convert MAC address %s to hexadecimal\n", psMac);
    return (1);
}

//!
//! Prints the test application usage string
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
static void usage(void)
{
    fprintf(stderr, "usage: send_arp <if> <ip> <mac> [vlan]\n\n");
}

//!
//! Main entry point of the application
//!
//! @param[in] argc the number of parameter passed on the command line
//! @param[in] argv the list of arguments
//!
//! @return EUCA_OK on success or EUCA_ERROR on failure.
//!
//! @see
//!
//! @pre
//!
//! @post
//!
//! @note
//!
int main(int argc, char *argv[])
{
#define NB_ARG_MIN       4
#define APP_ARG_INDEX    0
#define IF_ARG_INDEX     1
#define IP_ARG_INDEX     2
#define MAC_ARG_INDEX    3
#define VLAN_ARG_INDEX   4

    int vlan = -1;
    char *psIp = NULL;
    char *psMac = NULL;
    char *psIf = NULL;

    if (argc < NB_ARG_MIN) {
        usage();
        return (0);
    }

    psIf = argv[IF_ARG_INDEX];
    psIp = argv[IP_ARG_INDEX];
    psMac = argv[MAC_ARG_INDEX];
    if (argc > NB_ARG_MIN)
        vlan = atoi(argv[VLAN_ARG_INDEX]);

    send_gratuitous_arp(psIf, psIp, psMac, vlan);
    return (0);

#undef NB_ARGC
#undef APP_ARG_INDEX
#undef IF_ARG_INDEX
#undef IP_ARG_INDEX
#undef MAC_ARG_INDEX
#undef VLAN_ARG_INDEX
}
#endif /* USE_EUCA_ARP */
