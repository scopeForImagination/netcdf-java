/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import ucar.array.ArrayDouble.StorageD;
import ucar.array.ArrayFloat.StorageF;
import ucar.array.StructureMembers.Member;
import ucar.ma2.DataType;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

/** Superclass for implementations of Array of StructureData. */
public class StructureDataArray extends ucar.array.Array<StructureData> {

  public static int[] makeMemberOffsets(Structure structure) {
    int pos = 0;
    int count = 0;
    int[] offset = new int[structure.getNumberOfMemberVariables()];
    for (Variable v2 : structure.getVariables()) {
      offset[count++] = pos;
      pos += v2.getSize();
    }
    return offset;
  }

  ////////////////////////////////////////////////////////////////////////////
  Storage<StructureData> storageSD;
  StructureMembers members;

  /** Create an empty Array of type StructureData and the given shape. */
  public StructureDataArray(StructureMembers members, int[] shape) {
    super(DataType.STRUCTURE, shape);
    this.members = members;
    storageSD = new StorageSD(new StructureData[(int) indexFn.length()]);
  }

  /** Create an Array of type StructureData and the given shape and storage. */
  public StructureDataArray(StructureMembers members, int[] shape, Storage<StructureData> storageSD) {
    super(DataType.STRUCTURE, shape);
    Preconditions.checkArgument(indexFn.length() == storageSD.getLength());
    this.members = members;
    this.storageSD = storageSD;
  }

  /** Create an Array of type StructureData and the given shape and storage with ByteBuffer and offsets. */
  public StructureDataArray(StructureMembers members, int[] shape, ByteBuffer bytes, int[] offsets) {
    super(DataType.STRUCTURE, shape);
    this.members = members;
    this.storageSD = new StorageBB(bytes, members, offsets, (int) length());;
  }

  /** Create an Array of type StructureData and the given indexFn and storage. */
  private StructureDataArray(StructureMembers members, IndexFn indexFn, Storage<StructureData> storageSD) {
    super(DataType.STRUCTURE, indexFn);
    this.members = members;
    this.storageSD = storageSD;
  }

  /** Get the StructureMembers. */
  public StructureMembers getStructureMembers() {
    return members;
  }

  /** Get a list of structure member names. */
  public List<String> getStructureMemberNames() {
    return members.getMemberNames();
  }

  @Override
  public long getSizeBytes() {
    return indexFn.length() * members.getStructureSize();
  }

  @Override
  void arraycopy(int srcPos, Object dest, int destPos, long length) {
    // TODO
  }

  @Override
  Array<StructureData> createView(IndexFn index) {
    return null;
  }

  @Override
  public Iterator<StructureData> fastIterator() {
    return storageSD.iterator();
  }

  @Override
  public Iterator<StructureData> iterator() {
    return indexFn.isCanonicalOrder() ? fastIterator() : new CanonicalIterator();
  }

  public StructureData sum() {
    return null;
  }

  @Override
  public StructureData get(int... index) {
    Preconditions.checkArgument(this.rank == index.length);
    return storageSD.get(indexFn.get(index));
  }

  @Override
  public StructureData get(ucar.array.Index index) {
    return get(index.getCurrentIndex());
  }

  @Override
  Storage<StructureData> storage() {
    return storageSD;
  }

  public void setStructureData(int index, StructureData sdata) {
    // TODO kludge
    ((StorageSD) storageSD).storage[index] = sdata;
  }

  /** Get the size of each StructureData object in bytes. */
  public int getStructureSize() {
    return members.getStructureSize();
  }

  private class CanonicalIterator implements Iterator<StructureData> {
    // used when the data is not in canonical order
    private final Iterator<Integer> iter = indexFn.iterator();

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public StructureData next() {
      return storageSD.get(iter.next());
    }
  }

  ////////////////////////////////////////////////////////////////

  static class StorageSD implements Storage<StructureData> {
    final StructureData[] storage;

    StorageSD(StructureData[] storage) {
      this.storage = storage;
    }

    @Override
    public long getLength() {
      return storage.length;
    }

    @Override
    public StructureData get(long elem) {
      return storage[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      // TODO
    }

    @Override
    public Iterator<StructureData> iterator() {
      return new Iter();
    }

    private final class Iter implements Iterator<StructureData> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < storage.length;
      }

      @Override
      public final StructureData next() {
        return storage[count++];
      }
    }
  }

  ////////////////////////////////////////////////////////////

  static class StorageBB implements Storage<StructureData> {
    private final ByteBuffer bbuffer;
    private final int nelems;
    private final StructureMembers members;
    private final int[] offset;

    StorageBB(ByteBuffer bbuffer, StructureMembers members, int[] offset, int nelems) {
      this.bbuffer = bbuffer;
      this.members = members;
      this.offset = offset;
      this.nelems = nelems;
    }

    @Override
    public long getLength() {
      return bbuffer.array().length;
    }

    @Override
    public StructureData get(long elem) {
      return new StructureDataBB((int) elem);
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      // TODO
    }

    @Override
    public Iterator<StructureData> iterator() {
      return new Iter();
    }

    private final class Iter implements Iterator<StructureData> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < nelems;
      }

      @Override
      public final StructureData next() {
        return new StructureDataBB(count++);
      }
    }

    class StructureDataBB extends StructureData {
      private final int recno;

      private StructureDataBB(int recno) {
        super(StorageBB.this.members);
        this.recno = recno;
      }

      @Override
      public Array<?> getMemberData(Member m) {
        DataType dataType = m.getDataType();
        int pos = recno * members.getStructureSize() + offset[m.getIndex()];
        int size = m.length();

        switch (dataType) {
          case DOUBLE:
            double[] darray = new double[size];
            for (int count = 0; count < size; count++) {
              darray[count] = bbuffer.getDouble(pos + 8 * count);
            }
            return new ArrayDouble(m.getShape(), new StorageD(darray));

          case FLOAT:
            float[] farray = new float[size];
            for (int count = 0; count < size; count++) {
              farray[count] = bbuffer.getFloat(pos + 4 * count);
            }
            return new ArrayFloat(m.getShape(), new StorageF(farray));

          default:
            return null;
          // throw new RuntimeException("unknown dataType " + dataType);
        }
      }
    }
  }

}
