/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sunspotworld.demo.util;

import javax.microedition.rms.*;

/**
 *
 * @author USUARIO
 */
public class PersistenceUnit
{
    private RecordStore rsm = null;
    public static final String LOG_RECORD_NON_COORDINATOR = "db_1";
    public static final String LOG_RECORD_COORDINATOR = "db_2";

    public PersistenceUnit()
    {

    }


    public void openRecordStoreForLog()
    {
        try
        {
            rsm = RecordStore.openRecordStore(LOG_RECORD_NON_COORDINATOR, true);

        }
        catch(Exception e)
        {
            dbHandleError(e.getMessage());
        }
    }

    public void openRecordStoreForMetrics()
    {
        try
        {
            rsm = RecordStore.openRecordStore(LOG_RECORD_COORDINATOR, true);
        }
        catch(Exception e)
        {
            dbHandleError(e.getMessage());
        }
    }

    public void closeRecStore()
    {
        try
        {
          rsm.closeRecordStore();
        }
        catch (Exception e)
        {
          dbHandleError(e.toString());
        }
    }

    public void writeRecord(String str)
    {
        byte[] rec = str.getBytes();
        try
        {
            rsm.addRecord(rec, 0, rec.length);
        }
        catch(Exception e)
        {
            dbHandleError(e.getMessage());
        }
    }

    public void readRecord() throws RecordStoreException
    {
        try
        {
          // Intentionally make this too small to test code below
          byte[] recData = new byte[5];
          int len;
          for (int i = 1; i <= rsm.getNumRecords(); i++)
          {
            if (rsm.getRecordSize(i) > recData.length)
              recData = new byte[rsm.getRecordSize(i)];

            len = rsm.getRecord(i, recData, 0);
            System.out.println("Record #" + i + ": " +
                    new String(recData, 0, len));
            System.out.println("------------------------------");
          }
        }
        catch (Exception e)
        {
          dbHandleError(e.toString());
        }
     }

    public void deleteRecStore()
    {
        if (RecordStore.listRecordStores() != null)
        {
          try
          {
            RecordStore.deleteRecordStore(LOG_RECORD_NON_COORDINATOR);
          }
          catch (Exception e)
          {
            dbHandleError(e.toString());
          }
        }
    }

    public int getRecordsAmount() throws RecordStoreNotOpenException
    {
        return rsm.getNumRecords();
    }

    private void dbHandleError(String in)
    {
        System.err.println("MSG_ERROR from persistence unit: "+in);
    }

}
