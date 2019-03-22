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
import com.cloudcomputing.cloudkarma.Commons.ContainerResourcesComparator;
import com.cloudcomputing.cloudkarma.Commons.MigratingTaskComparator;
import com.cloudcomputing.cloudkarma.Model.Instance;
import com.cloudcomputing.cloudkarma.Model.MigratingTask;
import com.cloudcomputing.cloudkarma.Model.ContainerResources;
import com.cloudcomputing.cloudkarma.Model.Resource;
import com.cloudcomputing.cloudkarma.Model.TaskQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static Logger logger = LoggerFactory.getLogger(Actions.class);

    public StartInstancesResult startInstances(List<String> instanceList){
        AmazonEC2Client ec2Client = getEc2Client(Regions.US_EAST_2);
        StartInstancesRequest request = new StartInstancesRequest().withInstanceIds(instanceList);
        StartInstancesResult response = ec2Client.startInstances(request);
        return response;
    }

    public StopInstancesResult stopInstances(List<String> instanceList){
        AmazonEC2Client ec2Client = getEc2Client(Regions.US_EAST_2);
        StopInstancesRequest request = new StopInstancesRequest().withInstanceIds(instanceList);
        StopInstancesResult response = ec2Client.stopInstances(request);
        return response;
    }

    public DescribeInstancesResult describeInstances(List<String> instanceList){
        //operates only on instance id not on ARN
        AmazonEC2Client ec2Client = getEc2Client(Regions.US_EAST_2);
        DescribeInstancesRequest describeInstancesRequest
                = new DescribeInstancesRequest();
        describeInstancesRequest.setInstanceIds(instanceList);
        DescribeInstancesResult response = ec2Client
                .describeInstances(describeInstancesRequest);
        return response;
    }

    public DescribeClustersResult describeClusters(List<String> clusterArnsList){
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
        if(!isZeroRunningTasksInCluster(cluster)){
            logger.info(String.format("Cluster:%s is already at its optimal configuration", cluster));
            return false;
        }

        //get instances for the cluster and stop tasks
        logger.info(String.format("Getting cluster EC2instances"));
        ListContainerInstancesResult activeEc2Instances = getInstancesForTheCluster(cluster);
        List<String> containerInstancesList = activeEc2Instances.getContainerInstanceArns();
        logger.info(String.format("%s EC2instances ARN to be stopped: %s", cluster, containerInstancesList));
        containerInstancesList = getContainerInstanceIds(containerInstancesList);
        logger.info(String.format("%s EC2instances Ec2Instance-Ids to be stopped: %s", cluster, containerInstancesList));
        //getEc2InstanceIds from containerInstance name
        List<String> ec2InstanceIds = new ArrayList<>();
        describeContainerInstances(containerInstancesList)
                .getContainerInstances()
                .forEach(containerInstance -> {
                    ec2InstanceIds.add(containerInstance.getEc2InstanceId());
                });
        logger.info(String.format("Updating autoscale configuration for cluster %s to 0", cluster));
        //stopInstances by setting capacity to 0 in the autoscale group
        getAutoScalingGroupNameFromInstanceId(ec2InstanceIds)
                .forEach(autoScalingGroupName -> updateAutoScaleGroup(autoScalingGroupName, 0));
        logger.info(String.format("Cluster %s Ec2instances have been stopped successfully.", cluster));
        return true;
    }

    List<MigratingTask> getMigratingTaskDetailsForTheCluster(String cluster){
        List<MigratingTask> migratingTasks = new ArrayList<>();
        List<String> clusterContainerResources = new ArrayList<>();
        clusterContainerResources.add("cluster-container-resources");
        List<ContainerResources> resources = getContainerResources(clusterContainerResources);
        List<MigratingTask> toMoveTasksList = new ArrayList<>();
        MigratingTask toMoveTasks = new MigratingTask();
        Instance instance = new Instance("","");
        toMoveTasks.setSource(instance);
        Resource resource = new Resource(45,54);
        toMoveTasks.setResource(resource);
        toMoveTasks.setTaskArn("ads");
        toMoveTasksList.add(toMoveTasks);
        filterInstancesBasedOnAvailableResources(resources, toMoveTasksList);
        return migratingTasks;
    }

    public static void filterInstances(){
        List<ContainerResources> resources = new ArrayList<>();
        for(int i = 0; i < 6; i++){
            ContainerResources containerResources = new ContainerResources();
            containerResources.setContainerInstanceArn(String.format("instance-Arn-%d",i));
            containerResources.setEc2InstanceId(String.format("ec2-instance-id-%d",i));
            containerResources.setRunningTasksCount(i);
            resources.add(containerResources);
        }

        resources.get(0).setFreeSpace(new ContainerResources.FreeSpace(200, 300));
        resources.get(1).setFreeSpace(new ContainerResources.FreeSpace(50, 80));
        resources.get(2).setFreeSpace(new ContainerResources.FreeSpace(100, 80));
        resources.get(3).setFreeSpace(new ContainerResources.FreeSpace(500, 600));
        resources.get(4).setFreeSpace(new ContainerResources.FreeSpace(200, 250));
        resources.get(5).setFreeSpace(new ContainerResources.FreeSpace(380, 480));



        List<MigratingTask> task = new ArrayList<>();
        for(int i = 0; i < 4; i++){
            MigratingTask migratingTask = new MigratingTask();
            migratingTask.setTaskArn(String.format("task-arn-%d",i));
            Resource resource = new Resource();
            migratingTask.setResource(resource);
            Instance instance = new Instance(String.format("ec2-instance-id-%d",i), String.format("instance-Arn:%d",i));
            migratingTask.setSource(instance);
            task.add(migratingTask);
        }
        task.get(0).setResource(new Resource(75, 50));
        task.get(1).setResource(new Resource(850, 450));
        task.get(2).setResource(new Resource(50, 100));
        task.get(3).setResource(new Resource(400, 500));

        filterInstancesBasedOnAvailableResources(resources, task);
    }

    private static void filterInstancesBasedOnAvailableResources(List<ContainerResources> containerResourceList,
                                                          List<MigratingTask> toMoveTasksList) {
        HashMap<String, List<MigratingTask>> resourceArnTasksMap = new HashMap<>();
        toMoveTasksList.forEach(toMoveTask -> {
            List<MigratingTask> migratingTasks = resourceArnTasksMap.get(toMoveTask.getSource().getResourceArn());
            if(migratingTasks == null)
                migratingTasks = new ArrayList<>();
            migratingTasks.add(toMoveTask);
            resourceArnTasksMap.put(toMoveTask.getSource().getResourceArn(), migratingTasks);
        });

        toMoveTasksList = toMoveTasksList
                .stream()
                .filter(toMoveTask -> (resourceArnTasksMap.get(toMoveTask.getSource().getResourceArn()).size() == 1))
                .collect(Collectors.toList());

        Collections.sort(toMoveTasksList, new MigratingTaskComparator());
        Collections.sort(containerResourceList, new ContainerResourcesComparator());

        PriorityQueue<TaskQueue> pq = new PriorityQueue<>(new Comparator<TaskQueue>() {
            @Override
            public int compare(TaskQueue t1, TaskQueue t2) {
                return t1.getMatchedResources().size() - t2.getMatchedResources().size();
            }
        });

        List<String> ec2InstancesToTurnOff = new ArrayList<>();

        for (int taskIndex = 0; taskIndex < toMoveTasksList.size(); taskIndex++) {
            List<Integer> matchedTargetList = new ArrayList<>();
            for (int resourcesIndex = 0; resourcesIndex < containerResourceList.size(); resourcesIndex++){
                if(isMatchingResourceFound(toMoveTasksList.get(taskIndex), containerResourceList.get(resourcesIndex))){
                    //source != destination
                    if(containerResourceList.get(resourcesIndex).getEc2InstanceId() != toMoveTasksList.get(taskIndex).getSource().getInstanceId())
                        matchedTargetList.add(resourcesIndex);
                }
            }
            if(matchedTargetList.size() > 0){
                TaskQueue tq = new TaskQueue();
                tq.setMatchedResources(matchedTargetList);
                tq.setTask(toMoveTasksList.get(taskIndex));
                pq.add(tq);
            }
        }



        List<MigratingTask> tasks = allocateDestinationContainerForTasks(pq, containerResourceList);
        System.out.println("\n\tSource\t\t\t\t\t\tDestination");
        System.out.println("-------------------------------------------");
        for(MigratingTask task : tasks){
            System.out.println(task.getSource().getInstanceId()+"\t\t-> \t"+task.getDestination().getInstanceId());
        }


    }

    private static List<MigratingTask> allocateDestinationContainerForTasks(PriorityQueue<TaskQueue> pq, List<ContainerResources> containerResourceList) {
        List<MigratingTask> migratingTasks = new ArrayList<>();
        System.out.println();
        System.out.println();
        while(pq.size() > 0){
            TaskQueue taskq = pq.remove();
            Integer remainingResourcesAfterAllocation = Integer.MAX_VALUE;
            for(int resourceIndex : taskq.getMatchedResources()){
                if(isMatchingResourceFound(taskq.getTask(), containerResourceList.get(resourceIndex))){
                    ContainerResources.FreeSpace freeSpace = containerResourceList.get(resourceIndex).getFreeSpace();
                    int cpu = (int)(freeSpace.getCpuAvailable()*1.3) - taskq.getTask().getResource().getCpu();
                    int memory = (int)(freeSpace.getMemoryAvailable()*1.3) - taskq.getTask().getResource().getMemory();

                    //check for best fit
                    if(remainingResourcesAfterAllocation > (cpu+memory)) {
                        remainingResourcesAfterAllocation = (cpu + memory);

                        //update destination node
                        taskq.getTask().setDestination(new Instance(containerResourceList.get(resourceIndex).getEc2InstanceId(),
                                containerResourceList.get(resourceIndex).getContainerInstanceArn()));

                        //update the container resources
                        ContainerResources.FreeSpace freeSpaceForTheContainerAfterAllocation = new ContainerResources.FreeSpace(cpu, memory);
                        containerResourceList.get(resourceIndex).setFreeSpace(freeSpaceForTheContainerAfterAllocation);
                    }
                }
            }

            //add to migrating tasks list
            migratingTasks.add(taskq.getTask());
            System.out.println("Saving resources for instance : " + taskq.getTask().getSource().getInstanceId());
        }

        return migratingTasks;
    }



    private static boolean isMatchingResourceFound(MigratingTask task, ContainerResources resource){
        double threshold = 1.3;
        return task.getResource().getMemory()*threshold <= resource.getFreeSpace().getMemoryAvailable() &&
                task.getResource().getCpu()*threshold <= resource.getFreeSpace().getCpuAvailable();
    }

    private List<ContainerResources> getContainerResources(List<String> containerInstances) {
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
        int count = describeTasksRunningInCluster(cluster).getTaskArns().size();
        logger.info(String.format("Cluster: %s has %d running tasks", cluster, count));
        return count == 0;
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
            if(matcher.find())
                containerInstanceIdList.add(matcher.group(2));
        });
        return containerInstanceIdList;
    }

}
