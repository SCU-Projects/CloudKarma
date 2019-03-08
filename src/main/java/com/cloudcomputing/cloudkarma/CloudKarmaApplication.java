package com.cloudcomputing.cloudkarma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class CloudKarmaApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudKarmaApplication.class, args);
		ApiCaller apiCaller = new ApiCaller();
		apiCaller.getEc2InstanceInfo();
		//apiCaller.getCloudWatchMetrics();
	}

}
