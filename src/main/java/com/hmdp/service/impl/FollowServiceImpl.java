package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.systemUtil.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

    /**
     * 取关或关注操作
     *
     * @param followId
     * @param isFollow
     * @return
     */
    @Override
    public Result follow(Long followId, boolean isFollow) {
        //获取用户id
        Long userId = UserHolder.getUser().getId();
        String key = "follows:" + userId;
        //判断是关注还是取关
        if (isFollow) {
            //关注
            Follow follow = new Follow();
            follow.setFollowUserId(followId);
            follow.setUserId(userId);
            boolean success = save(follow);
            if (success) {
                //关注保存到Redis
                stringRedisTemplate.opsForSet().add(key, followId.toString());
            }
        } else {
            //取关
            LambdaQueryWrapper<Follow> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(Follow::getUserId, userId).eq(Follow::getFollowUserId, followId);
            boolean success = remove(lambdaQueryWrapper);
            if (success) {
                //Redis移除关注的用户id
                stringRedisTemplate.opsForSet().remove(key, followId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 查询是否关注操作
     *
     * @param followId
     * @return
     */
    @Override
    public Result followOrNot(Long followId) {
        //获取用户id
        Long userId = UserHolder.getUser().getId();
        LambdaQueryWrapper<Follow> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Follow::getUserId, userId).eq(Follow::getFollowUserId, followId);
        int count = count(lambdaQueryWrapper);
        return Result.ok(count > 0);
    }

    /**
     * 查询共同关注
     *
     * @param id
     */
    @Override
    public Result common(Long id) {
        //1.获取当前用户
        Long userId = UserHolder.getUser().getId();
        String key1 = "follows:" + userId;
        //2.求交集
        String key2 = "follows:" + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        if (intersect == null || intersect.isEmpty()) {
            // 无交集
            return Result.ok(Collections.emptyList());
        }
        //3.解析id集合
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        //4.查询用户集合
        List<UserDTO> userDTOS = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOS);
    }
}
