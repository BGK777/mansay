package com.hmdp.entity;

import java.time.LocalDateTime;
import java.util.Date;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * (Comment)表实体类
 *
 * @author makejava
 * @since 2023-09-11 15:57:14
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_comment")
public class Comment  {
    //主键，评论id@TableId
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    //博客id
    private Long blogId;
    //用户id
    private Long userId;
    //评论内容
    private String commentText;
    //创建时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    //更新时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;



}

