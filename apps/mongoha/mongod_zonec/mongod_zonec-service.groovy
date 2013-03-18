service {
	extend "../../../services/mongodb/mongod"
	name "mongod_zonec"
	
	numInstances 3
	maxAllowedInstances 3
	
	compute {
		template "SMALL_LINUX_ZONEC"
	}
	
	lifecycle {
		start "mongod_start.groovy"
	}
}
