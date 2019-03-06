package com.cloudcomputing.cloudkarma;

import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesResult;
import com.amazonaws.services.ecs.model.DescribeClustersResult;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ApiCaller {

      @Autowired
      Actions actions;


      public void getEc2InstanceInfo(){
          List<String> instanceList = new ArrayList<>();
          instanceList.add("cluster-1");
          //StopInstancesResult result = actions.stopInstances(instanceList);
          //DescribeInstancesResult result = actions.describeInstances(instanceList);
          DescribeClustersResult describeEcsClusters = actions.describeEcsClusters(instanceList);
          System.out.println(describeEcsClusters);

      }
}
