package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderInfoDto {
    //订单表相关
    private Long orderId;
    private Integer orderStatus;
    private String createTime;

    //店铺相关
    private String shopName;

    //优惠卷相关
    private Integer voucherStatus;
    private String voucherName;
}
