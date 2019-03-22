package com.cloudcomputing.cloudkarma;

import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ecs.model.DescribeClustersResult;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesResult;
import com.cloudcomputing.cloudkarma.ContainerUsage.CloudWatchMetrics;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class ApiCaller {

      @Autowired
      Actions actions;

      @Autowired
      CloudWatchMetrics cloudWatchMetrics;


      public void getEc2InstanceInfo(){
          List<String> instanceList = new ArrayList<>();
          //instanceList.add("5d014b95036f4a20a4c0f81c56b60265");
          //instanceList.add("5d014b95036f4a20a4c0f81c56b60265");
          //instanceList.add("ed9aa8083f5045baab1b35963a1cd843");
          //instanceList.add("cluster-1");
          String resourcearn = "arn:aws:ecs:us-west-1:001287417591:cluster/c1";
          String taskDefiition = "task-definition-1:5";
          String instanceID = "i-0c4e37ce2e1dda839";

          //StopInstancesResult result = actions.stopInstances(instanceList);
          //DescribeInstancesResult result = actions.describeInstances(instanceList);
          //DescribeClustersResult describeEcsClusters = actions.describeClusters(instanceList);
          //actions.stopInstancesIfInactive("cluster-1");
          //DescribeContainerInstancesResult describeContainerInstancesResult = actions.describeContainerInstances(instanceList);

          Actions.filterInstances();
          //System.out.println(describeEcsClusters);
          System.out.println(actions.getTagsForResources(resourcearn));
          System.out.println(actions.getTaskDefinitionDetails(taskDefiition));
          actions.getTagResultForSB(instanceID);
          actions.getTagResultForNB(instanceID);
      }

      public void getCloudWatchMetrics(){
          List<String> instanceList = new ArrayList<>();
          instanceList.add("i-05b7ae869c356fe24");
          //return cloudWatchMetrics.getMetrics();
          //actions.describeInstances(instanceList);
      }
}
