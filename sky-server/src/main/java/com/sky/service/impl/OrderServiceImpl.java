package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Resource
    private AddressBookMapper addressBookMapper;

    @Resource
    private ShoppingCartMapper shoppingCartMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private WeChatPayUtil weChatPayUtil;

    @Resource
    private WebSocketServer webSocketServer;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {

        //处理各种业务异常(地址簿为空, 购物车数据为空)
        //查询地址簿
        AddressBook addressBook = addressBookMapper.selectOne(Wrappers.lambdaQuery(AddressBook.class)
                .eq(AddressBook::getUserId, BaseContext.getCurrentId())
                .eq(AddressBook::getId, ordersSubmitDTO.getAddressBookId()));
        if (ObjectUtils.isEmpty(addressBook)) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        //查询购物车
        List<ShoppingCart> shoppingCarts = shoppingCartMapper.selectList(Wrappers.lambdaQuery(ShoppingCart.class)
                .eq(ShoppingCart::getUserId, BaseContext.getCurrentId()));
        if (ObjectUtils.isEmpty(shoppingCarts)) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //查询用户
        User user = userMapper.selectById(BaseContext.getCurrentId());
        //向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        String orderNumber = String.valueOf(System.currentTimeMillis());
        orders.setNumber(orderNumber);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setUserId(BaseContext.getCurrentId());
        LocalDateTime orderTime = LocalDateTime.now();
        orders.setOrderTime(orderTime);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setPhone(addressBook.getPhone());
        orders.setAddress(addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orderMapper.insert(orders);
        //向订单明细插入n条数据
        ArrayList<Long> shoppingCartIds = new ArrayList<>();
        shoppingCarts.forEach(shoppingCart -> {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart, orderDetail, "id");
            //设置订单id
            orderDetail.setOrderId(orders.getId());
            orderDetailMapper.insert(orderDetail);
            //当前的购物车id添加到list准备删除
            shoppingCartIds.add(shoppingCart.getId());
        });
        //清空该用户的购物车的数据
        shoppingCartMapper.deleteBatchIds(shoppingCartIds);
        //封装vo
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderAmount(orders.getAmount())
                .orderNumber(orderNumber)
                .orderTime(orderTime)
                .build();
    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.selectById(userId);

        //调用微信支付接口，生成预支付交易单
        //TODO 跳过微信支付
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
        this.paySuccess(ordersPaymentDTO.getOrderNumber());
        JSONObject jsonObject = new JSONObject();
        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
        //支付成功, 通过websocket向客户端浏览器推送消息type, orderId, content
        Map<String, Object> map = new HashMap<>();
        map.put("type", 1);//1表示来单提醒, 2表示客户催单
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号: " + outTradeNo);
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    @Override
    public OrderVO orderDetail(Long id) {
        //查询订单
        Orders orders = orderMapper.selectOne(Wrappers.lambdaQuery(Orders.class)
                .eq(Orders::getId, id)
                .eq(Orders::getUserId, BaseContext.getCurrentId()));
        //查询订单明细
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(Wrappers.lambdaQuery(OrderDetail.class)
                .eq(OrderDetail::getOrderId, orders.getId()));
        //查询用户
        User user = userMapper.selectById(BaseContext.getCurrentId());
        //封装vo
        OrderVO orderDetailVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderDetailVO);
        orderDetailVO.setOrderDetailList(orderDetails);
        orderDetailVO.setUserid(user.getId());
        orderDetailVO.setUserName(user.getName());
        return orderDetailVO;
    }

    /**
     * 历史订单查询
     *
     * @param ordersHistoryPageQueryDTO
     * @return
     */
    @Override
    public PageResult historyOrders(OrdersHistoryPageQueryDTO ordersHistoryPageQueryDTO) {
        //分页查询订单
        PageHelper.startPage(ordersHistoryPageQueryDTO.getPage(), ordersHistoryPageQueryDTO.getPageSize());
        Integer status = ordersHistoryPageQueryDTO.getStatus();
        List<Orders> orders = orderMapper.selectList(Wrappers.lambdaQuery(Orders.class)
                .eq(status != null, Orders::getStatus, status)
                .eq(Orders::getUserId, BaseContext.getCurrentId()));
        PageInfo<Orders> ordersPageInfo = new PageInfo<>(orders);
        //查询订单明细
        List<Long> orderIds = new ArrayList<>(10);
        orders.forEach(order -> orderIds.add(order.getId()));
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(Wrappers.lambdaQuery(OrderDetail.class)
                .in(OrderDetail::getOrderId, orderIds));
        //封装Vo到list
        List<OrderVO> orderVOS = new ArrayList<>();
        orders.forEach(order -> {
            OrderVO orderVO = new OrderVO();
            List<OrderDetail> collect = orderDetails.stream()
                    .filter(orderDetail -> orderDetail.getOrderId().equals(order.getId()))
                    .collect(Collectors.toList());
            //封装vo
            BeanUtils.copyProperties(order, orderVO);
            orderVO.setOrderDetailList(collect);
            orderVOS.add(orderVO);
        });
        return new PageResult(ordersPageInfo.getTotal(), orderVOS);
    }

    /**
     * 订单搜索
     *
     * @param dto
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO dto) {
        //开始分页
        PageHelper.startPage(dto.getPage(), dto.getPageSize());
        //查询
        boolean orderTimeCondition = dto.getBeginTime() != null && dto.getEndTime() != null;
        List<Orders> orders = orderMapper.selectList(Wrappers.lambdaQuery(Orders.class)
                .like(dto.getNumber() != null, Orders::getNumber, dto.getNumber())
                .like(dto.getPhone() != null, Orders::getPhone, dto.getPhone())
                .eq(dto.getStatus() != null, Orders::getStatus, dto.getStatus())
                .between(orderTimeCondition, Orders::getOrderTime, dto.getBeginTime(), dto.getEndTime()));
        PageInfo<Orders> ordersPageInfo = new PageInfo<>(orders);
        //封装数据
        return new PageResult(ordersPageInfo.getTotal(), orders);
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        //查询待派送数量
        Long confirmed = orderMapper.selectCount(Wrappers.lambdaQuery(Orders.class)
                .eq(Orders::getStatus, Orders.CONFIRMED));
        //查询派送中数量
        Long deliveryInProgress = orderMapper.selectCount(Wrappers.lambdaQuery(Orders.class)
                .eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS));
        //查询待接单数量
        Long toBeConfirmed = orderMapper.selectCount(Wrappers.lambdaQuery(Orders.class)
                .eq(Orders::getStatus, Orders.TO_BE_CONFIRMED));
        //封装vo
        return OrderStatisticsVO.builder()
                .confirmed(Math.toIntExact(confirmed))
                .deliveryInProgress(Math.toIntExact(deliveryInProgress))
                .toBeConfirmed(Math.toIntExact(toBeConfirmed))
                .build();
    }

    /**
     * 客户催单
     * @param id
     */
    @Override
    public void reminder(Long id) {
        Orders orders = orderMapper.selectById(id);
        if (ObjectUtils.isEmpty(orders)){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //用户催单, 通过websocket向客户端浏览器推送消息type, orderId, content
        Map<String, Object> map = new HashMap<>();
        map.put("type", 2);//1表示来单提醒, 2表示客户催单
        map.put("orderId", id);
        map.put("content", "订单号: " + orders.getNumber());
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 用户取消订单
     * @param id
     */
    @Override
    public void cancel(Long id) {
        Orders orders = orderMapper.selectById(id);
        if (ObjectUtils.isEmpty(orders)){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason("用户取消订单");
        orderMapper.updateById(orders);
    }

    /**
     * 用户再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
        Orders orders = orderMapper.selectById(id);
        if (ObjectUtils.isEmpty(orders)){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(Wrappers.lambdaQuery(OrderDetail.class)
                .eq(OrderDetail::getOrderId, orders.getId()));
        orderDetails.forEach(orderDetail -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart, "id");
            //user_id
            shoppingCart.setUserId(orders.getUserId());
            shoppingCartMapper.insert(shoppingCart);
        });
    }

    /**
     * 管理端取消订单
     * @param dto
     */
    @Override
    public void cancel(OrdersCancelDTO dto) {
        Orders orders = orderMapper.selectById(dto.getId());
        if (ObjectUtils.isEmpty(orders)){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(dto.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(orders);
    }

    /**
     * 接单
     * @param dto
     */
    @Override
    public void confirm(OrdersConfirmDTO dto) {
        Orders orders = orderMapper.selectById(dto.getId());
        if (ObjectUtils.isEmpty(orders)){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.updateById(orders);
    }

    /**
     * 派送订单
     * @param id
     */
    @Override
    public void delivery(Long id) {
        Orders orders = orderMapper.selectById(id);
        if (ObjectUtils.isEmpty(orders)){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.updateById(orders);
    }

    /**
     * 完成订单
     * @param id
     */
    @Override
    public void complete(Long id) {
        Orders orders = orderMapper.selectById(id);
        if (ObjectUtils.isEmpty(orders)){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        orders.setStatus(Orders.COMPLETED);
        orderMapper.updateById(orders);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orders = orderMapper.selectById(ordersRejectionDTO.getId());
        if (ObjectUtils.isEmpty(orders)){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orderMapper.updateById(orders);
    }
}
