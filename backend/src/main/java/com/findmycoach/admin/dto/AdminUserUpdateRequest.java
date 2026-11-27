package com.findmycoach.admin.dto;

import com.findmycoach.user.Role;

public record AdminUserUpdateRequest(Role role, Boolean enabled) {
}
