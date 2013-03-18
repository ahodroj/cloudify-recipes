@Grab(group='com.gmongo', module='gmongo', version='0.8')
import com.gmongo.GMongo

import org.cloudifysource.dsl.context.ServiceContextFactory
import java.util.concurrent.TimeUnit

class ReplicaSet {
	def _id
	def members
}

class ReplicaMember {
	def _id
	def host
}

def configureReplicaSet(name, primary, primaryPort, secondary, secondaryPort, tertiary, tertiaryPort) {
	println "mongos_poststart.groovy: Setting up ${name} replica set"

	println "mongos_poststart.groovy: applying change to ${primary.hostAddress}:${primaryPort}"
	conn = new GMongo(primary.hostAddress, primaryPort)
	db = conn.getDB("admin")
	assert db != null
	println "Connection succeeded"
	println "mongos_poststart.groovy: ${name}: invoking replSetInitiate() on ${primary.hostAddress}:${primaryPort}"
	
	rsconf = [
			"_id": name, 
			"members": [
				["_id": 0, "host": "${primary.hostAddress}:${primaryPort}"],
				["_id": 1, "host": "${secondary.hostAddress}:${secondaryPort}"],
				["_id": 2, "host": "${tertiary.hostAddress}:${tertiaryPort}"]
			]	
		]
	
	result = db.command(["replSetInitiate" : rsconf])
	println "mongos_poststart.groovy: ${name}:result: ${result}"
	
	//println "mongos_poststart.groovy: ${name}: invoking rs.add(${secondary.hostAddress}:${secondaryPort})"
	//result = db.command(["rs.add": secondary.hostAddress + ":" + secondaryPort])
	//println "mongos_poststart.groovy: ${name}:result: ${result}"
	//println "mongos_poststart.groovy: ${name}: invoking rs.add(${tertiary.hostAddress}:${tertiaryPort})"
	//result = db.command(["rs.add": tertiary.hostAddress + ":" + tertiaryPort])
	//println "mongos_poststart.groovy: ${name}:result: ${result}"
	
}

config = new ConfigSlurper().parse(new File('mongos-service.properties').toURL())

println "mongos_poststart.groovy: sleeping for 60 secs..."
sleep(60)

serviceContext = ServiceContextFactory.getServiceContext()

println "mongos_poststart.groovy: serviceContext object is " + serviceContext

// Create the replica sets
zoneA = serviceContext.waitForService("mongod_zonea", 20, TimeUnit.SECONDS)
zoneB = serviceContext.waitForService("mongod_zoneb", 20, TimeUnit.SECONDS)
zoneC = serviceContext.waitForService("mongod_zonec", 20, TimeUnit.SECONDS)

println "mongos_poststart.groovy: Setting up replica sets and shards"
zoneAHosts = zoneA.waitForInstances(zoneA.numberOfPlannedInstances, 60, TimeUnit.SECONDS)
zoneBHosts = zoneB.waitForInstances(zoneB.numberOfPlannedInstances, 60, TimeUnit.SECONDS)
zoneCHosts = zoneC.waitForInstances(zoneC.numberOfPlannedInstances, 60, TimeUnit.SECONDS)

mza = serviceContext.attributes.mongod_zonea.instances
mzb = serviceContext.attributes.mongod_zoneb.instances
mzc = serviceContext.attributes.mongod_zonec.instances



configureReplicaSet("blue", zoneAHosts[0], mza[1].port, zoneBHosts[0], mzb[1].port, zoneCHosts[0], mzc[1].port)
configureReplicaSet("red", zoneBHosts[1], mzb[2].port, zoneCHosts[1], mzc[2].port, zoneAHosts[1], mza[2].port)
configureReplicaSet("green", zoneCHosts[2], mzc[3].port, zoneAHosts[2], mza[3].port, zoneBHosts[2], mzb[3].port)


//waiting for mongod service to become available, will not be needed in one of the upcoming builds 
println "mongos_poststart.groovy: Waiting for mongod..."
mongodService = serviceContext.waitForService("mongod_zonea", 20, TimeUnit.SECONDS)
if (mongodService == null) {
	throw new IllegalStateException("mongod service not found. mongod must be installed before mongos.");
}
mongodHostInstances = mongodService.waitForInstances(mongodService.numberOfPlannedInstances, 60, TimeUnit.SECONDS) 

println "mongos_poststart.groovy: mongodHostInstances length is "+ mongodHostInstances.length

currPort = serviceContext.attributes.thisInstance["port"] as int
println "mongos_poststart.groovy: Connecting to mongos on port ${currPort} ..."

mongo = new GMongo("127.0.0.1", currPort)
	
println "mongos_poststart.groovy: After new GMongo port ${currPort} ..."
	
db = mongo.getDB("admin")
assert db != null 	
println "Connection succeeded"	

zoneAHosts.each {
		mongodPort = serviceContext.attributes.mongod_zonea.instances[it.instanceID].port	
		mongodHost = it.hostAddress
		mongodReplicaSet = serviceContext.attributes.mongod_zonea.instances[it.instanceID].replicaSet		
		println "mongos_poststart.groovy: mongod #"+it.instanceID + " host and port = ${mongodHost}:${mongodPort}"		
		result = db.command(["addshard":"${mongodReplicaSet}/${mongodHost}:${mongodPort}"])		
		println "mongos_poststart.groovy: db result: ${result}"
}
    
	
result = db.command(["enablesharding":"crunchbase"])	
println "mongos_poststart.groovy: db result: ${result}"
	
result = db.command(["shardcollection":"crunchbase.issuer", "key":["issuer_key":1]])
println "mongos_poststart.groovy: db result: ${result}"
	

 


