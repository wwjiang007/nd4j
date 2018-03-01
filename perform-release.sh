#!/bin/bash
set -eu

if [[ $# < 2 ]]; then
    echo "Usage: bash perform-release.sh release_version snapshot_version [staging_repository]"
    exit 1
fi

RELEASE_VERSION=$1
SNAPSHOT_VERSION=$2
STAGING_REPOSITORY=${3:-}
SKIP_BUILD=${SKIP_BUILD:-0}
RELEASE_PROFILE=${RELEASE_PROFILE:-sonatype}

echo "Releasing version $RELEASE_VERSION ($SNAPSHOT_VERSION) to repository $RELEASE_PROFILE $STAGING_REPOSITORY"
echo "========================================================================================================="

if [[ ! -z $(git tag -l "nd4j-$RELEASE_VERSION") ]]; then
    echo "Error: Version $RELEASE_VERSION has already been released!"
    exit 1
fi

cd ../libnd4j
export TRICK_NVCC=YES
export LIBND4J_HOME="$(pwd)"
if [[ -z $(git tag -l "libnd4j-$RELEASE_VERSION") ]]; then
    if [[ "${SKIP_BUILD}" == "0" ]]; then
        bash buildnativeoperations.sh -c cpu
        bash buildnativeoperations.sh -c cuda -v 8.0
        bash buildnativeoperations.sh -c cuda -v 9.0
    fi
    git tag -s -a -m "libnd4j-$RELEASE_VERSION" "libnd4j-$RELEASE_VERSION"
    git tag -s -a -f -m "libnd4j-$RELEASE_VERSION" "latest_release"
fi
cd ../nd4j

mvn versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=$RELEASE_VERSION

if [[ "${SKIP_BUILD}" == "0" ]]; then
    if [[ -z ${STAGING_REPOSITORY:-} ]]; then
        # create new staging repository with everything in it
        source change-scala-versions.sh 2.10
        source change-cuda-versions.sh 8.0
        mvn clean deploy -Dgpg.executable=gpg2 -pl '!nd4j-backends/nd4j-tests' -DperformRelease -Dlocal.software.repository=$RELEASE_PROFILE -Dmaven.test.skip -Denforcer.skip -DstagingRepositoryId=$STAGING_REPOSITORY

        if [[ ! $(grep stagingRepository.id target/nexus-staging/staging/*.properties) =~ ^stagingRepository.id=(.*) ]]; then
            STAGING_REPOSITORY=
        else
            STAGING_REPOSITORY=${BASH_REMATCH[1]}
        fi

        source change-scala-versions.sh 2.11
        source change-cuda-versions.sh 9.0
        mvn clean deploy -Dgpg.executable=gpg2 -pl '!nd4j-backends/nd4j-tests' -DperformRelease -Dlocal.software.repository=$RELEASE_PROFILE -Dmaven.test.skip -Denforcer.skip -DstagingRepositoryId=$STAGING_REPOSITORY

        # build for Android with the NDK
        export ANDROID_NDK=~/Android/android-ndk/

        cd ../libnd4j
        bash buildnativeoperations.sh -c cpu -platform android-arm
        cd ../nd4j
        mvn clean deploy -Djavacpp.platform=android-arm -am -pl nd4j-backends/nd4j-backend-impls/nd4j-native -Dgpg.executable=gpg2 -DperformRelease -Dlocal.software.repository=$RELEASE_PROFILE -Dmaven.test.skip -Denforcer.skip -Dmaven.javadoc.skip -DstagingRepositoryId=$STAGING_REPOSITORY

        cd ../libnd4j
        bash buildnativeoperations.sh -c cpu -platform android-x86
        cd ../nd4j
        mvn clean deploy -Djavacpp.platform=android-x86 -am -pl nd4j-backends/nd4j-backend-impls/nd4j-native -Dgpg.executable=gpg2 -DperformRelease -Dlocal.software.repository=$RELEASE_PROFILE -Dmaven.test.skip -Denforcer.skip -Dmaven.javadoc.skip -DstagingRepositoryId=$STAGING_REPOSITORY

    else
        # build only partially (on other platforms) and deploy to given repository
        source change-scala-versions.sh 2.10
        source change-cuda-versions.sh 8.0
        mvn clean deploy -am -pl nd4j-backends/nd4j-backend-impls/nd4j-native,nd4j-backends/nd4j-backend-impls/nd4j-cuda -Dgpg.useagent=false -DperformRelease -Dlocal.software.repository=$RELEASE_PROFILE -Dmaven.test.skip -Denforcer.skip -Dmaven.javadoc.skip -DstagingRepositoryId=$STAGING_REPOSITORY

        source change-scala-versions.sh 2.11
        source change-cuda-versions.sh 9.0
        mvn clean deploy -am -pl nd4j-backends/nd4j-backend-impls/nd4j-native,nd4j-backends/nd4j-backend-impls/nd4j-cuda -Dgpg.useagent=false -DperformRelease -Dlocal.software.repository=$RELEASE_PROFILE -Dmaven.test.skip -Denforcer.skip -Dmaven.javadoc.skip -DstagingRepositoryId=$STAGING_REPOSITORY
    fi

    source change-scala-versions.sh 2.11
    source change-cuda-versions.sh 9.0
fi

git commit -s -a -m "Update to version $RELEASE_VERSION"
git tag -s -a -m "nd4j-$RELEASE_VERSION" "nd4j-$RELEASE_VERSION"
git tag -s -a -f -m "nd4j-$RELEASE_VERSION" "latest_release"

mvn versions:set -DallowSnapshots=true -DgenerateBackupPoms=false -DnewVersion=$SNAPSHOT_VERSION
git commit -s -a -m "Update to version $SNAPSHOT_VERSION"

echo "Successfully performed release of version $RELEASE_VERSION ($SNAPSHOT_VERSION) to repository $RELEASE_PROFILE $STAGING_REPOSITORY"
