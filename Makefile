
all: ndkbuild

init:
	git submodule update --init

# NOTE: adding v (verbose) flag for the beginning stage:
ndkbuild:
	rm -rf lib libs
	ndk-build
    # zip sqlite-native-driver-libs.zip libs/*/*

clean:
	rm -rf obj lib libs sqlite-native-driver.jar # *.zip

