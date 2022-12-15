package org.jiang.shpping.utils;

public class Constants {

    //升序排序
    public static final String SORT_ASC = "asc";

    public static final int ORDER_STATUS_H5_UNPAID = 0; // 未支付
    public static final int ORDER_STATUS_H5_NOT_SHIPPED = 1; // 待发货
    public static final int ORDER_STATUS_H5_SPIKE = 2; // 待收货
    public static final int ORDER_STATUS_H5_JUDGE = 3; // 待评价
    public static final int ORDER_STATUS_H5_COMPLETE = 4; // 已完成
    public static final int ORDER_STATUS_H5_VERIFICATION = 5; // 待核销
    public static final int ORDER_STATUS_H5_REFUNDING = -1; // 退款中
    public static final int ORDER_STATUS_H5_REFUNDED = -2; // 已退款
    public static final int ORDER_STATUS_H5_REFUND = -3; // 退款

    public static final int ORDER_STATUS_INT_PAID = 0; //已支付
    public static final int ORDER_STATUS_INT_SPIKE = 1; //待收货
    public static final int ORDER_STATUS_INT_BARGAIN = 2; //已收货，待评价
    public static final int ORDER_STATUS_INT_COMPLETE = 3; //已完成

    //订单操作类型
    public static final String ORDER_STATUS_STR_SPIKE_KEY = "send"; //待收货 KEY
    public static final String ORDER_LOG_REFUND_PRICE = "refund_price"; //退款
    public static final String ORDER_LOG_EXPRESS = "express"; //快递
    public static final String ORDER_LOG_DELIVERY = "delivery"; //送货
    public static final String ORDER_LOG_DELIVERY_GOODS = "delivery_goods"; //送货
    public static final String ORDER_LOG_REFUND_REFUSE = "refund_refuse"; //不退款
    public static final String ORDER_LOG_REFUND_APPLY = "apply_refund"; //
    public static final String ORDER_LOG_PAY_SUCCESS = "pay_success"; //支付成功
    public static final String ORDER_LOG_DELIVERY_VI = "delivery_fictitious"; //虚拟发货
    public static final String ORDER_LOG_EDIT = "order_edit"; //编辑订单
    public static final String ORDER_LOG_PAY_OFFLINE = "offline"; //线下付款订单


    //降序排序
    public static final String SORT_DESC = "desc";

    //城市数据 tree redis key
    public static final String CITY_LIST_TREE = "city_list_tree";
    //默认分页
    public static final int DEFAULT_PAGE = 1;

    public static final long ORDER_CASH_CONFIRM = (60);
    //默认分页
    public static final int DEFAULT_LIMIT = 6;

    public static final String DATE_FORMAT_MONTH = "yyyy-MM";
    public static final String DATE_FORMAT_DATE = "yyyy-MM-dd";
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    //用户等级升级
    public static final String USER_LEVEL_OPERATE_LOG_MARK = "尊敬的用户 【{$userName}】, 在{$date}赠送会员等级成为{$levelName}会员";
    public static final String USER_LEVEL_UP_LOG_MARK = "尊敬的用户 【{$userName}】, 在{$date}您升级为为{$levelName}会员";
    public static final String ORDER_STATUS_CACHE_CREATE_ORDER = "cache_key_create_order";

    //签到
    public static final Integer SIGN_TYPE_INTEGRAL = 1; //积分
    public static final Integer SIGN_TYPE_EXPERIENCE = 2; //经验
    public static final String SIGN_TYPE_INTEGRAL_TITLE = "签到积分奖励"; //积分
    public static final String SIGN_TYPE_EXPERIENCE_TITLE = "签到经验奖励"; //经验

    //商品类型
    public static final Integer PRODUCT_TYPE_NORMAL = 0;
}
