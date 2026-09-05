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
import java.io.Closeable;
import java.io.IOException;

/**
 * Reads data from an ISO 14496-12 file.
 */
public interface IsoDataReader extends Closeable {

  /** Factory for {@link IsoDataReader}. */
  interface Factory {
    IsoDataReader createDataSource(DataSource.Factory dataSourceFactory, Uri uri);
  }

  /**
   * Reads up to {@code length} bytes of data from the given position into the buffer.
   *
   * @param position the byte position to read from
   * @param buffer the buffer to read into
   * @param offset the offset in the buffer to start writing
   * @param length the maximum number of bytes to read
   * @return the number of bytes actually read, or -1 if the end of data is reached
   * @throws IOException if an I/O error occurs
   */
  int read(long position, byte[] buffer, int offset, int length) throws IOException;

  /**
   * Returns the total length of the data in bytes, or {@link C#LENGTH_UNSET} if unknown.
   *
   * @throws IOException if an I/O error occurs
   */
  long length() throws IOException;

  /**
   * Hints that a range of bytes will be needed soon. Default implementation is a no-op.
   *
   * @param position the start position of the range
   * @param length the length of the range in bytes
   * @throws IOException if an I/O error occurs
   */
  void prefetchRange(long position, long length) throws IOException;
}
