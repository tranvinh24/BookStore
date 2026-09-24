package com.example.BookVerse.dto.response;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private String period;       // e.g. "2026-08" or "2026-08-20"
    private Long totalRevenue;   // tong tien thu ve (Payment.SUCCESS)
    private Long totalOrders;    // tong don hang
    private Long paidOrders;     // so don da thanh toan
    private Long pendingOrders;  // so don cho xu ly
}