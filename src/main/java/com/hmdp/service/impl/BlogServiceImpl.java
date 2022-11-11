package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result getHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.queryAndSetUser(blog);
            this.isBlogLike(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result likedBlog(Long id) {
        //1.获取的登录用户id
        Long userId = UserHolder.getUser().getId();
        String blogKey = BLOG_LIKED_KEY + id;
        //2.判断用户是否点赞
        Double score = stringRedisTemplate.opsForZSet().score(blogKey, userId.toString());
        if (score == null) {
            //3.没点赞，数据库点赞加一
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            //3.1保存用户id到Redis
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(blogKey,userId.toString(),System.currentTimeMillis());
            }
        } else {
            //4.已点赞，取消点赞
            //4.1数据库点赞数减一
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            //4.2移除Redis中用户id
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(blogKey, String.valueOf(userId));
            }
        }
        return Result.ok();
    }

    @Override
    public Result getBlogLikes(Long id) {
        String blogKey = BLOG_LIKED_KEY + id;
        //查询top5的点赞用户
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(blogKey, 0, 5);
        if(top5 == null || top5.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        //获得id列表
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",", ids);
        List<User> users = userService.query()
                .in("id",ids).last("ORDER BY FIELD(id,"+idStr+")").list();
        //把查询到的user列表。转化成userDTO列表并返回
        List<UserDTO> UserDTOs = new ArrayList<>();
        users.forEach(user -> {
            UserDTO userDTO = new UserDTO();
            userDTO.setIcon(user.getIcon());
            userDTO.setId(user.getId());
            userDTO.setNickName(user.getNickName());
            UserDTOs.add(userDTO);
        });
        return Result.ok(UserDTOs);
    }

    @Override
    public Result getBlogById(Long id) {
        //1.查询Blog
        Blog blog = getById(id);
        if (blog == null) {
            //不存在，返回错误
            return Result.fail("Bolg不存在！");
        }
        //存在，查询用户信息，并添加用户信息
        queryAndSetUser(blog);
        //查询，blog是否被点过赞
        isBlogLike(blog);
        //返回Blog
        return Result.ok(blog);
    }

    private void isBlogLike(Blog blog) {
        //1.获取的登录用户id
        UserDTO user = UserHolder.getUser();
        if(user == null){
            //用户未登录，无需查询是否点赞
            return ;
        }
        Long userId = user.getId();
        String blogKey = BLOG_LIKED_KEY + blog.getId();
        //2.判断用户是否点赞
        Double score = stringRedisTemplate.opsForZSet().score(blogKey, userId.toString());
        blog.setIsLike(score != null);
    }

    /**
     * 查询并设置用户信息
     *
     * @param blog
     */
    private void queryAndSetUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
