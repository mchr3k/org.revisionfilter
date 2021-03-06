/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.ba.bcp;

import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.Edge;

/**
 * An abstract PatternElement subclass for pattern elements which
 * must match exactly one instruction and accept any kind of branch.
 * (Subclasses may override acceptBranch() to implement more selective
 * handling of branches.)
 *
 * @author David Hovemeyer
 * @see PatternElement
 */
public abstract class SingleInstruction extends PatternElement {
	@Override
		 public boolean acceptBranch(Edge edge, InstructionHandle source) {
		return true;
	}

	@Override
		 public int minOccur() {
		return 1;
	}

	@Override
		 public int maxOccur() {
		return 1;
	}

}

// vim:ts=4
