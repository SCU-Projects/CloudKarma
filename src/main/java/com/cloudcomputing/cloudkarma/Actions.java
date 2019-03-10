package com.cloudcomputing.cloudkarma;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesResult;
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityRequest;
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityResult;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.model.*;
import com.cloudcomputing.cloudkarma.Model.Instance;
import com.cloudcomputing.cloudkarma.Model.MigratingTask;
import com.cloudcomputing.cloudkarma.Model.ContainerResources;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.cloudcomputing.cloudkarma.Commons.Utilities.getAutoScalingClient;
import static com.cloudcomputing.cloudkarma.Commons.Utilities.getEc2Client;
import static com.cloudcomputing.cloudkarma.Commons.Utilities.getEcsClient;

@Service
public class Actions {

    public static StartInstancesResult startInstances(List<String> instanceList){
        AmazonEC2Client ec2Client = getEc2Client(Regions.US_EAST_2);
        StartInstancesRequest request = new StartInstancesRequest().withInstanceIds(instanceList);
        StartInstancesResult response = ec2Client.startInstances(request);
        return response;
    }

    public static StopInstancesResult stopInstances(List<String> instanceList){
        AmazonEC2Client ec2Client = getEc2Client(Regions.US_EAST_2);
        StopInstancesRequest request = new StopInstancesRequest().withInstanceIds(instanceList);
        StopInstancesResult response = ec2Client.stopInstances(request);
        return response;
    }

    public static DescribeInstancesResult describeInstances(List<String> instanceList){
        //operates only on instance id not on ARN
        AmazonEC2Client ec2Client = getEc2Client(Regions.US_EAST_2);
        DescribeInstancesRequest describeInstancesRequest
                = new DescribeInstancesRequest();
        describeInstancesRequest.setInstanceIds(instanceList);
        DescribeInstancesResult response = ec2Client
                .describeInstances(describeInstancesRequest);
        return response;
    }

    public static DescribeClustersResult describeClusters(List<String> clusterArnsList){
        AmazonECSClient ecsClient = getEcsClient(Regions.US_EAST_2);
        DescribeClustersRequest describeClustersRequest = new DescribeClustersRequest();
        describeClustersRequest.setClusters(clusterArnsList);
        DescribeClustersResult response = ecsClient.describeClusters(describeClustersRequest);
        return response;
    }

    public static DescribeContainerInstancesResult describeContainerInstances(List<String> containerInstances){
        //https://docs.aws.amazon.com/cli/latest/reference/ecs/describe-container-instances.html
        AmazonECSClient ecsClient = getEcsClient(Regions.US_EAST_2);
        DescribeContainerInstancesRequest describeContainerInstancesRequest = new DescribeContainerInstancesRequest();
        describeContainerInstancesRequest.withContainerInstances(containerInstances);
        describeContainerInstancesRequest.setCluster("cluster-1");
        DescribeContainerInstancesResult result = ecsClient.describeContainerInstances(describeContainerInstancesRequest);
        return result;
    }

