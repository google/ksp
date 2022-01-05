cd ../exhaustive-processor
./gradlew :processor:publishToMavenLocal
cd ..
mkdir tmp
cd tmp
git clone https://github.com/inorichi/tachiyomi.git
cd tachiyomi
git checkout 938339690eecdfe309d83264b6a89aff3c767687
git apply ../../runner/tachi.patch
#./gradlew :app:kspDevDebugKotlin
#cd ../..
#rm -rf tmp