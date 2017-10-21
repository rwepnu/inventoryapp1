package com.abnd.maso.inventory;

import android.Manifest;
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
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.abnd.maso.inventory.data.ProductContract.InventoryEntry;
import com.bumptech.glide.Glide;

import java.io.File;

import static com.abnd.maso.inventory.R.drawable.ic_broken_image;
import static com.abnd.maso.inventory.R.id.quantity;

/**
 * Created by rehabfahad on 6/3/17.
 */

public class ProductEditor extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = ProductEditor.class.getSimpleName();
    public static final int PICK_PHOTO_REQUEST = 20;
    public static final int EXTERNAL_STORAGE_REQUEST_PERMISSION_CODE = 21;
    //dentifier for the inventory data loader
    private static final int EXISTING_INVENTORY_LOADER = 0;
    //General Product QUERY PROJECTION
    public final String[] PRODUCT_COLS = {
            InventoryEntry._ID,
            InventoryEntry.COL_NAME,
            InventoryEntry.COL_QUANTITY,
            InventoryEntry.COL_PRICE,
            InventoryEntry.COL_DESCRIPTION,
            InventoryEntry.COL_ITEMS_SOLD,
            InventoryEntry.COL_PICTURE
    };

    private Uri mCurrentProductUri; //current product _ID in edit mode null in insert mode

    //Product UI elements
    private ImageView mProductPhoto;
    private EditText mProductName, mProductDescription, mProductInventory, mItemSold, mProductPrice;
    EditText quantityEdit;
    //Product Action Elements
    private ImageView actDelete, actUpdate, actOrderMore;
    private String mCurrentPhotoUri = "no images";
    private String mSudoEmail;
    private String mSudoProduct;
    private int mSudoQuantity = 50;
    Button decreaseQuantity;
    Button increaseQuantity;
    private boolean mProductHasChanged = false;

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        //Cast UI
        mProductPhoto = (ImageView) findViewById(R.id.image_product_photo);
        mProductName = (EditText) findViewById(R.id.inventory_item_name_edittext);
        mProductDescription = (EditText) findViewById(R.id.inventory_item_description_edittext);
        mProductInventory = (EditText) findViewById(quantity);
        decreaseQuantity = (Button) findViewById(R.id.decrease_quantity);
        increaseQuantity = (Button) findViewById(R.id.increase_quantity);
        mItemSold = (EditText) findViewById(R.id.current_sales_edittext);
        mProductPrice = (EditText) findViewById(R.id.inventory_item_price_edittext);


        //monitor activity so we can protect user
        mProductPhoto.setOnTouchListener(mTouchListener);
        mProductName.setOnTouchListener(mTouchListener);
        mProductDescription.setOnTouchListener(mTouchListener);
        mProductInventory.setOnTouchListener(mTouchListener);
        mItemSold.setOnTouchListener(mTouchListener);
        mProductPrice.setOnTouchListener(mTouchListener);

        //Cast ActionButtons
        actDelete = (ImageView) findViewById(R.id.delete_product_button);
        actUpdate = (ImageView) findViewById(R.id.save_product_button);
        actOrderMore = (ImageView) findViewById(R.id.order_supplier_button);

        //Make the photo click listener to update itself
        mProductPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPhotoProductUpdate(view);
            }
        });

        actUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveProduct();
            }
        });

        actDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDeleteConfirmationDialog();
            }
        });
        actOrderMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSubmitMore();
            }
        });


        //Check where we came from
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        if (mCurrentProductUri == null) {
            //User click new product
            setTitle(getString(R.string.add_product_title));
            actDelete.setVisibility(View.GONE);
            actOrderMore.setVisibility(View.GONE);

        } else {
            //User want to update a specific product
            setTitle(getString(R.string.edit_product_title));
            actDelete.setVisibility(View.VISIBLE);
            actOrderMore.setVisibility(View.VISIBLE);
            getLoaderManager().initLoader(EXISTING_INVENTORY_LOADER, null, this);
        }
        decreaseQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                subtractOneToQuantity();
                mProductHasChanged = true;
            }
        });

        increaseQuantity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sumOneToQuantity();
                mProductHasChanged = true;
            }
        });
    }

    private void subtractOneToQuantity() {
        String previousValueString = quantityEdit.getText().toString();
        int previousValue;
        if (previousValueString.isEmpty()) {
            return;
        } else if (previousValueString.equals("0")) {
            return;
        } else {
            previousValue = Integer.parseInt(previousValueString);
            quantityEdit.setText(String.valueOf(previousValue - 1));
        }
    }

    private void sumOneToQuantity() {
        String previousValueString = quantityEdit.getText().toString();
        int previousValue;
        if (previousValueString.isEmpty()) {
            previousValue = 0;
        } else {
            previousValue = Integer.parseInt(previousValueString);
        }
        quantityEdit.setText(String.valueOf(previousValue + 1));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                //if user didnt make changes
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(ProductEditor.this);
                    return true;
                }
                //User has made som change
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(ProductEditor.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void onPhotoProductUpdate(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //We are on M or above so we need to ask for runtime permissions
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                invokeGetPhoto();
            } else {
                // we are here if we do not all ready have permissions
                String[] permisionRequest = {Manifest.permission.READ_EXTERNAL_STORAGE};
                requestPermissions(permisionRequest, EXTERNAL_STORAGE_REQUEST_PERMISSION_CODE);
            }
        } else {
            //We are on an older devices so we dont have to ask for runtime permissions
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
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String pictureDirectoryPath = pictureDirectory.getPath();
        Uri data = Uri.parse(pictureDirectoryPath);
        photoPickerIntent.setDataAndType(data, "image/*");
        startActivityForResult(photoPickerIntent, PICK_PHOTO_REQUEST);
    }

    @Override
    public void onBackPressed() {
        //Go back if we have no changes
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }
        //otherwise Protect user from loosing info
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };
        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_PHOTO_REQUEST && resultCode == RESULT_OK) {
            if (data != null) {
                //If we are here, everything processed successfully and we have an Uri data
                Uri mProductPhotoUri = data.getData();
                mCurrentPhotoUri = mProductPhotoUri.toString();
                Log.d(TAG, "Selected images " + mProductPhotoUri);

                //We use Glide to import photo images
                Glide.with(this).load(mProductPhotoUri).placeholder(ic_broken_image).crossFade()
                        .fitCenter().into(mProductPhoto);
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, mCurrentProductUri, PRODUCT_COLS, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {

            int i_ID = 0;
            int i_COL_NAME = 1;
            int i_COL_QUANTITY = 2;
            int i_COL_PRICE = 3;
            int i_COL_DESCRIPTION = 4;
            int i_COL_ITEMS_SOLD = 5;
            int i_COL_SUPPLIER = 6;
            int i_COL_PICTURE = 7;

            // Extract values from current cursor
            String name = cursor.getString(i_COL_NAME);
            int quantity = cursor.getInt(i_COL_QUANTITY);
            float price = cursor.getFloat(i_COL_PRICE);
            String description = cursor.getString(i_COL_DESCRIPTION);
            int itemSold = cursor.getInt(i_COL_ITEMS_SOLD);
            String supplier = cursor.getString(i_COL_SUPPLIER);
            String photo = cursor.getString(i_COL_PICTURE);
            mCurrentPhotoUri = cursor.getString(i_COL_PICTURE);

            mSudoEmail = "orders@" + supplier + ".com";
            mSudoProduct = name;

            //We updates fields to values on DB
            mProductName.setText(name);
            mProductPrice.setText(String.valueOf(price));
            mProductInventory.setText(String.valueOf(quantity));
            mProductDescription.setText(description);
            mItemSold.setText(String.valueOf(itemSold));
            //Update photo using Glide
            Glide.with(this).load(mCurrentPhotoUri).placeholder(R.mipmap.ic_launcher).error(ic_broken_image)
                    .crossFade().fitCenter().into(mProductPhoto);
        }
    }

    private void saveProduct() {
        //Read Values from text field
        String nameString = mProductName.getText().toString().trim();
        String descriptionString = mProductDescription.getText().toString().trim();
        String inventoryString = mProductInventory.getText().toString().toString();
        String salesString = mItemSold.getText().toString().trim();
        String priceString = mProductPrice.getText().toString().trim();

        //Check if is new or if an update
        if (TextUtils.isEmpty(nameString) || TextUtils.isEmpty(descriptionString)
                || TextUtils.isEmpty(inventoryString) || TextUtils.isEmpty(salesString)
                || TextUtils.isEmpty(priceString)) {

            Toast.makeText(this, R.string.err_missing_textfields, Toast.LENGTH_SHORT).show();
            // No change has been made so we can return
            return;
        }

        //We set values for insert update
        ContentValues values = new ContentValues();

        values.put(InventoryEntry.COL_NAME, nameString);
        values.put(InventoryEntry.COL_DESCRIPTION, descriptionString);
        values.put(InventoryEntry.COL_QUANTITY, inventoryString);
        values.put(InventoryEntry.COL_ITEMS_SOLD, salesString);
        values.put(InventoryEntry.COL_PRICE, priceString);
        values.put(InventoryEntry.COL_PICTURE, mCurrentPhotoUri);

        if (mCurrentProductUri == null) {

            Uri insertedRow = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

            if (insertedRow == null) {
                Toast.makeText(this, R.string.err_inserting_product, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.ok_updated, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, InventoryActivity.class);
                startActivity(intent);
            }
        } else {
            // We are Updating
            int rowUpdated = getContentResolver().update(mCurrentProductUri, values, null, null);

            if (rowUpdated == 0) {
                Toast.makeText(this, R.string.err_inserting_product, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.ok_updated, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, InventoryActivity.class);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mProductName.setText("");
        mProductPrice.setText("");
        mProductInventory.setText("");
        mProductDescription.setText("");
        mItemSold.setText("");
    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void onSubmitMore() {

        String[] TO = {mSudoEmail};
        Intent emailIntent = new Intent(Intent.ACTION_SEND);

        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Order " + mSudoProduct);
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Please ship " + mSudoProduct +
                " in quantities " + mSudoQuantity);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            ex.printStackTrace();
        }
    }


    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
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

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteProduct() {
        if (mCurrentProductUri != null) {
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);
            if (rowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.editor_delete_pet_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_delete_pet_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }
}

