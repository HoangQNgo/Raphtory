#!/usr/bin/env bash 
 
IP="$(./getMyIP.sh)" 
 
ZooKeeper="192.168.1.5:2181" 
 
LAMName="testLam" 
 
Image="quay.io/miratepuffin/cluster" #if you want to use prebuilt one on my quay.io 
 
NumberOfPartitions=2
 
NumberOfUpdates=10
 
JVM="-Dcom.sun.management.jmxremote.rmi.port=9090 -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=9090  -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.local.only=false -Djava.rmi.server.hostname=$IP" 
if [ ! -d logs ]; then mkdir logs; fi 
rm -r logs/machine1Setup
if [ ! -d logs/machine1Setup ]; then mkdir logs/machine1Setup; fi 
if [ ! -d logs/machine1Setup/entityLogs/ ]; then mkdir logs/machine1Setup/entityLogs/; fi 
entityLogs=$(pwd)"/logs/machine1Setup/entityLogs" 
 
chmod 777 logs 
chmod 777 logs/machine1Setup
chmod 777 logs/machine1Setup/entityLogs
 
PM1Port=9201
PM1ID=1
(docker run -p $PM1Port:$PM1Port  --rm -e "BIND_PORT=$PM1Port" -e "HOST_IP=$IP" -e "HOST_PORT=$PM1Port" -v $entityLogs:/logs/entityLogs $Image partitionManager $PM1ID $NumberOfPartitions $ZooKeeper &) > logs/machine1Setup/partitionManager1.txt 
sleep 2 
echo "Partition Manager $PM1ID up and running at $IP:$PM1Port" 
 
Router1Port=9301
(docker run -p $Router1Port:$Router1Port  --rm -e "BIND_PORT=$Router1Port" -e "HOST_IP=$IP" -e "HOST_PORT=$Router1Port" $Image router $NumberOfPartitions $ZooKeeper &) > logs/machine1Setup/router1.txt 
sleep 1 
echo "Router 1 up and running at $IP:$Router1Port" 
 
Update1Port=9401
(docker run -p $Update1Port:$Update1Port  --rm -e "BIND_PORT=$Update1Port" -e "HOST_IP=$IP" -e "HOST_PORT=$Update1Port" $Image updateGen $NumberOfPartitions $NumberOfUpdates $ZooKeeper &) > logs/machine1Setup/updateGenerator1.txt 
sleep 1 
echo "Update Generator 1 up and running at $IP:$Update1Port" 
 
ClusterUpPort=9106 
 
(docker run -p $ClusterUpPort:$ClusterUpPort  --rm -e "BIND_PORT=$ClusterUpPort" -e "HOST_IP=$IP" -e "HOST_PORT=$ClusterUpPort" $Image ClusterUp $NumberOfPartitions $ZooKeeper &) > logs/machine1Setup/ClusterUp.txt 
sleep 1 
echo "CLUSTER UP"
 
