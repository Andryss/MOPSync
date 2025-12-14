package ru.itmo.mopsync.iotcontroller.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

/**
 * MongoDB configuration for custom type conversions.
 */
@Configuration
public class MongoConfig {

    /**
     * Custom conversions for MongoDB.
     *
     * @return MongoCustomConversions with OffsetDateTime converters
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                new OffsetDateTimeWriteConverter(),
                new OffsetDateTimeReadConverter()
        ));
    }

    /**
     * Converter to write OffsetDateTime to MongoDB as Date.
     */
    @WritingConverter
    static class OffsetDateTimeWriteConverter implements Converter<OffsetDateTime, Date> {
        @Override
        public Date convert(OffsetDateTime source) {
            return Date.from(source.toInstant());
        }
    }

    /**
     * Converter to read OffsetDateTime from MongoDB Date.
     */
    @ReadingConverter
    static class OffsetDateTimeReadConverter implements Converter<Date, OffsetDateTime> {
        @Override
        public OffsetDateTime convert(Date source) {
            return OffsetDateTime.ofInstant(source.toInstant(), ZoneOffset.UTC);
        }
    }
}
