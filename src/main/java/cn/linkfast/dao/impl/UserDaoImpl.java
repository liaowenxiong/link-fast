package cn.linkfast.dao.impl;

import cn.linkfast.dao.UserDao;
import cn.linkfast.entity.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户数据访问实现类
 * 使用内存存储模拟数据库操作
 */
@Repository
public class UserDaoImpl implements UserDao {
    
    // 使用内存存储模拟数据库
    private static final ConcurrentHashMap<Long, User> userStore = new ConcurrentHashMap<>();
    private static final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    public List<User> findAll() {
        return new ArrayList<>(userStore.values());
    }
    
    @Override
    public User findById(Long id) {
        return userStore.get(id);
    }
    
    @Override
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(idGenerator.getAndIncrement());
        }
        userStore.put(user.getId(), user);
        return user;
    }
    
    @Override
    public User update(User user) {
        if (userStore.containsKey(user.getId())) {
            userStore.put(user.getId(), user);
            return user;
        }
        return null;
    }
    
    @Override
    public void delete(Long id) {
        userStore.remove(id);
    }
}