package com.runwise.dto;
import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PlanItemDTO {
    @NotNull private LocalDate date;
    @NotNull @Pattern(regexp="easy|tempo|interval|long|rest") private String type;
    @NotNull @DecimalMin("0") @DecimalMax("300") @Digits(integer=3,fraction=2) private BigDecimal targetKm;
    @Size(max=255) private String note;
}
