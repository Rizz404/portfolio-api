package com.api.rizz.portfolio_api.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// * Biar Spring Boot mengenali ini sebagai Bean yang bisa dipanggil di mana aja
@Component
public class SnowflakeGenerator {

  private static final long EPOCH = 1704067200000L; // 2024-01-01T00:00:00Z
  private static final long DATACENTER_ID_BITS = 5L;
  private static final long WORKER_ID_BITS = 5L;
  private static final long SEQUENCE_BITS = 12L;

  private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
  private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

  private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
  private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
  private static final long TIMESTAMP_LEFT_SHIFT =
      SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
  private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

  private final long datacenterId;
  private final long workerId;

  private long sequence = 0L;
  private long lastTimestamp = -1L;

  public SnowflakeGenerator(
      @Value("${snowflake.datacenter-id:1}") long datacenterId,
      @Value("${snowflake.worker-id:1}") long workerId) {
    if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
      throw new IllegalArgumentException("datacenterId must be between 0 and " + MAX_DATACENTER_ID);
    }
    if (workerId < 0 || workerId > MAX_WORKER_ID) {
      throw new IllegalArgumentException("workerId must be between 0 and " + MAX_WORKER_ID);
    }

    this.datacenterId = datacenterId;
    this.workerId = workerId;
  }

  public synchronized long nextId() {
    long timestamp = System.currentTimeMillis();

    if (timestamp < lastTimestamp) {
      throw new IllegalStateException(
          "Clock moved backwards. Refusing to generate id for "
              + (lastTimestamp - timestamp)
              + " ms");
    }

    if (timestamp == lastTimestamp) {
      sequence = (sequence + 1) & SEQUENCE_MASK;
      if (sequence == 0) {
        timestamp = waitNextMillis(lastTimestamp);
      }
    } else {
      sequence = 0L;
    }

    lastTimestamp = timestamp;

    return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
        | (datacenterId << DATACENTER_ID_SHIFT)
        | (workerId << WORKER_ID_SHIFT)
        | sequence;
  }

  private long waitNextMillis(long lastTimestamp) {
    long timestamp = System.currentTimeMillis();
    while (timestamp <= lastTimestamp) {
      timestamp = System.currentTimeMillis();
    }
    return timestamp;
  }
}
