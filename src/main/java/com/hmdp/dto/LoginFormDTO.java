package com.hmdp.dto;

import lombok.Data;

@Data
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;
    // 登录类型，1.验证码登录 2.密码登录
    private String type;
}
