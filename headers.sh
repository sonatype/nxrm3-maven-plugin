#!/bin/bash
#
# Sonatype Nexus (TM) Open Source Version
# Copyright (c) 2007-2018 Sonatype, Inc.
# All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
#
# This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
# which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
#
# Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
# of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
# Eclipse Foundation. All other trademarks are the property of their respective owners.
#

dirname=`dirname $0`
dirname=`cd "$dirname" && pwd`
cd "$dirname"

op=$1; shift
case "$op" in
    'check' | 'format')
        ;;
     *)
        echo "usage: `basename $0` { check | format } [mvn-options]"
        exit 1
esac

# still depends on profiles defined in https://github.com/sonatype/buildsupport/blob/master/pom.xml
mvn -f ./pom.xml -N -P license-${op} "$@"
