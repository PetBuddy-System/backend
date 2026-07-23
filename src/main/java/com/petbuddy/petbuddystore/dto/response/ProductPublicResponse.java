package com.petbuddy.petbuddystore.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductPublicResponse extends ProductBaseResponse{
}