service {
	extend "../../../services/mongodb/mongos"
	lifecycle {
    postStart "mongos_postStart.groovy"
	}
}