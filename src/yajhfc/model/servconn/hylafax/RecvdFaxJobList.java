/*
 * YAJHFC - Yet another Java Hylafax client
 * Copyright (C) 2005-2010 Jonas Wolz
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package yajhfc.model.servconn.hylafax;

import gnu.hylafax.HylaFAXClient;
import gnu.inet.ftp.ServerResponseException;

import java.io.IOException;
import java.util.Vector;

import yajhfc.model.RecvFormat;
import yajhfc.model.TableType;
import yajhfc.model.servconn.FaxJob;

public class RecvdFaxJobList extends AbstractHylaFaxJobList<RecvFormat> {
    
    @Override
    public boolean isShowingErrorsSupported() {
        return columns.getCompleteView().contains(RecvFormat.e);
    }
    
    public TableType getJobType() {
        return TableType.RECEIVED;
    }

    @Override
    protected Vector<?> getJobListing(HylaFAXClient hyfc) throws IOException, ServerResponseException {
        synchronized (hyfc) {
            hyfc.rcvfmt(columns.getFormatString());
            return hyfc.getList("recvq");
        }
    }
    
    @Override
    protected FaxJob<RecvFormat> createFaxJob(String[] data) {
        return new RecvdFaxJob(this, data);
    }
    
    protected RecvdFaxJobList(HylaFaxListConnection parent) {
        super(parent, parent.fo.recvfmt);
    }
    
}
