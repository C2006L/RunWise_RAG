package com.runwise.admin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class AdminAccount {
    private Long id;
    private String username;
    @JsonIgnore
    @lombok.ToString.Exclude
    private String passwordHash;
    private String role;
    private Integer enabled;
}
