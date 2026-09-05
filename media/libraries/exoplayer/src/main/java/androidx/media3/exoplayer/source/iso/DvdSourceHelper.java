/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.media3.exoplayer.source.iso;

import android.net.Uri;
import androidx.media3.common.CacheDataReader;
import androidx.media3.common.MediaItem;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.extractor.iso.dvd.DvdStructure;
import androidx.media3.extractor.iso.udf.UdfFileSystem;
import java.io.IOException;

final class DvdSourceHelper {

  static DvdStructure parseStructure(CacheDataReader isoReader, UdfFileSystem udf) throws IOException {
    return new androidx.media3.extractor.iso.dvd.DvdIfoParser(isoReader, udf).parse();
  }

  static MediaSource buildSource(MediaItem mediaItem, DataSource.Factory dataSourceFactory, Uri isoUri, CacheDataReader isoReader, UdfFileSystem udf, int titleIndex) throws IOException {
    // TODO: Implement DVD source building with available APIs
    throw new UnsupportedOperationException("DVD source building not yet implemented for Media3 1.11");
  }
}
