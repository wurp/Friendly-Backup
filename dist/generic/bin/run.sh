cd $(dirname $0)/..
java -Dlog4j.configuration=file:"bin/log4j.properties" -DBackupConfigFile=var/BackupConfig.properties -jar lib/friendly-backup.jar
