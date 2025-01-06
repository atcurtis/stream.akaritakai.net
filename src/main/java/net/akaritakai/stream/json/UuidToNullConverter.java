package net.akaritakai.stream.json;

import com.fasterxml.jackson.databind.util.StdConverter;

import java.util.UUID;


public class UuidToNullConverter extends StdConverter<UUID, String> {
  @Override
  public String convert(UUID value) {
    return null;
  }
}
