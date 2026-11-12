package com.runwise.admin;

import com.runwise.common.PasswordHasher;

public final class AdminPassword {
    private AdminPassword() { }
    public static String hash(String password) { return PasswordHasher.hash(password); }
    public static boolean matches(String password,String stored) { return PasswordHasher.matches(password,stored); }
}
