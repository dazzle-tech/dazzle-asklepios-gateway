package com.dazzle.asklepios.web.rest.util;


import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

public final class PaginationUtil {
    private static final String HEADER_X_TOTAL_COUNT = "X-Total-Count";


    private PaginationUtil() {
    }
    public static <T> HttpHeaders generatePaginationHttpHeaders(UriComponentsBuilder uriBuilder, Page<T> page) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", Long.toString(page.getTotalElements()));
        return headers;
    }

}
