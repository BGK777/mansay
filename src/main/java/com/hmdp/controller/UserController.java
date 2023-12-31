package com.hmdp.controller;


import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.EditUserDto;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.systemUtil.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone) {
        //发送短信验证码并保存验证码
        return userService.sendCode(phone);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm){
        //实现登录功能
        return userService.login(loginForm);
    }

    /**
     * 保存个人信息涉及UserInfo和User
     * @param editUserDto
     * @return
     */
    @PostMapping("/save")
    public Result save(@RequestBody EditUserDto editUserDto){
        UserInfo userInfo = userInfoService.saveInfo(editUserDto);
        return Result.ok(userInfo);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(){
        return userService.logout();
    }

    /**
     * 获取登录用户信息
     * @return
     */
    @GetMapping("/me")
    public Result me(){
        //获取当前登录的用户并返回
        Long userId = UserHolder.getThreadLocal().get().getId();
        UserDTO userDTO = userService.getMe(userId);
        return Result.ok(userDTO);
    }

    /**
     * 查询用户详细信息
     * @param userId
     * @return
     */
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        // 返回
        return Result.ok(info);
    }

    /**
     * 更新用户详情
     * @param userInfo
     * @return
     */
    @PostMapping("/info/update")
    public Result updateUserInfo(@RequestBody UserInfo userInfo){
        userInfoService.updateInfo(userInfo);
        return Result.ok();
    }


    /**
     * 查询用户基本信息
     * @param userId
     * @return
     */
    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }

    /**
     * 签到功能
     * @return
     */
    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }

    /**
     * 查询连续签到
     */
    @PostMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }
}
