package com.realestate.config;

import com.realestate.entity.Property;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Binds query/form {@code propertyType} values, including legacy {@code APARTMENT} → {@code FLAT}.
 */
@Component
public class PropertyTypeConverter implements Converter<String, Property.PropertyType> {

    @Override
    public Property.PropertyType convert(String source) {
        return Property.PropertyType.fromApi(source);
    }
}
