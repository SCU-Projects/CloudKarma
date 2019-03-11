package com.cloudcomputing.cloudkarma;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.appstream.model.ListTagsForResourceRequest;
import com.amazonaws.services.appstream.model.ListTagsForResourceResult;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesResult;
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityRequest;
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityResult;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.*;
import com.amazonaws.util.EC2MetadataUtils;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class Actions{

    public static StartInstancesResult startInstances(List<String> instanceList){
        AmazonEC2Client ec2Client = getEc2Client(Regions.US_WEST_1);
        StartInstancesRequest request = new StartInstancesRequest().withInstanceIds(instanceList);
        StartInstancesResult response = ec2Client.startInstances(request);
        return response;
    }

    public static StopInstancesResult stopInstances(List<String> instanceList){
        AmazonEC2Client ec2Client = getEc2Client(Regions.US_WEST_1);
        StopInstancesRequest request = new StopInstancesRequest().withInstanceIds(instanceList);
        StopInstancesResult response = ec2Client.stopInstances(request);
        return response;
    }

    public static DescribeInstancesResult describeInstances(List<String> instanceList){
        //operates only on instance id not on ARN
        AmazonEC2Client ec2Client = getEc2Client(Regions.US_WEST_1);
        DescribeInstancesRequest describeInstancesRequest
                = new DescribeInstancesRequest();
        describeInstancesRequest.setInstanceIds(instanceList);
        DescribeInstancesResult response = ec2Client
                .describeInstances(describeInstancesRequest);
        return response;
    }

    public static DescribeClustersResult describeClusters(List<String> clusterArnsList){
        AmazonECSClient ecsClient = getEcsClient(Regions.US_WEST_1);
        DescribeClustersRequest describeClustersRequest = new DescribeClustersRequest();
        describeClustersRequest.setClusters(clusterArnsList);
        DescribeClustersResult response = ecsClient.describeClusters(describeClustersRequest);
        return response;
    }

    public static DescribeContainerInstancesResult describeContainerInstances(List<String> containerInstances){
        //https://docs.aws.amazon.com/cli/latest/reference/ecs/describe-container-instances.html
        AmazonECSClient ecsClient = getEcsClient(Regions.US_WEST_1);
        DescribeContainerInstancesRequest describeContainerInstancesRequest = new DescribeContainerInstancesRequest();
        describeContainerInstancesRequest.withContainerInstances(containerInstances);
        describeContainerInstancesRequest.setCluster("c1");
        DescribeContainerInstancesResult result = ecsClient.describeContainerInstances(describeContainerInstancesRequest);
        return result;
    }

    public static boolean stopInstancesIfInactive(String cluster){
        if(isZeroRunningTasksInCluster(cluster)){
            //get instances for the cluster and stop tasks
            ListContainerInstancesResult activeEc2Instances = getInstancesForTheCluster(cluster);
            List<String> containerInstancesList = activeEc2Instances.getContainerInstanceArns();
            containerInstancesList = getContainerInstanceIds(containerInstancesList);

            //getEc2InstanceIds from containerInstance name
            List<String> ec2InstanceIds = new ArrayList<>();
            describeContainerInstances(containerInstancesList)
                    .getContainerInstances()
                    .forEach(containerInstance -> {
                        ec2InstanceIds.add(containerInstance.getEc2InstanceId());
                    });

            //stopInstances by setting capacity to 0 in the autoscale group
            getAutoScalingGroupNameFromInstanceId(ec2InstanceIds)
                    .forEach(autoScalingGroupName -> updateAutoScaleGroup(autoScalingGroupName, 0));
            return true;
        }
        else
            return false;
    }

    private static boolean isZeroRunningTasksInCluster(String cluster){
        return describeTasksRunningInCluster(cluster).getTaskArns().size() == 0;
    }

    private static List<String> getAutoScalingGroupNameFromInstanceId(List<String>  instanceIds){
        Set<String> autoScalingGroupNameSet = new HashSet<>();
        AmazonAutoScalingClient autoScalingClient = getAutoScalingClient(Regions.US_WEST_1);
        DescribeAutoScalingInstancesRequest request = new DescribeAutoScalingInstancesRequest();
        request.setInstanceIds(instanceIds);
        DescribeAutoScalingInstancesResult result = autoScalingClient.describeAutoScalingInstances(request);
        result.getAutoScalingInstances()
                .forEach(autoScalingInstance -> autoScalingGroupNameSet.add(autoScalingInstance.getAutoScalingGroupName()));
        return autoScalingGroupNameSet.stream().collect(Collectors.toList());
    }

    private static SetDesiredCapacityResult updateAutoScaleGroup(String autoScalingGroupName, int capacity) {
        AmazonAutoScalingClient autoScalingClient = getAutoScalingClient(Regions.US_WEST_1);
        SetDesiredCapacityRequest request = new SetDesiredCapacityRequest();
        request.setAutoScalingGroupName(autoScalingGroupName);
        request.setDesiredCapacity(capacity);
        SetDesiredCapacityResult result = autoScalingClient.setDesiredCapacity(request);
        return result;
    }

    private static ListTasksResult describeTasksRunningInCluster(String cluster){
        AmazonECSClient ecsClient = getEcsClient(Regions.US_WEST_1);
        ListTasksRequest request = new ListTasksRequest();
        request.setCluster(cluster);
        ListTasksResult result = ecsClient.listTasks(request);
        return result;
    }

    private static ListContainerInstancesResult getInstancesForTheCluster(String cluster) {
        AmazonECSClient ecsClient = getEcsClient(Regions.US_WEST_1);
        ListContainerInstancesRequest request = new ListContainerInstancesRequest();
        request.setCluster(cluster);
        request.setStatus(ContainerInstanceStatus.ACTIVE);
        ListContainerInstancesResult result = ecsClient.listContainerInstances(request);
        return result;
    }

    private static List<String> getContainerInstanceIds(List<String> inputContainerInstanceIds){
        //extracts the ec2instance-id from the container instance ARN
        List<String> containerInstanceIdList = new ArrayList<>();
        Pattern p = Pattern.compile("(.+\\/)(.+)");
        inputContainerInstanceIds.forEach(containerInstanceId -> {
            Matcher matcher = p.matcher(containerInstanceId);
            if(matcher.find())
                containerInstanceIdList.add(matcher.group(2));
        });
        return containerInstanceIdList;
    }
//    ============================================================


    public static ListTagsForResourceResult getTagsForResources(String resourcearn){

        ListTagsForResourceRequest request1 = new ListTagsForResourceRequest();
        request1.setResourceArn(resourcearn);
        System.out.println(request1);
        ListTagsForResourceResult result = new ListTagsForResourceResult();
        result.getTags();
        return result;
    }

    public static DescribeTaskDefinitionResult getTaskDefinitionDetails(String taskDefinition){
        AmazonECS client = AmazonECSClientBuilder.standard().build();
        DescribeTaskDefinitionRequest request = new DescribeTaskDefinitionRequest().withTaskDefinition(taskDefinition);
        DescribeTaskDefinitionResult response = client.describeTaskDefinition(request);
        return response;
    }


    public static List<TagDescription> getTagResultForSB(String instanceID) {
        AmazonEC2 client = AmazonEC2ClientBuilder.standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
        DescribeTagsRequest req = new DescribeTagsRequest()
                .withFilters(new Filter("tag:sb", Collections.singletonList(instanceID)));
        DescribeTagsResult describeTagsResult = client.describeTags(req);
        List<TagDescription> tags = describeTagsResult.getTags();
        return tags;

    }

    public static List<TagDescription> getTagResultForNB(String instanceID) {
        AmazonEC2 client = AmazonEC2ClientBuilder.standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
        DescribeTagsRequest req = new DescribeTagsRequest()
                .withFilters(new Filter("tag:sb", Collections.singletonList(instanceID)));
        DescribeTagsResult describeTagsResult = client.describeTags(req);
        List<TagDescription> tags = describeTagsResult.getTags();
        return tags;

    }


    private static AmazonEC2Client getEc2Client(Regions region){
        AmazonEC2Client ec2Client = new AmazonEC2Client();
        ec2Client.setRegion(Region.getRegion(region));
        return ec2Client;
    }

    private static AmazonECSClient getEcsClient(Regions region){
        AmazonECSClient ecsClient = new AmazonECSClient();
        ecsClient.setRegion(Region.getRegion(region));
        return ecsClient;
    }

    private static AmazonAutoScalingClient getAutoScalingClient(Regions region){
        AmazonAutoScalingClient autoScalingClient = new AmazonAutoScalingClient();
        autoScalingClient.setRegion(Region.getRegion(region));
        return autoScalingClient;
    }


}
