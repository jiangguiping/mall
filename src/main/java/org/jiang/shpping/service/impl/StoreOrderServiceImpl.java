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
     * ???????????????
     *
     * @param request ?????????????????????
     * @return PreOrderResponse
     */
    @Override
    public Map<String,Object> preOrder(PreOrderRequest request) {
        if (CollUtil.isEmpty(request.getOrderDetails())) {
            throw new BizException("???????????????????????????????????????");
        }
         UserDTO user = UserHolder.getUser();
        //???????????????????????????
        OrderInfoVo orderInfoVo = validatePreOrderRequest(request, user);
        //??????????????????
        BigDecimal totalPrice;
        // ????????????
        totalPrice = orderInfoVo.getOrderDetailList().stream().map(e -> e.getVipPrice().multiply(new BigDecimal(e.getPayNum()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        orderInfoVo.setProTotalFee(totalPrice);
        int orderProNum = orderInfoVo.getOrderDetailList().stream().mapToInt(OrderInfoDetailVo::getPayNum).sum();
        orderInfoVo.setOrderProNum(orderProNum);

        //??????????????????
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
     * ?????????????????????
     *
     * @param preOrderNo ????????????
     * @return ???????????????
     */
    @Override
    public PreOrderResponse loadPreOrder(String preOrderNo) {
        String key = "user_order:" + preOrderNo;
        Long expire = stringRedisTemplate.opsForValue().getOperations().getExpire(key);
        if (expire == -2) {
             throw new BizException("????????????????????????");
        }
        String orderVoString = stringRedisTemplate.opsForValue().get(key);
        OrderInfoVo orderInfoVo = JSONObject.parseObject(orderVoString, OrderInfoVo.class);
        PreOrderResponse preOrderResponse = new PreOrderResponse();

        preOrderResponse.setOrderInfoVo(orderInfoVo);
        preOrderResponse.setYuePayStatus(String.valueOf(1));
        return preOrderResponse;
    }

    /**
     * ??????????????????
     *
     * @param request ??????????????????????????????
     * @return ComputedOrderPriceResponse
     */
    @Override
    public ComputedOrderPriceResponse computedOrderPrice(OrderComputedPriceRequest request) {
        String key = "user_order:" + request.getPreOrderNo();
        Long expire = stringRedisTemplate.opsForValue().getOperations().getExpire(key);
        if (expire == -2) {
            throw new BizException("????????????????????????");
        }
        String orderVoString = stringRedisTemplate.opsForValue().get(key);
        OrderInfoVo orderInfoVo = JSONObject.parseObject(orderVoString, OrderInfoVo.class);
         UserDTO user = UserHolder.getUser();

        return computedPrice(request,orderInfoVo,user);
    }

    /**
     * ????????????
     *
     * @param request ????????????????????????
     * @return MyRecord ????????????
     */
    @Override
    public HashMap<String, String> createOrder(CreateOrderRequest request) {
         UserDTO user = UserHolder.getUser();
        String key = "user_order:" + request.getPreOrderNo();
        Long expire = stringRedisTemplate.opsForValue().getOperations().getExpire(key);
        if (expire == -2) {
            throw new BizException("????????????????????????");
        }
        String orderVoString = stringRedisTemplate.opsForValue().get(key);
        OrderInfoVo orderInfoVo = JSONObject.parseObject(orderVoString, OrderInfoVo.class);
        if (!request.getPayType().equals(PayConstants.PAY_TYPE_YUE)) {
            throw new BizException("????????????");
        }


        String userAddressStr = "";
        if (request.getShippingType() == 1) { // ????????????
            if (request.getAddressId() <= 0) throw new BizException("?????????????????????");
            UserAddress userAddress = userAddressService.getById(request.getAddressId());
            if (ObjectUtil.isNull(userAddress) || userAddress.getIsDel()) {
                throw new BizException("??????????????????");
            }
            request.setRealName(userAddress.getRealName());
            request.setPhone(userAddress.getPhone());
            userAddressStr = userAddress.getProvince() + userAddress.getCity() + userAddress.getDistrict() + userAddress.getDetail();
        }else {
            throw new BizException("????????????");
        }
        // ????????????????????????
        OrderComputedPriceRequest orderComputedPriceRequest = new OrderComputedPriceRequest();
        orderComputedPriceRequest.setShippingType(request.getShippingType());
        orderComputedPriceRequest.setAddressId(request.getAddressId());
        orderComputedPriceRequest.setCouponId(request.getCouponId());
        orderComputedPriceRequest.setUseIntegral(request.getUseIntegral());
        ComputedOrderPriceResponse computedOrderPriceResponse = computedPrice(orderComputedPriceRequest, orderInfoVo, user);

        // ???????????????
        String orderNo = JanUtils.getOrderNo("order");

        int gainIntegral = 0;
        List<StoreOrderInfo> storeOrderInfos = new ArrayList<>();
        for (OrderInfoDetailVo detailVo : orderInfoVo.getOrderDetailList()) {
            // ????????????
            if (ObjectUtil.isNotNull(detailVo.getGiveIntegral()) && detailVo.getGiveIntegral() > 0) {
                gainIntegral += detailVo.getGiveIntegral() * detailVo.getPayNum();
            }
            // ????????????
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

        // ??????????????????
        if (computedOrderPriceResponse.getPayFee().compareTo(BigDecimal.ZERO) > 0) {
            // ??????????????????
            String integralStr = systemConfigService.getValueByKey(SysConfigConstants.CONFIG_KEY_INTEGRAL_RATE_ORDER_GIVE);
            if (StrUtil.isNotBlank(integralStr)) {
                BigDecimal integralBig = new BigDecimal(integralStr);
                int integral = integralBig.multiply(computedOrderPriceResponse.getPayFee()).setScale(0, BigDecimal.ROUND_DOWN).intValue();
                if (integral > 0) {
                    // ????????????
                    gainIntegral += integral;
                }
            }
        }
        // ???????????? ?????????????????????
        int isChannel = 3;

        StoreOrder storeOrder = new StoreOrder();
        storeOrder.setUid(user.getUid());
        storeOrder.setOrderId(orderNo);
        storeOrder.setRealName(request.getRealName());
        storeOrder.setUserPhone(request.getPhone());
        storeOrder.setUserAddress(userAddressStr);
        storeOrder.setTotalNum(orderInfoVo.getOrderProNum());
        storeOrder.setCouponId(Optional.ofNullable(request.getCouponId()).orElse(0));

        // ????????????
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
        // ???????????????
        if (storeOrder.getCouponId() > 0) {
            storeCouponUser = storeCouponUserService.getById(storeOrder.getCouponId());
            storeCouponUser.setStatus(1);
        }
        List<MyRecord> skuRecordList = validateProductStock(orderInfoVo, user);

        StoreCouponUser finalStoreCouponUser = storeCouponUser;
        Boolean execute = transactionTemplate.execute(e -> {
            for (MyRecord skuRecord : skuRecordList) {
                // ?????????????????????
                productService.operationStock(skuRecord.getInt("productId"), skuRecord.getInt("num"), "sub");
                // ???????????????????????????
                storeProductAttrValueService.operationStock(skuRecord.getInt("attrValueId"), skuRecord.getInt("num"), "sub", Constants.PRODUCT_TYPE_NORMAL);
            }
            create(storeOrder);
            storeOrderInfos.forEach(info -> info.setOrderId(storeOrder.getId()));
            // ???????????????
            if (storeOrder.getCouponId() > 0) {
                storeCouponUserService.updateById(finalStoreCouponUser);
            }
            // ???????????????????????????
            storeOrderInfoService.saveOrderInfos(storeOrderInfos);
            // ??????????????????
            storeOrderStatusService.createLog(storeOrder.getId(), Constants.ORDER_STATUS_CACHE_CREATE_ORDER, "????????????");

            // ?????????????????????
            if (CollUtil.isNotEmpty(orderInfoVo.getCartIdList())) {
                storeCartService.deleteCartByIds(orderInfoVo.getCartIdList());
            }
            return Boolean.TRUE;
        });

        if (!execute) {
            throw new BizException("??????????????????");
        }
        HashMap<String, String> map = new HashMap<>();
        map.put("orderNo",storeOrder.getOrderId());
        return map;
    }


    /**
     * ????????????
     * @param status ??????
     * @param pageDTO ??????
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

            // ????????????
            infoResponse.setOrderStatus(getH5OrderStatus(storeOrder));

            // ????????????????????????
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
     * ????????????
     *
     * @param orderId ??????id
     */
    @Override
    public StoreOrderDetailInfoResponse detailOrder(String orderId) {
         Integer uid = UserHolder.getUser().getUid();

        StoreOrderDetailInfoResponse storeOrderDetailResponse = new StoreOrderDetailInfoResponse();

        LambdaQueryWrapper<StoreOrder> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreOrder::getOrderId, orderId);
         StoreOrder storeOrder = dao.selectOne(lqw);

        if (ObjectUtil.isNull(storeOrder) || storeOrder.getIsDel() || storeOrder.getIsSystemDel()) {
            throw new BizException("???????????????");
        }
        if (!storeOrder.getUid().equals(uid)) {
            throw new BizException("???????????????");
        }

        BeanUtils.copyProperties(storeOrder, storeOrderDetailResponse);
        MyRecord orderStatusVo = getOrderStatusVo(storeOrder);

        // ????????????????????????
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

        // ??????????????????
        SystemStore systemStorePram = new SystemStore();
        systemStorePram.setId(storeOrder.getStoreId());
        storeOrderDetailResponse.setSystemStore(systemStoreService.getByCondition(systemStorePram));

        return storeOrderDetailResponse;
    }


    /**
     * ?????????????????????
     * @param uid ??????uid
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
     * ??????????????????
     *
     * @return ?????????????????????
     */
    @Override
    public OrderDataResponse orderData() {

         Integer userId = UserHolder.getUser().getUid();

        OrderDataResponse result = new OrderDataResponse();

        // ????????????
        Integer orderCount = getOrderCountByUid(userId);
        // ??????????????????
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
        // ???????????????
        BigDecimal sumPrice = getSumPayPriceByUid(userId);
        result.setSumPrice(sumPrice);
        // ?????????
        result.setUnPaidCount(unPaidCount);
        // ?????????
        result.setUnShippedCount(getTopDataUtil(Constants.ORDER_STATUS_H5_NOT_SHIPPED, userId));
        // ?????????
        result.setReceivedCount(getTopDataUtil(Constants.ORDER_STATUS_H5_SPIKE, userId));
        // ?????????
        result.setEvaluatedCount(getTopDataUtil(Constants.ORDER_STATUS_H5_JUDGE, userId));
        // ?????????
        result.setCompleteCount(getTopDataUtil(Constants.ORDER_STATUS_H5_COMPLETE, userId));
        // ?????????????????????????????????????????????
        result.setRefundCount(getTopDataUtil(Constants.ORDER_STATUS_H5_REFUNDING, userId));
        return result;
    }

    /**
     * ?????????????????????
     * @param id Integer ??????id
     * @return ????????????
     */
    @Override
    public Boolean delete(Integer id) {
        StoreOrder storeOrder = getById(id);
         Integer uid = UserHolder.getUser().getUid();
        if (ObjectUtil.isNull(storeOrder) || !uid.equals(storeOrder.getUid())) {
            throw new BizException("??????????????????????????????!");
        }
        if (storeOrder.getIsDel() || storeOrder.getIsSystemDel()) {
            throw new BizException("???????????????!");
        }
        if (storeOrder.getPaid()) {
            if (storeOrder.getRefundStatus() > 0 && !storeOrder.getRefundStatus().equals(2)) {
                throw new BizException("????????????????????????????????????!");
            }
            if (storeOrder.getRefundStatus().equals(0) && !storeOrder.getStatus().equals(3)) {
                throw new BizException("???????????????????????????!");
            }
        } else {
            throw new BizException("???????????????????????????!");
        }

        //????????????
        storeOrder.setIsDel(true);
        Boolean execute = transactionTemplate.execute(e -> {
            updateById(storeOrder);
            //??????
            storeOrderStatusService.createLog(storeOrder.getId(), "remove_order", "????????????");
            return Boolean.TRUE;
        });
        return execute;
    }

    /**
     * ????????????????????????
     *
     * @param request ????????????
     * @return Boolean
     */
    @Override
    public Boolean reply(StoreProductReplyAddRequest request) {
        if (StrUtil.isBlank(request.getOrderNo())) {
            throw new BizException("???????????????????????????");
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
     * ??????????????????????????????
     * @return MyRecord
     */
    private MyRecord getOrderStatusVo(StoreOrder storeOrder) {
        MyRecord record = new MyRecord();
        if (!storeOrder.getPaid()) {
            record.set("type", 0);
            record.set("title", "?????????");
            record.set("msg", "???????????????");
            List<String> configKeys = new ArrayList<>();
            configKeys.add("order_cancel_time");
            configKeys.add("order_activity_time");
            configKeys.add("order_bargain_time");
            configKeys.add("order_seckill_time");
            configKeys.add("order_pink_time");
            List<String> configValues = systemConfigService.getValuesByKes(configKeys);
            Date timeSpace;
            timeSpace = JanUtils.addSecond(storeOrder.getCreateTime(),Double.valueOf(configValues.get(0)).intValue() * 3600);
            record.set("msg", "??????" + JanUtils.dateToStr(timeSpace, Constants.DATE_FORMAT) +"???????????????");
        } else if (storeOrder.getRefundStatus() == 1) {
            record.set("type", -1);
            record.set("title", "???????????????");
            record.set("msg", "???????????????,???????????????");
        } else if (storeOrder.getRefundStatus() == 2) {
            record.set("type", -2);
            record.set("title", "?????????");
            record.set("msg", "???????????????,??????????????????");
        } else if (storeOrder.getRefundStatus() == 3) {
            record.set("type", -3);
            record.set("title", "?????????");
            record.set("msg", "??????????????????,??????????????????");
        } else if (storeOrder.getStatus() == 0) {
            record.set("type", 1);
            record.set("title", "?????????");
            record.set("msg", "???????????????,???????????????");
        } else if (storeOrder.getStatus() == 1) { // ???????????????
            // ?????????
            if (null != storeOrder.getDeliveryType() && storeOrder.getDeliveryType().equals(Constants.ORDER_STATUS_STR_SPIKE_KEY)) { // ??????
                StoreOrderStatus storeOrderStatus = new StoreOrderStatus();
                storeOrderStatus.setOid(storeOrder.getId());
                storeOrderStatus.setChangeType(Constants.ORDER_LOG_DELIVERY);
                List<StoreOrderStatus> sOrderStatusResults = storeOrderStatusService.getByEntity(storeOrderStatus);
                if (sOrderStatusResults.size()>0) {
                    record.set("type", 2);
                    record.set("title", "?????????");
                    record.set("msg", "???????????????,???????????????");
                }
            } else if (null != storeOrder.getDeliveryType() && storeOrder.getDeliveryType().equals(Constants.ORDER_LOG_EXPRESS)) {
                StoreOrderStatus storeOrderStatus = new StoreOrderStatus();
                storeOrderStatus.setOid(storeOrder.getId());
                storeOrderStatus.setChangeType(Constants.ORDER_LOG_EXPRESS);
                List<StoreOrderStatus> sOrderStatusResults = storeOrderStatusService.getByEntity(storeOrderStatus);
                if (sOrderStatusResults.size()>0) {
                    record.set("type", 2);
                    record.set("title", "?????????");
                    record.set("msg", "???????????????,???????????????");
                }
            }else {
                StoreOrderStatus storeOrderStatus = new StoreOrderStatus();
                storeOrderStatus.setOid(storeOrder.getId());
                storeOrderStatus.setChangeType(Constants.ORDER_LOG_DELIVERY_VI);
                List<StoreOrderStatus> sOrderStatusResults = storeOrderStatusService.getByEntity(storeOrderStatus);
                if (sOrderStatusResults.size()>0) {
                    record.set("type", 2);
                    record.set("title", "?????????");
                    record.set("msg", "????????????????????????");
                } else {
                    record.set("type", 2);
                    record.set("title", "?????????");
                    record.set("msg", "???????????????????????????");
                }
            }
        }else if (storeOrder.getStatus() == 2) {
            record.set("type", 3);
            record.set("title", "?????????");
            record.set("msg", "?????????,?????????????????????");
        }else if (storeOrder.getStatus() == 3) {
            record.set("type", 4);
            record.set("title", "????????????");
            record.set("msg", "????????????,??????????????????");
        }


        record.set("payTypeStr", "????????????");
        if (StringUtils.isNotBlank(storeOrder.getDeliveryType())) {
            record.set("deliveryType", StringUtils.isNotBlank(storeOrder.getDeliveryType()) ? storeOrder.getDeliveryType():"????????????");
        }

        return record;
    }

    /**
     * ??????H5????????????
     * @param storeOrder ????????????
     */
    private String getH5OrderStatus(StoreOrder storeOrder) {
        if (!storeOrder.getPaid()) {
            return "?????????";
        }
        if (storeOrder.getRefundStatus().equals(1)) {
            return "???????????????";
        }
        if (storeOrder.getRefundStatus().equals(2)) {
            return "?????????";
        }
        if (storeOrder.getRefundStatus().equals(3)) {
            return "?????????";
        }
        if (storeOrder.getStatus().equals(0)) {
            return "?????????";
        }
        if (storeOrder.getStatus().equals(1)) {
            return "?????????";
        }
        if (storeOrder.getStatus().equals(2)) {
            return "?????????";
        }
        if (storeOrder.getStatus().equals(3)) {
            return "?????????";
        }
        return "";
    }



    /**
     * h5 ???????????? where status ??????
     * @param queryWrapper ????????????
     * @param status ??????
     */
    public void statusApiByWhere(LambdaQueryWrapper<StoreOrder> queryWrapper, Integer status){
        switch (status){
            case Constants.ORDER_STATUS_H5_UNPAID: // ?????????
                queryWrapper.eq(StoreOrder::getPaid, false);
                queryWrapper.eq(StoreOrder::getStatus, 0);
                queryWrapper.eq(StoreOrder::getRefundStatus, 0);
                queryWrapper.eq(StoreOrder::getType, 0);
                break;
            case Constants.ORDER_STATUS_H5_NOT_SHIPPED: // ?????????
                queryWrapper.eq(StoreOrder::getPaid, true);
                queryWrapper.eq(StoreOrder::getStatus, 0);
                queryWrapper.eq(StoreOrder::getRefundStatus, 0);
//                queryWrapper.eq(StoreOrder::getShippingType, 1);
                break;
            case Constants.ORDER_STATUS_H5_SPIKE: // ?????????
                queryWrapper.eq(StoreOrder::getPaid, true);
                queryWrapper.eq(StoreOrder::getStatus, 1);
                queryWrapper.eq(StoreOrder::getRefundStatus, 0);
                break;
            case Constants.ORDER_STATUS_H5_JUDGE: //  ????????? ????????? ?????????
                queryWrapper.eq(StoreOrder::getPaid, true);
                queryWrapper.eq(StoreOrder::getStatus, 2);
                queryWrapper.eq(StoreOrder::getRefundStatus, 0);
                break;
            case Constants.ORDER_STATUS_H5_COMPLETE: // ?????????
                queryWrapper.eq(StoreOrder::getPaid, true);
                queryWrapper.eq(StoreOrder::getStatus, 3);
                queryWrapper.eq(StoreOrder::getRefundStatus, 0);
                break;
            case Constants.ORDER_STATUS_H5_REFUNDING: // ?????????
                queryWrapper.eq(StoreOrder::getPaid, true);
                queryWrapper.in(StoreOrder::getRefundStatus, 1, 3);
                break;
            case Constants.ORDER_STATUS_H5_REFUNDED: // ?????????
                queryWrapper.eq(StoreOrder::getPaid, true);
                queryWrapper.eq(StoreOrder::getRefundStatus, 2);
                break;
            case Constants.ORDER_STATUS_H5_REFUND: // ???????????????????????????
                queryWrapper.eq(StoreOrder::getPaid, true);
                queryWrapper.in(StoreOrder::getRefundStatus, 1,2,3);
                break;
        }
        queryWrapper.eq(StoreOrder::getIsDel, false);
        queryWrapper.eq(StoreOrder::getIsSystemDel, false);
    }

    /**
     * ????????????
     * @param storeOrder ????????????
     * @return ????????????
     */
    public boolean create(StoreOrder storeOrder) {
        return dao.insert(storeOrder) > 0;
    }

    /**
     * ????????????????????????????????????
     * @param orderInfoVo ????????????Vo
     * @return List<MyRecord>
     * skuRecord ??????????????????
     * ??????activityId             ????????????id
     * ??????activityAttrValueId    ????????????skuId
     * ??????productId              ?????????????????????id
     * ??????attrValueId            ?????????????????????skuId
     * ??????num                    ????????????
     */
    private List<MyRecord> validateProductStock(OrderInfoVo orderInfoVo, UserDTO user) {
        List<MyRecord> recordList = CollUtil.newArrayList();

        // ????????????
        List<OrderInfoDetailVo> orderDetailList = orderInfoVo.getOrderDetailList();
        orderDetailList.forEach(e -> {
            // ??????????????????
            StoreProduct storeProduct = productService.getById(e.getProductId());
            if (ObjectUtil.isNull(storeProduct)) {
                throw new BizException("??????????????????????????????");
            }
            if (storeProduct.getIsDel()) {
                throw new BizException("????????????????????????");
            }
            if (!storeProduct.getIsShow()) {
                throw new BizException("????????????????????????");
            }
            if (storeProduct.getStock().equals(0) || e.getPayNum() > storeProduct.getStock()) {
                throw new BizException("???????????????????????????");
            }
            // ?????????????????????????????????
            StoreProductAttrValue attrValue = attrValueService.getByIdAndProductIdAndType(e.getAttrValueId(), e.getProductId(), Constants.PRODUCT_TYPE_NORMAL);
            if (ObjectUtil.isNull(attrValue)) {
                throw new BizException("????????????????????????????????????");
            }
            if (attrValue.getStock() < e.getPayNum()) {
                throw new BizException("???????????????????????????");
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
        // ??????????????????
        ComputedOrderPriceResponse priceResponse = new ComputedOrderPriceResponse();
        // ????????????
        if (request.getShippingType().equals(2)) {// ??????????????????????????????
            priceResponse.setFreightFee(BigDecimal.ZERO);
        } else if (ObjectUtil.isNull(request.getAddressId()) || request.getAddressId() <= 0) {
            // ????????????????????????
            priceResponse.setFreightFee(BigDecimal.ZERO);
        } else {// ????????????????????????
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
     * ???????????????????????????
     * @param request ?????????????????????
     * @return OrderInfoVo
     */
    private OrderInfoVo validatePreOrderRequest(PreOrderRequest request, UserDTO user) {
         OrderInfoVo orderInfoVo = new OrderInfoVo();
         List<OrderInfoDetailVo> detailVoList = new ArrayList<>();
        if (request.getPreOrderType().equals("shoppingCart")) { //???????????????
            detailVoList = validatePreOrderShopping(request, user);
           List<Long> cartIdList = request.getOrderDetails().stream()
                    .map(PreOrderDetailRequest::getShoppingCartId)
                    .distinct()
                    .collect(Collectors.toList());
            orderInfoVo.setCartIdList(cartIdList);
        }
        if(request.getPreOrderType().equals("buyNow")) { //????????????
            //?????????????????????????????????
             PreOrderDetailRequest detailRequest = request.getOrderDetails().get(0);
            if (ObjectUtil.isNull(detailRequest.getProductId())) {
                throw new BizException("????????????????????????");
            }
            if (ObjectUtil.isNull(detailRequest.getAttrValueId())) {
                throw new BizException("?????????????????????????????????");
            }
            if (ObjectUtil.isNull(detailRequest.getProductNum()) || detailRequest.getProductNum() < 0) {
                throw new BizException("????????????????????????0");
            }

            //??????????????????
             StoreProduct storeProduct = productService.getById(detailRequest.getProductId());
            if (ObjectUtil.isNull(storeProduct)) {
                throw new BizException("????????????????????????????????????????????????");
            }
            if (storeProduct.getIsDel()) {
                throw new BizException("??????????????????????????????????????????");
            }
            if (!storeProduct.getIsShow()) {
                throw new BizException("??????????????????????????????????????????");
            }
            if (storeProduct.getStock() <detailRequest.getProductNum()) {
                throw new BizException("?????????????????????????????????????????????");
            }
            // ?????????????????????????????????
            StoreProductAttrValue attrValue = attrValueService.getByIdAndProductIdAndType(detailRequest.getAttrValueId(), detailRequest.getProductId(), Constants.PRODUCT_TYPE_NORMAL);
            if (ObjectUtil.isNull(attrValue)) {
                throw new BizException("??????????????????????????????????????????????????????");
            }
            if (attrValue.getStock() < detailRequest.getProductNum()) {
                throw new BizException("???????????????????????????????????????????????????");
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
        if (request.getPreOrderType().equals("again")) {// ????????????
            PreOrderDetailRequest detailRequest = request.getOrderDetails().get(0);
            detailVoList = validatePreOrderAgain(detailRequest, user);
        }
        orderInfoVo.setOrderDetailList(detailVoList);
        return orderInfoVo;

    }

    /**
     * ???????????????????????????
     * @param detailRequest ????????????
     * @return List<OrderInfoDetailVo>
     */
    private List<OrderInfoDetailVo> validatePreOrderAgain(PreOrderDetailRequest detailRequest, UserDTO user) {
         List<OrderInfoDetailVo> detailVoList = new ArrayList<>();

        if (StrUtil.isBlank(detailRequest.getOrderNo())) {
            throw new BizException("????????????????????????????????????");
        }
        StoreOrder storeOrder = getByOrderIdException(detailRequest.getOrderNo());

        if (storeOrder.getRefundStatus() > 0 || storeOrder.getStatus() != 3) {
            throw new BizException("?????????????????????????????????????????????");
        }

        List<StoreOrderInfoVo> infoVoList = storeOrderInfoService.getVoListByOrderId(storeOrder.getId());
        if (CollUtil.isEmpty(infoVoList)) {
            throw new BizException("?????????????????????");
        }
        SystemUserLevel userLevel = null;
        if (user.getLevel()>0) {
            userLevel = systemUserLevelService.getByLevelId(user.getLevel());
        }
        SystemUserLevel finalUserLevel = userLevel;
        infoVoList.forEach(e -> {
            OrderInfoDetailVo detailVo = e.getInfo();
            //??????????????????
             StoreProduct storeProduct = productService.getById(detailVo.getProductId());
            if (ObjectUtil.isNull(storeProduct)) {
                throw new BizException("????????????????????????????????????????????????");
            }
            if (storeProduct.getIsDel()) {
                throw new BizException("??????????????????????????????????????????");
            }
            if (!storeProduct.getIsShow()) {
                throw new BizException("??????????????????????????????????????????");
            }
            if (storeProduct.getStock() < detailVo.getPayNum()) {
                throw new BizException("?????????????????????????????????????????????");
            }
            // ?????????????????????????????????
            StoreProductAttrValue attrValue = attrValueService.getByIdAndProductIdAndType(detailVo.getAttrValueId(), detailVo.getProductId(), Constants.PRODUCT_TYPE_NORMAL);
            if (ObjectUtil.isNull(attrValue)) {
                throw new BizException("??????????????????????????????????????????????????????");
            }
            if (attrValue.getStock() < detailVo.getPayNum()) {
                throw new BizException("???????????????????????????????????????????????????");
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
            throw new BizException("???????????????");
        }
        if (storeOrder.getIsDel() || storeOrder.getIsSystemDel()) {
            throw new BizException("???????????????");
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
     * ????????????
     *
     * @param id ??????id
     * @return Boolean
     */
    @Override
    public Boolean take(Integer id) {
         StoreOrder storeOrder = getInfoById(id);
        if (!storeOrder.getStatus().equals(Constants.ORDER_STATUS_INT_SPIKE)) {
            throw new BizException("??????????????????");
        }
        //?????????????????????
        storeOrder.setStatus(Constants.ORDER_STATUS_INT_BARGAIN);
        boolean result = updateById(storeOrder);
        if (result) {
            //??????????????????redis
//            redisUtil.lPush(TaskConstants.ORDER_TASK_REDIS_KEY_AFTER_TAKE_BY_USER, id);
        }
        return result;
    }

    /**
     * ????????????
     *
     * @param id ??????id
     * @return Boolean
     */
    @Override
    public Boolean cancel(Integer id) {
         StoreOrder storeOrder = getInfoById(id);
        storeOrder.setIsDel(true);
        storeOrder.setIsSystemDel(true);
        boolean result = updateById(storeOrder);

        //??????????????????redis
//        redisUtil.lPush(Constants.ORDER_TASK_REDIS_KEY_AFTER_CANCEL_BY_USER, id);
        return result;
    }



    /**
     * ?????????????????????????????????
     * @param id ??????id
     * @return ????????????????????????
     */
    @Override
    public StoreOrder getInfoById(Integer id) {
        StoreOrder storeOrder = getById(id);
         Integer uid = UserHolder.getUser().getUid();

        if(null == storeOrder || !uid.equals(storeOrder.getUid())){
            //???????????????
            throw new BizException("??????????????????????????????!");
        }

        return storeOrder;
    }

    /**
     * ??????????????????????????????
     *
     * @param orderId ????????????
     * @return ApplyRefundOrderInfoResponse
     */
    @Override
    public ApplyRefundOrderInfoResponse applyRefundOrderInfo(String orderId) {
        StoreOrder storeOrder = getByOrderIdException(orderId);
        ApplyRefundOrderInfoResponse response = new ApplyRefundOrderInfoResponse();
        BeanUtils.copyProperties(storeOrder, response);
        // ????????????????????????
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
     * ??????????????????
     *
     * @param request ????????????
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
            throw new BizException("?????????????????????");
        }
        if (existStoreOrder.getRefundStatus() == 1) {
            throw new BizException("?????????????????????");
        }

        if (existStoreOrder.getRefundStatus() == 2) {
            throw new BizException("???????????????");
        }

        if (existStoreOrder.getRefundStatus() == 3) {
            throw new BizException("???????????????");
        }

        existStoreOrder.setRefundStatus(1);
        existStoreOrder.setRefundReasonTime(JanUtils.nowDateTime());
        existStoreOrder.setRefundReasonWap(request.getText());
        existStoreOrder.setRefundReasonWapExplain(request.getExplain());
        existStoreOrder.setRefundPrice(BigDecimal.ZERO);

        Boolean execute = transactionTemplate.execute(e -> {
            updateById(existStoreOrder);
            storeOrderStatusService.createLog(existStoreOrder.getId(), Constants.ORDER_LOG_REFUND_APPLY, "???????????????????????????" + request.getText());
            return Boolean.TRUE;
        });

//        if (execute) {
//            // ???????????????????????????????????????
//            SystemNotification notification = systemNotificationService.getByMark(NotifyConstants.APPLY_ORDER_REFUND_ADMIN_MARK);
//            if (notification.getIsSms().equals(1)) {
//                // ????????????????????????????????????
//                List<SystemAdmin> systemAdminList = systemAdminService.findIsSmsList();
//                if (CollUtil.isNotEmpty(systemAdminList)) {
//                    SmsTemplate smsTemplate = smsTemplateService.getDetail(notification.getSmsId());
//                    Integer tempId = Integer.valueOf(smsTemplate.getTempId());
//                    // ????????????
//                    systemAdminList.forEach(admin -> {
//                        smsService.sendOrderRefundApplyNotice(admin.getPhone(), existStoreOrder.getOrderId(), admin.getRealName(), tempId);
//                    });
//                }
//            }
//        }
        if (!execute) {
            throw new BizException("??????????????????");
        }
        return execute;
    }

    /**
     * ??????????????????
     *
     * @return ??????????????????
     */
    @Override
    public List<String> getRefundReason() {
        String reasonString = systemConfigService.getValueByKey(SysConfigConstants.CONFIG_KEY_STOR_REASON);
        reasonString = JanUtils.UnicodeToCN(reasonString);
        reasonString = reasonString.replace("rn", "n");
        return Arrays.asList(reasonString.split("\\n"));
    }


        /**
         * ??????????????????
         * @param orderId ??????id
         */
        @Override
        public Object expressOrder(String orderId) {
            HashMap<String,Object> resultMap = new HashMap<>();
            StoreOrder storeOrderPram = new StoreOrder();
            storeOrderPram.setOrderId(orderId);
            StoreOrder existOrder = getByEntityOne(storeOrderPram);
            if (ObjectUtil.isNull(existOrder)) {
                throw new BizException("????????????????????????");
            }
            if (!existOrder.getDeliveryType().equals(Constants.ORDER_LOG_EXPRESS) || StringUtils.isBlank(existOrder.getDeliveryType())) {
                throw new BizException("?????????????????????????????????");
            }

//            if (existOrder.getType().equals(1)) {// ???????????????
//                Express express = expressService.getByName(existOrder.getDeliveryName());
//                if (ObjectUtil.isNotNull(express)) {
//                    existOrder.setDeliveryCode(express.getCode());
//                } else {
//                    existOrder.setDeliveryCode("");
//                }
//            }
            //????????????
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
     * ????????????????????????
     * @param storeOrder ??????
     * @return ????????????
     */
    @Override
    public StoreOrder getByEntityOne(StoreOrder storeOrder) {
        LambdaQueryWrapper<StoreOrder> lqw = new LambdaQueryWrapper<>();
        lqw.setEntity(storeOrder);
        return dao.selectOne(lqw);
    }

    /**
     * ???????????????????????????
     *
     * @param getProductReply ??????????????????
     * @return ?????????
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
     * ????????????????????????
     * @param request ????????????
     * @param user ??????
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
                throw new BizException("???????????????????????????");
            }
            StoreCart storeCart = storeCartService.getByIdAndUid(e.getShoppingCartId(), user.getUid());
            if (ObjectUtil.isNull(storeCart)) {
                throw new BizException("?????????????????????????????????");
            }
            //??????????????????
             StoreProduct storeProduct = productService.getById(storeCart.getProductId());
            if (ObjectUtil.isNull(storeProduct)) {
                throw new BizException("????????????????????????????????????????????????");
            }
            if (storeProduct.getIsDel()) {
                throw new BizException("??????????????????????????????????????????");
            }
            if (!storeProduct.getIsShow()) {
                throw new BizException("??????????????????????????????????????????");
            }
            if (storeProduct.getStock() < storeCart.getCartNum()) {
                throw new BizException("?????????????????????????????????????????????");
            }
            //?????????????????????????????????
            // ?????????????????????????????????
            StoreProductAttrValue attrValue = attrValueService.getByIdAndProductIdAndType(Integer.valueOf(storeCart.getProductAttrUnique()),
                    storeCart.getProductId(),
                    Constants.PRODUCT_TYPE_NORMAL);
            if (ObjectUtil.isNull(attrValue)) {
                throw new BizException("??????????????????????????????????????????????????????");
            }
            if (attrValue.getStock() < storeCart.getCartNum()) {
                throw new BizException("?????????????????????????????????????????????");
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
