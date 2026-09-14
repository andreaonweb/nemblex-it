package com.nemblex.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PagedResponseTest {

    @Test
    void from_shouldCopyContentAndPageMetadata() {
        List<String> content = List.of("a", "b", "c");
        PageImpl<String> page = new PageImpl<>(content, PageRequest.of(1, 3), 10);

        PagedResponse<String> result = PagedResponse.from(page);

        assertThat(result.getContent()).containsExactly("a", "b", "c");
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getTotalElements()).isEqualTo(10);
        assertThat(result.getTotalPages()).isEqualTo(4);
    }

    @Test
    void from_shouldHandleEmptyPage() {
        PageImpl<String> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        PagedResponse<String> result = PagedResponse.from(page);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }
}
