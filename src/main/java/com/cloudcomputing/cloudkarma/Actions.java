package com.cloudcomputing.cloudkarma;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.model.DescribeClustersRequest;
import com.amazonaws.services.ecs.model.DescribeClustersResult;
import com.amazonaws.services.opsworks.model.DescribeEcsClustersRequest;
import com.amazonaws.services.opsworks.model.DescribeEcsClustersResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Actions {

    public static StartInstancesResult startInstances(List<String> instanceList){
        Region usEast = Region.getRegion(Regions.US_EAST_2);
        AmazonEC2Client ec2Client = new AmazonEC2Client();
        ec2Client.setRegion(usEast);
        StartInstancesRequest request = new StartInstancesRequest().withInstanceIds(instanceList);
        StartInstancesResult response = ec2Client.startInstances(request);
        return response;
    }

    public static StopInstancesResult stopInstances(List<String> instanceList){
        Region usEast = Region.getRegion(Regions.US_EAST_2);
        AmazonEC2Client ec2Client = new AmazonEC2Client();
        ec2Client.setRegion(usEast);
        StopInstancesRequest request = new StopInstancesRequest().withInstanceIds(instanceList);
        StopInstancesResult response = ec2Client.stopInstances(request);
        return response;
    }

    public static DescribeInstancesResult describeInstances(List<String> instanceList){
        Region usEast = Region.getRegion(Regions.US_EAST_2);
        AmazonEC2Client ec2Client = new AmazonEC2Client();
        ec2Client.setRegion(usEast);
        DescribeInstancesRequest describeInstancesRequest
                = new DescribeInstancesRequest();
        describeInstancesRequest.setInstanceIds(instanceList);
        DescribeInstancesResult response = ec2Client
                .describeInstances(describeInstancesRequest);
        return response;
    }

    public static DescribeClustersResult describeEcsClusters(List<String> clusterArnsList){
        Region usEast = Region.getRegion(Regions.US_EAST_2);
        AmazonECSClient ecsClient = new AmazonECSClient();
        ecsClient.setRegion(usEast);
        DescribeClustersRequest describeEcsClustersRequest = new DescribeClustersRequest();
        describeEcsClustersRequest.setClusters(clusterArnsList);
        DescribeClustersResult response = ecsClient.describeClusters(describeEcsClustersRequest);
        return response;
    }
}
