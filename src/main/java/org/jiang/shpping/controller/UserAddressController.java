package org.jiang.shpping.controller;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jiang.shpping.dto.PageDTO;
import org.jiang.shpping.entity.UserAddress;
import org.jiang.shpping.request.UserAddressDelRequest;
import org.jiang.shpping.request.UserAddressRequest;
import org.jiang.shpping.service.UserAddressService;
import org.jiang.shpping.vo.PageResult;
import org.jiang.shpping.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("api/front/address")
@Api(tags = "用户 -- 地址")
public class UserAddressController {

    @Autowired
    private UserAddressService userAddressService;



    @ApiOperation(value = "分页显示用户地址")
    @RequestMapping(value = "/list",method = RequestMethod.GET)
    public Result<PageResult<UserAddress>> getList(PageDTO paheDto) {

        return Result.ok(userAddressService.getList(paheDto));
    }

    @ApiOperation(value = "新增用户地址")
    @RequestMapping(value = "/edit",method = RequestMethod.POST)
    private Result<UserAddress> save(@RequestBody @Validated UserAddressRequest request) {
        return Result.ok(userAddressService.create(request));

    }

    @ApiOperation(value = "删除用户地址")
    @RequestMapping(value = "/del",method = RequestMethod.POST)
    public Result<String> delete(@RequestBody UserAddressDelRequest request) {
        if (userAddressService.delete(request.getId())) {
            return Result.ok();
        }else {
            return Result.fail();
        }
    }

    /**
     * 地址详情
     */
    @ApiOperation(value = "地址详情")
    @RequestMapping(value = "/detail/{id}", method = RequestMethod.GET)
    public Result<UserAddress> info(@PathVariable("id") Integer id) {
        return Result.ok(userAddressService.getDetail(id));
    }

    /**
     * 获取默认地址
     */
    @ApiOperation(value = "获取默认地址")
    @RequestMapping(value = "/default", method = RequestMethod.GET)
    public Result<UserAddress> getDefault() {
        return Result.ok(userAddressService.getDefault());

    }

    /**
     * 设置默认地址
     * @param request UserAddressDelRequest 参数
     */
    @ApiOperation(value = "设置默认地址")
    @RequestMapping(value = "/default/set", method = RequestMethod.POST)
    public Result<UserAddress> def(@RequestBody UserAddressDelRequest request) {
        if (userAddressService.def(request.getId())) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }



}
