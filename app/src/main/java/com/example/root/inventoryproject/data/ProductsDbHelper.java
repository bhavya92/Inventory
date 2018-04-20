package com.example.root.inventoryproject.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ProductsDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "inventory.db";
    private static final int DATABASE_VERSION = 1;

    public ProductsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String SQL_CREATE_PRODUCTS_TABLE = "CREATE TABLE " + ProductsContract.ProductEntry.TABLE_NAME + "( " +
                ProductsContract.ProductEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ProductsContract.ProductEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL, " +
                ProductsContract.ProductEntry.COLUMN_PRODUCT_PRICE + " REAL NOT NULL, " +
                ProductsContract.ProductEntry.COLUMN_PRODUCT_QUANTITY + " INTEGER DEFAULT 0, " +
                ProductsContract.ProductEntry.COLUMN_PRODUCT_SUPPLIER_NAME + " TEXT, " +
                ProductsContract.ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL + " TEXT, " +
                ProductsContract.ProductEntry.COLUMN_PRODUCT_SUPPLIER_NUMBER + " TEXT," +
                ProductsContract.ProductEntry.COLUMN_PRODUCT_PICTURE + " TEXT NOT NULL DEFAULT 'No Image'" + ");";

        db.execSQL(SQL_CREATE_PRODUCTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (db != null)
            db.execSQL("DROP TABLE " + ProductsContract.ProductEntry.TABLE_NAME + ";");
        onCreate(db);
    }
}
