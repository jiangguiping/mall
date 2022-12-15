package org.jiang.shpping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jiang.shpping.dto.PageDTO;
import org.jiang.shpping.request.OrderRefundApplyRequest;
import org.jiang.shpping.entity.StoreOrder;
import org.jiang.shpping.request.*;
import org.jiang.shpping.response.*;
import org.jiang.shpping.vo.PageResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface StoreOrderService extends IService<StoreOrder> {
    /**
     * 订单预下单
     * @param request 预下单请求参数
     * @return PreOrderResponse
     */
    Map<String, Object> preOrder(PreOrderRequest request);

    /**
     * 加载预下单信息
     * @param preOrderNo 预下单号
     * @return 预下单信息
     */
    PreOrderResponse loadPreOrder(String preOrderNo);

    /**
     * 计算订单价格
     * @param request 计算订单价格请求对象
     * @return ComputedOrderPriceResponse
     */
    ComputedOrderPriceResponse computedOrderPrice(OrderComputedPriceRequest request);


    /**
     * 创建订单
     * @param orderRequest 创建订单请求参数
     * @return MyRecord 订单编号
     */
    HashMap<String, String> createOrder(CreateOrderRequest orderRequest);

    PageResult<OrderDetailResponse> list(Integer type, PageDTO pageDTO);

    /**
     * 订单详情
     * @param orderId 订单id
     */
    StoreOrderDetailInfoResponse detailOrder(String orderId);

    /**
     * 订单状态数量
     * @return 订单状态数据量
     */
    OrderDataResponse orderData();

    /**
     * 订单删除
     * @param id 订单id
     * @return Boolean
     */
    Boolean delete(Integer id);

    /**
     * 创建订单商品评价
     * @param request 请求参数
     * @return Boolean
     */
    Boolean reply(StoreProductReplyAddRequest request);

    StoreOrder getByOderId(String orderId);

    /**
     * 订单收货
     * @param id 订单id
     * @return Boolean
     */
    Boolean take(Integer id);

    /**
     * 订单取消
     * @param id 订单id
     * @return Boolean
     */
    Boolean cancel(Integer id);

    StoreOrder getInfoById(Integer id);

    /**
     * 获取申请订单退款信息
     * @param orderId 订单编号
     * @return ApplyRefundOrderInfoResponse
     */
    ApplyRefundOrderInfoResponse applyRefundOrderInfo(String orderId);

    /**
     * 订单退款申请
     * @param request 申请参数
     * @return Boolean
     */
    Boolean refundApply(OrderRefundApplyRequest request);

    /**
     * 查询退款理由
     * @return 退款理由集合
     */
    List<String> getRefundReason();

    /**
     * 订单物流查看
     */
    Object expressOrder(String orderId);

    StoreOrder getByEntityOne(StoreOrder storeOrder);

    /**
     * 获取待评价商品信息
     * @param getProductReply 订单详情参数
     * @return 待评价
     */
    OrderProductReplyResponse getReplyProduct(GetProductReply getProductReply);
}
