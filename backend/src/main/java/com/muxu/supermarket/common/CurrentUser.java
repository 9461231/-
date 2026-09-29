package com.muxu.supermarket.common;

/**
 * 当前登录用户上下文（由 AuthInterceptor 填充）
 */
public class CurrentUser {

    public record UserInfo(Long userId, String username, String role, Long storeId) {
        public boolean hasRole(String... roles) {
            for (String r : roles) {
                if (r.equals(role)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final ThreadLocal<UserInfo> HOLDER = new ThreadLocal<>();

    public static void set(UserInfo user) {
        HOLDER.set(user);
    }

    public static UserInfo get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
