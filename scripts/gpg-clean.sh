#!/bin/bash
set -e

if [ $# -ne 1 -o ! -d "$1/gnupg" ]
then
	cat <<EOF 1>&2
Usage: $0 "<path containing gnupg dir>"
E.g.
$0 test/integ/happy1/client1/
will clean up the gpg files/directories in client1, if test/integ/happy1/client1/gnupg exists.
EOF
	exit -1
fi

BASEDIR="$1"

rm -rf "$BASEDIR/gnupg"
rm "$BASEDIR/gpg.name"
rm "$BASEDIR/gpg-public.key"
