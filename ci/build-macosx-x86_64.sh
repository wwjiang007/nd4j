#!/bin/bash
set -evx

while true; do echo .; sleep 60; done &

if [[ $TRAVIS_PULL_REQUEST == "false" ]]; then
    BRANCH=$TRAVIS_BRANCH
    MAVEN_PHASE="deploy"
else
    BRANCH=$TRAVIS_PULL_REQUEST_BRANCH
    MAVEN_PHASE="install"
    echo Skipping Mac OS X builds on pull request because of limited resources
    exit 0
fi

if ! git -C $TRAVIS_BUILD_DIR/.. clone https://github.com/deeplearning4j/libnd4j/ --depth=50 --branch=$BRANCH; then
     git -C $TRAVIS_BUILD_DIR/.. clone https://github.com/deeplearning4j/libnd4j/ --depth=50
fi

brew update
brew upgrade maven || true
brew install gcc || true
brew link --overwrite gcc

/usr/local/bin/gcc-? --version
mvn -version

if [[ "${CUDA:-}" == "8.0" ]]; then
    CUDA_URL=https://developer.nvidia.com/compute/cuda/8.0/Prod2/local_installers/cuda_8.0.61_mac-dmg
elif [[ "${CUDA:-}" == "9.0" ]]; then
    CUDA_URL=https://developer.nvidia.com/compute/cuda/9.0/Prod/local_installers/cuda_9.0.176_mac-dmg
elif [[ "${CUDA:-}" == "9.1" ]]; then
    CUDA_URL=https://developer.nvidia.com/compute/cuda/9.1/Prod/local_installers/cuda_9.1.85_mac
fi
if [[ -n ${CUDA_URL:-} ]]; then
    curl --retry 10 -L -o $HOME/cuda.dmg $CUDA_URL
    hdiutil mount $HOME/cuda.dmg
    sleep 5
    sudo /Volumes/CUDAMacOSXInstaller/CUDAMacOSXInstaller.app/Contents/MacOS/CUDAMacOSXInstaller --accept-eula --no-window
fi

cd $TRAVIS_BUILD_DIR/../libnd4j/
sed -i="" /cmake_minimum_required/d CMakeLists.txt
MAKEJ=2 bash buildnativeoperations.sh -c cpu -e ${EXT:-}
if [[ -n "${CUDA:-}" ]]; then
    MAKEJ=1 bash buildnativeoperations.sh -c cuda -v $CUDA -cc 30
fi
cd $TRAVIS_BUILD_DIR/
if [[ -n "${CUDA:-}" ]]; then
    bash change-cuda-versions.sh $CUDA
    EXTRA_OPTIONS='-pl !nd4j-uberjar'
else
    EXTRA_OPTIONS='-pl !nd4j-uberjar,!nd4j-backends/nd4j-backend-impls/nd4j-cuda,!nd4j-backends/nd4j-backend-impls/nd4j-cuda-platform,!nd4j-backends/nd4j-tests'
fi
bash change-scala-versions.sh $SCALA
mvn clean $MAVEN_PHASE -B -U --settings ./ci/settings.xml -Dmaven.javadoc.skip=true -Dmaven.test.skip=true -Dlocal.software.repository=sonatype \
    -Djavacpp.extension=${EXT:-} $EXTRA_OPTIONS

