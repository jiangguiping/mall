package org.jiang.shpping.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jiang.shpping.dto.PageDTO;
import org.jiang.shpping.request.CartNumRequest;
import org.jiang.shpping.request.CartRequest;
import org.jiang.shpping.response.CartInfoResponse;
import org.jiang.shpping.service.StoreCartService;
import org.jiang.shpping.vo.PageResult;
import org.jiang.shpping.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("api/front/cart")
@Api(tags = "商品 -- 购物车") //配合swagger使用
public class CartController {

    @Autowired
    private StoreCartService storeCartService;

    /**
     * 分页显示购物车表
     */
    @ApiOperation(value = "分页列表") //配合swagger使用
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ApiImplicitParams({
//            @ApiImplicitParam(name="isValid", value="类型，true-有效商品，false-无效商品", required = true),
            @ApiImplicitParam(name="page", value="页码", required = true),
            @ApiImplicitParam(name="limit", value="每页数量", required = true)
    })
    public Result<PageResult<CartInfoResponse>> getList(@Validated PageDTO pageDTO) {
        return Result.ok(storeCartService.getPageList(pageDTO));
    }

    /**
     * 新增购物车表
     * @param storeCartRequest 新增参数
     */
    @ApiOperation(value = "新增购物车表")
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public Result<HashMap<String,String>> save(@RequestBody @Validated CartRequest storeCartRequest) {
        String cartId = storeCartService.saveCate(storeCartRequest);
        if (StringUtils.isNotBlank(cartId)) {
            HashMap<String,String> result = new HashMap<>();
            result.put("cartId", cartId);
            return Result.ok(result);
        } else {
            return Result.fail();
        }
    }

    /**
     * 删除购物车表
     * @param ids 购物车ids
     */
    @ApiOperation(value = "删除购物车表")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public Result<String> delete(@RequestParam(value = "ids") List<Long> ids) {
        if (storeCartService.deleteCartByIds(ids)) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    /**
     * 修改商品数量
     * @param id integer id
     * @param number 修改的产品数量
     */
    @ApiOperation(value = "修改商品数量")
    @RequestMapping(value = "/num", method = RequestMethod.POST)
    public Result<String> update(@RequestParam Integer id, @RequestParam Integer number) {
        if (storeCartService.updateCartNum(id, number)) {
            return Result.ok();
        } else {
            return Result.fail();
        }
    }

    /**
     * 获取购物车数量
     */
    @ApiOperation(value = "获取购物车数量")
    @RequestMapping(value = "/count", method = RequestMethod.GET)
    public Result<Map<String, Integer>> count(@Validated CartNumRequest request) {
        return Result.ok(storeCartService.getUserCount(request));
    }

}
