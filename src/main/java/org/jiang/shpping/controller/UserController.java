package org.jiang.shpping.controller;

import io.swagger.annotations.ApiOperation;
import org.jiang.shpping.dto.LoginFormDTO;
import org.jiang.shpping.dto.UserDTO;
import org.jiang.shpping.request.LoginRequest;
import org.jiang.shpping.service.UserService;
import org.jiang.shpping.utils.UserHolder;
import org.jiang.shpping.vo.Result;
import org.jiang.shpping.vo.Result1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("api/front/user")
public class UserController {

    @Autowired
    private UserService userService;


    /**
     * 发送手机验证码
     */
    @ApiOperation("发送手机验证码")
    @PostMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        // 发送短信验证码并保存验证码
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     * @return
     */

    @ApiOperation("账号密码登录")
    @PostMapping("/login")
    public Result1 login(@RequestBody LoginFormDTO loginForm) {
        return userService.login(loginForm);
    }

    @GetMapping("/me")
    public Result me(){
        // 获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    /**
     * 账号密码登录
     */
    @ApiOperation(value = "账号密码登录")
    @RequestMapping(value = "/userlogin", method = RequestMethod.POST)
    public Result login(@RequestBody @Validated LoginRequest loginRequest) {
        return Result.ok(userService.userpasswordlogin(loginRequest));
    }


}
