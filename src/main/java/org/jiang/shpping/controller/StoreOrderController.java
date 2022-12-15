package org.jiang.shpping.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.jiang.shpping.dto.PageDTO;
import org.jiang.shpping.request.OrderRefundApplyRequest;
import org.jiang.shpping.request.*;
import org.jiang.shpping.response.*;
import org.jiang.shpping.service.StoreOrderService;
import org.jiang.shpping.vo.PageResult;
import org.jiang.shpping.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController("StoreOrderFrontController")
@RequestMapping("api/front/order")
@Api(tags = "订单")
public class StoreOrderController {

    @Autowired
    private StoreOrderService orderService;


    /**
     * 预下单
     */
    @ApiOperation(value = "预下单")
    @RequestMapping(value = "/pre/order", method = RequestMethod.POST)
    public Result<Map<String, Object>> preOrder(@RequestBody @Validated PreOrderRequest request) {
        return Result.ok(orderService.preOrder(request));
    }

    /**
     * 加载预下单
     */
    @ApiOperation(value = "加载预下单")
    @RequestMapping(value = "load/pre/{preOrderNo}", method = RequestMethod.GET)
    public Result<PreOrderResponse> loadPreOrder(@PathVariable String preOrderNo) {
        return Result.ok(orderService.loadPreOrder(preOrderNo));
    }

    /**
     * 根据参数计算订单价格
     */
    @ApiOperation(value = "计算订单价格")
    @RequestMapping(value = "/computed/price", method = RequestMethod.POST)
    public Result<ComputedOrderPriceResponse> computedPrice(@Validated @RequestBody OrderComputedPriceRequest request) {
        return Result.ok(orderService.computedOrderPrice(request));
    }

    /**
     * 创建订单
     */
    @ApiOperation(value = "创建订单")
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public Result<Map<String, String>> createOrder(@Validated @RequestBody CreateOrderRequest orderRequest) {
        return Result.ok(orderService.createOrder(orderRequest));
    }

    /**
     * 订单列表
     * @param type 类型
     * @param pageDTO 分页
     * @return 订单列表
     */
    @ApiOperation(value = "订单列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "评价等级|0=未支付,1=待发货,2=待收货,3=待评价,4=已完成,-3=售后/退款", required = true)
    })
    public Result<PageResult<OrderDetailResponse>> orderList(@RequestParam(name = "type") Integer type,
                                                             @ModelAttribute PageDTO pageDTO) {
        return Result.ok(orderService.list(type, pageDTO));
    }

    /**
     * 订单详情
     * @param orderId 订单编号
     * @return 订单详情
     */
    @ApiOperation(value = "订单详情")
    @RequestMapping(value = "/detail/{orderId}", method = RequestMethod.GET)
    public Result<StoreOrderDetailInfoResponse> orderDetail(@PathVariable String orderId) {
        return Result.ok(orderService.detailOrder(orderId));
    }

    /**
     * 订单头部信息
     * @return 查询集合数量
     */
    @ApiOperation(value = "订单头部数量")
    @RequestMapping(value = "/data", method = RequestMethod.GET)
    public Result<OrderDataResponse> orderData() {
        return Result.ok(orderService.orderData());
    }

    /**
     * 删除已完成订单
     * @param id String 订单号
     * @return 删除结果
     */
    @ApiOperation(value = "删除订单")
    @RequestMapping(value = "/del", method = RequestMethod.POST)
    public Result<Boolean> delete(@RequestParam Integer id) {
        if( orderService.delete(id)) {
            return Result.ok();
        }else{
            return Result.fail();
        }
    }

    /**
     * 订单评价
     * @param request StoreProductReplyAddRequest 评论参数
     */
    @ApiOperation(value = "添加评价订单")
    @RequestMapping(value = "/comment", method = RequestMethod.POST)
    public Result<Boolean> comment(@RequestBody @Validated StoreProductReplyAddRequest request) {
        if(orderService.reply(request)) {
            return Result.ok();
        }else{
            return Result.fail();
        }
    }


    /**
     * 订单收货
     * @param id Integer 订单id
     */
    @ApiOperation(value = "订单收货")
    @RequestMapping(value = "/take", method = RequestMethod.POST)
    public Result<Boolean> take(@RequestParam(value = "id") Integer id) {
        if(orderService.take(id)) {
            return Result.ok();
        }else{
            return Result.fail();
        }
    }

    /**
     * 订单取消
     * @param id Integer 订单id
     */
    @ApiOperation(value = "订单取消")
    @RequestMapping(value = "/cancel", method = RequestMethod.POST)
    public Result<Boolean> cancel(@RequestParam(value = "id") Integer id) {
        if(orderService.cancel(id)) {
            return Result.ok();
        }else{
            return Result.fail();
        }
    }


    /**
     * 获取申请订单退款信息
     * @param orderId 订单编号
     */
    @ApiOperation(value = "获取申请订单退款信息")
    @RequestMapping(value = "/apply/refund/{orderId}", method = RequestMethod.GET)
    public Result<ApplyRefundOrderInfoResponse> refundApplyOrder(@PathVariable String orderId) {
        return Result.ok(orderService.applyRefundOrderInfo(orderId));
    }

    /**
     * 订单退款申请
     * @param request OrderRefundApplyRequest 订单id
     */
    @ApiOperation(value = "订单退款申请")
    @RequestMapping(value = "/refund", method = RequestMethod.POST)
    public Result<Boolean> refundApply(@RequestBody @Validated OrderRefundApplyRequest request) {
        if(orderService.refundApply(request)) {
            return Result.ok();
        }else{
            return Result.fail();
        }
    }


    /**
     * 查询订单退款理由
     * @return 退款理由
     */
    @ApiOperation(value = "订单退款理由（商家提供）")
    @RequestMapping(value = "/refund/reason", method = RequestMethod.GET)
    public Result<List<String>> refundReason() {
        return Result.ok(orderService.getRefundReason());
    }



    /**
     * 根据订单号查询物流信息
     * @param orderId 订单号
     * @return 物流信息
     */
    @ApiOperation(value = "物流信息查询")
    @RequestMapping(value = "/express/{orderId}", method = RequestMethod.GET)
    public Result<Object> getExpressInfo(@PathVariable String orderId) {
        return Result.ok(orderService.expressOrder(orderId));
    }


    @ApiOperation(value = "待评价商品信息查询")
    @RequestMapping(value = "/product", method = RequestMethod.POST)
    public Result<OrderProductReplyResponse> getOrderProductForReply(@Validated @RequestBody GetProductReply request) {
        return Result.ok(orderService.getReplyProduct(request));
    }

}
