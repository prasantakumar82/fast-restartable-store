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
package com.terracottatech.frs;

import com.terracottatech.frs.compaction.Compactor;
import com.terracottatech.frs.object.ObjectManager;

import java.nio.ByteBuffer;

/**
 * Factory interface for creating GettableAction instances.
 * <p>
 * This factory is responsible for creating actions that can be used to store
 * and retrieve data in the Fast Restartable Store. Implementations of this
 * interface create specific types of GettableAction objects, such as PutAction
 * (standard action) or CipherPutAction (specialized encrypted action).
 *
 * @author prasanta
 */
public interface GettableActionFactory {

  /**
   * Creates a new GettableAction instance.
   *
   * @param objectManager object manager that will handle the action's data
   * @param compactor     compactor used for garbage collection of old entries
   * @param id            identifier for the restartable object
   * @param key           key for the entry
   * @param value         value of the entry
   * @param isRecovering  whether recovery is in process
   * @return A new GettableAction instance
   */
  GettableAction create(ObjectManager<ByteBuffer, ByteBuffer, ByteBuffer> objectManager,
      Compactor compactor, ByteBuffer id, ByteBuffer key, ByteBuffer value, boolean isRecovering);
}
