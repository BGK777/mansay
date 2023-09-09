package com.hmdp.utils.interceptorUtil;

import com.hmdp.dto.UserDTO;
import com.hmdp.utils.systemUtil.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
        //判断ThreadLocal中是否有用户
        UserDTO user = UserHolder.getThreadLocal().get();
        if( user == null){
            //没有，拦截
            response.setStatus(401);
            return false;
        }
        //放行
        return true;
    }

}
