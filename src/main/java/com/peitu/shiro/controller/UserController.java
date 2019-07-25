package com.peitu.shiro.controller;

import com.peitu.shiro.config.shiro.ShiroSessionListener;
import com.peitu.shiro.domain.User;
import com.peitu.shiro.service.UserService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.apache.shiro.subject.Subject;
import org.crazycake.shiro.RedisSessionDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * @author Rising
 * @date 2019/7/18
 */
@Controller
public class UserController {

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private ShiroSessionListener shiroSessionListener;

    /**
     * 访问项目根路径
     *
     * @return
     */
    @GetMapping(value = "/")
    public String root() {
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        if (user == null) {
            return "redirect:/login";
        } else {
            return "redirect:/user/home";
        }

    }

    /**
     * 登录页
     *
     * @return
     */
    @GetMapping("login")
    public String login(String kickOut, Model model) {
        if ("1".equals(kickOut)) {
            model.addAttribute("msg", "您的账号在其他地方登录，如若不是本人操作，为了您账号安全，请尽快修改密码！");
            return "login";
        }
        Subject subject = SecurityUtils.getSubject();
        User user = (User) subject.getPrincipal();
        if (user == null) {
            return "login";
        } else {
            return "redirect:/user/home";
        }
    }

    @RequestMapping("noAuthority")
    @ResponseBody
    public String authority() {
        return "您没有权限！";
    }

    @RequestMapping("user/home")
    public String home() {
        return "home";
    }

    @RequestMapping("user/permission")
    public String permission() {
        return "permission";
    }

    @RequestMapping("user/add")
    public String add() {
        return "add";
    }

    @RequestMapping("user/product")
    public String product() {
        return "product";
    }

    @Autowired
    RedisSessionDAO redisSessionDAO;

    /**
     * 登录方法
     *
     * @param userName
     * @param password
     * @param model
     * @return
     */
    @PostMapping("login/user")
    public String loginUser(@RequestParam("userName") String userName,
                            @RequestParam("password") String password, Model model) {

        /**
         * 密码加密：
         *     shiro提供的md5加密
         *     Md5Hash:
         *      参数一：加密的内容
         *              111111   --- abcd
         *      参数二：盐（加密的混淆字符串）（用户登录的用户名）
         *              111111+混淆字符串
         *      参数三：加密次数
         *
         */
        password = new Md5Hash(password, userName, 3).toString();
        UsernamePasswordToken token = new UsernamePasswordToken(userName, password);
        Subject subject = SecurityUtils.getSubject();
        subject.login(token);

        User user = (User) subject.getPrincipal();
        String sid = (String) subject.getSession().getId();

        model.addAttribute("sessionId", sid);
        model.addAttribute("user", user);
        model.addAttribute("onlineCount", redisSessionDAO.getActiveSessions().size());

        return "home";

    }


    /**
     * For Test
     *
     * @return
     */
    @RequestMapping("test")
    @ResponseBody
    public String test() {

        return "ok!";
    }

}
