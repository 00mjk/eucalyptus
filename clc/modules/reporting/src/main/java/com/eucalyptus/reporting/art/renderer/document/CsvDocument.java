/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.reporting.art.renderer.document;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class CsvDocument
	implements Document
{
	private List<String> colVals = null;
    private Writer writer;
    private boolean rowHasLabel = false;
    private int rowIndent = 3;

    @Override
    public void setUnlabeledRowIndent( final int num ) {
      this.rowIndent = num;
    }

    @Override
    public void setWriter(Writer writer)
    {
    	this.writer = writer;
    }
    
    @Override
	public Document open()
    	throws IOException
    {
    	return this;
    }
    
    @Override
	public Document close()
    	throws IOException
    {
    	writer.flush();
    	return this;
    }
    
    @Override
	public Document tableOpen()
    	throws IOException
    {
    	return this;
    }

    private void writeRow()
    	throws IOException
    {
    	StringBuilder sb = new StringBuilder();
    	for (int i=0; i<colVals.size(); i++) {
    		if (i>0) sb.append(",");
    		sb.append(colVals.get(i));
    	}
    	sb.append("\n");
    	writer.write(sb.toString());
    }
    
    @Override
	public Document tableClose()
		throws IOException
	{
    	if (colVals != null) {
    		writeRow();
    	}
    colVals = new ArrayList<String>();
    return this;
	}
    
    @Override
	public Document textLine(String text, int emphasis)
    	throws IOException
    {
    	writer.write(text + "\n");
    	return this;    	
    }

    @Override
	public Document newRow()
    	throws IOException
    {
    	if (colVals != null && !colVals.isEmpty()) {
    		writeRow();
    	}
    	colVals = new ArrayList<String>();
    	rowHasLabel = false;
    	return this;    	
    }

    @Override
	public Document addLabelCol(int indent, String val)
		throws IOException
	{
    	rowHasLabel = true;
    	addEmptyLabelCols(indent);
    	addCol(val, 1);
    	addEmptyLabelCols(rowIndent-(indent+1));
    	return this;
	}	


    @Override
	public Document addValCol(String val)
    	throws IOException
    {
        return addCol(val, 1);
    }

  @Override
  public Document addValCol(Integer val)
      throws IOException
  {
    return addCol((val==null)?null:val.toString(), 1);
  }

  @Override
	public Document addValCol(Long val)
		throws IOException
    {
        return addCol((val==null)?null:val.toString(), 1);
    }

    @Override
	public Document addValCol(Double val)
		throws IOException
    {
        return addCol((val==null)?null:String.format("%3.1f", val), 1);
    }

    @Override
	public CsvDocument addValCol(String val, int colspan, String align)
		throws IOException
	{
    	return addCol(val, colspan);
	}

    private CsvDocument addCol(String val, int colspan)
		throws IOException
    {
    	if (!rowHasLabel) {
    		addEmptyLabelCols(rowIndent);
    		rowHasLabel = true;
    	}
    	colVals.add(val);
    	for (int i=1; i<colspan; i++) {
    		colVals.add("");
    	}
        return this;
    }

    @Override
	public CsvDocument addEmptyValCols(int num)
		throws IOException
   {
        for (int i=0; i<num; i++) {
        	colVals.add("");
        }
        return this;
    }

    @Override
	public Document addEmptyLabelCols(int num)
		throws IOException
	{
    	for (int i=0; i<num; i++) {
        	colVals.add("");
    	}
    	return this;
	}
}
