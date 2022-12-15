package org.jiang.shpping.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jiang.shpping.dto.PageDTO;
import org.jiang.shpping.response.UserRelationResponse;
import org.jiang.shpping.request.UserCollectAllRequest;
import org.jiang.shpping.request.UserCollectRequest;
import org.jiang.shpping.service.StoreProductRelationService;
import org.jiang.shpping.vo.PageResult;
import org.jiang.shpping.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("api/front/collect")
@Api(tags = "用户 -- 点赞/收藏")
public class UserCollectController {

    @Autowired
    private StoreProductRelationService storeProductRelationService;

    /**
     * 我的收藏列表
     */
    @ApiOperation(value = "我的收藏列表")
    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public Result<PageResult<UserRelationResponse>> getList(@Validated PageDTO pageDTO) {
        return Result.ok(storeProductRelationService.getUserList(pageDTO));
    }


    /**
     * 添加收藏产品
     * @param request StoreProductRelationRequest 新增参数
     */
    @ApiOperation(value = "添加收藏产品")
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public Result<String> save(@RequestBody @Validated UserCollectRequest request) {
        if (storeProductRelationService.add(request)) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    /**
     * 添加收藏产品
     * @param request UserCollectAllRequest 新增参数
     */
    @ApiOperation(value = "批量收藏")
    @RequestMapping(value = "/all", method = RequestMethod.POST)
    public Result<String> all(@RequestBody @Validated UserCollectAllRequest request) {
        if (storeProductRelationService.all(request)) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    /**
     * 取消收藏产品
     */
    @ApiOperation(value = "取消收藏产品")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public Result<String> delete(@RequestBody String requestJson) {
        if (storeProductRelationService.delete(requestJson)) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    /**
     * 取消收藏产品(通过商品)
     */
    @ApiOperation(value = "取消收藏产品(通过商品)")
    @RequestMapping(value = "/cancel/{proId}", method = RequestMethod.POST)
    public Result<String> cancel(@PathVariable Integer proId) {
        if (storeProductRelationService.deleteByProIdAndUid(proId)) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }



}