    public static boolean stopInstancesIfInactive(String cluster){
        if(!isZeroRunningTasksInCluster(cluster))
            return false;
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

    List<MigratingTask> getMigratingTaskDetailsForTheCluster(String cluster){
        List<MigratingTask> migratingTasks = new ArrayList<>();
        List<String> clusterContainerResources = new ArrayList<>();
        clusterContainerResources.add("sdadsdsdsdfdsf");
        List<ContainerResources> resources = getContainerResources(clusterContainerResources);
        List<MigratingTask> toMoveTasksList = new ArrayList<>();
        MigratingTask toMoveTasks = new MigratingTask();
        Instance instance = new Instance("","");
        toMoveTasks.setSource(instance);
        toMoveTasks.setResource();
        List<String> taskList = new ArrayList<>();
        taskList.add("dsfsd");
        toMoveTasks.setTaskList(taskList);
        toMoveTasksList.add(toMoveTasks);
        filterInstancesBasedOnAvailableResources(resources, toMoveTasksList);
        return migratingTasks;
    }

    private static void filterInstancesBasedOnAvailableResources(List<ContainerResources> containerResourceList, List<ToMoveTasks> toMoveTasksList){
        containerResourceList
                .stream()
                .filter()
                .map(containerResources -> {

                })
    }

    private static List<ContainerResources> getContainerResources(List<String> containerInstances) {
        DescribeContainerInstancesResult result = describeContainerInstances(containerInstances);
        List<ContainerResources> resources = new ArrayList<>();

        result.getContainerInstances().forEach(containerInstance -> {
            containerInstance.getRemainingResources().forEach(res -> {
                ContainerResources.FreeSpace freeSpace = new ContainerResources.FreeSpace();
                if (res.getName().equals("CPU")) {
                    ((ContainerResources.FreeSpace) freeSpace).setCpuAvailable(res.getIntegerValue());
                }
                if (res.getName().equals("MEMORY")) {
                    ((ContainerResources.FreeSpace) freeSpace).setMemoryAvailable(res.getIntegerValue());
                }
                ContainerResources containerResources = new ContainerResources(containerInstance.getEc2InstanceId(),
                        containerInstance.getContainerInstanceArn(), containerInstance.getRunningTasksCount(), freeSpace);
                resources.add(containerResources);
            });
        });
        return resources;
    }

    private static boolean isZeroRunningTasksInCluster(String cluster){
        return describeTasksRunningInCluster(cluster).getTaskArns().size() == 0;
    }

    private static List<String> getAutoScalingGroupNameFromInstanceId(List<String>  instanceIds){
        Set<String> autoScalingGroupNameSet = new HashSet<>();
        AmazonAutoScalingClient autoScalingClient = getAutoScalingClient(Regions.US_EAST_2);
        DescribeAutoScalingInstancesRequest request = new DescribeAutoScalingInstancesRequest();
        request.setInstanceIds(instanceIds);
        DescribeAutoScalingInstancesResult result = autoScalingClient.describeAutoScalingInstances(request);
        result.getAutoScalingInstances()
                .forEach(autoScalingInstance -> autoScalingGroupNameSet.add(autoScalingInstance.getAutoScalingGroupName()));
        return autoScalingGroupNameSet.stream().collect(Collectors.toList());
    }

    private static SetDesiredCapacityResult updateAutoScaleGroup(String autoScalingGroupName, int capacity) {
        AmazonAutoScalingClient autoScalingClient = getAutoScalingClient(Regions.US_EAST_2);
        SetDesiredCapacityRequest request = new SetDesiredCapacityRequest();
        request.setAutoScalingGroupName(autoScalingGroupName);
        request.setDesiredCapacity(capacity);
        SetDesiredCapacityResult result = autoScalingClient.setDesiredCapacity(request);
        return result;
    }

    private static ListTasksResult describeTasksRunningInCluster(String cluster){
        AmazonECSClient ecsClient = getEcsClient(Regions.US_EAST_2);
        ListTasksRequest request = new ListTasksRequest();
        request.setCluster(cluster);
        ListTasksResult result = ecsClient.listTasks(request);
        return result;
    }

    private static ListContainerInstancesResult getInstancesForTheCluster(String cluster) {
        AmazonECSClient ecsClient = getEcsClient(Regions.US_EAST_2);
        ListContainerInstancesRequest request = new ListContainerInstancesRequest();
        request.setCluster(cluster);
        request.setStatus(ContainerInstanceStatus.ACTIVE);
        ListContainerInstancesResult result = ecsClient.listContainerInstances(request);
        return result;
    }

    private static List<String> getContainerInstanceIds(List<String> inputContainerInstanceIds) {
        //extracts the ec2instance-id from the container instance ARN
        List<String> containerInstanceIdList = new ArrayList<>();
        Pattern p = Pattern.compile("(.+\\/)(.+)");
        inputContainerInstanceIds.forEach(containerInstanceId -> {
            Matcher matcher = p.matcher(containerInstanceId);
            if (matcher.find())
                containerInstanceIdList.add(matcher.group(2));
        });
        return containerInstanceIdList;
    }

}
