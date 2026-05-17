package com.sprintsense.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActionItem {
    private String task;
    private String owner;
    private String priority;
}
