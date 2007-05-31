#!/bin/bash -x

#
# The contents of this file are subject to the terms
# of the Common Development and Distribution License
# (the License).  You may not use this file except in
# compliance with the License.
# 
# You can obtain a copy of the license at
# https://glassfish.dev.java.net/public/CDDLv1.0.html.
# See the License for the specific language governing
# permissions and limitations under the License.
# 
# When distributing Covered Code, include this CDDL
# Header Notice in each file and include the License file
# at https://glassfish.dev.java.net/public/CDDLv1.0.html.
# If applicable, add the following below the CDDL Header,
# with the fields enclosed by brackets [] replaced by
# you own identifying information:
# "Portions Copyrighted [year] [name of copyright owner]"
# 
# Copyright 2007 Sun Microsystems Inc. All Rights Reserved
#

# checks if the argument is under the control of CVS.
# if it's in, return 0, otherwise 1
ent="`dirname "$1"`/CVS/Entries"
if [ ! -e "$ent" ];
then
  # no entry file
  exit 1
fi

grep "\/`basename "$1"`\/" "$ent" > /dev/null
if [ $? -eq 0 ];
then
  # found
  exit 0
else
  # not
  exit 1
fi