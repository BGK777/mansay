package com.hmdp.utils.systemUtil;

import com.hmdp.dto.UserDTO;

public class UserHolder {
    //唯一ThreadLocal实例
    public ThreadLocal<UserDTO> tl;

    //私有化工具类构造器
    private UserHolder(){
        tl = new ThreadLocal<>();
    }

    static class inner {
        private static final UserHolder userHolder = new UserHolder();
    }

    public static ThreadLocal<UserDTO> getThreadLocal(){
        return inner.userHolder.tl;
    }

//    public static void saveUser(UserDTO userDTO){
//        getThreadLocal().set(userDTO);
//    }
//
//    public static UserDTO getUser(){
//        return getThreadLocal().get();
//    }
//
//    public static void removeUser(){
//        getThreadLocal().remove();
//    }
}
