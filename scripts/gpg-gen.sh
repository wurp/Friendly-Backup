#!/bin/bash
set -e

if [ $# -ne 2 ]
then
	cat <<EOF 1>&2
Usage: $0 "<path where gnupg dir should be placed>" "email@example.com"
E.g.
$0 test/integ/happy1/client1/ "joe@gmail.com"
will create test/integ/happy1/client1/gnupg which contains the secret & public key for joe.
EOF
	exit -1
fi

# Generate a new keypair:
BASEDIR="$1"
GNAME="$2"

mkdir -p "$BASEDIR/gnupg"

echo "$GNAME" > "$BASEDIR/gpg.name" 

#Example of creating keys non-interactively
#echo password | gpg --batch --yes --passphrase-fd 0 --homedir $BASEDIR/gnupg/ --quick-generate-key "$GNAME"
gpg --homedir "$BASEDIR/gnupg/" --quick-generate-key "$GNAME"

gpg --homedir "$BASEDIR/gnupg/" --export -a "$GNAME" > $BASEDIR/gpg-public.key
