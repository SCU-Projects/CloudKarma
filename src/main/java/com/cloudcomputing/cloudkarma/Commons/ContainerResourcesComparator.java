package com.cloudcomputing.cloudkarma.Commons;

import com.cloudcomputing.cloudkarma.Model.ContainerResources;


public class ContainerResourcesComparator implements java.util.Comparator<ContainerResources> {
    @Override
    public int compare(ContainerResources r1, ContainerResources r2) {
        //first by CPU and then by memory
        if(r1.getFreeSpace().getCpuAvailable() == r2.getFreeSpace().getCpuAvailable())
            return r1.getFreeSpace().getMemoryAvailable() - r2.getFreeSpace().getMemoryAvailable();
        return r1.getFreeSpace().getCpuAvailable() - r2.getFreeSpace().getCpuAvailable();
    }
}
