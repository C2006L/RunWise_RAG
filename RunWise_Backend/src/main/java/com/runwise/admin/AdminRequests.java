package com.runwise.admin;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

public final class AdminRequests {
    private AdminRequests() { }
    @Data
    public static class Login {
        @NotBlank @Pattern(regexp="[A-Za-z0-9_.-]{3,64}") private String username;
        @NotBlank @Size(max=128) private String password;
    }
    @Data
    public static class Section {
        @NotBlank @Size(max=128) private String heading;
        @NotBlank @Size(max=8000) private String body;
    }
    @Data
    public static class Article {
        @NotBlank @Size(max=128) private String title;
        @NotNull @Pattern(regexp="knee|ankle|shin") private String category;
        @NotNull @Size(max=255) private String summary;
        @NotNull @Size(max=255) private String tags;
        @NotEmpty @Size(max=20) @Valid private List<Section> sections;
        @NotNull @Min(1) @Max(120) private Integer readMinutes;
        @NotNull @Min(0) @Max(100000) private Integer sort;
        @NotNull @Min(0) private Integer revision;
    }
    @Data
    public static class Status {
        @NotNull @Pattern(regexp="PUBLISHED|OFFLINE") private String status;
        @NotNull @Min(1) private Integer revision;
    }
}
