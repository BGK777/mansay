package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.EditUserDto;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.mapper.UserInfoMapper;
import com.hmdp.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.systemUtil.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-24
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

    @Resource
    private IUserService userService;

    @Override
    public void updateInfo(UserInfo userInfo) {
        updateById(userInfo);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserInfo saveInfo(EditUserDto editUserDto) {
        UserDTO userDTO = UserHolder.getThreadLocal().get();
        Long userId = userDTO.getId();
//        //保存到ThreadLocal
//        userDTO.setNickName(editUserDto.getNickName());
//        UserHolder.getThreadLocal().set(userDTO);
        //保存到数据库User表
        User user = userService.getById(userId);
        user.setNickName(editUserDto.getNickName());
        userService.updateById(user);

        //保存到数据库Info表
        UserInfo userInfo = getById(userId);
        //这里判断这个用户是否是第一次登录，还没有建立详细信息
        if(userInfo == null){
            userInfo = new UserInfo();
            userInfo.setUserId(userId);
            baseMapper.insert(userInfo);
        }else {


        }        BeanUtil.copyProperties(editUserDto,userInfo);
        baseMapper.updateById(userInfo);
        return userInfo;
    }
}
