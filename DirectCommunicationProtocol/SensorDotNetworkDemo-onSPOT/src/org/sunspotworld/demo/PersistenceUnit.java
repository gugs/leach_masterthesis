package org.sunspotworld.demo;

import javax.microedition.rms.*;

/**
 *
 * @author USUARIO
 */
public class PersistenceUnit
{
    private RecordStore rsm = null;

    public static final String LOG_TIME_ELAPSED = "db_4";
    public static final String LOG_NODE_METRIC = "db_3";
    public static final byte OVERALL = 1;


    public PersistenceUnit()
    {
        try
        {
            openRecordStoreForTimeElapsed();
            if (rsm.getNumRecords() < 2)
            {
                writeRecord("0");
                writeRecord("0");
            }
            closeRecStore();
            openRecordStoreForNonCoordinatorMetric();
            if (rsm.getNumRecords() < 1)
            {
                writeRecord("0");
            }
            closeRecStore();
            
        }
        catch (RecordStoreNotOpenException ex)
        {
            ex.printStackTrace();
        }
        
    }


    public void updateTimeElapsed(byte indexNumber, String value) throws Exception
    {
        if(indexNumber == (byte)0 || indexNumber == (byte)1
                || indexNumber == (byte)2)
        {
            openRecordStoreForTimeElapsed();
            updateRecord(indexNumber, value);
            closeRecStore();
        }
        else
            throw new Exception("Unexpected index number!");
        
    }

    public void updatePacketsCounterNonCoordinator(byte indexNumber,
            String value) throws Exception
    {
        openRecordStoreForNonCoordinatorMetric();

        switch(indexNumber)
        {
            case OVERALL:
                updateRecord(indexNumber, value);
                closeRecStore();
                break;
        }
    }

    public void openRecordStoreForTimeElapsed()
    {
        try
        {
            rsm = RecordStore.openRecordStore(LOG_TIME_ELAPSED, true);

        }
        catch(Exception e)
        {
            dbHandleError(e.getMessage());
        }
    }

    public void openRecordStoreForNonCoordinatorMetric()
    {
        try
        {
            rsm = RecordStore.openRecordStore(LOG_NODE_METRIC,
                    true);

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

    private void writeRecord(String str)
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

    public String readRecord() throws RecordStoreException
    {
        String output = "";

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
            output += "Record #" + i+ ": " +
                    new String(recData, 0, len)+"\n";
          }
        }
        catch (Exception e)
        {
          dbHandleError(e.toString());
        }
        return output;
     }

    private void updateRecord(int index, String newValue)
    {

        byte[] newValueByte = newValue.getBytes();

        try
        {
            rsm.setRecord(index, newValueByte, 0, newValueByte.length);
        }
        catch (RecordStoreNotOpenException ex)
        {
            ex.printStackTrace();
        }
        catch (InvalidRecordIDException ex)
        {
            System.out.println("Error Index: " + index);
            ex.printStackTrace();
        }
        catch (RecordStoreException ex)
        {
            ex.printStackTrace();
        }
    }

    public void deleteRecStoreOfTimeElapsed()
    {
        if (RecordStore.listRecordStores() != null)
        {
            try
            {
                RecordStore.deleteRecordStore(LOG_TIME_ELAPSED);
            }
            catch (RecordStoreException ex)
            {
                ex.printStackTrace();
            }
        }
    }


    public void deleteRecStoreNonCoordinator()
    {
        if (RecordStore.listRecordStores() != null)
        {
            try
            {
                RecordStore.deleteRecordStore(LOG_NODE_METRIC);
            }
            catch (RecordStoreException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    private void dbHandleError(String in)
    {
        System.err.println("MSG_ERROR from persistence unit: "+in);
    }

    public boolean isNull()
    {
        if (RecordStore.listRecordStores() == null)
            return true;
        return false;
    }
}
