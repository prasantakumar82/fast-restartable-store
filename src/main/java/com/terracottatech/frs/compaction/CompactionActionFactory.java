/*
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.terracottatech.frs.compaction;

import com.terracottatech.frs.object.ObjectManager;
import com.terracottatech.frs.object.ObjectManagerEntry;

import java.nio.ByteBuffer;

/**
 * Factory interface for creating CompactionAction objects. Implements the
 * Factory Method pattern
 * to allow different implementations to create appropriate CompactionAction
 * instances.
 */
public interface CompactionActionFactory {
  /**
   * Creates a CompactionAction from an ObjectManagerEntry.
   *
   * @param objectManager The object manager that contains the entry
   * @param entry         The entry from the object manager to be converted
   * @return A CompactionAction containing the entry's data
   * @throws NullPointerException if entry is null
   */
  CompactionAction create(ObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectManager,
      ObjectManagerEntry<ByteBuffer, ByteBuffer, ByteBuffer> entry);
}
