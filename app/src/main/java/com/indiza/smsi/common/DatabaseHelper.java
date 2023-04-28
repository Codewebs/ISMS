package com.indiza.smsi.common;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "ISMSbase";
    public static final String SETTINGS_TABLE_NAME = "ISMSSettings";
    public static ArrayList DBsettings;
    public DatabaseHelper(Context context) {
        super(context,DATABASE_NAME,null,1);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "create table "+ SETTINGS_TABLE_NAME +"(id integer primary key, SIM_FOR_SEND text,NBRE_MESSAGE_PUB text,SEC_LATENCE_MSG text, " +
                        "NBRE_CHIFFRE_BY_NUM text,NUMERO_COURT text,SMSC_ADDRESS text )"
        );
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+SETTINGS_TABLE_NAME);
        onCreate(db);
    }
    public boolean insertSettings() {

        if(DBsettings.size()<1) {
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                ContentValues contentValues = new ContentValues();
                contentValues.put("SIM_FOR_SEND", Constants.SIM_FOR_SEND);
                contentValues.put("NBRE_MESSAGE_PUB", Constants.NBRE_MESSAGE_PUB);
                contentValues.put("SEC_LATENCE_MSG", Constants.SEC_LATENCE_MSG);
                contentValues.put("NBRE_CHIFFRE_BY_NUM", Constants.NBRE_CHIFFRE_BY_NUM);
                contentValues.put("NUMERO_COURT", Constants.NUMERO_COURT);
                contentValues.put("SMSC_ADDRESS", Constants.SMSC_ADDRESS.equals("")?"-":Constants.SMSC_ADDRESS);
                db.insert(SETTINGS_TABLE_NAME, null, contentValues);
                return true;
            } catch (Exception ex) { Log.e("TAG SettingInsert", ex.getMessage());
                return false;
            }
        }else{
            return updateSetting();
        }
    }
    public ArrayList getDbSettings(String table) {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<String> array_list = new ArrayList<String>();
        Cursor res = db.rawQuery("select (id ||':'||trim(SIM_FOR_SEND) || ':' || NBRE_MESSAGE_PUB || ':'|| SEC_LATENCE_MSG || ':'|| NBRE_CHIFFRE_BY_NUM || ':'|| NUMERO_COURT || ':'|| SMSC_ADDRESS) as settings from " + table, null);
        res.moveToFirst();
        while (res.isAfterLast() == false) {
            if ((res != null) && (res.getCount() > 0))
                array_list.add(res.getString(res.getColumnIndexOrThrow("settings")));
            String[] valeur = res.getString(res.getColumnIndexOrThrow("settings")).split(":");
            Constants.SIM_FOR_SEND = Integer.parseInt(valeur[1].trim());
            Constants.NBRE_MESSAGE_PUB = valeur[2].trim();
            Constants.SEC_LATENCE_MSG = valeur[3].trim();
            Constants.NBRE_CHIFFRE_BY_NUM = Integer.parseInt(valeur[4].trim());
            Constants.NUMERO_COURT = valeur[5].trim();
            Constants.SMSC_ADDRESS = valeur[6].trim().equals("-")?"":valeur[6];

            res.moveToNext();
        }
        return array_list;
    }
    public boolean updateSetting() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + SETTINGS_TABLE_NAME + " SET SIM_FOR_SEND = " + "'" + Constants.SIM_FOR_SEND + "'," + "NBRE_MESSAGE_PUB = " + "'" + Constants.NBRE_MESSAGE_PUB + "' , " + "SEC_LATENCE_MSG = " + "'" + Constants.SEC_LATENCE_MSG + "' , " + "NBRE_CHIFFRE_BY_NUM = " + "'" + Constants.NBRE_CHIFFRE_BY_NUM + "', " + "NUMERO_COURT = " + "'" + Constants.NUMERO_COURT + "' , " + "SMSC_ADDRESS = " + "'" + (Constants.SMSC_ADDRESS.equals("")?"":Constants.SMSC_ADDRESS) + "' ");
        return true;
    }
    public boolean delete(String table) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE from " + table);
        return true;
    }

}
