package com.cloudcomputing.cloudkarma.Commons;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ecs.AmazonECSClient;

public class Utilities {
    public static AmazonEC2Client getEc2Client(Regions region){
        AmazonEC2Client ec2Client = new AmazonEC2Client();
        ec2Client.setRegion(Region.getRegion(region));
        return ec2Client;
    }

    public static AmazonECSClient getEcsClient(Regions region){
        AmazonECSClient ecsClient = new AmazonECSClient();
        ecsClient.setRegion(Region.getRegion(region));
        return ecsClient;
    }

    public static AmazonAutoScalingClient getAutoScalingClient(Regions region){
        AmazonAutoScalingClient autoScalingClient = new AmazonAutoScalingClient();
        autoScalingClient.setRegion(Region.getRegion(region));
        return autoScalingClient;
    }
}
