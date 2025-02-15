/*
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
package io.pixelsdb.pixels.trino.block;

import io.airlift.slice.Slice;
import io.trino.spi.block.Block;
import io.trino.spi.block.ByteArrayBlock;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;

import static java.lang.Math.ceil;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * This class is copied from io.trino.spi.block.BlockUtil, because it's not public.
 */
final class BlockUtil
{
    private static final double BLOCK_RESET_SKEW = 1.25;

    private static final int DEFAULT_CAPACITY = 64;
    // See java.util.ArrayList for an explanation
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private BlockUtil()
    {
    }

    static void checkArrayRange(int[] array, int offset, int length)
    {
        requireNonNull(array, "array is null");
        if (offset < 0 || length < 0 || offset + length > array.length) {
            throw new IndexOutOfBoundsException(format("Invalid offset %s and length %s in array with %s elements",
                    offset, length, array.length));
        }
    }

    static void checkArrayRange(boolean[] array, int offset, int length)
    {
        requireNonNull(array, "array is null");
        if (offset < 0 || length < 0 || offset + length > array.length) {
            throw new IndexOutOfBoundsException(format("Invalid offset %s and length %s in array with %s elements",
                    offset, length, array.length));
        }
    }

    static void checkValidRegion(int positionCount, int positionOffset, int length)
    {
        if (positionOffset < 0 || length < 0 || positionOffset + length > positionCount) {
            throw new IndexOutOfBoundsException(format("Invalid position %s and length %s in block with %s positions",
                    positionOffset, length, positionCount));
        }
    }

    static void checkValidPositions(boolean[] positions, int positionCount)
    {
        if (positions.length != positionCount) {
            throw new IllegalArgumentException(format("Invalid positions array size %d, actual position count is %d",
                    positions.length, positionCount));
        }
    }

    static void checkValidPosition(int position, int positionCount)
    {
        if (position < 0 || position >= positionCount) {
            throw new IllegalArgumentException(format("Invalid position %s in block with %s positions",
                    position, positionCount));
        }
    }

    static int calculateNewArraySize(int currentSize)
    {
        // grow array by 50%
        long newSize = (long) currentSize + (currentSize >> 1);

        // verify new size is within reasonable bounds
        if (newSize < DEFAULT_CAPACITY) {
            newSize = DEFAULT_CAPACITY;
        }
        else if (newSize > MAX_ARRAY_SIZE) {
            newSize = MAX_ARRAY_SIZE;
            if (newSize == currentSize) {
                throw new IllegalArgumentException(format("Cannot grow array beyond '%s'", MAX_ARRAY_SIZE));
            }
        }
        return (int) newSize;
    }

    static int calculateBlockResetSize(int currentSize)
    {
        long newSize = (long) ceil(currentSize * BLOCK_RESET_SKEW);

        // verify new size is within reasonable bounds
        if (newSize < DEFAULT_CAPACITY) {
            newSize = DEFAULT_CAPACITY;
        }
        else if (newSize > MAX_ARRAY_SIZE) {
            newSize = MAX_ARRAY_SIZE;
        }
        return (int) newSize;
    }

    static int calculateBlockResetBytes(int currentBytes)
    {
        long newBytes = (long) ceil(currentBytes * BLOCK_RESET_SKEW);
        if (newBytes > MAX_ARRAY_SIZE) {
            return MAX_ARRAY_SIZE;
        }
        return (int) newBytes;
    }

    /**
     * Recalculate the <code>offsets</code> array for the specified range.
     * The returned <code>offsets</code> array contains <code>length + 1</code> integers
     * with the first value set to 0.
     * If the range matches the entire <code>offsets</code> array,  the input array will be returned.
     */
    static int[] compactOffsets(int[] offsets, int index, int length)
    {
        if (index == 0 && offsets.length == length + 1) {
            return offsets;
        }

        int[] newOffsets = new int[length + 1];
        for (int i = 1; i <= length; i++) {
            newOffsets[i] = offsets[index + i] - offsets[index];
        }
        return newOffsets;
    }

    /**
     * Returns a slice containing values in the specified range of the specified slice.
     * If the range matches the entire slice, the input slice will be returned.
     * Otherwise, a copy will be returned.
     */
    static Slice compactSlice(Slice slice, int index, int length)
    {
        if (slice.isCompact() && index == 0 && length == slice.length()) {
            return slice;
        }
        return slice.copy(index, length);
    }

    /**
     * Returns an array containing elements in the specified range of the specified array.
     * If the range matches the entire array, the input array will be returned.
     * Otherwise, a copy will be returned.
     */
    static boolean[] compactArray(boolean[] array, int index, int length)
    {
        if (index == 0 && length == array.length) {
            return array;
        }
        return Arrays.copyOfRange(array, index, index + length);
    }

    static byte[] compactArray(byte[] array, int index, int length)
    {
        if (index == 0 && length == array.length) {
            return array;
        }
        return Arrays.copyOfRange(array, index, index + length);
    }

    static short[] compactArray(short[] array, int index, int length)
    {
        if (index == 0 && length == array.length) {
            return array;
        }
        return Arrays.copyOfRange(array, index, index + length);
    }

