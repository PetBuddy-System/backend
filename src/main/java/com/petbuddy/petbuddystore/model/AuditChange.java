package com.petbuddy.petbuddystore.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditChange {
    private String field;
    private String oldValue;
    private String newValue;
}