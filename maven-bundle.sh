#!/bin/sh
# Builds dev.signops:android, signs every file with the SignOps Maven key and zips it for the Central Portal
# (Publish Component, upload the zip). Key and passphrase: ~/.config/signops/maven-signing.env (15 Nov 2026).
set -e
cd "$(dirname "$0")"
. ~/.config/signops/maven-signing.env
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
rm -rf signops/build/maven-bundle
./gradlew -q :signops:testDebugUnitTest :signops:publishReleasePublicationToBundleRepository
B=signops/build/maven-bundle
rm -f $B/dev/signops/android/maven-metadata.xml*
for f in $(find $B -type f \( -name '*.aar' -o -name '*.jar' -o -name '*.pom' -o -name '*.module' \)); do
  gpg --batch --yes --pinentry-mode loopback --passphrase "$MAVEN_GPG_PASSPHRASE" --local-user "$MAVEN_GPG_KEY" --armor --detach-sign "$f"
done
V=$(ls $B/dev/signops/android)
(cd $B && rm -f ../signops-android-$V.zip && zip -qr ../signops-android-$V.zip dev)
echo "$(pwd)/signops/build/signops-android-$V.zip"
