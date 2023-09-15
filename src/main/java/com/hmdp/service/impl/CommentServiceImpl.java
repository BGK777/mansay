package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.CommentDto;
import com.hmdp.dto.Result;
import com.hmdp.entity.Comment;
import com.hmdp.entity.User;
import com.hmdp.mapper.CommentMapper;
import com.hmdp.service.CommentService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.systemUtil.UserHolder;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
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
    public HashMap<String, Object> getPage(Long blogId, Integer page, Integer size) {
        LambdaQueryWrapper<Comment> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Comment::getBlogId,blogId);
        IPage<Comment> pageInfo = new Page<>(page,size);
        page(pageInfo,lqw);
        List<Comment> records = pageInfo.getRecords();
        ArrayList<CommentDto> commentDtos = new ArrayList<>();
        for (Comment comment: records) {
            CommentDto commentDto = new CommentDto();
            User user = userService.getById(comment.getUserId());
            commentDto.setId(comment.getId());
            commentDto.setIcon(user.getIcon());
            commentDto.setUserId(comment.getUserId());
            commentDto.setNickName(user.getNickName());
            commentDto.setCommentText(comment.getCommentText());
            commentDtos.add(commentDto);
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("totalPage",pageInfo.getPages());
        map.put("curSize",records.size());
        map.put("curPage",pageInfo.getCurrent());
        map.put("list",commentDtos);
        return map;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Result saveComment(Long blogId, String commentText) throws UnsupportedEncodingException {
        if(StringUtils.isBlank(commentText)){
            return Result.fail("内容不能为空！");
        }
        commentText = URLDecoder.decode(commentText, "UTF-8");
        Comment comment = new Comment();
        Long userId = UserHolder.getThreadLocal().get().getId();
        comment.setUserId(userId);
        comment.setBlogId(blogId);
        comment.setCommentText(commentText);
        save(comment);
        return Result.ok();
    }

    @Override
    public void updateComment(Long id, String commentText) {
        Comment comment = getById(id);
        comment.setCommentText(commentText);
        updateById(comment);
    }
}
