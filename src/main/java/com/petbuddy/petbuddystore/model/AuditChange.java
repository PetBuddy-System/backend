package com.petbuddy.petbuddystore.model;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditChange {
    private String field;
    private String oldValue;
    private String newValue;
}