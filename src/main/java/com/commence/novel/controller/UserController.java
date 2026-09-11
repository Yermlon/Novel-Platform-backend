package com.commence.novel.controller;

import com.commence.novel.DTO.RegisterParam;
import com.commence.novel.DTO.UserDTO;
import com.commence.novel.entity.User;
import com.commence.novel.exception.*;
import com.commence.novel.repository.UserRepository;
import com.commence.novel.service.UserService;
import com.commence.novel.utils.JwtUtil;
import com.commence.novel.utils.Result;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/send-register-code")
    public Result sendRegisterCodeController(@RequestParam @NotBlank(message = "邮箱不能为空")String email){
        return userService.sendRegisterCode(email);
    }

    @PostMapping("/login")
    public Result loginController(
            @RequestParam @NotBlank(message = "用户名不能为空") String uname,
            @RequestParam @NotBlank(message = "密码不能为空") String password,
            HttpServletRequest request) {

        String ip = getClientIP(request);
        User user = userService.loginService(uname, password, ip);

        if (user != null) {
            // 1. 生成 JWT Token
            String token = jwtUtil.generateToken(user.getUid(), user.getUname(), user.getRole());

            // 2. 构建返回数据（包含 Token 和用户信息）
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("token", token);
            resultData.put("uid", user.getUid());
            resultData.put("uname", user.getUname());
            resultData.put("role", user.getRole());
            resultData.put("penName", user.getPenName());

            return Result.success(resultData, "登录成功！");
        } else {
            return Result.error("123", "账号或密码错误!");
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip.contains(",")) {
            ip = ip.split(",")[0];
        }
        return ip;
    }

    @PostMapping("/register")
    public Result<UserDTO> registerController(@RequestBody RegisterParam  param){
        if (param == null){
            return Result.error("400","请求参数不能为空");
        }//避免空指针
        User newUser = param.getNewUser();
        String roleType = param.getRoleType();
        String code = param.getCode();

        if (newUser == null){
            return Result.error("457","用户信息不能为空");
        }

        if (StringUtils.isBlank(newUser.getUname())){
            return Result.error("457-1","用户名不能为空");
        }
        if (StringUtils.isBlank(newUser.getPassword())){
            return Result.error("457-2","密码不能为空");
        }
        if (newUser.getPassword().length() < 6){
            return Result.error("457-3","密码长度不能少于6位");
        }
        if (StringUtils.isBlank(newUser.getEmail())){
            return Result.error("457-4","邮箱不能为空");
        }
        if (!isValidEmail(newUser.getEmail())){
            return Result.error("457-5","请输入有效的邮箱地址");
        }
        if (StringUtils.isBlank(roleType)){
            return Result.error("457-6","请选择用户类型(读者/作者)");
        }
        if (StringUtils.isBlank(code)){
            return Result.error("457-7","验证码不能为空");
        }
        if (code.length() != 6){
            return Result.error("457-8","验证码必须为6位数字");
        }

        //调用服务层注册
        try{
            User user = userService.registerService(newUser,roleType,code);
            if (user==null){
                log.error("服务层返回user为null,注册失败");
                return Result.error("500","注册失败：服务处理异常");
            }
            UserDTO userDTO = new UserDTO();
            BeanUtils.copyProperties(user,userDTO,"password");
            return Result.success(userDTO,"注册成功！");
        }catch (UsernameExistsException e){
            return Result.error("456-1","用户名已存在，请更换");
        }catch (EmailExistsException e){
            return Result.error("456-2","邮箱已被注册，请更换");
        }catch (CaptchaMismatchException e){
            return Result.error("456-3","验证码错误");
        }catch (CaptchaExpiredException e){
            return Result.error("456-4","验证码已过期,请重新获取");
        }catch (CaptchaNotFoundException e){
            return Result.error("456-5","未找到验证码");
        }catch (Exception e){
            log.error("注册失败",e);
            return Result.error("500","注册失败");
        }
    }

    //校验邮箱格式
    private boolean isValidEmail(String email){
        String regex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return Pattern.matches(regex,email);
    }

    @PostMapping("/send-reset-code")
    public Result sendResetCodeController(@RequestParam @NotBlank(message = "邮箱不能为空") String email){
        return userService.sendResetCode(email);
    }

    @PostMapping("/reset-password")
    public Result resetPasswordController(
            @RequestParam String email,
            @RequestParam String code,
            @RequestParam String newPassword
    ){
        return userService.resetPassword(email,code,newPassword);
    }

    @GetMapping("/current")
    public Result getCurrentUser(HttpServletRequest request) {
        // 1. 从请求属性中获取JWT解析的uid
        Long uid = (Long) request.getAttribute("uid");
        if (uid == null) {
            return Result.error("10086", "未登录(游客状态)");
        }
        // 2. 调用 Service 方法（而非 Controller 直接查库）
        return userService.getCurrentUser(uid);
    }

    @PostMapping("/logout")
    public Result logout() {
        // 调用 Service 方法（即使现在逻辑简单，也保留扩展入口）
        return userService.logout();
    }

    @GetMapping("/check-penname")
    public Result checkPenName(@RequestParam String penName){
        return userService.checkPenName(penName);
    }

    // ========== apply-author 接口，从Token解析用户 ==========
    @PostMapping("/apply-author")
    public Result applyAuthor(@RequestBody Map<String,String> request, HttpServletRequest httpRequest) {
        // 1. 从请求属性获取JWT拦截器解析的uid
        Long uid = (Long) httpRequest.getAttribute("uid");
        if (uid == null) {
            return Result.error("1006", "请先登录后再申请");
        }

        // 2. 获取笔名参数
        String penName = request.get("penName");
        if (penName == null) {
            return Result.error("1012", "笔名不能为空");
        }

        // 3. 调用修改后的service方法
        return userService.applyAuthor(penName, uid);
    }

    @GetMapping("/all")
    public Result getAllUsers(HttpServletRequest request) {
        // 1. 校验权限：仅 ADMIN 角色可访问（需在JWT拦截器中解析role）
        String role = (String) request.getAttribute("role");
        if (role == null || !"ADMIN".equals(role)) {
            return Result.error("403", "无权限访问用户管理功能");
        }

        // 2. 调用Service查询所有用户
        return userService.getAllUsers();
    }

    @PostMapping("/update-avatar")
    public Result updateAvatar(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            // 从 request 或 token 里获取 userId
            Long userId = (Long) request.getAttribute("uid");
            if (userId == null) {
                return Result.error("1006", "请先登录");
            }

            String avatarUrl = (String) params.get("avatarUrl");
            if (avatarUrl == null || avatarUrl.isBlank()) {
                return Result.error("2001", "头像URL不能为空");
            }

            // 调用 Service 更新数据库
            userService.updateAvatarUrl(userId, avatarUrl);
            return Result.success(null, "头像更新成功");
        } catch (Exception e) {
            return Result.error("2001", "头像更新失败：" + e.getMessage());
        }
    }

}