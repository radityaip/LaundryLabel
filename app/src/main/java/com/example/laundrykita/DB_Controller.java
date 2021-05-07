package com.example.laundrykita;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by WawanBeneran on 6/12/2017.
 * NIM.12131294
 */

public class DB_Controller extends SQLiteOpenHelper {
    public DB_Controller(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, "TEST.DB", factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE MENU(ID INTEGER PRIMARY KEY AUTOINCREMENT, NAMA TEXT UNIQUE);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS MENU;");
        onCreate(sqLiteDatabase);
    }

    public void insert_menu(String firstname){
        ContentValues contentValues = new ContentValues();
        contentValues.put("NAMA", firstname);
        this.getWritableDatabase().insertOrThrow("MENU","",contentValues);
    }

    public void delete_menu(){
        this.getWritableDatabase().delete("MENU",null,null);
    }

    public void update_student(String old_firstname, String new_firstname){
        this.getWritableDatabase().execSQL("UPDATE MENU SET NAMA='"+new_firstname+"' WHERE NAMA='"+old_firstname+"'");
    }

    public String list_all_menu(){
        Cursor cursor = this.getReadableDatabase().rawQuery("SELECT * FROM MENU", null);
        StringBuilder inputan = new StringBuilder();
        inputan.setLength(0);
        while (cursor.moveToNext()){
            inputan.append(cursor.getString(1)+"\n");
        }
        String singleMenu = inputan.toString();
        return(singleMenu);

    }

}
