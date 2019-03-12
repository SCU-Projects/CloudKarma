package com.cloudcomputing.cloudkarma.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContainerResources {
    String ec2InstanceId;
    String containerInstanceArn;
    int runningTasksCount;
    @Getter @Setter FreeSpace freeSpace;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class FreeSpace {
    	@Getter @Setter int cpuAvailable;
         @Getter @Setter int memoryAvailable;
    }
}
