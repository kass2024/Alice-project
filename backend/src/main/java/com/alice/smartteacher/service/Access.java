package com.alice.smartteacher.service;

import com.alice.smartteacher.model.Enums;
import com.alice.smartteacher.model.UserAccount;
import com.alice.smartteacher.web.ApiException;

public final class Access {
    private Access() {}

    public static boolean managesSchool(UserAccount user) {
        return user.getRole() == Enums.Role.ADMIN || user.getRole() == Enums.Role.HEAD_OF_STUDIES;
    }

    public static void requireAdmin(UserAccount user) {
        if (user.getRole() != Enums.Role.ADMIN) {
            throw new ApiException(403, "Only an admin can do this.");
        }
    }

    public static void requireManager(UserAccount user) {
        if (!managesSchool(user)) {
            throw new ApiException(403, "Only school management can do this.");
        }
    }
}
