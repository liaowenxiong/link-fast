package cn.linkfast.dao;

import cn.linkfast.entity.User;
import java.util.List;

/**
 * 用户数据访问接口
 */
public interface UserDao {
    
    /**
     * 查找所有用户
     */
    List<User> findAll();
    
    /**
     *根据ID查找用户
     */
    User findById(Long id);
    
    /**
     * 保存用户
     */
    User save(User user);
    
    /**
     * 更新用户
     */
    User update(User user);
    
    /**
     * 删除用户
     */
    void delete(Long id);
}