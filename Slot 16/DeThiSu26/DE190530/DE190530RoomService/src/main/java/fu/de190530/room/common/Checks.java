package fu.de190530.room.common;

import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.beans.PropertyDescriptor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import java.util.Arrays;

@Component
public class Checks {
    private final Validator validator;
    public Checks(Validator validator) { this.validator = validator; }
    public void validate(Object dto, boolean create) {
        var errors = create ? validator.validate(dto, Default.class, Create.class)
                            : validator.validate(dto);
        if (!errors.isEmpty()) {
            String message = errors.stream().map(e -> e.getPropertyPath() + ": " + e.getMessage())
                    .sorted().collect(java.util.stream.Collectors.joining("; "));
            throw new ApiException(406, 2, message);
        }
        // Optional strings in a partial update must still be nonblank when supplied.
        var wrapper = new BeanWrapperImpl(dto);
        for (var property : wrapper.getPropertyDescriptors()) {
            if (property.getPropertyType() == String.class) {
                Object value = wrapper.getPropertyValue(property.getName());
                if (value instanceof String s && s.isBlank()) {
                    throw new ApiException(406, 2, property.getName() + " must not be blank");
                }
            }
        }
    }
    public static void merge(Object patch, Object target, String... ignored) {
        var wrapper = new BeanWrapperImpl(patch);
        var skips = new java.util.HashSet<>(Arrays.asList(ignored));
        for (PropertyDescriptor p : wrapper.getPropertyDescriptors()) {
            if (wrapper.getPropertyValue(p.getName()) == null) skips.add(p.getName());
        }
        BeanUtils.copyProperties(patch, target, skips.toArray(String[]::new));
    }
    public static PageRequest page(int page, int size, String id) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ApiException(406, 2, "page must be >= 0 and size must be between 1 and 100");
        }
        return PageRequest.of(page, size, Sort.by(id));
    }
    public static <E extends Enum<E>> E filter(String value, Class<E> type) {
        if (value == null) return null;
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException e) { throw new ApiException(406, 2, "Invalid " + type.getSimpleName()); }
    }
}
