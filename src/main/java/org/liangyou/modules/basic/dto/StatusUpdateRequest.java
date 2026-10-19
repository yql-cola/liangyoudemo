package org.liangyou.modules.basic.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@Data
public class StatusUpdateRequest {

    @NotNull(message = "status-required")
    @Min(value = 0, message = "status-invalid")
    @Max(value = 1, message = "status-invalid")
    private Integer status;
}
