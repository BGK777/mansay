package com.hmdp.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Shop;
import com.hmdp.service.IBlogService;
import com.hmdp.utils.enumUtil.SystemConstants;
import com.hmdp.utils.systemUtil.UserHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;

    /**
     * 提交bolg
     * @param blog
     * @return
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }

    /**
     * 是否点赞
     * @param id
     * @return
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.likedBlog(id);
    }

    /**
     * 获取个人blog列表
     * @param current
     * @return
     */
    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO userDTO = UserHolder.getThreadLocal().get();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", userDTO.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * 热门blog
     * @param current
     * @return
     */
    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.getHotBlog(current);
    }

    /**
     * 根据id获取blog
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result getBlogById(@PathVariable("id") Long id) {
        return blogService.getBlogById(id);
    }

    @GetMapping("/likes/{id}")
    public Result getBlogLikes(@PathVariable("id") Long id) {
        return blogService.getBlogLikes(id);
    }

    @GetMapping("/of/user")
    public Result queryBlogByUserId(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam("id") Long id) {
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", id).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * 是否关注
     * @param max
     * @param offset
     * @return
     */
    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(
            @RequestParam("lastId") Long max ,
            @RequestParam(value = "offset",defaultValue = "0") Integer offset) {
        return blogService.queryBlogOfFollow(max,offset);
    }

    /**
     * 根据店铺id返回Blog列表
     * @param shopId
     * @return
     */

    private double a  = 3.1;
    @GetMapping("/shop/{shopId}/{page}")
    public Result queryBlogByShopId(@PathVariable("shopId") Long shopId,
                                    @PathVariable("page") Integer page){
        HashMap<String,Object> map = blogService.getBlogsByShopId(shopId,page);
        return Result.ok(map);
    }
}
