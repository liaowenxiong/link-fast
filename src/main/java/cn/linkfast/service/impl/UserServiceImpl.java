package cn.linkfast.service.impl;

import cn.linkfast.dao.UserDao;
import cn.linkfast.entity.User;
import cn.linkfast.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务实现类
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserDao userDao;
    
    @Override
    public List<User> findAllUsers() {
        return userDao.findAll();
    }
    
    @Override
    public User findUserById(Long id) {
        return userDao.findById(id);
    }
    
    @Override
    public User createUser(User user) {
        return userDao.save(user);
    }
    
    @Override
    public User updateUser(User user) {
        User existingUser = userDao.findById(user.getId());
        if (existingUser != null) {
            return userDao.update(user);
        }
        return null;
    }
    
    @Override
    public boolean deleteUser(Long id) {
        User user = userDao.findById(id);
        if (user != null) {
            userDao.delete(id);
            return true;
        }
        return false;
    }
}