    static int[] compactArray(int[] array, int index, int length)
    {
        if (index == 0 && length == array.length) {
            return array;
        }
        return Arrays.copyOfRange(array, index, index + length);
    }

    static long[] compactArray(long[] array, int index, int length)
    {
        if (index == 0 && length == array.length) {
            return array;
        }
        return Arrays.copyOfRange(array, index, index + length);
    }

    static int countSelectedPositionsFromOffsets(boolean[] positions, int[] offsets, int offsetBase)
    {
        checkArrayRange(offsets, offsetBase, positions.length);
        int used = 0;
        for (int i = 0; i < positions.length; i++) {
            int offsetStart = offsets[offsetBase + i];
            int offsetEnd = offsets[offsetBase + i + 1];
            used += ((positions[i] ? 1 : 0) * (offsetEnd - offsetStart));
        }
        return used;
    }

    static int countAndMarkSelectedPositionsFromOffsets(boolean[] positions, int[] offsets,
                                                        int offsetBase, boolean[] elementPositions)
    {
        checkArrayRange(offsets, offsetBase, positions.length);
        int used = 0;
        for (int i = 0; i < positions.length; i++) {
            int offsetStart = offsets[offsetBase + i];
            int offsetEnd = offsets[offsetBase + i + 1];
            if (positions[i]) {
                used += (offsetEnd - offsetStart);
                Arrays.fill(elementPositions, offsetStart, offsetEnd, true);
            }
        }
        return used;
    }

    /**
     * Returns <tt>true</tt> if the two specified arrays contain the same object in every position.
     * Unlike the {@link Arrays#equals(Object[], Object[])} method, this method compares using reference equals.
     */
    static boolean arraySame(Object[] array1, Object[] array2)
    {
        if (array1 == null || array2 == null || array1.length != array2.length) {
            throw new IllegalArgumentException("array1 and array2 cannot be null and should have same length");
        }

        for (int i = 0; i < array1.length; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the input blocks array if all blocks are already loaded, otherwise returns
     * a new blocks array with all blocks loaded
     */
    static Block[] ensureBlocksAreLoaded(Block[] blocks)
    {
        for (int i = 0; i < blocks.length; i++) {
            Block loaded = blocks[i].getLoadedBlock();
            if (loaded != blocks[i]) {
                // Transition to new block creation mode after the first newly loaded block is encountered
                Block[] loadedBlocks = blocks.clone();
                loadedBlocks[i++] = loaded;
                for (; i < blocks.length; i++) {
                    loadedBlocks[i] = blocks[i].getLoadedBlock();
                }
                return loadedBlocks;
            }
        }
        // No newly loaded blocks
        return blocks;
    }

    static boolean[] copyIsNullAndAppendNull(@Nullable boolean[] isNull, int offsetBase, int positionCount)
    {
        int desiredLength = offsetBase + positionCount + 1;
        boolean[] newIsNull = new boolean[desiredLength];
        if (isNull != null) {
            checkArrayRange(isNull, offsetBase, positionCount);
            System.arraycopy(isNull, 0, newIsNull, 0, desiredLength - 1);
        }
        // mark the last element to append null
        newIsNull[desiredLength - 1] = true;
        return newIsNull;
    }

    static int[] copyOffsetsAndAppendNull(int[] offsets, int offsetBase, int positionCount)
    {
        int desiredLength = offsetBase + positionCount + 1;
        checkArrayRange(offsets, offsetBase, positionCount);
        int[] newOffsets = Arrays.copyOf(offsets, desiredLength);
        // Null element does not move the offset forward
        newOffsets[desiredLength - 1] = newOffsets[desiredLength - 2];
        return newOffsets;
    }

    /**
     * Returns a new int array of size capacity if the input buffer is null or
     * smaller than the capacity. Returns the original array otherwise.
     * Any original values in the input buffer will be preserved in the output.
     */
    public static int[] ensureCapacity(@Nullable int[] buffer, int capacity)
    {
        if (buffer == null) {
            buffer = new int[capacity];
        }
        else if (buffer.length < capacity) {
            buffer = Arrays.copyOf(buffer, capacity);
        }

        return buffer;
    }

    /**
     * Ideally, the underlying nulls array in Block implementations should be a byte array instead of a boolean array.
     * This method is used to perform that conversion until the Block implementations are changed.
     */
    static Optional<ByteArrayBlock> getNulls(@Nullable boolean[] valueIsNull, int arrayOffset, int positionCount)
    {
        if (valueIsNull == null) {
            return Optional.empty();
        }
        byte[] booleansAsBytes = new byte[positionCount];
        boolean foundAnyNull = false;
        for (int i = 0; i < positionCount; i++) {
            booleansAsBytes[i] = (byte) (valueIsNull[arrayOffset + i] ? 1 : 0);
            foundAnyNull = foundAnyNull || valueIsNull[arrayOffset + i];
        }
        if (!foundAnyNull) {
            return Optional.empty();
        }
        return Optional.of(new ByteArrayBlock(booleansAsBytes.length, Optional.empty(), booleansAsBytes));
    }
}
