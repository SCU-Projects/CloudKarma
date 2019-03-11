package com.cloudcomputing.cloudkarma.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigratingTask {
    Instance source;
    Instance destination;
    List<String> tagsList;
    Resource resource;
    String TaskArn;
}
