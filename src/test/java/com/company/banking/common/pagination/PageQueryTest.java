package com.company.banking.common.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.banking.common.constants.ErrorCode;
import com.company.banking.common.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageQueryTest {

    @Test
    void defaultsMatchOpenApi() {
        PageQuery query = PageQuery.defaults();

        assertThat(query.page()).isEqualTo(0);
        assertThat(query.size()).isEqualTo(20);
        assertThat(query.sort().property()).isEqualTo("createdAt");
        assertThat(query.sort().direction()).isEqualTo(SortDirection.DESC);
        assertThat(query.offset()).isZero();
    }

    @Test
    void ofResolvesProvidedValues() {
        PageQuery query = PageQuery.of(2, 10, "fullName,asc");

        assertThat(query.page()).isEqualTo(2);
        assertThat(query.size()).isEqualTo(10);
        assertThat(query.sort().property()).isEqualTo("fullName");
        assertThat(query.sort().direction()).isEqualTo(SortDirection.ASC);
        assertThat(query.offset()).isEqualTo(20);
    }

    @Test
    void ofRejectsNegativePage() {
        assertThatThrownBy(() -> PageQuery.of(-1, 20, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void ofRejectsSizeOutOfRange() {
        assertThatThrownBy(() -> PageQuery.of(0, 0, null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> PageQuery.of(0, 101, null))
                .isInstanceOf(BusinessException.class);
    }
}

class PageResponseTest {

    @Test
    void ofComputesTotalPagesAndMeta() {
        PageResponse<String> response = PageResponse.of(
                List.of("a", "b"),
                100,
                PageQuery.of(0, 20, null)
        );

        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.totalElements()).isEqualTo(100);
        assertThat(response.totalPages()).isEqualTo(5);
        assertThat(response.currentPage()).isZero();
        assertThat(response.pageSize()).isEqualTo(20);
    }

    @Test
    void contentIsImmutableCopy() {
        List<String> mutable = new java.util.ArrayList<>(List.of("x"));
        PageResponse<String> response = PageResponse.of(mutable, 1, 0, 20);

        mutable.add("y");

        assertThat(response.content()).containsExactly("x");
        assertThatThrownBy(() -> response.content().add("z"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

class SortSpecTest {

    @Test
    void parseUsesDescWhenDirectionOmitted() {
        SortSpec spec = SortSpec.parse("createdAt");

        assertThat(spec.property()).isEqualTo("createdAt");
        assertThat(spec.direction()).isEqualTo(SortDirection.DESC);
    }
}
