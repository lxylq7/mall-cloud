package com.lxylq7.auth;

/**
 * 当前用户上下文
 */
public class CurrentUserContext {

    private static final ThreadLocal<CurrentUser> TL = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        TL.set(user);
    }

    public static CurrentUser get() {
        return TL.get();
    }

    public static void clear() {
        TL.remove();
    }

    public static class CurrentUser {
        private long userId;
        private String username;

        public long getUserId() {
            return userId;
        }

        public void setUserId(long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}
