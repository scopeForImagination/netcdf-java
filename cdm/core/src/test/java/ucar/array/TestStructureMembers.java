/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ucar.array.StructureMembers.Member;
import ucar.array.StructureMembers.MemberBuilder;
import ucar.ma2.DataType;

/** Test {@link StructureMembers} */
public class TestStructureMembers {

  @Test
  public void testBasics() {
    StructureMembers.Builder builder = StructureMembers.builder();
    builder.setName("name");
    builder.addMember("mname1", "mdesc1", "munits1", DataType.BYTE, new int[] {11, 11});
    MemberBuilder mbuilder = StructureMembers.memberBuilder();
    mbuilder.setName("mname2").setDesc("mdesc2").setUnits("munits2").setDataType(DataType.UBYTE)
        .setShape(new int[] {7, 11});
    builder.addMember(mbuilder);
    MemberBuilder mbuilder3 = StructureMembers.memberBuilder();
    mbuilder3.setName("mname3").setDataType(DataType.SHORT);
    builder.addMember(1, mbuilder3);

    assertThat(builder.hasMember("nope")).isFalse();
    assertThat(builder.hasMember("mname2")).isTrue();

    StructureMembers sm = builder.build();
    assertThat(sm.getName()).isEqualTo("name");
    assertThat(sm.getMember(0).getName()).isEqualTo("mname1");
    assertThat(sm.getMember(1).getName()).isEqualTo("mname3");
    assertThat(sm.getMember(2).getName()).isEqualTo("mname2");
    assertThat(sm.numberOfMembers()).isEqualTo(3);
    assertThat(sm.getMemberNames()).isEqualTo(ImmutableList.of("mname1", "mname3", "mname2"));

    int count = 0;
    for (Member m : sm.getMembers()) {
      assertThat(m).isEqualTo(sm.findMember(m.getName()));
      assertThat(m.getIndex()).isEqualTo(count++);
    }
    assertThat(sm.findMember(null)).isNull();

    Member m = sm.findMember("mname1");
    assertThat(m).isNotNull();
    assertThat(m.getDataType()).isEqualTo(DataType.BYTE);
    assertThat(m.getDescription()).isEqualTo("mdesc1");
    assertThat(m.getUnitsString()).isEqualTo("munits1");
    assertThat(m.getShape()).isEqualTo(new int[] {11, 11});
    assertThat(m.getSize()).isEqualTo(121);
    assertThat(m.getSizeBytes()).isEqualTo(121);
    assertThat(m.getIndex()).isEqualTo(0);
    assertThat(m.isVariableLength()).isEqualTo(false);
    assertThat(m.getStructureMembers()).isNull();

    Member m2 = sm.findMember("mname3");
    assertThat(m2).isNotNull();
    assertThat(m2.getDataType()).isEqualTo(DataType.SHORT);
    assertThat(m2.getShape()).isEqualTo(new int[] {});
    assertThat(m2.getSize()).isEqualTo(1);
    assertThat(m2.getSizeBytes()).isEqualTo(2);

    try {
      assertThat(sm.getMember(3)).isNull();
      fail();
    } catch (Exception e) {
      ;// expected
    }

    assertThat(sm.getStructureSize()).isEqualTo(200);
    assertThat(sm.toString())
        .isEqualTo("StructureMembers{name=name, members=[mname1, mname3, mname2], structureSize=200}");
    assertThat(sm.toBuilder().build()).isEqualTo(sm);
  }

  @Test
  public void testNestedStructureMembers() {
    StructureMembers.Builder builder = StructureMembers.builder();
    builder.setName("nested");
    builder.addMember("mname1", "mdesc1", "munits1", DataType.BYTE, new int[] {11, 11});
    MemberBuilder mbuilder = StructureMembers.memberBuilder();
    mbuilder.setName("mname2").setDesc("mdesc2").setUnits("munits2").setDataType(DataType.UBYTE)
        .setShape(new int[] {7, 11});
    builder.addMember(mbuilder);
    StructureMembers nested = builder.build();

    builder = StructureMembers.builder();
    builder.setName("top");
    builder.addMember("nname1", "ndesc1", "nunits1", DataType.DOUBLE, new int[] {11});
    builder.addMember("nname2", "ndesc2", "nunits2", DataType.INT, new int[] {3, 3, 3});
    MemberBuilder nbuilder =
        StructureMembers.memberBuilder().setName("struct").setStructureMembers(nested).setDataType(DataType.STRUCTURE);
    builder.addMember(nbuilder);

    StructureMembers sm = builder.build();
    assertThat(sm.findMember("mname2")).isNull();
    assertThat(sm.findMember("nname2")).isNotNull();
    assertThat(sm.getStructureSize()).isEqualTo(88 + 108 + 198);

    Member m = sm.findMember("struct");
    assertThat(m).isNotNull();
    assertThat(m.getDataType()).isEqualTo(DataType.STRUCTURE);
    assertThat(m.getDescription()).isNull();
    assertThat(m.getUnitsString()).isNull();
    assertThat(m.getShape()).isEqualTo(new int[] {});
    assertThat(m.getSize()).isEqualTo(1);
    assertThat(m.getSizeBytes()).isEqualTo(198);
    assertThat(m.getIndex()).isEqualTo(2);
    assertThat(m.isVariableLength()).isEqualTo(false);

    StructureMembers ncm = m.getStructureMembers();
    assertThat(ncm).isNotNull();
    assertThat(ncm.numberOfMembers()).isEqualTo(2);
    assertThat(ncm.findMember("mname2")).isNotNull();
    assertThat(ncm.findMember("nname2")).isNull();
    assertThat(ncm.getStructureSize()).isEqualTo(198);
  }

}
