#!/bin/bash
#
# This script is designed to compress and delete old tomcat
# log files.  Rotation is handled by tomcat.  It will manage any logs with the prefixes listed.
#

MAX_DAYS=90

cd /usr/local/tomcat/logs

#Iterate prefixes
# NOTE: the following are possible prefixes; however, since rotiation/naming is not date-based, this will not work as expected
# the newest  log is always suffixed '.1', and other existing archives are incremented upwards
# consider editing log4j/tomcat settings to change that
# 'labkey-action-stats.tsv.*' 'labkey-errors.log.*' 'labkey-query-stats.tsv.*' 'labkey.log.*'

for prefix in 'localhost_access_log.*' 'ehr-etl.log.*'
do

#Delete logs over 30 days old
for f in $(find $prefix -type f -mtime +${MAX_DAYS} 2>/dev/null)
do
        #echo "rm $f"
        rm $f
done

#Compress any remaining files, only if unchanged within the past day
for f in $(find $prefix -type f -mtime +1 -not -name "*.gz" 2>/dev/null)
do
        #echo "gzip $f"
        gzip $f
done

done

touch /var/log/tomcat_log_cleanup