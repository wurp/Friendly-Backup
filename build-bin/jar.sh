cd `dirname $0`
echo Building jar
rm ../friendly-backup.jar
jar cfm ../friendly-backup.jar ../META-INF/MANIFEST.MF -C ../bin com -C ../resource .
echo Built jar, copying files
cp ../friendly-backup.jar  ../dist/generic/lib/
cp -r ../lib/* ../dist/generic/lib/
