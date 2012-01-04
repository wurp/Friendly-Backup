cd `dirname $0`
jar cfm ../friendly-backup.jar ../META-INF/MANIFEST.MF -C ../bin com -C ../resource .
cp ../friendly-backup.jar  ../dist/
cp -r ../lib/ ../dist/
