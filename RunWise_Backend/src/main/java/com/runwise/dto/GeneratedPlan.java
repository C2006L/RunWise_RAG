package com.runwise.dto;
import lombok.Data;
import java.util.List;

@Data
public class GeneratedPlan {
    private String status;
    private List<String> questions;
    private String explanation;
    private List<PlanItemDTO> days;
    private List<String> sources;
    private String model;
    private String knowledgeVersion;
}
