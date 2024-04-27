package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.json.JacksonObjectMapper;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private WeChatProperties weChatProperties;

    private static final String WxLoginApiUrl = "https://api.weixin.qq.com/sns/jscode2session";


    private String name;
    private String sex;
    private String avatar;
    /**
     * 微信登录
     *
     * @param userLoginDTO
     * @return
     */
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        //
        String openid = getOpenId(userLoginDTO.getCode());
        //判断openid是否为空, 如果为空表示登录失败, 抛出业务失败
        if (!StringUtils.hasText(openid)) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //判断是否为新的用户, 如果是新用户添加到用户表, 存入openid
        User user = userMapper.selectOne(Wrappers.lambdaQuery(User.class)
                .eq(User::getOpenid, openid));
        //如果是新对象,自动完成注册
        if (ObjectUtils.isEmpty(user)) {
            user = User.builder()
                    .openid(openid)
                    .name(this.name)
//                    .phone(jsonObject.getString("phone"))
                    .sex(this.sex)
//                    .idNumber(jsonObject.getString("idNumber"))
                    .avatar(this.avatar)
                    .build();
            userMapper.insert(user);
        }
        //返回这个对象
        return user;
    }

    /**
     * 根据授权码获取openid
     * @param code
     * @return
     */
    private String getOpenId(String code){
        //调用微信接口服务,获得当前微信用户的openid https://api.weixin.qq.com/sns/jscode2session
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WxLoginApiUrl, map);

        JSONObject jsonObject = JSON.parseObject(json);
        //设置name, sex, avatar
        this.name = jsonObject.getString("nickName");
        this.sex = jsonObject.getString("gender");
        this.avatar = jsonObject.getString("avatarUrl");
        return jsonObject.getString("openid");
    }
}
