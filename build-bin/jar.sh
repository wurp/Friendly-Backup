cd `dirname $0`
echo Building jar
jar cfm ../friendly-backup.jar ../META-INF/MANIFEST.MF -C ../bin com -C ../resource .
echo Built jar, copying files
cp ../friendly-backup.jar  ../dist/lib/
cp -r ../lib/ ../dist/
