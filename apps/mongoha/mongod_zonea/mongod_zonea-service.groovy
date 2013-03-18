service {
	extend "../../../services/mongodb/mongod"
	name "mongod_zonea"
	
	numInstances 3
	maxAllowedInstances 3
	
	compute {
		template "SMALL_LINUX_ZONEA"
	}
	
	lifecycle {
		start "mongod_start.groovy"
	}
}
