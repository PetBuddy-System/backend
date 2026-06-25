package com.petbuddy.petbuddystore.dto.cart;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartData {
    String userId;

    @Builder.Default
    List<CartItemData> items = new ArrayList<>();
}
