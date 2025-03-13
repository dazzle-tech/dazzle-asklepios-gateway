package com.dazzle.asklepios.web.rest.vm;

import com.dazzle.asklepios.service.dto.AdminUserDTO;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * View Model extending the AdminUserDTO, which is meant to be used in the user management UI.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ManagedUserVM extends AdminUserDTO {

    public static final int PASSWORD_MIN_LENGTH = 4;

    public static final int PASSWORD_MAX_LENGTH = 100;

    @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
    private String password;

    public ManagedUserVM() {
    }
}
