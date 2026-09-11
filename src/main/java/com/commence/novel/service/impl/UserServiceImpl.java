package com.commence.novel.service.impl;

import com.commence.novel.DTO.UserDTO;
import com.commence.novel.entity.LoginLog;
import com.commence.novel.entity.User;
import com.commence.novel.entity.VerificationCode;
import com.commence.novel.exception.*;
import com.commence.novel.repository.LoginLogRepository;
import com.commence.novel.repository.UserRepository;
import com.commence.novel.repository.VerificationCodeRepository;
import com.commence.novel.service.UserService;
import com.commence.novel.utils.Result; // 移除 PermissionUtils 导入（不再依赖Session）
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @Autowired
    private JavaMailSender mailSender;

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private LoginLogRepository loginLogRepository;

    // 密码加密器
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public User loginService(String uname, String password,String ip) {
        LoginLog log = new LoginLog();
        log.setUname(uname);
        log.setIp(ip);
        log.setLoginTime(LocalDateTime.now());

        User user = userRepository.findByUname(uname);
        if (user == null) {
            log.setStatus("FAIL");
            log.setMessage("用户名不存在");
            loginLogRepository.save(log);
            return null;
        }
        // 验证密码（加密）
        if(!passwordEncoder.matches(password, user.getPassword())) {
            log.setUserId(user.getUid());
            log.setStatus("FAIL");
            log.setMessage("密码错误");
            loginLogRepository.save(log);
            return null;
        }

        log.setUserId(user.getUid());
        log.setStatus("SUCCESS");
        log.setMessage("登录成功");
        loginLogRepository.save(log);

        return user;
    }

    @Override
    @Transactional
    public Result sendRegisterCode(String email){
        if(userRepository.findByEmail(email) !=null){
            return Result.error("1011","该邮箱已注册");
        }

        // 生成6位数字验证码
        String code = RandomStringUtils.randomNumeric(6);

        try {
            verificationCodeRepository.deleteByEmail(email);
        } catch (Exception e) {
            // 忽略删除失败的异常（如记录已被其他线程删除）
            log.warn("删除旧验证码失败（邮箱：{}），可能已被其他线程删除: {}", email, e.getMessage());
        }

        // 保存新验证码
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setCreateTime(LocalDateTime.now());
        verificationCodeRepository.save(verificationCode);

        // 发送邮件
        try{
            sendEmail(
                    email,
                    "注册验证码",
                    "你的注册验证码是："+code+" ,3分钟内有效"
            );
            log.info("邮件已成功发送至：{}",email);
            return Result.success(null,"验证码已发送到邮箱");
        }catch (MailException e){
            log.error("邮件发送失败（收件人：{}):{}",email,e.getMessage());
            return Result.error(null,"验证码发送失败，请稍后重试");
        }
    }

    @Override
    @Transactional
    public User registerService(User user, String roleType, String code) {
        // ========== 邮箱非空 + 格式校验 ==========
        String email = user.getEmail();
        // 1. 邮箱非空校验
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        String cleanEmail = email.trim(); // 统一处理去空格后的邮箱
        user.setEmail(cleanEmail); // 把处理后的邮箱回写到user对象，保证后续使用的是干净值

        // 2. 邮箱格式校验（通用正则）
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (!cleanEmail.matches(emailRegex)) {
            throw new IllegalArgumentException("邮箱格式不正确，请输入有效的邮箱地址");
        }

        // 验证验证码
        Optional<VerificationCode> codeOpt = verificationCodeRepository.findTopByEmailOrderByCreateTimeDesc(cleanEmail);
        if (!codeOpt.isPresent()) {
            throw new CaptchaNotFoundException("未找到该邮箱的验证码,请重新获取");
        }
        VerificationCode dbCode = codeOpt.get();

        // 检查是否过期
        LocalDateTime expireTime = dbCode.getCreateTime().plusMinutes(dbCode.getExpireMinutes());
        if (LocalDateTime.now().isAfter(expireTime)) {
            throw new CaptchaExpiredException("验证码已过期,请重新获取");
        }

        if (!dbCode.getCode().equals(code)) {
            throw new CaptchaMismatchException("验证码错误");
        }

        // 验证用户名是否已存在
        if (userRepository.findByUname(user.getUname()) != null) {
            throw new UsernameExistsException("用户名已存在");
        }
        // 验证邮箱是否已注册
        if (userRepository.findByEmail(cleanEmail) != null) {
            throw new EmailExistsException("邮箱已被注册");
        }

        // 选择角色
        if ("AUTHOR".equals(roleType)) {
            user.setRole(roleType);
            String penName = user.getPenName();

            if (StringUtils.isBlank(penName)) {
                throw new RuntimeException("笔名不能为空");
            }
            if (!penName.matches("^[\\u4e00-\\u9fa5a-zA-Z0-9_]{2,12}$")) {
                throw new RuntimeException("笔名格式不正确，支持2-12个汉字、字母、数字和下划线");
            }
            if (userRepository.existsByPenName(penName)) {
                throw new RuntimeException("该笔名已被使用，请更换");
            }
        } else {
            user.setRole("READER");
            user.setPenName(null);
        }

        // 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User newUser = userRepository.save(user);

        // 注册成功后删除验证码
        verificationCodeRepository.deleteByEmail(cleanEmail);

        return newUser;
    }

    @Override
    @Transactional
    public Result sendResetCode(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return Result.error("1001","该邮箱未注册");
        }

        // 生成6位数字验证码
        String code = RandomStringUtils.randomNumeric(6);

        verificationCodeRepository.deleteByEmail(email);

        // 保存新验证码
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(code);
        verificationCode.setCreateTime(LocalDateTime.now());
        verificationCode.setExpireMinutes(3);
        verificationCodeRepository.save(verificationCode);

        // 发送邮件
        try{
            sendEmail(
                    email,
                    "重置密码验证码",
                    "你的验证码是："+code+" ,3分钟内有效"
            );
            log.info("邮件已成功发送至：{}",email);
            return Result.success(null,"验证码已发送到邮箱");
        }catch (MailException e){
            log.error("邮件发送失败（收件人：{}):{}",email,e.getMessage());
            return Result.error(null,"验证码发送失败，请稍后重试");
        }
    }

    @Transactional
    @Override
    public Result resetPassword(String email,String code,String newPassword) {
        Optional<VerificationCode> codeOpt = verificationCodeRepository.findTopByEmailOrderByCreateTimeDesc(email);
        if (!codeOpt.isPresent()) {
            return Result.error("1002","未找到验证码，请重新获取");
        }
        VerificationCode dbCode = codeOpt.get();

        LocalDateTime expireTime = dbCode.getCreateTime().plusMinutes(dbCode.getExpireMinutes());
        if (LocalDateTime.now().isAfter(expireTime)) {
            return Result.error("1003","验证码已过期，请重新获取");
        }

        if (!dbCode.getCode().equals(code)) {
            return Result.error("1004","验证码错误");
        }

        if (newPassword == null || newPassword.length() < 6) {
            return Result.error("1005","新密码长度至少6位");
        }

        User user = userRepository.findByEmail(email);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        verificationCodeRepository.deleteByEmail(email);

        return Result.success(null,"密码重置成功,请重新登录");
    }

    private  void sendEmail(String to, String subject, String content) throws MailException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("2468987860@qq.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }

    @Override
    public Result getCurrentUser(Long uid){
        // 根据 uid 查询用户
        User user = userRepository.findById(uid).orElse(null);
        if (user == null) {
            return Result.error("10086","未登录(游客状态)");
        }
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user,userDTO,"password");
        return Result.success(userDTO,"获取用户信息成功");
    }

    @Override
    public Result logout() {
        // JWT 是无状态认证，无需销毁会话，直接返回成功（前端清除 Token 即可）
        return Result.success(null,"退出成功");
    }

    // 检查笔名是否存在（无修改）
    @Override
    public Result checkPenName(String penName) {
        if (StringUtils.isBlank(penName)) {
            return Result.error("1012","笔名不能为空");
        }
        if (!penName.matches("^[\\u4e00-\\u9fa5a-zA-Z0-9_]{2,12}$")) {
            return Result.error("1015","笔名格式不正确,支持2-12个汉字、字母、数字和下划线");
        }
        if (userRepository.existsByPenName(penName)){
            return Result.error("1013","该笔名已被使用");
        }
        return Result.success(null,"笔名可用");
    }

    @Override
    @Transactional
    public Result applyAuthor(String penName, Long uid){
        User currentUser = userRepository.findById(uid)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!"READER".equals(currentUser.getRole())){
            if ("AUTHOR".equals(currentUser.getRole())){
                return Result.error("1014","您已成为作者，无需重复申请");
            }
            return Result.error("1007","仅读者可申请成为作者");
        }

        Result penNameCheckResult = checkPenName(penName);
        if (!"0".equals(penNameCheckResult.getCode())) {
            return penNameCheckResult;
        }

        // 更新用户状态
        currentUser.setPenName(penName);
        currentUser.setRole("AUTHOR");
        userRepository.save(currentUser);

        return Result.success(null, "恭喜您成为作者");
    }

    @Override
    public Result getAllUsers() {
        // 查询所有用户（排除密码字段，返回安全的用户信息）
        List<User> userList = userRepository.findAll();
        if (userList.isEmpty()) {
            return Result.success(null, "暂无用户数据");
        }

        // 转换为 UserDTO（排除密码）
        List<UserDTO> userDTOList = new ArrayList<>();
        for (User user : userList) {
            UserDTO dto = new UserDTO();
            BeanUtils.copyProperties(user, dto, "password"); // 忽略密码字段
            userDTOList.add(dto);
        }

        return Result.success(userDTOList, "查询所有用户成功");
    }

    @Override
    public void updateAvatarUrl(Long userId, String avatarUrl) {
        // 1. 用 JPA 的 findById 查找用户
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 设置新头像URL
        user.setAvatarUrl(avatarUrl);

        // 3. 用 save() 方法更新（JPA 自动识别为更新操作）
        userRepository.save(user);
    }
}