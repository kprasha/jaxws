#Builds nightly binary and source bundles. 
#Bundles will be pushed to java.net.

cd jax-ws-sources/jaxws-ri
rm -rf build
ant -Dbuild.id=jaxws-rearch-2005-nightly_$BUILD_ID clean generate-weekly-binary generate-weekly-source
