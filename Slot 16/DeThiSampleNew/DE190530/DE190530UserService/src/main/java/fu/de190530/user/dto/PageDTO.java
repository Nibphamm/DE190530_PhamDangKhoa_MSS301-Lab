package fu.de190530.user.dto;

import org.springframework.data.domain.Page;

public record PageDTO(int size, int page, int totalPages, long totalElements,
                      boolean first, boolean last, Object content) {
    public static PageDTO of(Page<?> page, Object content) {
        return new PageDTO(page.getSize(), page.getNumber(), page.getTotalPages(),
                page.getTotalElements(), page.isFirst(), page.isLast(), content);
    }
}
