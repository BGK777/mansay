package com.hmdp.controller;


import com.hmdp.dto.CommentDto;
import com.hmdp.dto.Result;
import com.hmdp.service.CommentService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * (Comment)表控制层
 *
 * @author makejava
 * @since 2023-09-11 15:57:13
 */
@RestController
@RequestMapping("comment")
public class CommentController{
    /**
     * 服务对象
     */
    @Resource
    private CommentService commentService;

    /**
     * 根据blogId分页查询所有blog
     * @return 所有数据
     */
    @GetMapping("/page/{blogId}/{page}/{size}")
    public Result selectAll(@PathVariable("blogId") Long blogId,
                            @PathVariable("page") Integer page,
                            @PathVariable("size") Integer size) {
        ArrayList<CommentDto> list =  commentService.getPage(blogId,page,size);
        return Result.ok(list);
    }

    /**
     * 新增数据
     *
     * @param
     * @return 新增结果
     */
    @PostMapping("/save/{blogId}")
    public Result insert(@PathVariable("blogId") Long blogId,@RequestBody String commentText) throws UnsupportedEncodingException {
        commentService.saveComment(blogId,commentText);
        return Result.ok();
    }

    /**
     * 更新数据
     *
     * @param
     * @return 更新结果
     */
    @PostMapping("/update/{id}")
    public Result update(@PathVariable("id") Long id,@RequestBody String commentText) {
        commentService.updateComment(id,commentText);
        return Result.ok();
    }

    /**
     * 删除数据
     *
     * @param
     * @return 删除结果
     */
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable("id") Long id) {
        commentService.removeById(id);
        return Result.ok();
    }
}

