package org.liangyou.modules.basic.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequest {

    @NotNull(message = "status-required")
    private Integer status;
}
