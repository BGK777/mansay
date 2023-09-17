package com.hmdp.service;

import com.hmdp.dto.OrderInfoDto;
import com.hmdp.dto.QureyData;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result secKillVoucher(Long voucherId);

    void createVoucherOrder(VoucherOrder voucherOrder);

    ArrayList<OrderInfoDto> pageOrderList(String userId, QureyData qureyData);
}
