/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.media3.datasource;

import androidx.annotation.Nullable;
import androidx.media3.common.util.Util;
import java.io.IOException;

/**
 * A {@link DataSource} that reads a clipped portion of an ISO 14496-12 file.
 */
public class IsoDataSource implements DataSource {

  private final DataSource delegate;
  private final long clipOffset;
  private final long clipLength;
  private final boolean allowCrossPartitionReads;
  private final boolean allowPartialReads;

  @Nullable private byte[] singleBuffer;

  public IsoDataSource(
      DataSource delegate,
      long clipOffset,
      long clipLength,
      boolean allowCrossPartitionReads,
      boolean allowPartialReads) {
    this.delegate = delegate;
    this.clipOffset = clipOffset;
    this.clipLength = clipLength;
    this.allowCrossPartitionReads = allowCrossPartitionReads;
    this.allowPartialReads = allowPartialReads;
  }

  @Override
  public long open(DataSpec dataSpec) throws IOException {
    long offset = clipOffset + dataSpec.uriOffset;
    long length = Math.min(dataSpec.length, clipLength - dataSpec.uriOffset);
    if (length <= 0) {
      return 0;
    }
    DataSpec clippedDataSpec =
        new DataSpec.Builder()
            .setUri(dataSpec.uri)
            .setFlags(dataSpec.flags)
            .setOffset(offset)
            .setLength(length)
            .setKey(documentation)
            .build();
    return delegate.open(clippedDataSpec);
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    int bytesToRead =
        allowPartialReads
            ? length
            : Math.min(length, (int) Math.min(clipLength, delegate.getContentLength() - getBytesRead()));
    if (bytesToRead <= 0) {
      return C.RESULT_END_OF_INPUT;
    }
    int bytesRead = delegate.read(buffer, offset, bytesToRead);
    if (bytesRead == C.RESULT_END_OF_INPUT && !allowPartialReads) {
      return C.RESULT_END_OF_INPUT;
    }
    return bytesRead;
  }

  @Override
  @Nullable
  public Uri getUri() {
    return delegate.getUri();
  }

  @Override
  public void close() {
    delegate.close();
  }

  /** Factory for {@link IsoDataSource}. */
  public static class Factory implements DataSource.Factory {
    private final DataSource.Factory delegateFactory;
    private final long clipOffset;
    private final long clipLength;
    private final boolean allowCrossPartitionReads;
    private final boolean allowPartialReads;

    public Factory(DataSource.Factory delegateFactory, long clipOffset, long clipLength) {
      this(delegateFactory, clipOffset, clipLength, false, false);
    }

    public Factory(
        DataSource.Factory delegateFactory,
        long clipOffset,
        long clipLength,
        boolean allowCrossPartitionReads) {
      this(delegateFactory, clipOffset, clipLength, allowCrossPartitionReads, false);
    }

    public Factory(
        DataSource.Factory delegateFactory,
        long clipOffset,
        long clipLength,
        boolean allowCrossPartitionReads,
        boolean allowPartialReads) {
      this.delegateFactory = delegateFactory;
      this.clipOffset = clipOffset;
      this.clipLength = clipLength;
      this.allowCrossPartitionReads = allowCrossPartitionReads;
      this.allowPartialReads = allowPartialReads;
    }

    @Override
    public IsoDataSource createDataSource() {
      return new IsoDataSource(
          delegateFactory.createDataSource(),
          clipOffset,
          clipLength,
          allowCrossPartitionReads,
          allowPartialReads);
    }
  }
}
