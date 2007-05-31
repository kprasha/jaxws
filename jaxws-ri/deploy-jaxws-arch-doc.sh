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

#
# generate the architecture document and deploy that into the java.net CVS repository
#

ant architecture-document

cd build

if [ -e jaxws-architecture-document-www ]
then
  cd jaxws-architecture-document-www
  cvs update -Pd
  cd ..
else
  cvs "-d:pserver:kohsuke@kohsuke.sfbay:/cvs" -z9 co -d jaxws-architecture-document-www jax-ws-architecture-document/www
fi

cd jaxws-architecture-document-www

cp -R ../javadoc/* doc

# ignore everything under CVS, then
# ignore all files that are already in CVS, then
# add the rest of the files
find . -name CVS -prune -o -exec bash ../../tools/scripts/in-cvs.sh {} \; -o \( -print -a -exec cvs add {} \+ \)

# sometimes the first commit fails
cvs commit -m "commit 1 " || cvs commit -m "commit 2"
