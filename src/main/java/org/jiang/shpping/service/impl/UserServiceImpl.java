package org.jiang.shpping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jiang.shpping.dao.UserDao;
import org.jiang.shpping.dto.LoginFormDTO;
import org.jiang.shpping.dto.UserDTO;
import org.jiang.shpping.entity.User;
import org.jiang.shpping.exception.BizException;
import org.jiang.shpping.request.LoginRequest;
import org.jiang.shpping.service.UserService;
import org.jiang.shpping.utils.RegexUtils;
import org.jiang.shpping.utils.UserHolder;
import org.jiang.shpping.vo.Result;
import org.jiang.shpping.vo.Result1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.jiang.shpping.utils.RedisConstants.*;
import static org.jiang.shpping.utils.SystemConstants.*;


@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserDao, User> implements UserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserDao dao;


    @Override
    public List<User> test() {
         LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        return this.baseMapper.selectList(userLambdaQueryWrapper);
    }

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1.校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 3.符合，生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 4.保存验证码到 session
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 5.发送验证码
        log.debug("发送短信验证码成功，验证码：{}", code);
        // 返回ok
        return Result.ok();
    }

    @Override
    public Result1 login(LoginFormDTO loginForm) {
        String phone =loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2.如果不符合 符合错误信息
            return Result1.fail("手机号格式错误");
        }
        //3.从redis获取验证码并校验
         String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
         String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)) {
            //不一致，报错
            return Result1.fail("验证码错误");
        }
        //4.一致,根据手机号查询用户
         User user = query().eq("phone", phone).one();
        //5.判断用户是否存在
        if (ObjectUtil.isNull(user)) {
            //6.不存在 创建新用户并保存
            user = createUserWithPhone(phone,loginForm.getPassword());
        }

        if(!user.getStatus()) {
            throw new  BizException("此账号被禁用");
        }

//        if (!user.getPwd().equals(loginForm.getPassword())) {
//            throw new BizException("密码错误");
//        }

        //7.保存用户信息到 redis中
        //7.1.随机生成token 作为登录令牌
        String token = UUID.randomUUID().toString(true);
        //7.2.将User 对象转为HashMap存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
         //7.4设置token有效期
        stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL,TimeUnit.MINUTES);
        return Result1.ok(token);
    }

    /**
     * 重置连续签到天数
     *
     * @param userId Integer 用户id
     * @author Mr.Zhang
     * @since 2020-04-28
     */
    @Override
    public void repeatSignNum(Integer userId) {
        User user = new User();
        user.setUid(userId);
        user.setSignNum(0);
        updateById(user);
    }

    @Override
    public Result userpasswordlogin(LoginRequest loginRequest) {
        User user = getByPhone(loginRequest.getPhone());
        if (ObjectUtil.isNull(user)) {
            throw new BizException("此账号未注册");
        }
        if (!user.getStatus()) {
            throw new BizException("此账号被禁用");
        }

        if (!user.getPwd().equals(loginRequest.getPassword())) {
            throw new BizException("密码错误");
        }

        String token = UUID.randomUUID().toString(true);
        //7.2.将User 对象转为HashMap存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);

        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        //7.4设置token有效期
        stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL,TimeUnit.MINUTES);
        return Result.ok(token);
    }

    /**
     * 根据手机号查询用户
     * @param phone 用户手机号
     * @return 用户信息
     */
    public User getByPhone(String phone) {
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getPhone, phone);
        return dao.selectOne(lqw);
    }

    private User createUserWithPhone(String phone, String password) {
        //1.创建用户
        User user =new User();
        user.setPhone(phone);
        user.setNickname(USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));
        user.setPwd(password);
        user.setStatus(false);
        //2.保存用户
        save(user);
        return user;
    }

    /**
     * 获取个人资料
     *
     * @return User
     * @author Mr.Zhang
     * @since 2020-04-28
     */
    @Override
    public User getInfo() {
        Integer uid = null;
        try {
            uid = UserHolder.getUser().getUid();
        }catch (Exception e) {
            throw new  BizException("用户没有登录");
        }
        return getById(uid);
    }

}
