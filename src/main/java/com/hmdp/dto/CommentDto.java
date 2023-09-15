package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    private Long id;
    //评论人ID
    private Long userId;
    //昵称，默认是随机字符
    private String nickName;
    //用户头像
    private String icon = "";
    //评论内容
    private String commentText;
}
