package com.hmdp.service;

import com.hmdp.dto.EditUserDto;
import com.hmdp.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-24
 */
public interface IUserInfoService extends IService<UserInfo> {

    void updateInfo(UserInfo userInfo);

    UserInfo saveInfo(EditUserDto editUserDto);
}
