package com.finly.backend.admin.views;

import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

final class AdminGridUtil {

    private AdminGridUtil() {
    }

    static Pageable pageable(Query<?, ?> query, String defaultSortProperty) {
        int page = query.getOffset() / query.getLimit();
        int size = query.getLimit();
        Sort sort = toSort(query.getSortOrders(), defaultSortProperty);
        return PageRequest.of(page, size, sort);
    }

    private static Sort toSort(List<QuerySortOrder> sortOrders, String defaultSortProperty) {
        if (sortOrders == null || sortOrders.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, defaultSortProperty);
        }

        QuerySortOrder first = sortOrders.get(0);
        Sort.Direction direction = first.getDirection() == SortDirection.ASCENDING ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, first.getSorted());
    }
}
