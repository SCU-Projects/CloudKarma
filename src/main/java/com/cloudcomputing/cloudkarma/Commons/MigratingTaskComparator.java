package com.cloudcomputing.cloudkarma.Commons;


import com.cloudcomputing.cloudkarma.Model.MigratingTask;

public class MigratingTaskComparator implements java.util.Comparator<MigratingTask> {

    @Override
    public int compare(MigratingTask t1, MigratingTask t2) {
        //first by CPU and then by memory
        if(t1.getResource().getCpu() == t2.getResource().getCpu())
            return t1.getResource().getMemory() - t2.getResource().getMemory();
        return t1.getResource().getCpu() - t2.getResource().getCpu();
    }
}