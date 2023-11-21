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

source $dir/common.sh
source $dir/install_jdk17.sh

pushd $dir/../

MAVEN_SETTINGS_FILE=$(realpath .)/settings.xml

setup_environment_secrets
create_settings_xml_file $MAVEN_SETTINGS_FILE

# install and run unit tests
mvn install -B -V -DskipITs

# change to release version
mvn versions:set -DremoveSnapshot

# stage release
mvn clean deploy -B \
  -DskipTests=true \
  --settings ${MAVEN_SETTINGS_FILE} \
  -DperformRelease=true \
  -Dgpg.executable=gpg \
  -Dgpg.passphrase=${GPG_PASSPHRASE} \
  -Dgpg.homedir=${GPG_HOMEDIR} \
  -P release

export MAVEN_OPTS="--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.desktop/java.awt.font=ALL-UNNAMED"
# promote release
# if [[ -n "${AUTORELEASE_PR}" ]]
# then
  mvn nexus-staging:release -B \
    -DperformRelease=true \
    --settings=settings.xml \
    -P release
#fi

popd
