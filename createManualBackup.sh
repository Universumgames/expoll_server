scriptDir=$(dirname $0)

ssh -t mt32 "stty raw -echo; sudo docker exec -t expoll_database sh -c \"stty raw -echo; mysqldump -u root -ppassword expoll | gzip -9nf\"" > $scriptDir/backups/expoll_db_$(date +%Y_%m_%d-%H_%M).sql.gz
