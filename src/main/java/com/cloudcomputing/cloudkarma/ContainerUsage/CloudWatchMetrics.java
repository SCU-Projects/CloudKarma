package com.cloudcomputing.cloudkarma.ContainerUsage;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;

import java.util.Date;


import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;

import java.util.Date;


@Service
public class CloudWatchMetrics{

    private static AmazonCloudWatchClient client(final String awsAccessKey, final String awsSecretKey) {
        final AmazonCloudWatchClient client = new AmazonCloudWatchClient(new BasicAWSCredentials(awsAccessKey, awsSecretKey));
        //need to set end point
        client.setEndpoint("");
        return client;
    }

    public static GetMetricStatisticsRequest request(final String instanceId) {
        final long twentyFourHrs = 1000 * 60 * 60 * 24;
        final int oneHour = 60 * 60;
        return new GetMetricStatisticsRequest()
                .withStartTime(new Date(new Date().getTime()- twentyFourHrs))
                .withNamespace("AWS/EC2")
                .withPeriod(oneHour)
                .withDimensions(new Dimension().withName("InstanceId").withValue(instanceId))
                .withMetricName("CPUUtilization")
                .withStatistics("Average", "Maximum")
                .withEndTime(new Date());
    }

    public static void toStdOut(final GetMetricStatisticsResult result, final String instanceId) {
        System.out.println(result); // outputs empty result: {Label: CPUUtilization,Datapoints: []}
        for (final Datapoint dataPoint : result.getDatapoints()) {
            System.out.printf("%s instance's average CPU utilization : %s%n", instanceId, dataPoint.getAverage());
            System.out.printf("%s instance's max CPU utilization : %s%n", instanceId, dataPoint.getMaximum());
        }
    }
    public static GetMetricStatisticsResult getMetrics() {
//        final String awsAccessKey = "";
//        final String awsSecretKey = "";
        final String instanceId = "i-05b7ae869c356fe24";

        //final AmazonCloudWatchClient client = client(awsAccessKey, awsSecretKey);
        AmazonCloudWatchClient client1 = new AmazonCloudWatchClient();
        client1.setRegion(com.amazonaws.regions.Region.getRegion(Regions.US_EAST_2));
        final GetMetricStatisticsRequest request = request(instanceId);
        System.out.println("Request " + request);
        final GetMetricStatisticsResult result = client1.getMetricStatistics(request);
        System.out.println("Result " + result);
        toStdOut(result, instanceId);
        return result;
    }

    //public static getEcsDashboard

}

