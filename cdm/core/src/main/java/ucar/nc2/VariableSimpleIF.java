/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import ucar.ma2.DataType;

/** A lightweight abstraction of a Variable. */
public interface VariableSimpleIF extends Comparable<VariableSimpleIF> {

  /** full, backslash escaped name of the data Variable */
  String getFullName();

  /** short name of the data Variable */
  String getShortName();

  /** description of the Variable, or null if none. */
  @Nullable
  String getDescription();

  /** Units of the Variable, or null if none. */
  @Nullable
  String getUnitsString();

  /** Variable rank */
  int getRank();

  /** Variable shape */
  int[] getShape();

  /** Dimension List. empty for a scalar variable. */
  ImmutableList<Dimension> getDimensions();

  /** Variable's data type */
  DataType getDataType();

  /** Attributes for the variable. */
  AttributeContainer attributes();

}
