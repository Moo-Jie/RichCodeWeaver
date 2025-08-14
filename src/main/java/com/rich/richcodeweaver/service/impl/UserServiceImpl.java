package com.rich.richcodeweaver.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.rich.richcodeweaver.constant.UserConstant;
import com.rich.richcodeweaver.exception.BusinessException;
import com.rich.richcodeweaver.exception.ErrorCode;
import com.rich.richcodeweaver.model.dto.user.UserQueryRequest;
import com.rich.richcodeweaver.model.entity.User;
import com.rich.richcodeweaver.mapper.UserMapper;
import com.rich.richcodeweaver.model.enums.UserRoleEnum;
import com.rich.richcodeweaver.model.vo.LoginUserVO;
import com.rich.richcodeweaver.model.vo.UserVO;
import com.rich.richcodeweaver.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.rich.richcodeweaver.constant.UserConstant.*;

/**
 * 用户服务层实现
 *
 * @author DuRuiChi
 * @create 2025/8/5
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    // 加密盐值
    private static final String PASSWORD_SALT = "rich";

    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @return 新注册用户的ID
     * @throws BusinessException 参数错误或业务异常
     **/
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 参数基础校验
        validateAccount(userAccount);
        validatePassword(userPassword, checkPassword);

        // 2. 检查账号唯一性
        checkAccountUnique(userAccount);

        // 3. 密码加密存储
        String encryptPassword = encryptPassword(userPassword);

        // 4. 构建用户对象并存储
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName(DEFAULT_USER_NAME);
        user.setUserRole(UserRoleEnum.USER.getValue());
        user.setUserAvatar(APP_AVATAR);
        user.setUserProfile(DEFAULT_PROFILE);

        if (!this.save(user)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户注册失败");
        }
        return user.getId();
    }

    /**
     * 构造登录用户视图对象
     *
     * @param user 用户实体对象
     * @return 登录用户VO对象，输入为空时返回null
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      HTTP请求对象
     * @return 脱敏后的登录用户信息
     * @throws BusinessException 参数错误或认证失败
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 参数基础校验
        validateAccount(userAccount);
        if (StrUtil.isBlank(userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }

        // 2. 密码加密并验证
        String encryptPassword = encryptPassword(userPassword);
        User user = validateUserCredentials(userAccount, encryptPassword);

        // 3. 存储登录状态并返回脱敏信息
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return getLoginUserVO(user);
    }

    /**
     * 获取当前登录用户
     *
     * @param request HTTP请求对象
     * @return 当前登录用户实体
     * @throws BusinessException 未登录或登录态无效
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1. 从session获取登录态
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 2. 基础类型校验
        if (!(userObj instanceof User)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "登录状态异常");
        }

        // 3. 查询最新用户信息
        User currentUser = (User) userObj;
        return this.getByIdOptional(currentUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户不存在"));
    }

    /**
     * 获取用户视图对象
     *
     * @param user 用户实体
     * @return 脱敏后的用户VO，输入为空时返回null
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 用户列表转换为视图对象列表
     *
     * @param userList 用户实体列表
     * @return 脱敏后的用户VO列表（永远非null）
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .filter(vo -> vo != null) // 过滤掉转换失败的空对象
                .collect(Collectors.toList());
    }

    /**
     * 用户退出登录
     *
     * @param request HTTP请求对象
     * @return 总是返回true
     * @throws BusinessException 用户未登录
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 检查登录状态
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未登录");
        }
        // 清除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 构建查询条件包装器
     *
     * @param userQueryRequest 查询参数对象
     * @return 查询条件包装器
     * @throws BusinessException 参数为空
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询参数不能为空");
        }

        // 查询条件
        Long id = userQueryRequest.getId();
        String userAccount = StrUtil.trimToNull(userQueryRequest.getUserAccount());
        String userName = StrUtil.trimToNull(userQueryRequest.getUserName());
        String userProfile = StrUtil.trimToNull(userQueryRequest.getUserProfile());
        String userRole = StrUtil.trimToNull(userQueryRequest.getUserRole());
        String sortField = StrUtil.trimToNull(userQueryRequest.getSortField());
        String sortOrder = StrUtil.trimToNull(userQueryRequest.getSortOrder());

        QueryWrapper queryWrapper = QueryWrapper.create();
        // 精确匹配
        if (id != null) {
            queryWrapper.eq("id", id);
        }
        if (userRole != null) {
            queryWrapper.eq("userRole", userRole);
        }
        // 模糊查询
        if (userAccount != null) {
            queryWrapper.like("userAccount", userAccount);
        }
        if (userName != null) {
            queryWrapper.like("userName", userName);
        }
        if (userProfile != null) {
            queryWrapper.like("userProfile", userProfile);
        }
        // 排序条件
        if (sortField != null && sortOrder != null) {
            boolean isAsc = "ascend".equalsIgnoreCase(sortOrder);
            queryWrapper.orderBy(sortField, isAsc);
        }
        return queryWrapper;
    }

    /**
     * 密码加密
     *
     * @param userPassword 明文密码
     * @return MD5加密后的密码
     * @throws BusinessException 密码为空
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    @Override
    public String getEncryptPassword(String userPassword) {
        if (StrUtil.isBlank(userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }
        String input = userPassword + PASSWORD_SALT;
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 验证用户账号合规性
     *
     * @param account
     * @return void
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    private void validateAccount(String account) {
        if (StrUtil.isBlank(account)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能为空");
        }
        if (account.length() < MIN_ACCOUNT_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    String.format("账号长度至少为%d位", MIN_ACCOUNT_LENGTH));
        }
    }

    /**
     * 验证密码合规性
     *
     * @param password
     * @param checkPassword
     * @return void
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    private void validatePassword(String password, String checkPassword) {
        if (StrUtil.hasBlank(password, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }
        if (password.length() < MIN_PASSWORD_LENGTH || checkPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    String.format("密码长度至少为%d位", MIN_PASSWORD_LENGTH));
        }
        if (!password.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
    }

    /**
     * 检查账号唯一性
     *
     * @param account
     * @return void
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    private void checkAccountUnique(String account) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userAccount", account)
                .select("id");
        if (mapper.selectCountByQuery(queryWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已被注册");
        }
    }

    /**
     * 验证用户凭证
     *
     * @param account
     * @param encryptedPassword
     * @return com.rich.richcodeweaver.model.entity.User
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    private User validateUserCredentials(String account, String encryptedPassword) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userAccount", account)
                .eq("userPassword", encryptedPassword);
        return this.getOneOpt(queryWrapper)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误"));
    }

    /**
     * 安全获取用户信息（Optional包装）
     *
     * @param id
     * @return java.util.Optional<com.rich.richcodeweaver.model.entity.User>
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    private Optional<User> getByIdOptional(Long id) {
        return Optional.ofNullable(id)
                .map(this::getById);
    }

    /**
     * 密码加密方法
     *
     * @param password
     * @return java.lang.String
     * @author DuRuiChi
     * @create 2025/8/5
     **/
    private String encryptPassword(String password) {
        if (StrUtil.isBlank(password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }
        return getEncryptPassword(password);
    }
}