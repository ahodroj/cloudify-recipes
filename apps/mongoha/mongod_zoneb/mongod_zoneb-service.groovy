service {
	extend "../../../services/mongodb/mongod"
	name "mongod_zoneb"
	
	numInstances 3
	maxAllowedInstances 3
	
	compute {
		template "SMALL_LINUX_ZONEB"
	}
	
	lifecycle {
		start "mongod_start.groovy"
	}
}
