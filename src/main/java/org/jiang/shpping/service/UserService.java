package org.jiang.shpping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jiang.shpping.dto.LoginFormDTO;
import org.jiang.shpping.entity.User;
import org.jiang.shpping.request.LoginRequest;
import org.jiang.shpping.vo.Result;
import org.jiang.shpping.vo.Result1;

import javax.servlet.http.HttpSession;
import java.util.List;

public interface UserService extends IService<User> {

    User getInfo();

    List<User> test();



    Result sendCode(String phone, HttpSession session);

    Result1 login(LoginFormDTO loginForm);

    void repeatSignNum(Integer userId);

    Result userpasswordlogin(LoginRequest loginRequest);
}
