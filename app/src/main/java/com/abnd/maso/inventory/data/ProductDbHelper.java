package com.abnd.maso.inventory.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.abnd.maso.inventory.data.ProductContract.InventoryEntry;

/**
 * Created by rehabfahad on 6/3/17.
 */
public class ProductDbHelper extends SQLiteOpenHelper {

    public static final String TAG = ProductDbHelper.class.getSimpleName();
    private static final String DATABASE_NAME = "inventory.db";
    private static final int DATABASE_VERSION = 1;


    public ProductDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_INVENTORY = "CREATE TABLE " + InventoryEntry.TABLE_NAME + " ("
                + InventoryEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + InventoryEntry.COL_NAME + " TEXT NOT NULL, "
                + InventoryEntry.COL_QUANTITY + " INTEGER NOT NULL DEFAULT 0, "
                + InventoryEntry.COL_PRICE + " REAL NOT NULL DEFAULT 0.0, "
                + InventoryEntry.COL_PICTURE + " TEXT NOT NULL DEFAULT 'No images', "
                + InventoryEntry.COL_DESCRIPTION + " TEXT NOT NULL DEFAULT 'New Product ', "
                + InventoryEntry.COL_ITEMS_SOLD + " INTEGER NOT NULL DEFAULT 0 "
                + ");";

        db.execSQL(SQL_CREATE_INVENTORY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + InventoryEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
