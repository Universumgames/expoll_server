scriptDir=$(dirname $0)

ssh -t remoteServer "stty raw -echo; sudo docker exec -t expoll_database sh -c \"stty raw -echo; mysqldump -u root -pPASSWORD expoll | gzip -9nf\"" > $scriptDir/../backups/expoll_db_$(date +%Y_%m_%d-%H_%M).sql.gz
