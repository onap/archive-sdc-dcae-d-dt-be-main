# Deploy-DCAE

1. default.rb = file that defines default to parameters used inside the cookbook
2. recipes/dcae_setup.rb = create and configure application.properties and logback-spring.xml using template defined in dcae-application.properties.erb with values from defaults.rb or from environments.json
3. recipes/jetty_setup.rb = configure the jetty (ssl)
4. roles/dcae-fe.json = chef roles (which recipe to run)
5. Dockerfile = SDC Base line jettystartup.sh = run all of the above and run docker-entrypoint.sh that starts the jetty
6. pom.xml = docker profile.
			 tag for onap
			 build-helper-maven-plugin = extract version from the war
			 maven-resources-plugin - copy the war to docker/target
			 docker-maven-plugin = creates the docker - configured with the dockerFile location

