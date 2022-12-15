package org.jiang.shpping.utils;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:"; //验证码key
    public static final Long LOGIN_CODE_TTL = 2L; //验证码失效时间

    public static final String LOGIN_USER_KEY = "login:token:"; //用户登录 token
    public static final Long LOGIN_USER_TTL = 36000L; //token 失效时间
}
