cd dockers
for file in *.docker; do
    echo start $file
    sh $file
done
