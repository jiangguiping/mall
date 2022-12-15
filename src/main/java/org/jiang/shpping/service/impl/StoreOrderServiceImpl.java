package org.jiang.shpping.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jiang.shpping.constants.PayConstants;
import org.jiang.shpping.constants.SysConfigConstants;
import org.jiang.shpping.dao.StoreOrderDao;
import org.jiang.shpping.dto.PageDTO;
import org.jiang.shpping.dto.UserDTO;
import org.jiang.shpping.entity.*;
import org.jiang.shpping.exception.BizException;
import org.jiang.shpping.request.*;
import org.jiang.shpping.response.*;
import org.jiang.shpping.service.*;
import org.jiang.shpping.utils.BeanCopyUtils;
import org.jiang.shpping.utils.Constants;
import org.jiang.shpping.utils.JanUtils;
import org.jiang.shpping.utils.UserHolder;
import org.jiang.shpping.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class StoreOrderServiceImpl extends ServiceImpl<StoreOrderDao, StoreOrder> implements StoreOrderService {

    @Resource
    private StoreOrderDao dao;




    @Autowired
    private StoreCartService storeCartService;

    @Autowired
    private ProductService productService;

    @Autowired
    private StoreProductAttrValueService attrValueService;
    @Autowired
    private StoreOrderInfoService storeOrderInfoService;

    @Autowired
    private SystemUserLevelService systemUserLevelService;

    @Autowired
    private StoreProductAttrValueService storeProductAttrValueService;

    @Autowired
    private SystemStoreService systemStoreService;

    @Autowired
    private UserAddressService userAddressService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private StoreCouponUserService storeCouponUserService;

    @Autowired
    private StoreOrderStatusService storeOrderStatusService;

    @Autowired
    private StoreProductReplyService storeProductReplyService;






    /**
     * 订单预下单
     *
     * @param request 预下单请求参数
     * @return PreOrderResponse
     */
    @Override
    public Map<String,Object> preOrder(PreOrderRequest request) {
        if (CollUtil.isEmpty(request.getOrderDetails())) {
            throw new BizException("预下单订单详情列表不能为空");
        }
         UserDTO user = UserHolder.getUser();
        //校验预下单商品信息
        OrderInfoVo orderInfoVo = validatePreOrderRequest(request, user);
        //商品总计金额
        BigDecimal totalPrice;
        // 普通商品
        totalPrice = orderInfoVo.getOrderDetailList().stream().map(e -> e.getVipPrice().multiply(new BigDecimal(e.getPayNum()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        orderInfoVo.setProTotalFee(totalPrice);
        int orderProNum = orderInfoVo.getOrderDetailList().stream().mapToInt(OrderInfoDetailVo::getPayNum).sum();
        orderInfoVo.setOrderProNum(orderProNum);

        //获取默认地址
        UserAddress userAddress = userAddressService.getDefaultByUid(user.getUid());
        orderInfoVo.setFreightFee(BigDecimal.ZERO);

        orderInfoVo.setPayFee(orderInfoVo.getProTotalFee().add(orderInfoVo.getFreightFee()));
        orderInfoVo.setUserIntegral(user.getIntegral());
        orderInfoVo.setUserBalance(user.getNowMoney());
        String key = user.getUid() + JanUtils.getNowTime().toString()+JanUtils.getUuid();
        stringRedisTemplate.opsForValue().set("user_order:" + key, JSONObject.toJSONString(orderInfoVo),Constants.ORDER_CASH_CONFIRM, TimeUnit.MINUTES);
        Map<String, Object> map = new HashMap<>();
        map.put("preOrderNo", key);
        return map;
    }

    /**
     * 加载预下单信息
     *
     * @param preOrderNo 预下单号
     * @return 预下单信息
     */
    @Override
    public PreOrderResponse loadPreOrder(String preOrderNo) {
        String key = "user_order:" + preOrderNo;
        Long expire = stringRedisTemplate.opsForValue().getOperations().getExpire(key);
        if (expire == -2) {
             throw new BizException("预下单订单不存在");
        }
        String orderVoString = stringRedisTemplate.opsForValue().get(key);
        OrderInfoVo orderInfoVo = JSONObject.parseObject(orderVoString, OrderInfoVo.class);
        PreOrderResponse preOrderResponse = new PreOrderResponse();

        preOrderResponse.setOrderInfoVo(orderInfoVo);
        preOrderResponse.setYuePayStatus(String.valueOf(1));
        return preOrderResponse;
    }

    /**
     * 计算订单价格
     *
     * @param request 计算订单价格请求对象
     * @return ComputedOrderPriceResponse
     */
    @Override
    public ComputedOrderPriceResponse computedOrderPrice(OrderComputedPriceRequest request) {
        String key = "user_order:" + request.getPreOrderNo();
        Long expire = stringRedisTemplate.opsForValue().getOperations().getExpire(key);
        if (expire == -2) {
            throw new BizException("预下单订单不存在");
        }
        String orderVoString = stringRedisTemplate.opsForValue().get(key);
        OrderInfoVo orderInfoVo = JSONObject.parseObject(orderVoString, OrderInfoVo.class);
         UserDTO user = UserHolder.getUser();

        return computedPrice(request,orderInfoVo,user);
    }

    /**
     * 创建订单
     *
     * @param request 创建订单请求参数
     * @return MyRecord 订单编号
     */
    @Override
    public HashMap<String, String> createOrder(CreateOrderRequest request) {
         UserDTO user = UserHolder.getUser();
        String key = "user_order:" + request.getPreOrderNo();
        Long expire = stringRedisTemplate.opsForValue().getOperations().getExpire(key);
        if (expire == -2) {
            throw new BizException("预下单订单不存在");
        }
        String orderVoString = stringRedisTemplate.opsForValue().get(key);
        OrderInfoVo orderInfoVo = JSONObject.parseObject(orderVoString, OrderInfoVo.class);
        if (!request.getPayType().equals(PayConstants.PAY_TYPE_YUE)) {
            throw new BizException("支付错误");
        }


        String userAddressStr = "";
        if (request.getShippingType() == 1) { // 快递配送
            if (request.getAddressId() <= 0) throw new BizException("请选择收货地址");
            UserAddress userAddress = userAddressService.getById(request.getAddressId());
            if (ObjectUtil.isNull(userAddress) || userAddress.getIsDel()) {
                throw new BizException("收货地址有误");
            }
            request.setRealName(userAddress.getRealName());
            request.setPhone(userAddress.getPhone());
            userAddressStr = userAddress.getProvince() + userAddress.getCity() + userAddress.getDistrict() + userAddress.getDetail();
        }else {
            throw new BizException("参数错误");
        }
        // 计算订单各种价格
        OrderComputedPriceRequest orderComputedPriceRequest = new OrderComputedPriceRequest();
        orderComputedPriceRequest.setShippingType(request.getShippingType());
        orderComputedPriceRequest.setAddressId(request.getAddressId());
        orderComputedPriceRequest.setCouponId(request.getCouponId());
        orderComputedPriceRequest.setUseIntegral(request.getUseIntegral());
        ComputedOrderPriceResponse computedOrderPriceResponse = computedPrice(orderComputedPriceRequest, orderInfoVo, user);

        // 生成订单号
        String orderNo = JanUtils.getOrderNo("order");

        int gainIntegral = 0;
        List<StoreOrderInfo> storeOrderInfos = new ArrayList<>();
        for (OrderInfoDetailVo detailVo : orderInfoVo.getOrderDetailList()) {
            // 赠送积分
            if (ObjectUtil.isNotNull(detailVo.getGiveIntegral()) && detailVo.getGiveIntegral() > 0) {
                gainIntegral += detailVo.getGiveIntegral() * detailVo.getPayNum();
            }
            // 订单详情
            StoreOrderInfo soInfo = new StoreOrderInfo();
            soInfo.setProductId(detailVo.getProductId());
            soInfo.setInfo(JSON.toJSON(detailVo).toString());
            soInfo.setUnique(detailVo.getAttrValueId().toString());
            soInfo.setOrderNo(orderNo);
            soInfo.setProductName(detailVo.getProductName());
            soInfo.setAttrValueId(detailVo.getAttrValueId());
            soInfo.setImage(detailVo.getImage());
            soInfo.setSku(detailVo.getSku());
            soInfo.setPrice(detailVo.getPrice());
            soInfo.setPayNum(detailVo.getPayNum());
            soInfo.setWeight(detailVo.getWeight());
            soInfo.setVolume(detailVo.getVolume());
            if (ObjectUtil.isNotNull(detailVo.getGiveIntegral()) && detailVo.getGiveIntegral() > 0) {
                soInfo.setGiveIntegral(detailVo.getGiveIntegral());
            } else {
                soInfo.setGiveIntegral(0);
            }
            soInfo.setIsReply(false);
            soInfo.setIsSub(detailVo.getIsSub());
            if (ObjectUtil.isNotNull(detailVo.getVipPrice())) {
                soInfo.setVipPrice(detailVo.getVipPrice());
            } else {
                soInfo.setVipPrice(detailVo.getPrice());
            }

            storeOrderInfos.add(soInfo);
        }

        // 下单赠送积分
        if (computedOrderPriceResponse.getPayFee().compareTo(BigDecimal.ZERO) > 0) {
            // 赠送积分比例
            String integralStr = systemConfigService.getValueByKey(SysConfigConstants.CONFIG_KEY_INTEGRAL_RATE_ORDER_GIVE);
            if (StrUtil.isNotBlank(integralStr)) {
                BigDecimal integralBig = new BigDecimal(integralStr);
                int integral = integralBig.multiply(computedOrderPriceResponse.getPayFee()).setScale(0, BigDecimal.ROUND_DOWN).intValue();
                if (integral > 0) {
                    // 添加积分
                    gainIntegral += integral;
                }
            }
        }
        // 支付渠道 默认：余额支付
        int isChannel = 3;

        StoreOrder storeOrder = new StoreOrder();
        storeOrder.setUid(user.getUid());
        storeOrder.setOrderId(orderNo);
        storeOrder.setRealName(request.getRealName());
        storeOrder.setUserPhone(request.getPhone());
        storeOrder.setUserAddress(userAddressStr);
        storeOrder.setTotalNum(orderInfoVo.getOrderProNum());
        storeOrder.setCouponId(Optional.ofNullable(request.getCouponId()).orElse(0));

        // 订单总价
        BigDecimal totalPrice = computedOrderPriceResponse.getProTotalFee().add(computedOrderPriceResponse.getFreightFee());

        storeOrder.setTotalPrice(totalPrice);
        storeOrder.setProTotalPrice(computedOrderPriceResponse.getProTotalFee());
        storeOrder.setTotalPostage(computedOrderPriceResponse.getFreightFee());
        storeOrder.setCouponPrice(computedOrderPriceResponse.getCouponFee());
        storeOrder.setPayPrice(computedOrderPriceResponse.getPayFee());
        storeOrder.setPayPostage(computedOrderPriceResponse.getFreightFee());
        storeOrder.setDeductionPrice(computedOrderPriceResponse.getDeductionPrice());
        storeOrder.setPayType(request.getPayType());
        storeOrder.setUseIntegral(computedOrderPriceResponse.getUsedIntegral());
        storeOrder.setGainIntegral(gainIntegral);
        storeOrder.setMark(StringEscapeUtils.escapeHtml4(request.getMark()));
        storeOrder.setCreateTime(JanUtils.nowDateTime());
        storeOrder.setShippingType(request.getShippingType());
        storeOrder.setIsChannel(isChannel);
        storeOrder.setPaid(false);
        storeOrder.setCost(BigDecimal.ZERO);
        storeOrder.setType(0);

        StoreCouponUser storeCouponUser = new StoreCouponUser();
        // 优惠券修改
        if (storeOrder.getCouponId() > 0) {
            storeCouponUser = storeCouponUserService.getById(storeOrder.getCouponId());
            storeCouponUser.setStatus(1);
        }
        List<MyRecord> skuRecordList = validateProductStock(orderInfoVo, user);

        StoreCouponUser finalStoreCouponUser = storeCouponUser;
        Boolean execute = transactionTemplate.execute(e -> {
            for (MyRecord skuRecord : skuRecordList) {
                // 普通商品口库存
                productService.operationStock(skuRecord.getInt("productId"), skuRecord.getInt("num"), "sub");
                // 普通商品规格扣库存
                storeProductAttrValueService.operationStock(skuRecord.getInt("attrValueId"), skuRecord.getInt("num"), "sub", Constants.PRODUCT_TYPE_NORMAL);
            }
            create(storeOrder);
            storeOrderInfos.forEach(info -> info.setOrderId(storeOrder.getId()));
            // 优惠券修改
            if (storeOrder.getCouponId() > 0) {
                storeCouponUserService.updateById(finalStoreCouponUser);
            }
            // 保存购物车商品详情
            storeOrderInfoService.saveOrderInfos(storeOrderInfos);
            // 生成订单日志
            storeOrderStatusService.createLog(storeOrder.getId(), Constants.ORDER_STATUS_CACHE_CREATE_ORDER, "订单生成");

            // 清除购物车数据
            if (CollUtil.isNotEmpty(orderInfoVo.getCartIdList())) {
                storeCartService.deleteCartByIds(orderInfoVo.getCartIdList());
            }
            return Boolean.TRUE;
        });

        if (!execute) {
            throw new BizException("订单生成失败");
        }
        HashMap<String, String> map = new HashMap<>();
        map.put("orderNo",storeOrder.getOrderId());
        return map;
    }


    /**
     * 订单列表
     * @param status 类型
     * @param pageDTO 分页
     * @return CommonPage<OrderDetailResponse>
     */
    @Override
    public PageResult<OrderDetailResponse> list(Integer status, PageDTO pageDTO) {

        UserDTO user = UserHolder.getUser();
        Page<StoreOrder> page =new Page<>(pageDTO.getPage(), pageDTO.getLimit());
        LambdaQueryWrapper<StoreOrder> lqw = new LambdaQueryWrapper<>();

        lqw.eq(StoreOrder::getUid, user.getUid());
        lqw.orderByDesc(StoreOrder::getId);
        statusApiByWhere(lqw, status);
        Page<StoreOrder> orderList = dao.selectPage(page, lqw);



        List<OrderDetailResponse> responseList = CollUtil.newArrayList();

        for (StoreOrder storeOrder : orderList.getRecords()) {
            OrderDetailResponse infoResponse = new OrderDetailResponse();
            BeanUtils.copyProperties(storeOrder, infoResponse);

            // 订单状态
            infoResponse.setOrderStatus(getH5OrderStatus(storeOrder));

            // 订单详情对象列表
            List<StoreOrderInfo> orderInfoList = storeOrderInfoService.getListByOrderNo(storeOrder.getOrderId());
            List<OrderInfoResponse> infoResponseList = CollUtil.newArrayList();
            orderInfoList.forEach(e -> {
                OrderInfoResponse orderInfoResponse = new OrderInfoResponse();
                orderInfoResponse.setStoreName(e.getProductName());
                orderInfoResponse.setImage(e.getImage());
                orderInfoResponse.setCartNum(e.getPayNum());
                orderInfoResponse.setPrice(ObjectUtil.isNotNull(e.getVipPrice()) ? e.getVipPrice() : e.getPrice());
                orderInfoResponse.setProductId(e.getProductId());
                infoResponseList.add(orderInfoResponse);
            });
            infoResponse.setOrderInfoList(infoResponseList);
            responseList.add(infoResponse);
        }

        List<OrderDetailResponse> userAddressdao = BeanCopyUtils.copyList(responseList,OrderDetailResponse.class);

        return new PageResult<>(userAddressdao, (int) orderList.getTotal());
    }

    /**
     * 订单详情
     *
     * @param orderId 订单id
     */
    @Override
    public StoreOrderDetailInfoResponse detailOrder(String orderId) {
         Integer uid = UserHolder.getUser().getUid();

        StoreOrderDetailInfoResponse storeOrderDetailResponse = new StoreOrderDetailInfoResponse();

        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getOrderId, orderId);
         StoreOrder storeOrder = dao.selectOne(lqw);

        if (ObjectUtil.isNull(storeOrder) || storeOrder.getIsDel() || storeOrder.getIsSystemDel()) {
            throw new BizException("订单不存在");
        }
        if (!storeOrder.getUid().equals(uid)) {
            throw new BizException("订单不存在");
        }

        BeanUtils.copyProperties(storeOrder, storeOrderDetailResponse);
        MyRecord orderStatusVo = getOrderStatusVo(storeOrder);

        // 订单详情对象列表
        List<OrderInfoResponse> infoResponseList = CollUtil.newArrayList();
        List<StoreOrderInfo> infoList = storeOrderInfoService.getListByOrderNo(storeOrder.getOrderId());
        infoList.forEach(e -> {
            OrderInfoResponse orderInfoResponse = new OrderInfoResponse();
            orderInfoResponse.setStoreName(e.getProductName());
            orderInfoResponse.setImage(e.getImage());
            orderInfoResponse.setCartNum(e.getPayNum());
            orderInfoResponse.setPrice(ObjectUtil.isNotNull(e.getVipPrice()) ? e.getVipPrice() : e.getPrice());
            orderInfoResponse.setProductId(e.getProductId());
            orderInfoResponse.setIsReply(e.getIsReply() ? 1 : 0);
            orderInfoResponse.setAttrId(e.getAttrValueId());
            orderInfoResponse.setSku(e.getSku());
            infoResponseList.add(orderInfoResponse);
        });
        storeOrderDetailResponse.setOrderInfoList(infoResponseList);

        // 系统门店信息
        SystemStore systemStorePram = new SystemStore();
        systemStorePram.setId(storeOrder.getStoreId());
        storeOrderDetailResponse.setSystemStore(systemStoreService.getByCondition(systemStorePram));

        return storeOrderDetailResponse;
    }


    /**
     * 获取订单总数量
     * @param uid 用户uid
     * @return Integer
     */
    public Integer getOrderCountByUid(Integer uid) {
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getPaid, true);
        lqw.eq(StoreOrder::getIsDel, false);
        lqw.eq(StoreOrder::getUid, uid);
        lqw.lt(StoreOrder::getRefundStatus, 2);
        return dao.selectCount(lqw);
    }
    /**
     * 订单状态数量
     *
     * @return 订单状态数据量
     */
    @Override
    public OrderDataResponse orderData() {

         Integer userId = UserHolder.getUser().getUid();

        OrderDataResponse result = new OrderDataResponse();

        // 订单数量
        Integer orderCount = getOrderCountByUid(userId);
        // 待支付订单数
        Integer unPaidCount = getTopDataUtil(Constants.ORDER_STATUS_H5_UNPAID, userId);

        if (orderCount.equals(0)) {
            result.setOrderCount(0);
            result.setSumPrice(BigDecimal.ZERO);
            result.setUnPaidCount(unPaidCount);
            result.setUnShippedCount(0);
            result.setReceivedCount(0);
            result.setEvaluatedCount(0);
            result.setCompleteCount(0);
            result.setRefundCount(0);
            return result;
        }

        result.setOrderCount(orderCount);
        // 总消费金额
        BigDecimal sumPrice = getSumPayPriceByUid(userId);
        result.setSumPrice(sumPrice);
        // 未支付
        result.setUnPaidCount(unPaidCount);
        // 待发货
        result.setUnShippedCount(getTopDataUtil(Constants.ORDER_STATUS_H5_NOT_SHIPPED, userId));
        // 待收货
        result.setReceivedCount(getTopDataUtil(Constants.ORDER_STATUS_H5_SPIKE, userId));
        // 待核销
        result.setEvaluatedCount(getTopDataUtil(Constants.ORDER_STATUS_H5_JUDGE, userId));
        // 已完成
        result.setCompleteCount(getTopDataUtil(Constants.ORDER_STATUS_H5_COMPLETE, userId));
        // 退款中和已退款（只展示退款中）
        result.setRefundCount(getTopDataUtil(Constants.ORDER_STATUS_H5_REFUNDING, userId));
        return result;
    }

    /**
     * 删除已完成订单
     * @param id Integer 订单id
     * @return 删除结果
     */
    @Override
    public Boolean delete(Integer id) {
        StoreOrder storeOrder = getById(id);
         Integer uid = UserHolder.getUser().getUid();
        if (ObjectUtil.isNull(storeOrder) || !uid.equals(storeOrder.getUid())) {
            throw new BizException("没有找到相关订单信息!");
        }
        if (storeOrder.getIsDel() || storeOrder.getIsSystemDel()) {
            throw new BizException("订单已删除!");
        }
        if (storeOrder.getPaid()) {
            if (storeOrder.getRefundStatus() > 0 && !storeOrder.getRefundStatus().equals(2)) {
                throw new BizException("订单在退款流程中无法删除!");
            }
            if (storeOrder.getRefundStatus().equals(0) && !storeOrder.getStatus().equals(3)) {
                throw new BizException("只能删除已完成订单!");
            }
        } else {
            throw new BizException("未支付订单无法删除!");
        }

        //可以删除
        storeOrder.setIsDel(true);
        Boolean execute = transactionTemplate.execute(e -> {
            updateById(storeOrder);
            //日志
            storeOrderStatusService.createLog(storeOrder.getId(), "remove_order", "删除订单");
            return Boolean.TRUE;
        });
        return execute;
    }

    /**
     * 创建订单商品评价
     *
     * @param request 请求参数
     * @return Boolean
     */
    @Override
    public Boolean reply(StoreProductReplyAddRequest request) {
        if (StrUtil.isBlank(request.getOrderNo())) {
            throw new BizException("订单号参数不能为空");
        }
        return storeProductReplyService.create(request);
    }


    public Integer getTopDataUtil(Integer status, Integer userId) {
        LambdaQueryWrapper<StoreOrder> lqw = new LambdaQueryWrapper<>();
        statusApiByWhere(lqw, status);
        lqw.eq(StoreOrder::getUid,userId);
        return dao.selectCount(lqw);
    }



    public BigDecimal getSumPayPriceByUid(Integer userId) {
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.select(StoreOrder::getPayPrice);
        lqw.eq(StoreOrder::getPaid, true);
        lqw.eq(StoreOrder::getIsDel, false);
        lqw.eq(StoreOrder::getUid, userId);
        lqw.lt(StoreOrder::getRefundStatus, 2);
        List<StoreOrder> orderList = dao.selectList(lqw);
        return orderList.stream().map(StoreOrder::getPayPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取订单状态相关信息
     * @return MyRecord
     */
    private MyRecord getOrderStatusVo(StoreOrder storeOrder) {
        MyRecord record = new MyRecord();
        if (!storeOrder.getPaid()) {
            record.set("type", 0);
            record.set("title", "未支付");
            record.set("msg", "订单未支付");
            List<String> configKeys = new ArrayList<>();
            configKeys.add("order_cancel_time");
            configKeys.add("order_activity_time");
            configKeys.add("order_bargain_time");
            configKeys.add("order_seckill_time");
            configKeys.add("order_pink_time");
            List<String> configValues = systemConfigService.getValuesByKes(configKeys);
            Date timeSpace;
            timeSpace = JanUtils.addSecond(storeOrder.getCreateTime(),Double.valueOf(configValues.get(0)).intValue() * 3600);
            record.set("msg", "请在" + JanUtils.dateToStr(timeSpace, Constants.DATE_FORMAT) +"前完成支付");
        } else if (storeOrder.getRefundStatus() == 1) {
            record.set("type", -1);
            record.set("title", "申请退款中");
            record.set("msg", "商家审核中,请耐心等待");
        } else if (storeOrder.getRefundStatus() == 2) {
            record.set("type", -2);
            record.set("title", "已退款");
            record.set("msg", "已为您退款,感谢您的支持");
        } else if (storeOrder.getRefundStatus() == 3) {
            record.set("type", -3);
            record.set("title", "退款中");
            record.set("msg", "正在为您退款,感谢您的支持");
        } else if (storeOrder.getStatus() == 0) {
            record.set("type", 1);
            record.set("title", "未发货");
            record.set("msg", "商家未发货,请耐心等待");
        } else if (storeOrder.getStatus() == 1) { // 待收货处理
            // 待收货
            if (null != storeOrder.getDeliveryType() && storeOrder.getDeliveryType().equals(Constants.ORDER_STATUS_STR_SPIKE_KEY)) { // 送货
                StoreOrderStatus storeOrderStatus = new StoreOrderStatus();
                storeOrderStatus.setOid(storeOrder.getId());
                storeOrderStatus.setChangeType(Constants.ORDER_LOG_DELIVERY);
                List<StoreOrderStatus> sOrderStatusResults = storeOrderStatusService.getByEntity(storeOrderStatus);
                if (sOrderStatusResults.size()>0) {
                    record.set("type", 2);
                    record.set("title", "待收货");
                    record.set("msg", "商家已送货,请耐心等待");
                }
            } else if (null != storeOrder.getDeliveryType() && storeOrder.getDeliveryType().equals(Constants.ORDER_LOG_EXPRESS)) {
                StoreOrderStatus storeOrderStatus = new StoreOrderStatus();
                storeOrderStatus.setOid(storeOrder.getId());
                storeOrderStatus.setChangeType(Constants.ORDER_LOG_EXPRESS);
                List<StoreOrderStatus> sOrderStatusResults = storeOrderStatusService.getByEntity(storeOrderStatus);
                if (sOrderStatusResults.size()>0) {
                    record.set("type", 2);
                    record.set("title", "待收货");
                    record.set("msg", "商家已发货,请耐心等待");
                }
            }else {
                StoreOrderStatus storeOrderStatus = new StoreOrderStatus();
                storeOrderStatus.setOid(storeOrder.getId());
                storeOrderStatus.setChangeType(Constants.ORDER_LOG_DELIVERY_VI);
                List<StoreOrderStatus> sOrderStatusResults = storeOrderStatusService.getByEntity(storeOrderStatus);
                if (sOrderStatusResults.size()>0) {
                    record.set("type", 2);
                    record.set("title", "待收货");
                    record.set("msg", "服务商已虚拟发货");
                } else {
                    record.set("type", 2);
                    record.set("title", "待收货");
                    record.set("msg", "退款拒绝订单已发货");
                }
            }
        }else if (storeOrder.getStatus() == 2) {
            record.set("type", 3);
            record.set("title", "待评价");
            record.set("msg", "已收货,快去评价一下吧");
        }else if (storeOrder.getStatus() == 3) {
            record.set("type", 4);
            record.set("title", "交易完成");
            record.set("msg", "交易完成,感谢您的支持");
        }


        record.set("payTypeStr", "余额支付");
        if (StringUtils.isNotBlank(storeOrder.getDeliveryType())) {
            record.set("deliveryType", StringUtils.isNotBlank(storeOrder.getDeliveryType()) ? storeOrder.getDeliveryType():"其他方式");
        }

        return record;
    }

    /**
     * 获取H5订单状态
     * @param storeOrder 订单对象
     */
    private String getH5OrderStatus(StoreOrder storeOrder) {
        if (!storeOrder.getPaid()) {
            return "待支付";
        }
        if (storeOrder.getRefundStatus().equals(1)) {
            return "申请退款中";
        }
        if (storeOrder.getRefundStatus().equals(2)) {
            return "已退款";
        }
        if (storeOrder.getRefundStatus().equals(3)) {
            return "退款中";
        }
        if (storeOrder.getStatus().equals(0)) {
            return "待发货";
        }
        if (storeOrder.getStatus().equals(1)) {
            return "待收货";
        }
        if (storeOrder.getStatus().equals(2)) {
            return "待评价";
        }
        if (storeOrder.getStatus().equals(3)) {
            return "已完成";
        }
        return "";
    }



    /**
     * h5 订单查询 where status 封装
     * @param queryWrapper 查询条件
     * @param status 状态
     */
    public void statusApiByWhere(LambdaQueryWrapper<StoreOrder> queryWrapper, Integer status){
        switch (status){
            case Constants.ORDER_STATUS_H5_UNPAID: // 未支付
                queryWrapper.eq(StoreOrder::getPaid, false);
                queryWrapper.eq(StoreOrder::getStatus, 0);
                queryWrapper.eq(StoreOrder::getRefundStatus, 0);
                queryWrapper.eq(StoreOrder::getType, 0);
                break;
            case Constants.ORDER_STATUS_H5_NOT_SHIPPED: // 待发货
                queryWrapper.eq(StoreOrder::getPaid, true);
                queryWrapper.eq(StoreOrder::getStatus, 0);
                queryWrapper.eq(StoreOrder::getRefundStatus, 0);
//                queryWrapper.eq(StoreOrder::getShippingType, 1);
                break;
            case Constants.ORDER_STATUS_H5_SPIKE: // 待收货
                queryWrapper.eq(StoreOrder::getPaid, true);
                queryWrapper.eq(StoreOrder::getStatus, 1);
                queryWrapper.eq(StoreOrder::getRefundStatus, 0);
                break;
            case Constants.ORDER_STATUS_H5_JUDGE: //  已支付 已收货 待评价
                queryWrapper.eq(StoreOrder::getPaid, true);
                queryWrapper.eq(StoreOrder::getStatus, 2);
                queryWrapper.eq(StoreOrder::getRefundStatus, 0);
                break;
            case Constants.ORDER_STATUS_H5_COMPLETE: // 已完成
                queryWrapper.eq(StoreOrder::getPaid, true);
                queryWrapper.eq(StoreOrder::getStatus, 3);
                queryWrapper.eq(StoreOrder::getRefundStatus, 0);
                break;
            case Constants.ORDER_STATUS_H5_REFUNDING: // 退款中
                queryWrapper.eq(StoreOrder::getPaid, true);
                queryWrapper.in(StoreOrder::getRefundStatus, 1, 3);
                break;
            case Constants.ORDER_STATUS_H5_REFUNDED: // 已退款
                queryWrapper.eq(StoreOrder::getPaid, true);
                queryWrapper.eq(StoreOrder::getRefundStatus, 2);
                break;
            case Constants.ORDER_STATUS_H5_REFUND: // 包含已退款和退款中
                queryWrapper.eq(StoreOrder::getPaid, true);
                queryWrapper.in(StoreOrder::getRefundStatus, 1,2,3);
                break;
        }
        queryWrapper.eq(StoreOrder::getIsDel, false);
        queryWrapper.eq(StoreOrder::getIsSystemDel, false);
    }

    /**
     * 创建订单
     * @param storeOrder 订单参数
     * @return 结果标识
     */
    public boolean create(StoreOrder storeOrder) {
        return dao.insert(storeOrder) > 0;
    }

    /**
     * 校验商品库存（生成订单）
     * @param orderInfoVo 订单详情Vo
     * @return List<MyRecord>
     * skuRecord 扣减库存对象
     * ——activityId             活动商品id
     * ——activityAttrValueId    活动商品skuId
     * ——productId              普通（主）商品id
     * ——attrValueId            普通（主）商品skuId
     * ——num                    购买数量
     */
    private List<MyRecord> validateProductStock(OrderInfoVo orderInfoVo, UserDTO user) {
        List<MyRecord> recordList = CollUtil.newArrayList();

        // 普通商品
        List<OrderInfoDetailVo> orderDetailList = orderInfoVo.getOrderDetailList();
        orderDetailList.forEach(e -> {
            // 查询商品信息
            StoreProduct storeProduct = productService.getById(e.getProductId());
            if (ObjectUtil.isNull(storeProduct)) {
                throw new BizException("购买的商品信息不存在");
            }
            if (storeProduct.getIsDel()) {
                throw new BizException("购买的商品已删除");
            }
            if (!storeProduct.getIsShow()) {
                throw new BizException("购买的商品已下架");
            }
            if (storeProduct.getStock().equals(0) || e.getPayNum() > storeProduct.getStock()) {
                throw new BizException("购买的商品库存不足");
            }
            // 查询商品规格属性值信息
            StoreProductAttrValue attrValue = attrValueService.getByIdAndProductIdAndType(e.getAttrValueId(), e.getProductId(), Constants.PRODUCT_TYPE_NORMAL);
            if (ObjectUtil.isNull(attrValue)) {
                throw new BizException("购买的商品规格信息不存在");
            }
            if (attrValue.getStock() < e.getPayNum()) {
                throw new BizException("购买的商品库存不足");
            }
            MyRecord record = new MyRecord();
            record.set("productId", e.getProductId());
            record.set("num", e.getPayNum());
            record.set("attrValueId", e.getAttrValueId());
            recordList.add(record);
        });
        return recordList;
    }

    private ComputedOrderPriceResponse computedPrice(OrderComputedPriceRequest request, OrderInfoVo orderInfoVo, UserDTO user) {
        // 计算各种价格
        ComputedOrderPriceResponse priceResponse = new ComputedOrderPriceResponse();
        // 计算运费
        if (request.getShippingType().equals(2)) {// 到店自提，不计算运费
            priceResponse.setFreightFee(BigDecimal.ZERO);
        } else if (ObjectUtil.isNull(request.getAddressId()) || request.getAddressId() <= 0) {
            // 快递配送，无地址
            priceResponse.setFreightFee(BigDecimal.ZERO);
        } else {// 快递配送，有地址
            UserAddress userAddress = userAddressService.getById(request.getAddressId());
            if (ObjectUtil.isNull(userAddress)) {
                priceResponse.setFreightFee(BigDecimal.ZERO);
            } else {
                priceResponse.setFreightFee(orderInfoVo.getFreightFee());
            }
        }



        return null;
    }


    /**
     * 校验预下单商品信息
     * @param request 预下单请求参数
     * @return OrderInfoVo
     */
    private OrderInfoVo validatePreOrderRequest(PreOrderRequest request, UserDTO user) {
         OrderInfoVo orderInfoVo = new OrderInfoVo();
         List<OrderInfoDetailVo> detailVoList = new ArrayList<>();
        if (request.getPreOrderType().equals("shoppingCart")) { //购物车购买
            detailVoList = validatePreOrderShopping(request, user);
           List<Long> cartIdList = request.getOrderDetails().stream()
                    .map(PreOrderDetailRequest::getShoppingCartId)
                    .distinct()
                    .collect(Collectors.toList());
            orderInfoVo.setCartIdList(cartIdList);
        }
        if(request.getPreOrderType().equals("buyNow")) { //立即购买
            //立即购买只会有一条详情
             PreOrderDetailRequest detailRequest = request.getOrderDetails().get(0);
            if (ObjectUtil.isNull(detailRequest.getProductId())) {
                throw new BizException("商品编号不能为空");
            }
            if (ObjectUtil.isNull(detailRequest.getAttrValueId())) {
                throw new BizException("商品规格属性值不能为空");
            }
            if (ObjectUtil.isNull(detailRequest.getProductNum()) || detailRequest.getProductNum() < 0) {
                throw new BizException("购买数量必须大于0");
            }

            //查询商品信息
             StoreProduct storeProduct = productService.getById(detailRequest.getProductId());
            if (ObjectUtil.isNull(storeProduct)) {
                throw new BizException("商品信息不存在，请刷新后重新选择");
            }
            if (storeProduct.getIsDel()) {
                throw new BizException("商品已删除，请刷新后重新选择");
            }
            if (!storeProduct.getIsShow()) {
                throw new BizException("商品已下架，请刷新后重新选择");
            }
            if (storeProduct.getStock() <detailRequest.getProductNum()) {
                throw new BizException("商品库存不足，请刷新后重新选择");
            }
            // 查询商品规格属性值信息
            StoreProductAttrValue attrValue = attrValueService.getByIdAndProductIdAndType(detailRequest.getAttrValueId(), detailRequest.getProductId(), Constants.PRODUCT_TYPE_NORMAL);
            if (ObjectUtil.isNull(attrValue)) {
                throw new BizException("商品规格信息不存在，请刷新后重新选择");
            }
            if (attrValue.getStock() < detailRequest.getProductNum()) {
                throw new BizException("商品规格库存不足，请刷新后重新选择");
            }
            SystemUserLevel userLevel = null;
            if (user.getLevel() > 0) {
                userLevel = systemUserLevelService.getByLevelId(user.getLevel());
            }
            OrderInfoDetailVo detailVo = new OrderInfoDetailVo();
            detailVo.setProductId(storeProduct.getId());
            detailVo.setProductName(storeProduct.getStoreName());
            detailVo.setAttrValueId(attrValue.getId());
            detailVo.setSku(attrValue.getSuk());
            detailVo.setPrice(attrValue.getPrice());
            detailVo.setPayNum(detailRequest.getProductNum());
            detailVo.setImage(StrUtil.isNotBlank(attrValue.getImage()) ? attrValue.getImage() : storeProduct.getImage());
            detailVo.setVolume(attrValue.getVolume());
            detailVo.setWeight(attrValue.getWeight());
            detailVo.setTempId(storeProduct.getTempId());
            detailVo.setIsSub(storeProduct.getIsSub());
            detailVo.setProductType(Constants.PRODUCT_TYPE_NORMAL);
            detailVo.setVipPrice(detailVo.getPrice());
            detailVo.setGiveIntegral(storeProduct.getGiveIntegral());
            if (ObjectUtil.isNotNull(userLevel)) {
                detailVo.setVipPrice(detailVo.getPrice());
            }
            detailVoList.add(detailVo);

        }
        if (request.getPreOrderType().equals("again")) {// 再次购买
            PreOrderDetailRequest detailRequest = request.getOrderDetails().get(0);
            detailVoList = validatePreOrderAgain(detailRequest, user);
        }
        orderInfoVo.setOrderDetailList(detailVoList);
        return orderInfoVo;

    }

    /**
     * 再次下单预下单校验
     * @param detailRequest 请求参数
     * @return List<OrderInfoDetailVo>
     */
    private List<OrderInfoDetailVo> validatePreOrderAgain(PreOrderDetailRequest detailRequest, UserDTO user) {
         List<OrderInfoDetailVo> detailVoList = new ArrayList<>();

        if (StrUtil.isBlank(detailRequest.getOrderNo())) {
            throw new BizException("再次购买订单编号不能为空");
        }
        StoreOrder storeOrder = getByOrderIdException(detailRequest.getOrderNo());

        if (storeOrder.getRefundStatus() > 0 || storeOrder.getStatus() != 3) {
            throw new BizException("只有已完成状态订单才能再次购买");
        }

        List<StoreOrderInfoVo> infoVoList = storeOrderInfoService.getVoListByOrderId(storeOrder.getId());
        if (CollUtil.isEmpty(infoVoList)) {
            throw new BizException("订单详情未找到");
        }
        SystemUserLevel userLevel = null;
        if (user.getLevel()>0) {
            userLevel = systemUserLevelService.getByLevelId(user.getLevel());
        }
        SystemUserLevel finalUserLevel = userLevel;
        infoVoList.forEach(e -> {
            OrderInfoDetailVo detailVo = e.getInfo();
            //查询商品信息
             StoreProduct storeProduct = productService.getById(detailVo.getProductId());
            if (ObjectUtil.isNull(storeProduct)) {
                throw new BizException("商品信息不存在，请刷新后重新选择");
            }
            if (storeProduct.getIsDel()) {
                throw new BizException("商品已删除，请刷新后重新选择");
            }
            if (!storeProduct.getIsShow()) {
                throw new BizException("商品已下架，请刷新后重新选择");
            }
            if (storeProduct.getStock() < detailVo.getPayNum()) {
                throw new BizException("商品库存不足，请刷新后重新选择");
            }
            // 查询商品规格属性值信息
            StoreProductAttrValue attrValue = attrValueService.getByIdAndProductIdAndType(detailVo.getAttrValueId(), detailVo.getProductId(), Constants.PRODUCT_TYPE_NORMAL);
            if (ObjectUtil.isNull(attrValue)) {
                throw new BizException("商品规格信息不存在，请刷新后重新选择");
            }
            if (attrValue.getStock() < detailVo.getPayNum()) {
                throw new BizException("商品规格库存不足，请刷新后重新选择");
            }
            OrderInfoDetailVo tempDetailVo = new OrderInfoDetailVo();
            tempDetailVo.setProductId(storeProduct.getId());
            tempDetailVo.setProductName(storeProduct.getStoreName());
            tempDetailVo.setAttrValueId(attrValue.getId());
            tempDetailVo.setSku(attrValue.getSuk());
            tempDetailVo.setPrice(attrValue.getPrice());
            tempDetailVo.setPayNum(detailVo.getPayNum());
            tempDetailVo.setImage(StrUtil.isNotBlank(attrValue.getImage()) ? attrValue.getImage() : storeProduct.getImage());
            tempDetailVo.setVolume(attrValue.getVolume());
            tempDetailVo.setWeight(attrValue.getWeight());
            tempDetailVo.setTempId(storeProduct.getTempId());
            tempDetailVo.setGiveIntegral(storeProduct.getGiveIntegral());
            tempDetailVo.setIsSub(storeProduct.getIsSub());
            tempDetailVo.setProductType(Constants.PRODUCT_TYPE_NORMAL);
            tempDetailVo.setVipPrice(attrValue.getPrice());
            if (ObjectUtil.isNotNull(finalUserLevel)) {
                tempDetailVo.setVipPrice(attrValue.getPrice());
            }
            detailVoList.add(tempDetailVo);
        });
        return detailVoList;
    }

    private StoreOrder getByOrderIdException(String orderId) {
        StoreOrder storeOrder = getByOderId(orderId);
        if (ObjectUtil.isNull(storeOrder)) {
            throw new BizException("订单不存在");
        }
        if (storeOrder.getIsDel() || storeOrder.getIsSystemDel()) {
            throw new BizException("订单不存在");
        }
        return storeOrder;
    }


    @Override
    public StoreOrder getByOderId(String orderId) {
        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getOrderId, orderId);
        return dao.selectOne(lqw);
    }

    /**
     * 订单收货
     *
     * @param id 订单id
     * @return Boolean
     */
    @Override
    public Boolean take(Integer id) {
         StoreOrder storeOrder = getInfoById(id);
        if (!storeOrder.getStatus().equals(Constants.ORDER_STATUS_INT_SPIKE)) {
            throw new BizException("订单状态错误");
        }
        //已收货，待评价
        storeOrder.setStatus(Constants.ORDER_STATUS_INT_BARGAIN);
        boolean result = updateById(storeOrder);
        if (result) {
            //后续操作放入redis
//            redisUtil.lPush(TaskConstants.ORDER_TASK_REDIS_KEY_AFTER_TAKE_BY_USER, id);
        }
        return result;
    }

    /**
     * 订单取消
     *
     * @param id 订单id
     * @return Boolean
     */
    @Override
    public Boolean cancel(Integer id) {
         StoreOrder storeOrder = getInfoById(id);
        storeOrder.setIsDel(true);
        storeOrder.setIsSystemDel(true);
        boolean result = updateById(storeOrder);

        //后续操作放入redis
//        redisUtil.lPush(Constants.ORDER_TASK_REDIS_KEY_AFTER_CANCEL_BY_USER, id);
        return result;
    }



    /**
     * 根据订单号查询订单信息
     * @param id 订单id
     * @return 计算后的价格集合
     */
    @Override
    public StoreOrder getInfoById(Integer id) {
        StoreOrder storeOrder = getById(id);
         Integer uid = UserHolder.getUser().getUid();

        if(null == storeOrder || !uid.equals(storeOrder.getUid())){
            //订单号错误
            throw new BizException("没有找到相关订单信息!");
        }

        return storeOrder;
    }

    /**
     * 获取申请订单退款信息
     *
     * @param orderId 订单编号
     * @return ApplyRefundOrderInfoResponse
     */
    @Override
    public ApplyRefundOrderInfoResponse applyRefundOrderInfo(String orderId) {
        StoreOrder storeOrder = getByOrderIdException(orderId);
        ApplyRefundOrderInfoResponse response = new ApplyRefundOrderInfoResponse();
        BeanUtils.copyProperties(storeOrder, response);
        // 订单详情对象列表
        List<StoreOrderInfoOldVo> infoVoList = storeOrderInfoService.getOrderListByOrderId(storeOrder.getId());
        List<OrderInfoResponse> infoResponseList = CollUtil.newArrayList();
        infoVoList.forEach(e -> {
            OrderInfoResponse orderInfoResponse = new OrderInfoResponse();
            orderInfoResponse.setStoreName(e.getInfo().getProductName());
            orderInfoResponse.setImage(e.getInfo().getImage());
            orderInfoResponse.setCartNum(e.getInfo().getPayNum());
            orderInfoResponse.setPrice(e.getInfo().getPrice());
            orderInfoResponse.setProductId(e.getProductId());
            infoResponseList.add(orderInfoResponse);
        });
        response.setOrderInfoList(infoResponseList);
        return response;
    }

    /**
     * 订单退款申请
     *
     * @param request 申请参数
     * @return Boolean
     */
    @Override
    public Boolean refundApply(OrderRefundApplyRequest request) {
        StoreOrder storeOrderPram = new StoreOrder();
        storeOrderPram.setOrderId(request.getUni());
        storeOrderPram.setIsDel(false);
        storeOrderPram.setPaid(true);
        StoreOrder existStoreOrder = getByEntityOne(storeOrderPram);
        if (null == existStoreOrder) {
            throw new BizException("支付订单不存在");
        }
        if (existStoreOrder.getRefundStatus() == 1) {
            throw new BizException("正在申请退款中");
        }

        if (existStoreOrder.getRefundStatus() == 2) {
            throw new BizException("订单已退款");
        }

        if (existStoreOrder.getRefundStatus() == 3) {
            throw new BizException("订单退款中");
        }

        existStoreOrder.setRefundStatus(1);
        existStoreOrder.setRefundReasonTime(JanUtils.nowDateTime());
        existStoreOrder.setRefundReasonWap(request.getText());
        existStoreOrder.setRefundReasonWapExplain(request.getExplain());
        existStoreOrder.setRefundPrice(BigDecimal.ZERO);

        Boolean execute = transactionTemplate.execute(e -> {
            updateById(existStoreOrder);
            storeOrderStatusService.createLog(existStoreOrder.getId(), Constants.ORDER_LOG_REFUND_APPLY, "用户申请退款原因：" + request.getText());
            return Boolean.TRUE;
        });

//        if (execute) {
//            // 发送用户退款管理员提醒短信
//            SystemNotification notification = systemNotificationService.getByMark(NotifyConstants.APPLY_ORDER_REFUND_ADMIN_MARK);
//            if (notification.getIsSms().equals(1)) {
//                // 查询可已发送短信的管理员
//                List<SystemAdmin> systemAdminList = systemAdminService.findIsSmsList();
//                if (CollUtil.isNotEmpty(systemAdminList)) {
//                    SmsTemplate smsTemplate = smsTemplateService.getDetail(notification.getSmsId());
//                    Integer tempId = Integer.valueOf(smsTemplate.getTempId());
//                    // 发送短信
//                    systemAdminList.forEach(admin -> {
//                        smsService.sendOrderRefundApplyNotice(admin.getPhone(), existStoreOrder.getOrderId(), admin.getRealName(), tempId);
//                    });
//                }
//            }
//        }
        if (!execute) {
            throw new BizException("申请退款失败");
        }
        return execute;
    }

    /**
     * 查询退款理由
     *
     * @return 退款理由集合
     */
    @Override
    public List<String> getRefundReason() {
        String reasonString = systemConfigService.getValueByKey(SysConfigConstants.CONFIG_KEY_STOR_REASON);
        reasonString = JanUtils.UnicodeToCN(reasonString);
        reasonString = reasonString.replace("rn", "n");
        return Arrays.asList(reasonString.split("\\n"));
    }


        /**
         * 订单物流查看
         * @param orderId 订单id
         */
        @Override
        public Object expressOrder(String orderId) {
            HashMap<String,Object> resultMap = new HashMap<>();
            StoreOrder storeOrderPram = new StoreOrder();
            storeOrderPram.setOrderId(orderId);
            StoreOrder existOrder = getByEntityOne(storeOrderPram);
            if (ObjectUtil.isNull(existOrder)) {
                throw new BizException("未找到该订单信息");
            }
            if (!existOrder.getDeliveryType().equals(Constants.ORDER_LOG_EXPRESS) || StringUtils.isBlank(existOrder.getDeliveryType())) {
                throw new BizException("该订单不存在快递订单号");
            }

//            if (existOrder.getType().equals(1)) {// 视频号订单
//                Express express = expressService.getByName(existOrder.getDeliveryName());
//                if (ObjectUtil.isNotNull(express)) {
//                    existOrder.setDeliveryCode(express.getCode());
//                } else {
//                    existOrder.setDeliveryCode("");
//                }
//            }
            //快递信息
//            LogisticsResultVo expressInfo = logisticsService.info(existOrder.getDeliveryId(), null, Optional.ofNullable(existOrder.getDeliveryCode()).orElse(""), storeOrderPram.getUserPhone());

            List<StoreOrderInfoVo> list = storeOrderInfoService.getVoListByOrderId(existOrder.getId());
            List<HashMap<String, Object>> cartInfos = CollUtil.newArrayList();
            for (StoreOrderInfoVo infoVo : list) {
                HashMap<String, Object> cartInfo = new HashMap<>();
                cartInfo.put("payNum", infoVo.getInfo().getPayNum());
                cartInfo.put("price", infoVo.getInfo().getPrice());
                cartInfo.put("productName", infoVo.getInfo().getProductName());
                cartInfo.put("productImg", infoVo.getInfo().getImage());
                cartInfos.add(cartInfo);
            }
            HashMap<String, Object> orderInfo = new HashMap<>();
            orderInfo.put("deliveryId", existOrder.getDeliveryId());
            orderInfo.put("deliveryName", existOrder.getDeliveryName());
            orderInfo.put("deliveryType", existOrder.getDeliveryType());
            orderInfo.put("info", cartInfos);

            resultMap.put("order", orderInfo);
//            resultMap.put("express", expressInfo);
            return resultMap;

        }

    /**
     * 订单基本查询一条
     * @param storeOrder 参数
     * @return 查询结果
     */
    @Override
    public StoreOrder getByEntityOne(StoreOrder storeOrder) {
        LambdaQueryWrapper<StoreOrder> lqw = new LambdaQueryWrapper<>();
        lqw.setEntity(storeOrder);
        return dao.selectOne(lqw);
    }

    /**
     * 获取待评价商品信息
     *
     * @param getProductReply 订单详情参数
     * @return 待评价
     */
    @Override
    public OrderProductReplyResponse getReplyProduct(GetProductReply getProductReply) {
        StoreOrderInfo storeOrderInfo = storeOrderInfoService.getByUniAndOrderId(getProductReply.getUni(), getProductReply.getOrderId());
        OrderInfoDetailVo scr = JSONObject.parseObject(storeOrderInfo.getInfo(), OrderInfoDetailVo.class);
        OrderProductReplyResponse response = new OrderProductReplyResponse();
        response.setCartNum(scr.getPayNum());
        response.setTruePrice(scr.getPrice());
        response.setProductId(scr.getProductId());
        response.setImage(scr.getImage());
        response.setSku(scr.getSku());
        response.setStoreName(scr.getProductName());
        return response;
    }






    /**
     * 购物车预下单校验
     * @param request 请求参数
     * @param user 用户
     * @return List<OrderInfoDetailVo>
     */
    private List<OrderInfoDetailVo> validatePreOrderShopping(PreOrderRequest request, UserDTO user) {
        List<OrderInfoDetailVo> detailVoList = new ArrayList<>();
        SystemUserLevel userLevel = null;
        if (user.getLevel() > 0) {
            userLevel = systemUserLevelService.getByLevelId(user.getLevel());
        }
        SystemUserLevel finalUserLevel =userLevel;
        request.getOrderDetails().forEach(e -> {
            if (ObjectUtil.isNull(e.getShoppingCartId())) {
                throw new BizException("购物车编号不能为空");
            }
            StoreCart storeCart = storeCartService.getByIdAndUid(e.getShoppingCartId(), user.getUid());
            if (ObjectUtil.isNull(storeCart)) {
                throw new BizException("未找到对应的购物车信息");
            }
            //查询商品信息
             StoreProduct storeProduct = productService.getById(storeCart.getProductId());
            if (ObjectUtil.isNull(storeProduct)) {
                throw new BizException("商品信息不存在，请刷新后重新选择");
            }
            if (storeProduct.getIsDel()) {
                throw new BizException("商品已删除，请刷新后重新选择");
            }
            if (!storeProduct.getIsShow()) {
                throw new BizException("商品已下架，请刷新后重新选择");
            }
            if (storeProduct.getStock() < storeCart.getCartNum()) {
                throw new BizException("商品库存不足，请刷新后重新选择");
            }
            //查询商品规格属性值信息
            // 查询商品规格属性值信息
            StoreProductAttrValue attrValue = attrValueService.getByIdAndProductIdAndType(Integer.valueOf(storeCart.getProductAttrUnique()),
                    storeCart.getProductId(),
                    Constants.PRODUCT_TYPE_NORMAL);
            if (ObjectUtil.isNull(attrValue)) {
                throw new BizException("商品规格信息不存在，请刷新后重新选择");
            }
            if (attrValue.getStock() < storeCart.getCartNum()) {
                throw new BizException("商品库存不足，请刷新后重新选择");
            }
            OrderInfoDetailVo detailVo =new OrderInfoDetailVo();
            detailVo.setProductId(storeProduct.getId());
            detailVo.setProductName(storeProduct.getStoreName());
            detailVo.setAttrValueId(attrValue.getId());
            detailVo.setSku(attrValue.getSuk());
            detailVo.setPrice(attrValue.getPrice());
            detailVo.setPayNum(storeCart.getCartNum());
            detailVo.setImage(StrUtil.isNotBlank(attrValue.getImage()) ? attrValue.getImage() : storeProduct.getImage());
            detailVo.setVolume(attrValue.getVolume());
            detailVo.setWeight(attrValue.getWeight());
            detailVo.setTempId(storeProduct.getTempId());
            detailVo.setGiveIntegral(storeProduct.getGiveIntegral());
            detailVo.setIsSub(storeProduct.getIsSub());
            detailVo.setProductType(Constants.PRODUCT_TYPE_NORMAL);
            detailVo.setVipPrice(detailVo.getPrice());
            if (ObjectUtil.isNotNull(finalUserLevel)) {
                BigDecimal vipPrice = detailVo.getPrice().multiply(new BigDecimal(finalUserLevel.getDiscount())).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
                detailVo.setVipPrice(vipPrice);
            }
            detailVoList.add(detailVo);
        });
        return detailVoList;

    }
}
