package com.commence.novel.service;

import com.commence.novel.entity.User;
import com.commence.novel.utils.Result;


public interface UserService {
    // 发送注册验证码
    Result sendRegisterCode(String email);

    // 登录业务逻辑
    User loginService(String uname, String password,String ip);

    // 注册业务逻辑
    User registerService(User user,String roleType,String code);

    // 重置密码业务逻辑
    Result sendResetCode(String email);

    Result resetPassword(String email, String code,String newPassword);

    Result getCurrentUser(Long uid);

    Result logout();

    // 检查笔名是否存在
    Result checkPenName(String penName);

    Result applyAuthor(String penName, Long uid);

    //查询所有用户（管理员
    Result getAllUsers();


    // 更新用户头像URL
    void updateAvatarUrl(Long userId, String avatarUrl);
}