application {
	name="mongoha"
	
	service {
		name = "mongod_zonea"		
	}
	
	service {
		name = "mongod_zoneb"
	}
	
	service {
		name = "mongod_zonec"	
	}
	
	service {
		name = "mongoConfig"		
	}
	
	service {
		name = "mongos"
		dependsOn = ["mongoConfig", "mongod_zonea", "mongod_zoneb", "mongod_zonec"]
	}
}