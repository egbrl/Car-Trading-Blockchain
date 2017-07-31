# Live Env Documentation

The car-demo.iso image is based on Ubuntu 16.04. The OS will automatically launch the needed docker containers on startup. The startup script will ask you to establish an internet connection on startup.

Instructions used to bootstrap the iso can be found here:
[https://nathanpfry.com/how-to-customize-an-ubuntu-installation-disc/](https://nathanpfry.com/how-to-customize-an-ubuntu-installation-disc/)

## Live USB
* Make an empty DOS partitoin table
* Create an empty FAT32 partition
* Copy image to drive with [UNetbootin](https://unetbootin.github.io/)
* To boot instantly with small timeout, replace the grub configuration file on the drive in `boot/grub/grub.cfg` with the provided [grub.cfg](https://github.com/EGabb/Car-Trading-Blockchain/blob/live-iso/live-usb/grub.cfg)
