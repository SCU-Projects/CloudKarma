package com.cloudcomputing.cloudkarma.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskQueue {
    MigratingTask task;
    List<Integer> matchedResources;
}
