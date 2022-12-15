package org.jiang.shpping.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jiang.shpping.dto.PageDTO;
import org.jiang.shpping.entity.StoreProduct;
import org.jiang.shpping.entity.UserAddress;
import org.jiang.shpping.request.ProductRequest;
import org.jiang.shpping.response.IndexProductResponse;
import org.jiang.shpping.response.ProductDetailReplyResponse;
import org.jiang.shpping.response.ProductDetailResponse;
import org.jiang.shpping.response.StoreProductReplayCountResponse;
import org.jiang.shpping.service.ProductService;
import org.jiang.shpping.vo.CategoryTreeVo;
import org.jiang.shpping.vo.PageResult;
import org.jiang.shpping.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController("ProductController")
@RequestMapping("api/front")
@Api(tags = "商品")
public class ProductController {

    @Autowired
    private ProductService productService;




    /**
     * 获取分类
     */
    @ApiOperation(value = "获取分类")
    @RequestMapping(value = "/category", method = RequestMethod.GET)
    public Result<List<CategoryTreeVo>> getCategory() {
        return Result.ok(productService.getCategory());
    }



    @ApiOperation(value = "商品列表")
    @RequestMapping(value = "/test",method = RequestMethod.GET)
    public Result<PageResult<StoreProduct>> getList(@Validated ProductRequest request, @Validated PageDTO pageDTO) {
        return Result.ok(productService.findH5List(request,pageDTO));
    }


    //商品评论数量
    @ApiOperation(value = "商品评论数量")
    @RequestMapping(value = "/reply/config/{id}",method = RequestMethod.GET)
    public Result<StoreProductReplayCountResponse> getReplyCount(@PathVariable Integer id) {
        return Result.ok(productService.getReplyCount(id));
    }


    @ApiOperation(value = "商品详情评论")
    @RequestMapping(value = "/reply/product/{id}", method = RequestMethod.GET)
    public Result<ProductDetailReplyResponse> getProductReply(@PathVariable Integer id) {
        return Result.ok(productService.getProductReply(id));
    }

    /**
     * 商品详情
     */
    @ApiOperation(value = "商品详情")
    @RequestMapping(value = "/product/detail/{id}", method = RequestMethod.GET)
    @ApiImplicitParam(name = "type", value = "normal-正常，video-视频")
    public Result<ProductDetailResponse> getDetail(@PathVariable Integer id, @RequestParam(value = "type", defaultValue = "normal") String type) {
        return Result.ok(productService.getDetail(id, type));
    }

    /**
     * 商品规格详情
     */
    @ApiOperation(value = "商品规格详情")
    @RequestMapping(value = "/product/sku/detail/{id}", method = RequestMethod.GET)
    public Result<ProductDetailResponse> getSkuDetail(@PathVariable Integer id) {
        return Result.ok(productService.getSkuDetail(id));
    }






}
