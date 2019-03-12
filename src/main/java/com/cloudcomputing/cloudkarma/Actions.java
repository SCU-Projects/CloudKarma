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

    public DescribeContainerInstancesResult describeContainerInstances(List<String> containerInstances){
        //https://docs.aws.amazon.com/cli/latest/reference/ecs/describe-container-instances.html
        AmazonECSClient ecsClient = getEcsClient(Regions.US_EAST_2);
        DescribeContainerInstancesRequest describeContainerInstancesRequest = new DescribeContainerInstancesRequest();
        describeContainerInstancesRequest.withContainerInstances(containerInstances);
        describeContainerInstancesRequest.setCluster("cluster-1");
        DescribeContainerInstancesResult result = ecsClient.describeContainerInstances(describeContainerInstancesRequest);
        return result;
    }

    public boolean stopInstancesIfInactive(String cluster){
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
        Resource resource = new Resource(45,54);
        toMoveTasks.setResource(resource);
        toMoveTasks.setTaskArn("ads");
        toMoveTasksList.add(toMoveTasks);
        filterInstancesBasedOnAvailableResources(resources, toMoveTasksList);
        return migratingTasks;
    }

    public static void filterInstances(){
        List<ContainerResources> resources = new ArrayList<>();
        for(int i = 0; i < 5; i++){
            ContainerResources containerResources = new ContainerResources();
            containerResources.setContainerInstanceArn(String.format("%d",i));
            containerResources.setEc2InstanceId(String.format("%d",i));
            containerResources.setRunningTasksCount(i);
            resources.add(containerResources);
        }

        resources.get(0).setFreeSpace(new ContainerResources.FreeSpace(400,400));
        resources.get(1).setFreeSpace(new ContainerResources.FreeSpace(250, 300));
        resources.get(2).setFreeSpace(new ContainerResources.FreeSpace(70, 70));
        resources.get(3).setFreeSpace(new ContainerResources.FreeSpace(80, 100));
        resources.get(4).setFreeSpace(new ContainerResources.FreeSpace(120, 150));



        List<MigratingTask> task = new ArrayList<>();
        for(int i = 0; i < 10; i++){
            MigratingTask migratingTask = new MigratingTask();
            migratingTask.setTaskArn(String.format("%d",i));
            Resource resource = new Resource();
            migratingTask.setResource(resource);
            Instance instance = new Instance(String.format("instance:%d",i), String.format("resource-arn:%d",i));
            migratingTask.setSource(instance);
            task.add(migratingTask);
        }
        task.get(0).setResource(new Resource(80, 40));
        task.get(1).setResource(new Resource(100, 70));
        task.get(2).setResource(new Resource(20, 30));
        task.get(3).setResource(new Resource(150, 100));
        task.get(4).setResource(new Resource(90, 80));
        task.get(5).setResource(new Resource(40, 200));
        task.get(6).setResource(new Resource(80, 100));
        task.get(7).setResource(new Resource(60, 50));
        task.get(8).setResource(new Resource(150, 150));
        task.get(9).setResource(new Resource(25, 25));


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
        
        for (int taskIndex = 0; taskIndex < toMoveTasksList.size(); taskIndex++) {
            List<Integer> matchedTargetList = new ArrayList<>();
            for (int resourcesIndex = 0; resourcesIndex < containerResourceList.size(); resourcesIndex++){
                if(isMatchingResourceFound(toMoveTasksList.get(taskIndex), containerResourceList.get(resourcesIndex)))
                    matchedTargetList.add(resourcesIndex);
            }
            if(matchedTargetList.size() > 0){
                TaskQueue tq = new TaskQueue();
                tq.setMatchedResources(matchedTargetList);
                tq.setTask(toMoveTasksList.get(taskIndex));
                pq.add(tq);
            }
        }

        List<MigratingTask> tasks = allocateDestinationContainerForTasks(pq, containerResourceList);
        for(MigratingTask task : tasks){
            System.out.println("Source:"+task.getSource().getResourceArn()+"-> \tDestn:"+task.getDestination().getResourceArn());
        }


    }

    private static List<MigratingTask> allocateDestinationContainerForTasks(PriorityQueue<TaskQueue> pq, List<ContainerResources> containerResourceList) {
        List<MigratingTask> migratingTasks = new ArrayList<>();
        while(pq.size() > 0){
            TaskQueue taskq = pq.remove();
            for(int resourceIndex : taskq.getMatchedResources()){
                if(isMatchingResourceFound(taskq.getTask(), containerResourceList.get(resourceIndex))){
                    taskq.getTask().setDestination(new Instance(containerResourceList.get(resourceIndex).getEc2InstanceId(),
                            containerResourceList.get(resourceIndex).getContainerInstanceArn()));
                    migratingTasks.add(taskq.getTask());
                    ContainerResources.FreeSpace freeSpace = new ContainerResources.FreeSpace();
                    freeSpace.setCpuAvailable(freeSpace.getCpuAvailable() - (int)(taskq.getTask().getResource().getCpu()*1.3));
                    freeSpace.setMemoryAvailable(freeSpace.getMemoryAvailable() - (int)(taskq.getTask().getResource().getMemory()*1.3));
                    containerResourceList.get(resourceIndex).setFreeSpace(freeSpace);
                    System.out.println("Saving resources for instance : " + taskq.getTask().getSource().getResourceArn());
                }
            }
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
            if(matcher.find())
                containerInstanceIdList.add(matcher.group(2));
        });
        return containerInstanceIdList;
    }
	
	private static List<ContainerResources> getContainerResources(List<String> containerInstances) {
		DescribeContainerInstancesResult result = describeContainerInstances(containerInstances);
		List<ContainerResources> resources = new ArrayList<>();
		result.getContainerInstances().forEach(containerInstance -> {
			
			ContainerResources remainingResources = new ContainerResources();
			remainingResources.setContainerInstanceArn(containerInstance.getContainerInstanceArn());
			remainingResources.setEc2InstanceId(containerInstance.getEc2InstanceId());
			remainingResources.setRunningTasksCount(containerInstance.getRunningTasksCount());

			containerInstance.getRemainingResources().forEach(res -> {
				if (res.getName().equals("CPU")) {
					remainingResources.getFreeSpace().setCpuAvailable(res.getIntegerValue());
				}
				if (res.getName().equals("MEMORY")) {
					remainingResources.getFreeSpace().setMemoryAvailable(res.getIntegerValue());
				}
			});
			
			
			System.out.println(remainingResources); // for debugging
			resources.add(remainingResources);
		});
		return resources;

	}
	
	private static void taskMonitoring(List<String> containerInstances) {
		DescribeContainerInstancesResult result = describeContainerInstances(containerInstances);
		for (ContainerInstance c : result.getContainerInstances()) {
			if(c.getRunningTasksCount()==0){
				//stop that container instance
			}
		}
	}

}
