package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //验证手机号是否正确
        if(!RegexUtils.isCodeInvalid(phone)){
            //不正确，返回失败
            return Result.fail("手机格式错误！");
        }
        //正确，生成验证码,并保存到Redis
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
//        session.setAttribute("code",code);
        //发送验证码
        log.info("验证码========>{}",code);
        //返回ok
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //手机号和验证码
        String code = loginForm.getCode();
        String phone = loginForm.getPhone();
        //验证手机号
        if(!RegexUtils.isCodeInvalid(phone)){
            //不正确，返回失败
            return Result.fail("手机格式错误！");
        }
        //验证验证码受否正确,从Redis中取出验证码
//        Object cacheCode = session.getAttribute("code");
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if(cacheCode == null || !cacheCode.equals(code)){
            //验证码不一致
            return Result.fail("验证码不一致!");
        }
        //根据手机号查询用户
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getPhone,phone);
        User user = this.getOne(lambdaQueryWrapper);
//        User user = query().eq("phone", phone).one();
        //不存在
        if(user == null){
            user = createUserWithPhone(phone);
        }
        //存在,保存到Redis
        //生成token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        //将user转化为Hash储存
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //设置对象里的值都转化为String储存在Redis中，也可以自己手动创建新的Map一个个转换，这里用到了万能工具类
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).
                        setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString()));
        //执行储存
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY+token,userMap);
        //设置token有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY+token,LOGIN_USER_TTL,TimeUnit.MINUTES);
        //返回token
        return Result.ok(token);
    }

    @Override
    public Result logout() {
        //获取当前用户id
       return null;
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(6));
        this.save(user);
        return user;
    }
}
