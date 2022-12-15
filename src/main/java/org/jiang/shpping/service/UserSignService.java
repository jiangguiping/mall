package org.jiang.shpping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jiang.shpping.dto.PageDTO;
import org.jiang.shpping.entity.UserAddress;
import org.jiang.shpping.entity.UserSign;
import org.jiang.shpping.response.UserSignInfoResponse;
import org.jiang.shpping.vo.PageResult;
import org.jiang.shpping.vo.SystemGroupDataSignConfigVo;
import org.jiang.shpping.vo.UserSignMonthVo;
import org.jiang.shpping.vo.UserSignVo;

import java.util.HashMap;
import java.util.List;

public interface UserSignService extends IService<UserSign> {
    PageResult<UserSignVo> getList(PageDTO pageDTO);

    PageResult<UserSignMonthVo> getListGroupMonth(PageDTO pageDTO);

    /**
     * 签到
     * @return SystemGroupDataSignConfigVo
     */
    SystemGroupDataSignConfigVo sign();

    /**
     * 签到配置
     * @return List<SystemGroupDataSignConfigVo>
     */
    List<SystemGroupDataSignConfigVo> getSignConfig();

    /**
     * 今日记录详情
     * @return HashMap
     */
    HashMap<String, Object> get();

    /**
     * 获取用户签到信息
     * @return UserSignInfoResponse
     */
    UserSignInfoResponse getUserSignInfo();
}
