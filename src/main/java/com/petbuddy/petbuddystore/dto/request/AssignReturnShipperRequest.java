package com.petbuddy.petbuddystore.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignReturnShipperRequest {

    @NotBlank(message = "Shipper id is required")
    private String shipperId;

}