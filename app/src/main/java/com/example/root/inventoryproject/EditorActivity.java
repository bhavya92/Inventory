package com.example.root.inventoryproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.root.inventoryproject.data.ProductsContract;

import java.io.File;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int PICK_PHOTO_REQUEST = 20;
    public static final int EXTERNAL_STORAGE_REQUEST_PERMISSION_CODE = 21;
    private static final int EXISTING_PET_LOADER = 0;
    public final String[] PROJECTION = {
            ProductsContract.ProductEntry._ID,
            ProductsContract.ProductEntry.COLUMN_PRODUCT_NAME,
            ProductsContract.ProductEntry.COLUMN_PRODUCT_PRICE,
            ProductsContract.ProductEntry.COLUMN_PRODUCT_QUANTITY,
            ProductsContract.ProductEntry.COLUMN_PRODUCT_SUPPLIER_NAME,
            ProductsContract.ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL,
            ProductsContract.ProductEntry.COLUMN_PRODUCT_SUPPLIER_NUMBER,
            ProductsContract.ProductEntry.COLUMN_PRODUCT_PICTURE
    };
    private Uri currentProductUri;
    private EditText productNameEditText;
    private EditText priceEditText;
    private EditText supplierNameEditText;
    private TextView quantityTextView;
    private EditText supplierEmailEditText;
    private EditText supplierPhoneEditText;
    private int quantity;
    private ImageView productImageView;
    private boolean productChanged;
    private String currentPhotoUri = "no images";
    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            productChanged = true;
            return false;
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        productNameEditText = findViewById(R.id.product_name);
        priceEditText = findViewById(R.id.price);
        quantityTextView = findViewById(R.id.quantity);
        supplierNameEditText = findViewById(R.id.supplier_name);
        supplierEmailEditText = findViewById(R.id.supplier_email);
        supplierPhoneEditText = findViewById(R.id.supplier_phone_number);
        productImageView = findViewById(R.id.product_image_edit);

        productNameEditText.setOnTouchListener(touchListener);
        priceEditText.setOnTouchListener(touchListener);
        supplierNameEditText.setOnTouchListener(touchListener);
        supplierEmailEditText.setOnTouchListener(touchListener);
        supplierPhoneEditText.setOnTouchListener(touchListener);
        productImageView.setOnTouchListener(touchListener);

        productImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                productImageUpdate(v);
            }
        });

        Intent intent = getIntent();
        currentProductUri = intent.getData();

        if (currentProductUri == null) {
            setTitle("Add Product");
            invalidateOptionsMenu();
        } else {
            setTitle("Edit Product");
            getLoaderManager().initLoader(EXISTING_PET_LOADER, null, this);
        }
        Button decrease = findViewById(R.id.decrement);
        decrease.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (quantity > 0) {
                    quantity--;
                    quantityTextView.setText(String.format("%d", quantity));
                } else {
                    Toast.makeText(getApplicationContext(), "Quantity cannot be negative", Toast.LENGTH_SHORT).show();
                }
                productChanged = true;
            }
        });

        Button increase = findViewById(R.id.increment);
        increase.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                quantity++;
                quantityTextView.setText(String.format("%d", quantity));
                productChanged = true;
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home:
                if (!productChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    }
                };
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            case R.id.action_save:
                saveProduct();
                return true;
            case R.id.action_order:
                Intent intent;
                String email = supplierEmailEditText.getText().toString();
                String phone = supplierPhoneEditText.getText().toString();
                if (!TextUtils.isEmpty(email)) {
                    intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", email, null));
                    String name = productNameEditText.getText().toString();
                    String price = priceEditText.getText().toString();
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Product Order");
                    intent.putExtra(Intent.EXTRA_TEXT, name + "\n" + price);
                    startActivity(Intent.createChooser(intent, "Send email..."));
                } else if (!TextUtils.isEmpty(phone)) {
                    intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + phone));
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Cannot make order without email address/phone number", Toast.LENGTH_SHORT).show();
                }

        }
        return super.onOptionsItemSelected(item);
    }

    public void productImageUpdate(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                invokeGetPhoto();
            } else {

                String[] permissionRequest = {Manifest.permission.READ_EXTERNAL_STORAGE};
                requestPermissions(permissionRequest, EXTERNAL_STORAGE_REQUEST_PERMISSION_CODE);
            }
        } else {
            invokeGetPhoto();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == EXTERNAL_STORAGE_REQUEST_PERMISSION_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //We got a GO from the user
            invokeGetPhoto();
        } else {
            Toast.makeText(this, R.string.err_external_storage_permissions, Toast.LENGTH_LONG).show();
        }
    }

    private void invokeGetPhoto() {
        // invoke the image gallery using an implict intent.
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);

        // where do we want to find the data?
        File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String pictureDirectoryPath = pictureDirectory.getPath();
        // finally, get a URI representation
        Uri data = Uri.parse(pictureDirectoryPath);

        // set the data and type.  Get all image types.
        photoPickerIntent.setDataAndType(data, "image/*");

        // we will invoke this activity, and get something back from it.
        startActivityForResult(photoPickerIntent, PICK_PHOTO_REQUEST);
    }

    @Override
    public void onBackPressed() {
        if (!productChanged) {
            super.onBackPressed();
            return;
        }


        DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        };

        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void saveProduct() {
        String productName = productNameEditText.getText().toString().trim();
        String productPrice = priceEditText.getText().toString().trim();
        String productQuantity = quantityTextView.getText().toString();
        String supplierName = supplierNameEditText.getText().toString().trim();
        String supplierEmail = supplierEmailEditText.getText().toString().trim();
        String supplierPhone = supplierPhoneEditText.getText().toString().trim();

        if (currentProductUri == null &&
                TextUtils.isEmpty(productName) || TextUtils.isEmpty(productPrice) || TextUtils.isEmpty(productQuantity) ||
                currentPhotoUri.equals("no images") || TextUtils.isEmpty(supplierName) || TextUtils.isEmpty(supplierEmail)
                || TextUtils.isEmpty(supplierPhone) || productQuantity.equals("0")) {
            Toast.makeText(this, "Please enter the data", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(ProductsContract.ProductEntry.COLUMN_PRODUCT_NAME, productName);
        float price = 0;
        if (!TextUtils.isEmpty(productPrice)) {
            price = Float.parseFloat(productPrice.replace(',', '.'));
        }
        contentValues.put(ProductsContract.ProductEntry.COLUMN_PRODUCT_PRICE, price);
        contentValues.put(ProductsContract.ProductEntry.COLUMN_PRODUCT_QUANTITY, productQuantity);
        contentValues.put(ProductsContract.ProductEntry.COLUMN_PRODUCT_SUPPLIER_NAME, supplierName);
        contentValues.put(ProductsContract.ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL, supplierEmail);
        contentValues.put(ProductsContract.ProductEntry.COLUMN_PRODUCT_SUPPLIER_NUMBER, supplierPhone);
        contentValues.put(ProductsContract.ProductEntry.COLUMN_PRODUCT_PICTURE, currentPhotoUri);

        if (currentProductUri == null) {

            Uri newUri = getContentResolver().insert(ProductsContract.ProductEntry.CONTENT_URI, contentValues);

            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {

            int rowsAffected = getContentResolver().update(currentProductUri, contentValues, null, null);

            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_update_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        finish();
    }

    private void deleteProduct() {
        if (currentProductUri != null) {
            int rowsDeleted = getContentResolver().delete(currentProductUri, null, null);

            if (rowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_PHOTO_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                //If we are here, everything processed successfully and we have an Uri data
                Uri productPhotoUri = data.getData();
                currentPhotoUri = productPhotoUri.toString();

                Glide.with(this).load(productPhotoUri)
                        .crossFade()
                        .fitCenter()
                        .into(productImageView);
            }
        }
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton("Keep Editing", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });


        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, currentProductUri, PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data == null || data.getCount() < 1) {
            return;
        }

        if (data.moveToFirst()) {
            String name = data.getString(data.getColumnIndex(ProductsContract.ProductEntry.COLUMN_PRODUCT_NAME));
            float price = data.getFloat(data.getColumnIndex(ProductsContract.ProductEntry.COLUMN_PRODUCT_PRICE));
            quantity = data.getInt(data.getColumnIndex(ProductsContract.ProductEntry.COLUMN_PRODUCT_QUANTITY));
            String supplierName = data.getString(data.getColumnIndex(ProductsContract.ProductEntry.COLUMN_PRODUCT_SUPPLIER_NAME));
            String supplierEmail = data.getString(data.getColumnIndex(ProductsContract.ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL));
            String supplierPhone = data.getString(data.getColumnIndex(ProductsContract.ProductEntry.COLUMN_PRODUCT_SUPPLIER_NUMBER));
            currentPhotoUri = data.getString(data.getColumnIndex(ProductsContract.ProductEntry.COLUMN_PRODUCT_PICTURE));

            productNameEditText.setText(name);
            priceEditText.setText(String.format("%f", price));
            quantityTextView.setText(String.format("%d", quantity));
            supplierNameEditText.setText(supplierName);
            supplierEmailEditText.setText(supplierEmail);
            supplierPhoneEditText.setText(supplierPhone);
            Glide.with(this).load(currentPhotoUri)
                    .placeholder(R.mipmap.ic_launcher)
                    .crossFade()
                    .fitCenter()
                    .into(productImageView);

        }
    }

    private void showDeleteConfirmationDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        productNameEditText.setText("");
        priceEditText.setText("");
        quantityTextView.setText("");
        supplierNameEditText.setText("");
        supplierEmailEditText.setText("");
        supplierPhoneEditText.setText("");
    }
}
