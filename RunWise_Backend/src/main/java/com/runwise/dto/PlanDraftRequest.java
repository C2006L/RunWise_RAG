package com.runwise.dto;
import lombok.Data;
import javax.validation.constraints.*;
import java.time.LocalDate;

@Data
public class PlanDraftRequest {
    @NotNull @Pattern(regexp="maintain|5k|10k|fitness") private String goal;
    @NotNull private LocalDate startDate;
    @NotNull @Min(1) @Max(6) private Integer sessionsPerWeek;
    @NotNull private Boolean noDiscomfortConfirmed;
    @Size(max=500) private String constraints;
    @Pattern(regexp="[A-Za-z0-9_-]{1,80}") private String conversationId;
}
