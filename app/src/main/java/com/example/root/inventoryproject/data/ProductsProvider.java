package com.example.root.inventoryproject.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.root.inventoryproject.R;

public class ProductsProvider extends ContentProvider {

    private static final int PRODUCTS = 0;
    private static final int PRODUCTS_ID = 1;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(ProductsContract.CONTENT_AUTHORITY, ProductsContract.PATH, PRODUCTS);
        sUriMatcher.addURI(ProductsContract.CONTENT_AUTHORITY, ProductsContract.PATH + "/#", PRODUCTS_ID);
    }

    private ProductsDbHelper productsDbHelper;

    @Override
    public boolean onCreate() {
        productsDbHelper = new ProductsDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        SQLiteDatabase sqLiteDatabase = productsDbHelper.getReadableDatabase();
        Cursor cursor;
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                cursor = sqLiteDatabase.query(ProductsContract.ProductEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;

            case PRODUCTS_ID:

                selection = ProductsContract.ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                cursor = sqLiteDatabase.query(ProductsContract.ProductEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);

                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return ProductsContract.ProductEntry.CONTENT_LIST_TYPE;
            case PRODUCTS_ID:
                return ProductsContract.ProductEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return insertProduct(uri, values);
            default:
                throw new IllegalArgumentException(R.string.bad_uri + uri.toString());
        }
    }

    private Uri insertProduct(Uri uri, ContentValues values) {

        String name = values.getAsString(ProductsContract.ProductEntry.COLUMN_PRODUCT_NAME);
        if (name == null)
            throw new IllegalArgumentException("Product Requires a name.");

        Integer price = values.getAsInteger(ProductsContract.ProductEntry.COLUMN_PRODUCT_PRICE);
        if (price == null || price < 0)
            throw new IllegalArgumentException("Price can't be null or negative");

        int quantity = values.getAsInteger(ProductsContract.ProductEntry.COLUMN_PRODUCT_QUANTITY);
        if (quantity < 0)
            throw new IllegalArgumentException("Quantity can't be negative");


        SQLiteDatabase sqLiteDatabase = productsDbHelper.getWritableDatabase();
        long rowId = sqLiteDatabase.insert(ProductsContract.ProductEntry.TABLE_NAME, null, values);

        if (rowId == -1) {
            Log.e("ProductsProvider:", "Failed to insert a new row");
            return null;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, rowId);
    }


    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        int match = sUriMatcher.match(uri);
        SQLiteDatabase sqLiteDatabase = productsDbHelper.getWritableDatabase();
        int rowDeleted;
        switch (match) {
            case PRODUCTS:
                rowDeleted = sqLiteDatabase.delete(ProductsContract.ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PRODUCTS_ID:
                selection = ProductsContract.ProductEntry._ID + "=?";
                selectionArgs = new String[]{"" + ContentUris.parseId(uri)};
                rowDeleted = sqLiteDatabase.delete(ProductsContract.ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Cannot delete for uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return updateProduct(uri, values, selection, selectionArgs);
            case PRODUCTS_ID:
                selection = ProductsContract.ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateProduct(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Can't update the data for uri: " + uri);
        }
    }

    private int updateProduct(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (values.containsKey(ProductsContract.ProductEntry.COLUMN_PRODUCT_NAME))
            if (values.getAsString(ProductsContract.ProductEntry.COLUMN_PRODUCT_NAME) == null)
                throw new IllegalArgumentException("Name cant be null");

        if (values.containsKey(ProductsContract.ProductEntry.COLUMN_PRODUCT_QUANTITY)) {
            int quantity = values.getAsInteger(ProductsContract.ProductEntry.COLUMN_PRODUCT_QUANTITY);
            if (quantity < 0)
                throw new IllegalArgumentException("Quantity can't be negative");
        }

        if (values.containsKey(ProductsContract.ProductEntry.COLUMN_PRODUCT_PRICE)) {
            Integer price = values.getAsInteger(ProductsContract.ProductEntry.COLUMN_PRODUCT_PRICE);
            if (price == null || price < 0)
                throw new IllegalArgumentException("Price cannot be null or negative");
        }

        if (values.size() == 0)
            return 0;

        SQLiteDatabase sqLiteDatabase = productsDbHelper.getWritableDatabase();
        int rowUpdated = sqLiteDatabase.update(ProductsContract.ProductEntry.TABLE_NAME, values, selection, selectionArgs);
        if (rowUpdated != 0)
            getContext().getContentResolver().notifyChange(uri, null);

        return rowUpdated;
    }
}
