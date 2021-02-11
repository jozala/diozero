#!/bin/sh

PATH=/home/vagrant/rpi-tools/arm-bcm2708/gcc-linaro-arm-linux-gnueabihf-raspbian/bin:$PATH
make clean && make CROSS_COMPILE=arm-linux-gnueabihf- ARCH=armv6 CC_CFLAGS="-mfpu=vfp -mfloat-abi=hard"
cp libdiozero-system-utils.so armv6/libdiozero-system-utils.so
