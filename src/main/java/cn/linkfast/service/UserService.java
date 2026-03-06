package cn.linkfast.service;

import cn.linkfast.entity.User;
import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 查找所有用户
     */
    List<User> findAllUsers();
    
    /**
     *根据ID查找用户
     */
    User findUserById(Long id);
    
    /**
     * 创建用户
     */
    User createUser(User user);
    
    /**
     * 更新用户
     */
    User updateUser(User user);
    
    /**
     * 删除用户
     */
    boolean deleteUser(Long id);
}