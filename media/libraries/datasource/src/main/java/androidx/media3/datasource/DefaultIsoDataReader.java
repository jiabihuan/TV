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

import android.net.Uri;
import androidx.annotation.Nullable;
import java.io.IOException;

/**
 * Default implementation of {@link IsoDataReader} that reads from a {@link DataSource}.
 */
public class DefaultIsoDataReader implements IsoDataReader {

  private final DataSource dataSource;
  private final DataSpec dataSpec;
  @Nullable private byte[] buffer;

  public DefaultIsoDataReader(DataSource.Factory dataSourceFactory, Uri uri) throws IOException {
    this.dataSource = dataSourceFactory.createDataSource();
    this.dataSpec = new DataSpec(uri);
    dataSource.open(dataSpec);
  }

  @Override
  public int read(long position, byte[] buffer, int offset, int length) throws IOException {
    long currentOffset = dataSource.getPosition();
    if (currentOffset != position) {
      // Seek if needed
      DataSpec seekSpec = new DataSpec.Builder()
          .setUri(dataSpec.uri)
          .setOffset(position)
          .setLength(length)
          .build();
      dataSource.close();
      dataSource.open(seekSpec);
    }
    return dataSource.read(buffer, offset, length);
  }

  @Override
  public long length() throws IOException {
    long contentLength = dataSource.getContentLength();
    return contentLength == C.LENGTH_UNSET ? C.LENGTH_UNSET : contentLength;
  }

  @Override
  public void prefetchRange(long position, long length) throws IOException {
    // No-op for default implementation
  }

  @Override
  public void close() {
    try {
      dataSource.close();
    } catch (Exception e) {
      // Ignore
    }
  }
}
