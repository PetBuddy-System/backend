package com.petbuddy.petbuddystore.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleUserInfoResponse {
    String sub;
    String email;

    @JsonProperty("email_verified")
    Boolean emailVerified;

    String name;
    String picture;

    @JsonProperty("given_name")
    String givenName;

    @JsonProperty("family_name")
    String familyName;
}
