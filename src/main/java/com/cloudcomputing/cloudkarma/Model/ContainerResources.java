package com.cloudcomputing.cloudkarma.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContainerResources {
    String ec2InstanceId;
    String containerInstanceArn;
    int runningTasksCount;
    FreeSpace freeSpace;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FreeSpace {
        int cpuAvailable;
        int memoryAvailable;
    }
}
