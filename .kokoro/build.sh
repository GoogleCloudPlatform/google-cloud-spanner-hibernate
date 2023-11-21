#!/bin/bash
# Copyright 2019-2020 Google LLC
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA

set -eo pipefail

dir=$(dirname "$0")

sudo apt install -y openjdk-17-jdk openjdk-17-jre

echo $JAVA_HOME
echo $PATH

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH"

ls -lha /usr/lib/jvm
java -version
mvn -version

pushd $dir/../
./mvnw install -B -V -DskipITs
popd

source $dir/release_snapshot.sh
