package com.hmdp.controller;


import com.hmdp.dto.OrderInfoDto;
import com.hmdp.dto.QureyData;
import com.hmdp.dto.Result;
import com.hmdp.service.IVoucherOrderService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    @Resource
    private IVoucherOrderService iVoucherOrderService;

    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return iVoucherOrderService.secKillVoucher(voucherId);
    }

    /**
     * 根据用户id，获取订单列表
     * @param userId
     * @return
     */
    @PostMapping("/list/{userId}")
    public Result orderList(@PathVariable("userId") String userId, @RequestBody QureyData qureyData){
         ArrayList<OrderInfoDto> orderList = iVoucherOrderService.pageOrderList(userId,qureyData);
         return Result.ok(orderList);
    }
}
