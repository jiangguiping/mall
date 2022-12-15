package org.jiang.shpping.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jiang.shpping.service.SystemCityService;
import org.jiang.shpping.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController("CityFrontController")
@RequestMapping("api/front/city")
@Api(tags = "城市服务")
public class CityController {
    @Autowired
    private SystemCityService systemCityService;

    /**
     * 城市服务树形结构数据
     */
    @ApiOperation(value = "树形结构")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public Result<Object> register(){
        return Result.ok(systemCityService.getListTree());
    }
}
