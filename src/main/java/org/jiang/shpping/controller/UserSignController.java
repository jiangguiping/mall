package org.jiang.shpping.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jiang.shpping.dto.PageDTO;
import org.jiang.shpping.response.UserSignInfoResponse;
import org.jiang.shpping.service.UserSignService;
import org.jiang.shpping.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("api/front/user/sign")
@Api(tags = "用户 -- 签到")
public class UserSignController {

    @Autowired
    private UserSignService userSignService;


    /**
     * 签到列表
     * @param pageDTO 分页参数
     */
    @ApiOperation(value = "签到列表")
    @RequestMapping(value = "/listtwo", method = RequestMethod.GET)
    public Result<PageResult<UserSignVo>> getList(@Validated PageDTO pageDTO) {
        return Result.ok(userSignService.getList(pageDTO));
    }

    /**
     * 配置
     */
    @ApiOperation(value = "配置")
    @RequestMapping(value = "/config", method = RequestMethod.GET)
    public Result<List<SystemGroupDataSignConfigVo>> config() {
        return Result.ok(userSignService.getSignConfig());
    }

    /**
     * 签到列表
     * @param pageDTO 分页参数
     */
    @ApiOperation(value = "签到列表，年月纬度")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public Result<PageResult<UserSignMonthVo>> getListGroupMonth(@Validated PageDTO pageDTO) {
        return Result.ok(userSignService.getListGroupMonth(pageDTO));
    }

    /**
     * 签到
     */
    @ApiOperation(value = "签到")
    @RequestMapping(value = "/integral", method = RequestMethod.GET)
    public Result<SystemGroupDataSignConfigVo> info() {
        return Result.ok(userSignService.sign());
    }


    /**
     * 今日记录详情
     */
    @ApiOperation(value = "今日记录详情")
    @RequestMapping(value = "/get", method = RequestMethod.GET)
    public Result<HashMap<String, Object>> get() {
        return Result.ok(userSignService.get());
    }

    /**
     * 签到用户信息
     */
    @ApiOperation(value = "签到用户信息")
    @RequestMapping(value = "/user", method = RequestMethod.POST)
    public Result<UserSignInfoResponse> getUserInfo() {
        return Result.ok(userSignService.getUserSignInfo());
    }



}
