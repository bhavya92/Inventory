package com.example.root.inventoryproject;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.root.inventoryproject.data.ProductsContract;

import java.text.MessageFormat;

public class ProductCursorAdapter extends CursorAdapter {

    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        TextView nameTextView = view.findViewById(R.id.name);
        TextView priceTextView = view.findViewById(R.id.price);
        TextView quantityTextView = view.findViewById(R.id.quantity);
        ImageView productImageView = view.findViewById(R.id.product_image_thumbnail);

        String name = cursor.getString(cursor.getColumnIndex(ProductsContract.ProductEntry.COLUMN_PRODUCT_NAME));
        float price = cursor.getFloat(cursor.getColumnIndex(ProductsContract.ProductEntry.COLUMN_PRODUCT_PRICE));
        final int quantity = cursor.getInt(cursor.getColumnIndex(ProductsContract.ProductEntry.COLUMN_PRODUCT_QUANTITY));
        int thumbnail = cursor.getColumnIndex(ProductsContract.ProductEntry.COLUMN_PRODUCT_PICTURE);
        Uri thumbUri = Uri.parse(cursor.getString(thumbnail));

        int id = cursor.getInt(cursor.getColumnIndex(ProductsContract.ProductEntry._ID));
        final Uri uri = ContentUris.withAppendedId(ProductsContract.ProductEntry.CONTENT_URI, id);

        nameTextView.setText(name);
        priceTextView.setText(MessageFormat.format("Price: ${0}", price));
        quantityTextView.setText(MessageFormat.format("Quantity: {0}", quantity));

        Glide.with(context).load(thumbUri)
                .placeholder(R.mipmap.ic_launcher)
                .crossFade()
                .centerCrop()
                .into(productImageView);

        Button sale = view.findViewById(R.id.sale);
        sale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (quantity > 0) {
                    int q = quantity;
                    q--;
                    ContentValues values = new ContentValues();
                    values.put(ProductsContract.ProductEntry.COLUMN_PRODUCT_QUANTITY, q);
                    int row = v.getContext().getContentResolver().update(uri, values, null, null);
                    context.getContentResolver().notifyChange(uri, null);
                } else {
                    Toast.makeText(context, "Not in stock", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }


}
