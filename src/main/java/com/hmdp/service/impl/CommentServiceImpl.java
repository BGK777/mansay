package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.CommentDto;
import com.hmdp.entity.Comment;
import com.hmdp.entity.User;
import com.hmdp.mapper.CommentMapper;
import com.hmdp.service.CommentService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.systemUtil.UserHolder;
import com.sun.org.apache.bcel.internal.generic.NEW;
import netscape.security.UserDialogHelper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * (Comment)表服务实现类
 *
 * @author makejava
 * @since 2023-09-11 15:57:15
 */
@Service("commentService")
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Resource
    IUserService userService;
    @Override
    public ArrayList<CommentDto> getPage(Long blogId,Integer page, Integer size) {
        LambdaQueryWrapper<Comment> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Comment::getBlogId,blogId);
        IPage<Comment> pageInfo = new Page<>(page,size);
        page(pageInfo,lqw);
        List<Comment> records = pageInfo.getRecords();
        ArrayList<CommentDto> commentDtos = new ArrayList<>();
        for (Comment comment: records) {
            CommentDto commentDto = new CommentDto();
            User user = userService.getById(comment.getUserId());
            commentDto.setIcon(user.getIcon());
            commentDto.setNickName(user.getNickName());
            commentDto.setCommentText(comment.getCommentText());
            commentDtos.add(commentDto);
        }
        return commentDtos;
    }

    @Override
    public void saveComment(Long blogId, String commentText) throws UnsupportedEncodingException {
        String commentTex = URLDecoder.decode(commentText, "UTF-8");
        Long userId = UserHolder.getThreadLocal().get().getId();
        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setBlogId(blogId);
        comment.setCommentText(commentTex);
        save(comment);
    }

    @Override
    public void updateComment(Long id, String commentText) {
        Comment comment = getById(id);
        comment.setCommentText(commentText);
        updateById(comment);
    }
}
