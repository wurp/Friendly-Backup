#!/bin/bash

set -e

if [ $# -ne 2 ]
then
	cat <<EOF 1>&2
Usage: $0 "<path containing gnupg of friend 1>" "<path containing gnupg of friend 2>"
E.g.
$0 test/integ/happy1/client1/ test/integ/happy1/client2/
will put test/integ/happy1/client2/ in test/integ/happy1/client1/'s public keyring and vice versa
EOF
	exit -1
fi

DIR1="$1"
DIR2="$2"

# Make dir1 be friends with dir2, and dir2 be friends with dir1
gpg --homedir "$DIR1"/gnupg/ --import "$DIR2"/gpg-public.key
gpg --homedir "$DIR2"/gnupg/ --import "$DIR1"/gpg-public.key
