package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.CommentDto;
import com.hmdp.entity.Comment;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;


/**
 * (Comment)表服务接口
 *
 * @author makejava
 * @since 2023-09-11 15:57:15
 */
public interface CommentService extends IService<Comment> {

    ArrayList<CommentDto> getPage(Long blogId, Integer page, Integer size);

    void saveComment(Long blogId, String commentText) throws UnsupportedEncodingException;

    void updateComment(Long id, String commentText);
}
