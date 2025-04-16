package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    /**
     * 插入用户信息
     * @param user
     */
    void insert(User user);

    /**
     * 根据openId查询用户信息
     * @param openId
     */
    @Select("SELECT * FROM user WHERE openid = #{openId}")
    User selectByOpenId(String openId);

}
