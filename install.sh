set -e

if [ -z "$1" ]
then
	echo "Usage: install <dir in which to install>" 1>&2
	exit -1
fi

export destdir=$(readlink -m "$1")

cd $(dirname "$0")

export fbdir=$(readlink -m .)

mkdir -p "$destdir"/bin/var
cp -r "$fbdir"/dist/generic/* "$destdir"/bin/
[ -f "$destdir"/bin/lib/friendly-backup.jar ] && mv "$destdir"/bin/lib/friendly-backup.jar "$destdir"/bin/lib/friendly-backup.jar.bak
ln -s $(readlink -m "$fbdir"/target/Friendly-Backup-*-jar-with-dependencies.jar) "$destdir"/bin/lib/friendly-backup.jar
