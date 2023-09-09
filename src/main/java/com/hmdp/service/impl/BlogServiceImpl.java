package com.hmdp.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.enumUtil.SystemConstants;
import com.hmdp.utils.systemUtil.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.hmdp.utils.enumUtil.RedisConstants.BLOG_LIKED_KEY;
import static com.hmdp.utils.enumUtil.RedisConstants.FEED_KEY;

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
    @Resource
    private IFollowService followService;
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
        Long userId = UserHolder.getThreadLocal().get().getId();
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
        String idStr = CharSequenceUtil.join(",", ids);
        List<User> users = userService.query()
                .in("id",ids).last("ORDER BY FIELD(id,"+idStr+")").list();
        //把查询到的user列表。转化成userDTO列表并返回
        List<UserDTO> userDTOS = new ArrayList<>();
        users.forEach(user -> {
            UserDTO userDTO = new UserDTO();
            userDTO.setIcon(user.getIcon());
            userDTO.setId(user.getId());
            userDTO.setNickName(user.getNickName());
            userDTOS.add(userDTO);
        });
        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO userDTO = UserHolder.getThreadLocal().get();
        Long userId = userDTO.getId();
        blog.setUserId(userId);
        // 保存探店博文
        boolean isSuccess = save(blog);
        if(!isSuccess){
            Result.fail("发布失败,内容，标题等不能为空！");
        }
        // 查询笔记作者的粉丝
        LambdaQueryWrapper<Follow> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Follow::getFollowUserId,userId);
        List<Follow> follows = followService.list();
        //推送笔记id给所有粉丝
        for(Follow follow : follows){
            //粉丝id
            Long fansId = follow.getUserId();
            //推送
            String key = FEED_KEY + fansId;
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());
        }
        // 返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        //1.获取当前用户，找到收件箱
        Long userId = UserHolder.getThreadLocal().get().getId();
        String key = FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 3);
        //判断非空
        if(typedTuples == null || typedTuples.isEmpty()){
            return Result.ok();
        }
        //解析数据：blogId,minTime(时间戳)，offset
        List<Long> ids = new ArrayList<>();
        long minTime = 0;
        int os = 1;
        for(ZSetOperations.TypedTuple<String> typedTuple : typedTuples){
            //获取id
            String idStr = typedTuple.getValue();
            ids.add(Long.valueOf(Objects.requireNonNull(idStr)));
            //获取分数，时间戳
            long time = Objects.requireNonNull(typedTuple.getScore()).longValue();
            if(time == minTime){
                os++;
            }else {
                minTime = time;
                os = 1;
            }
        }
        //根据id查询blog
        String idStr = CharSequenceUtil.join(",", ids);
        List<Blog> blogs = query()
                .in("id",ids).last("ORDER BY FIELD(id,"+idStr+")").list();
        for (Blog blog : blogs) {
            //存在，查询用户信息，并添加用户信息
            queryAndSetUser(blog);
            //查询，blog是否被点过赞
            isBlogLike(blog);
        }
        //分装并返回
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(os);
        scrollResult.setMinTime(minTime);
        return Result.ok(scrollResult);
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
        UserDTO user = UserHolder.getThreadLocal().get();
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
