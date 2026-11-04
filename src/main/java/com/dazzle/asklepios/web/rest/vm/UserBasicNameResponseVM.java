package com.dazzle.asklepios.web.rest.vm;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserBasicNameResponseVM {

    private String firstName;
    private String lastName;
}
