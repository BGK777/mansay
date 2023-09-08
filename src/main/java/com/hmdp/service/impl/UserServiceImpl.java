package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.MD5;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.systemUtil.HttpUtils;
import com.hmdp.utils.systemUtil.RegexUtils;
import com.hmdp.utils.systemUtil.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.enumUtil.RedisConstants.*;
import static com.hmdp.utils.enumUtil.SystemConstants.USER_NICK_NAME_PREFIX;

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
    public Result sendCode(String phone) {
        //验证手机号是否正确
        if(!RegexUtils.isCodeInvalid(phone)){
            //不正确，返回失败
            return Result.fail("手机格式错误！");
        }
        //正确，生成验证码,并保存到Redis
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //发送验证码
        sms(phone,code);
        //返回ok
        return Result.ok();
    }

    /**
     * 验证码短信发送
     * @param phone
     * @param code
     */
    private void sms(String phone,String code){
        String host = "https://gyytz.market.alicloudapi.com";
        String path = "/sms/smsSend";
        String method = "POST";
        String appcode = "27eb2c19dffa48759988d809eec4700f";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> querys = new HashMap<String, String>();
        querys.put("mobile", phone);
        querys.put("param", "**code**:"+code+",**minute**:5");

//smsSignId（短信前缀）和templateId（短信模板），可登录国阳云控制台自助申请。参考文档：http://help.guoyangyun.com/Problem/Qm.html

        querys.put("smsSignId", "2e65b1bb3d054466b82f0c9d125465e2");
        querys.put("templateId", "908e94ccf08b4476ba6c876d13f084ad");
        Map<String, String> bodys = new HashMap<String, String>();

        try {
            /**
             * 重要提示如下:
             * HttpUtils请从\r\n\t    \t* https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/src/main/java/com/aliyun/api/gateway/demo/util/HttpUtils.java\r\n\t    \t* 下载
             *
             * 相应的依赖请参照
             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/pom.xml
             */
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
            //获取response的body
            //System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Result login(LoginFormDTO loginForm) {
        //手机号和验证码
        String code = loginForm.getCode();
        String phone = loginForm.getPhone();
        String password = loginForm.getPassword();
        //验证手机号
        if(!RegexUtils.isCodeInvalid(phone)){
            //不正确，返回失败
            return Result.fail("手机格式错误！");
        }

        //判断登录类型 true为验证码登录，false为密码登录
        if(StringUtils.isEmpty(password)){
            //验证验证码受否正确,从Redis中取出验证码
            String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
            if(cacheCode == null || !cacheCode.equals(code)){
                //验证码不一致
                return Result.fail("验证码不一致!");
            }
        }

        //根据手机号查询用户
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getPhone,phone);
        User user = this.getOne(lambdaQueryWrapper);

        //不存在
        if(user == null){
            user = createUserWithPhone(phone,password);
        }else if (!StringUtils.isEmpty(password)){
            String md5DigestAsHex = DigestUtils.md5DigestAsHex(password.getBytes());
            if (user.getPassword() == null || Objects.equals(user.getPassword(), "")){
                user.setPassword(md5DigestAsHex);
                saveOrUpdate(user);
            }else {
                if (!md5DigestAsHex.equals(user.getPassword())) return  Result.fail("密码错误!");
            }
        }

        String token = saveToken(user);
        //返回token
        return Result.ok(token);
    }

    /**
     * 保存token到redis
     * @param user
     * @return
     */
    private String saveToken(User user) {
        //存在,保存到Redis
        //生成token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        //将user转化为Hash储存
        //只需要保存基本信息，转化为UserDto做到信息屏蔽，保护隐私
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //设置对象里的值都转化为String储存在Redis中，也可以自己手动创建新的Map一个个转换，这里用到了万能工具类
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true).
                        setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString()));
        //执行储存
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY+token,userMap);
        //设置token有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY+token,LOGIN_USER_TTL,TimeUnit.MINUTES);
        return token;
    }

    @Override
    public Result logout() {
        //清除ThreadLocal中的用户信息，这样在拦截器哪里在ThreadLocal中查询不到用户信息自然会被拦截，跳转到登录界面
        //手动删除ThreadLocal中的用户信息，可以避免内存泄漏，因为是弱引用，gc后key会回收，而value是强引用不会被回收
        UserHolder.removeUser();
       return Result.ok();
    }

    @Override
    public Result sign() {
        //获取用户id
        Long userId = UserHolder.getUser().getId();
        //获取日期
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        //拼接字符串
        String key = USER_SIGN_KEY + userId + keySuffix ;
        //获取这个月是第几天
        int dayOfMonth = now.getDayOfMonth();
        //向redis写入签到
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        //获取用户id
        Long userId = UserHolder.getUser().getId();
        //获取日期
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        //拼接字符串
        String key = USER_SIGN_KEY + userId + keySuffix ;
        //获取这个月是第几天
        int dayOfMonth = now.getDayOfMonth();
        //Redis获取今天截止的签到十进制数
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        //判断非空
        if(result == null || result.isEmpty()){
            return Result.ok(0);
        }
        Long num = result.get(0);
        if(num == null || num == 0){
            return Result.ok(0);
        }
        //计数器
        int count = 0;
        while (true){
            // 让这个数字与1做与运算，得到数字的最后一个bit位  // 判断这个bit位是否为0
            if ((num & 1) == 0) {
                // 如果为0，说明未签到，结束
                break;
            }else {
                // 如果不为0，说明已签到，计数器+1
                count++;
            }
            // 把数字右移一位，抛弃最后一个bit位，继续下一个bit位
            num >>>= 1;
        }
        return Result.ok(count);
    }

    private User createUserWithPhone(String phone,String password) {
        User user = new User();
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setPhone(phone);
        //MD5加密
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());
        user.setPassword(md5Password);
        user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(6));
        this.save(user);
        return user;
    }
}
