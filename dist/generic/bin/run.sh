cd $(dirname $0)/..
java -Dlog4j.configurationFile=file:"bin/log4j2.properties" -DBackupConfigFile=var/BackupConfig.properties -jar lib/friendly-backup.jar
